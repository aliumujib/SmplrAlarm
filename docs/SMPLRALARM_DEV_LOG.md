# SmplrAlarm Refactor Dev Log

This document is a chronological-ish dev log of the major refactors applied to SmplrAlarm while moving from the legacy Room + SharedPreferences stack to an `AlarmDefinition` + `RoomAlarmStore` architecture with descriptor-based notifications.

It is written for maintainers and advanced consumers who want to understand *what actually changed*, not just the final APIs.

---

## 1. Core architecture changes

- **Introduced `AlarmDefinition` as the single source of truth for alarms.**
  - Holds: `id`, `hour`, `minute`, `second`, `millis`, `weekdays: List<WeekDays>`, `isActive`, `metadata: Map<String, String>`, and `nextTriggerTime`.
  - Replaces the previous `AlarmNotification` + assorted Room entities as the primary persisted model.

- **Added `AlarmDefinitionEntity` + `AlarmStoreDatabase` + `DaoAlarmDefinition`.**
  - Minimal Room schema focused only on alarm definition state.
  - Backed by a dedicated `alarm_store.db` via `AlarmStoreDatabase.getInstance(context)`.

- **Introduced `AlarmStore` and `RoomAlarmStore`.**
  - `AlarmStore` defines `insert`, `update`, `delete`, `get`, `getAll` (and helpers used by receivers).
  - `RoomAlarmStore` is the shipped opinionated implementation, using Room + kotlinx.serialization for:
    - `NotificationConfig` / `NotificationItem`.
    - `NotificationTargetDescriptor` activation targets.
    - `metadata` as a `Map<String, String>`.
  - All previous direct usages of `AlarmNotificationRepository` are removed in favor of `AlarmStore`.

- **Introduced `AlarmScheduler` + `AlarmSchedulerImpl`.**
  - `AlarmScheduler` abstracts scheduling, cancellation, and renewal.
  - `AlarmSchedulerImpl` wraps `AlarmService` and is responsible for:
    - Calling `AlarmService.setAlarm` / `cancelAlarm`.
    - Updating `nextTriggerTime` in `AlarmStore` on schedule/renew/reschedule.

- **Added `AlarmIdGenerator`.**
  - `AlarmIdGenerator` abstraction plus `DefaultAlarmIdGenerator` (time-based) are used by `SmplrAlarmAPI` when the user does not supply a `requestCode`.

---

## 2. Top-level API and DSL changes

- **Top-level entry points are now `suspend`.**
  - `smplrAlarmSet`, `smplrAlarmUpdate`, `smplrAlarmCancel`, `smplrAlarmRenewMissingAlarms` and `smplrAlarmUpdate` all became `suspend` functions.
  - These functions now simply construct `SmplrAlarmAPI` and call suspend methods, instead of starting their own background `CoroutineScope(Dispatchers.IO)`.

- **`SmplrAlarmAPI` is fully backed by abstractions.**
  - Constructor now accepts:

    ```kotlin
    class SmplrAlarmAPI(
        context: Context,
        store: AlarmStore = RoomAlarmStore(context),
        idGenerator: AlarmIdGenerator = DefaultAlarmIdGenerator,
        logger: SmplrAlarmLogger = DefaultSmplrAlarmLogger,
        scheduler: AlarmScheduler = AlarmSchedulerImpl(AlarmService(context), store),
    )
    ```

  - Internally stores these and uses them for all operations.

- **`SmplrAlarmAPI` behavior refactor.**
  - `setAlarm()` → `suspend fun setAlarm(): Int`
    - Validates time; throws `IllegalArgumentException` for invalid hour/minute.
    - Ensures that when a `notification {}` is provided, a matching `notificationChannel {}` is also configured; otherwise throws `IllegalStateException`.
    - Chooses the final `requestCode` (user-supplied or via `AlarmIdGenerator`).
    - Builds an `AlarmDefinition` containing:
      - Time fields (`hour`, `minute`, `second`, `millis`).
      - `weekdays`.
      - `isActive`.
      - `metadata`.
      - `NotificationConfig` for channel + notifications + activation targets.
    - Inserts definition via `AlarmStore` and schedules via `AlarmScheduler.schedule`.
    - Also calls through to `AlarmService.setAlarm` (legacy behavior preserved for now).

  - `updateAlarm()` → `suspend fun updateAlarm()`
    - Requires a valid `requestCode`; throws if missing.
    - Validates time overrides if supplied.
    - Enforces `notificationChannel {}` when updating notifications.
    - Cancels the existing OS alarm via `AlarmService.cancelAlarm(requestCode)`.
    - Loads the current `AlarmDefinition` from `AlarmStore` and applies the DSL overrides:
      - Updated hour/minute when provided.
      - Updated weekdays when provided, or keeps existing.
      - Updated `isActive` if supplied.
      - Updated metadata.
    - Writes the updated definition back to `AlarmStore`.
    - Re-schedules or cancels via `AlarmScheduler` based on `isActive`.

  - `renewMissingAlarms()` → `suspend fun renewMissingAlarms()`
    - Reads all definitions from `AlarmStore`.
    - For each active alarm that does not have a corresponding `AlarmService` pending intent, calls `AlarmScheduler.renew`.

  - `removeAlarm()` → `suspend fun removeAlarm()`
    - Requires a valid `requestCode`.
    - Cancels via `AlarmService` and `AlarmScheduler`.
    - Deletes the alarm from `AlarmStore`.

- **Removed JSON change/listener API.**
  - `SmplrAlarmListRequestAPI` and `smplrAlarmChangeOrRequestListener` have been removed from the public API surface.
  - The new architecture expects host apps to observe alarms via their own store or the `AlarmStore` abstraction instead of a JSON stream.

---

## 3. Notification & activation model

- **Made notification models serializable and intent-free.**
  - `NotificationChannelItem` and `NotificationItem` are now `@Serializable`.
  - `NotificationItem` has no `Intent` fields; instead it models visuals + targets:

    ```kotlin
    @Serializable
    data class NotificationItem(
        val smallIcon: Int? = null,
        val title: String? = null,
        val message: String? = null,
        val bigText: String? = null,
        val autoCancel: Boolean? = null,
        val firstButtonText: String? = null,
        val secondButtonText: String? = null,
        val firstButtonTarget: NotificationTargetDescriptor? = null,
        val secondButtonTarget: NotificationTargetDescriptor? = null,
        val dismissTarget: NotificationTargetDescriptor? = null,
    )
    ```

- **Introduced `NotificationTargetDescriptor`.**
  - A sealed, serializable hierarchy describing activation targets without storing raw `Intent`s:
    - `ScreenTarget` (activity).
    - `ServiceTarget`.
    - `BroadcastTarget`.
  - Each carries `packageName`, class name, optional `action`, and a `Map<String, String>` of extras.

- **Added conversion helpers between Intents and descriptors.**
  - In `ReceiverModels.kt`:
    - `NotificationTargetDescriptor.toIntent(context)` builds the actual `Intent` at runtime.
    - `screenTargetFromIntent(intent)`, `serviceTargetFromIntent(intent)`, `broadcastTargetFromIntent(intent)` capture an existing `Intent` into a descriptor.
  - These helpers are used by the sample app and are available to host apps that still construct Intent-based flows at the edge.

- **Refactored `AlarmNotificationAPI` and notification building.**
  - `AlarmNotificationAPI` now configures `NotificationItem` purely with text/icons plus descriptor-based button/dismiss targets.
  - `Extensions+Notifications.showNotification` now:
    - Turns each `NotificationTargetDescriptor` into an `Intent` and `PendingIntent` at runtime.
    - Uses these when wiring content, full-screen, action buttons, and dismiss handlers into the Android `Notification`.

- **`NotificationConfig` simplified but kept.**
  - Still acts as a container for:
    - `NotificationChannelItem` (channel metadata).
    - `NotificationItem` (visuals + button/dismiss targets).
    - Top-level activation targets (content/full-screen/alarm-received descriptors).
  - It is fully serializable and is stored inside `AlarmDefinition` via `RoomAlarmStore`.

---

## 4. Receivers & services

- **`AlarmReceiver` now works through `AlarmStore` + descriptors.**
  - On receive:
    - Reads the request ID from extras.
    - Fetches the `AlarmDefinition` from `RoomAlarmStore`.
    - Extracts `NotificationConfig` and uses channel + notification + descriptors to build:
      - Content `Intent`.
      - Full-screen `Intent` (if configured).
      - Alarm-received `Intent`.
    - Delegates to `showNotification` with these three intents.
    - Handles one-shot vs repeating alarms via `isActive` and `AlarmSchedulerImpl`.
  - Throws explicit `IllegalArgumentException`s when the request ID is invalid or the definition is missing, and logs via `SmplrAlarmLogger`.

- **`ActivateAppReceiver` uses descriptors.**
  - On indicator tap:
    - Fetches the `AlarmDefinition` from `RoomAlarmStore`.
    - Reads `contentTarget` from `NotificationConfig`.
    - Converts it to an `Intent` with `toIntent(context)` and launches it with `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP`.

- **`RebootReceiver` and `TimeChangeReceiver` use `AlarmStore` + `AlarmSchedulerImpl`.**
  - Both iterate over active `AlarmDefinition`s and reschedule using `AlarmSchedulerImpl`.
  - `RebootReceiver` uses `nextTriggerTime` to avoid reviving alarms that are far in the past.

- **`AlarmService` tightened.**
  - Now throws when alarm scheduling permissions are missing instead of silently logging.
  - Remains responsible for exact-time scheduling via `AlarmManager.setAlarmClock`.
  - Uses helpers that support hours, minutes, seconds, and millis.
  - Made `open` to allow tests to subclass and assert calls.

---

## 5. Logging and dispatchers

- **Added `SmplrAlarmLogger` abstraction.**
  - Logging interface:

    ```kotlin
    interface SmplrAlarmLogger {
        fun v(message: String, throwable: Throwable? = null)
        fun d(message: String, throwable: Throwable? = null)
        fun i(message: String, throwable: Throwable? = null)
        fun w(message: String, throwable: Throwable? = null)
        fun e(message: String, throwable: Throwable? = null)
    }
    ```

  - `DefaultSmplrAlarmLogger` is Timber-backed.
  - `SmplrAlarmLoggerHolder.logger` is the shared global logger used by receivers, services, and extensions.
  - `SmplrAlarmAPI` accepts a logger and registers it in the holder.

- **Centralized coroutine dispatching.**
  - Introduced `SmplrAlarmDispatchers` + `SmplrAlarmDispatchersHolder` to control coroutine dispatchers.
  - Added `launchIo { ... }` helper (now public) to run small background tasks on the shared IO dispatcher.
  - Receivers (`AlarmReceiver`, `RebootReceiver`, `TimeChangeReceiver`) and some services use this helper instead of ad-hoc `CoroutineScope(Dispatchers.IO)`.

---

## 6. Sample app changes

- **Compose theming and dark mode.**
  - Added `SmplrAlarmTheme` based on Material 3 with light/dark color schemes.
  - Wrapped `AlarmScreen` in `SmplrAlarmTheme`.
  - Fixed hard-coded colors that broke dark mode.

- **Alarm precision UI.**
  - `AlarmScreen` now lets the user input seconds and milliseconds.
  - These values are threaded through `AlarmViewModel` into `SmplrAlarmAPI`.

- **ViewModel and state management.**
  - `AlarmViewModel` now exposes a `scheduleState: StateFlow<AlarmScheduleState>` to model:
    - Idle / Loading / Success(requestCode) / Error(throwable).
  - `AlarmScreen` observes `scheduleState` with `collectAsState()` and updates `alarmId` + toasts on success or failure.
  - All calls to `smplrAlarmSet` / `smplrAlarmUpdate` / `smplrAlarmCancel` are launched in `viewModelScope` and awaited.

- **Edge integration with descriptors.**
  - `AlarmViewModel` uses:
    - `screenTargetFromIntent(...)` for activity-based targets.
    - `broadcastTargetFromIntent(...)` for broadcast-based actions.
  - These are passed into the DSL’s `contentTarget { ... }`, `alarmReceivedTarget { ... }`, and `alarmNotification` button/dismiss targets.

- **Notification update path.**
  - Added a `updateNotification(requestCode)` helper that calls `smplrAlarmUpdate` with a new `NotificationItem` and a `notificationChannel` config.
  - The sample UI now uses this instead of calling internal APIs directly from the Composable.

- **Removed legacy JSON list UI.**
  - The old JSON alarm list listener and display were removed from the sample app.

---

## 7. Testing

- **Unit tests.**
  - Added tests for time helpers to confirm correct handling of hour/minute/second/millis.
  - Added tests for `AlarmSchedulerImpl` wiring to ensure parameters are correctly passed to `AlarmService.setAlarm`.

- **Android instrumentation tests.**
  - `RoomAlarmStoreAndroidTest`: verifies round-tripping of `AlarmDefinition`, including seconds and millis.
  - `AlarmSchedulerImplAndroidTest`: exercises scheduling using a real `AlarmService` context.
  - `AlarmServiceInstrumentedTest`: checks basic scheduling behavior and permission handling.
  - `AlarmReceiverInstrumentedTest`: covers one-shot and repeating alarms and interaction with `RoomAlarmStore`.

---

## 8. Open items / future work

- Expose explicit BYO-mode top-level functions that accept custom `AlarmStore`/`AlarmScheduler` for apps like FocusModes.
- Introduce an optional metadata serializer abstraction (`MetadataSerializer<T>`) instead of always using raw `Map<String, String>`.
- Consider flattening or simplifying `NotificationConfig` if it becomes redundant now that `NotificationItem` carries button/dismiss targets and descriptors.
- Add explicit pruning helpers (in `AlarmStore`) to allow host apps to delete very old, inactive definitions based on `nextTriggerTime`.
- Expand tests around reboot/time-change flows and error handling to cover edge cases introduced by the new suspend-based APIs.
