# SmplrAlarm Cleanup & Refactor Notes

This document captures the follow-up cleanups and design decisions applied during the refactor from the legacy Room+SharedPreferences storage to the new `AlarmDefinition` + `RoomAlarmStore` architecture.

It is intended for maintainers of the library and advanced consumers (e.g., FocusModes) who want to understand:

- What was removed  ✅
- What has been intentionally left as a minimal/placeholder implementation
- Where behavior changed (especially around content/activation flows) ✅
- How logging is currently handled and where to plug in a custom logger (planned)

---

## 1. Removal of the legacy Room + SharedPreferences stack

### 1.1 What was removed ✅

The following legacy storage components have been **deleted** from the library module:

- **Repository & database:**
  - `repository/AlarmNotificationRepository.kt`
  - `repository/AlarmNotificationDatabase.kt`

- **Legacy Room schema for notifications:**
  - `repository/dao/` (DAOs for old entities)
  - `repository/entity/` (e.g. `AlarmNotificationEntity`, `NotificationEntity`, `NotificationChannelEntity`)
  - `repository/relations/`
  - `repository/migrations/`

- **Helpers that depended on those entities:**
  - `Extensions+Entities.kt`: `NotificationEntity.convertToNotificationItem()` now removed.
  - `Extensions+JsonConversions.kt`: `AlarmNotificationEntity.activeDaysAsWeekdaysList()` removed.
  - `ReceiverModels.kt`: `extract*Entity()` helpers removed.

The **only storage path** in the library is now:

- `AlarmDefinition` (core model)
- `AlarmDefinitionEntity` + `DaoAlarmDefinition` + `AlarmStoreDatabase`
- `RoomAlarmStore` (opinionated `AlarmStore` implementation using Room + kotlinx.serialization)

### 1.2 Migration of call sites ✅

All internal usages of the old repository have been migrated to the new abstractions:

- **`SmplrAlarmAPI`**
  - Now uses:
    - `AlarmStore` (default: `RoomAlarmStore`)
    - `AlarmScheduler` (default: `AlarmSchedulerImpl` wrapping `AlarmService`)
    - `AlarmDefinition` + `NotificationConfig` as the single source of truth.
  - `setAlarm`, `updateAlarm`, `removeAlarm`, and `renewMissingAlarms` all operate through `AlarmStore`/`AlarmScheduler`.

- **Receivers:**
  - `AlarmReceiver` now reads `AlarmDefinition` from `RoomAlarmStore` and uses `NotificationConfig` to build notifications.
  - `RebootReceiver` re-schedules all active `AlarmDefinition` entries via `AlarmSchedulerImpl`.
  - `TimeChangeReceiver` renews all active alarms via `AlarmSchedulerImpl.renew`.
  - `ActivateAppReceiver` no longer depends on the legacy repo (see below for behavior notes).

---

## 2. Known placeholders and TODOs

Several implementations are intentionally minimal and/or marked with TODOs.

### 2.1 AlarmSchedulerImpl: date/time semantics ✅

`AlarmSchedulerImpl` now delegates full time precision to `AlarmService` and keeps `nextTriggerTime` in sync:

```kotlin
override fun schedule(
    id: Int,
    hour: Int,
    minute: Int,
    second: Int,
    weekDays: List<WeekDays>,
) {
    alarmService.setAlarm(
        requestCode = id,
        hour = hour,
        min = minute,
        weekDays = weekDays,
        second = second,
    )

    updateNextTriggerTime(id, hour, minute, weekDays)
}

override fun rescheduleTomorrow(definition: AlarmDefinition) {
    // Currently we reschedule using the same time-of-day and weekdays,
    // relying on the shared Calendar helper to compute the next
    // occurrence and update nextTriggerTime in the store.
    schedule(
        id = definition.id,
        hour = definition.hour,
        minute = definition.minute,
        second = definition.second,
        weekDays = definition.weekdays,
    )
}

override fun renew(definition: AlarmDefinition) {
    // Same behavior as rescheduleTomorrow: re-schedule with current
    // data, letting the time-calculation helper and nextTriggerTime
    // update determine the actual next fire time.
    schedule(
        id = definition.id,
        hour = definition.hour,
        minute = definition.minute,
        second = definition.second,
        weekDays = definition.weekdays,
    )
}
```

**Current status:**

- `second` and `millis` are fully supported in the time calculation path (`Extensions+Calendar`, `AlarmService`, `AlarmSchedulerImpl`, `SmplrAlarmAPI`).
- `nextTriggerTime` is updated on every schedule/renew/reschedule using the shared helper.
- `rescheduleTomorrow` and `renew` both currently mean “compute the next occurrence with the same time-of-day and weekdays”. The naming is historical; semantics are unified.

### 2.2 Reboot cleanup using nextTriggerTime ✅ (conservative policy)

In `RebootReceiver` we now re-schedule **only** active alarms whose `nextTriggerTime` is either `null` (legacy/first run) or still in the future:

```kotlin
CoroutineScope(Dispatchers.IO).launch {
    val definitions = store.getAll()
    val now = System.currentTimeMillis()

    definitions
        .asSequence()
        .filter { it.isActive }
        .filter { def ->
            val next = def.nextTriggerTime
            next == null || next >= now
        }
        .forEach { definition ->
            scheduler.schedule(
                id = definition.id,
                hour = definition.hour,
                minute = definition.minute,
                second = definition.second,
                weekDays = definition.weekdays,
            )
        }
}
```

**Remaining work (optional):**

- Introduce explicit pruning helpers (e.g. in the store) so host apps can delete very old, inactive alarms based on `nextTriggerTime` and their own policies.

---

## 3. Content intents, activation receivers, and behavior changes

### 3.1 Previous behavior (legacy repo based)

Previously, the library:

- Stored `contentIntent` and related `Intent` instances in `SharedPreferences`/Room via `AlarmNotificationRepository`.
- Used `ActivateAppReceiver` + those persisted intents to open screens when the user tapped indicator notifications.
- Drove notification content and actions using a mix of:
  - `AlarmNotification` entities
  - Stored `Intent` blobs.

### 3.2 Current behavior (post-cleanup) ✅

After the migration:

- **API surface:**
  - `SmplrAlarmAPI` still exposes DSL setters for:
    - `contentIntent { Intent }`
    - `receiverIntent { Intent }`
    - `alarmReceivedIntent { Intent }`
  - These are used when creating the in-memory `AlarmNotification` at `setAlarm` time, and ultimately flow into `NotificationItem` / `NotificationChannelItem` where applicable.

-- **Notification creation:**
  - `Extensions+Notifications.showNotification` remains the single place that builds Android `Notification` objects and `PendingIntent`s.
  - It supports content intents, full-screen intents, dismiss intents, and action button intents.

-- **Alarm firing (`AlarmReceiver`):**
  - `AlarmReceiver` now:
    - Looks up `AlarmDefinition` from `RoomAlarmStore`.
    - Reads `definition.notificationConfig` and uses `channel` + `notification` to call `showNotification`.
    - Uses `notificationConfig.contentTarget` / `fullScreenTarget` / `alarmReceivedTarget` to reconstruct `Intent`s via `NotificationTargetDescriptor.toIntent(context)` and passes them as `contentIntent`, `fullScreenIntent`, and `alarmReceivedIntent` to `showNotification`.
    - Handles one-shot vs repeating alarms via `isActive` and `AlarmSchedulerImpl`.

-- **Activation receiver (`ActivateAppReceiver`):**

  ```kotlin
  private fun onAlarmIndicatorTapped(context: Context, requestId: Int) =
      CoroutineScope(Dispatchers.IO).launch {
          if (requestId == -1) return@launch

          val store = RoomAlarmStore(context)
          val definition = store.get(requestId) ?: return@launch
          val target = definition.notificationConfig?.contentTarget ?: return@launch

          val intent = target.toIntent(context).apply {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
          }

          context.startActivity(intent)
      }
  ```

  - This receiver is now wired to the `contentTarget` descriptor; it no longer uses stored Intents.

### 3.3 Impact and options

This refactor intentionally removed persistence of arbitrary `Intent` instances. As a consequence:

- Behavior that depended on **reconstructing arbitrary stored Intents at alarm fire time (or indicator taps)** is no longer supported.
- Notification actions defined via `NotificationItem` are still supported and fully functional, because they are built at alarm creation and encoded as `PendingIntent`s.

**Implemented improvement:**

- We adopted the **descriptor-based activation** model using a sealed `NotificationTargetDescriptor` (Screen/Service/Broadcast) stored inside `NotificationConfig` and serialized via Room. `AlarmReceiver` and `ActivateAppReceiver` now reconstruct `Intent`s at runtime from these descriptors and avoid persisting raw `Intent`s.

---

## 4. Logging and future logger abstraction

### 4.1 Logger abstraction (current state)

The library now uses a pluggable logger abstraction instead of Timber directly:

```kotlin
interface SmplrAlarmLogger {
    fun v(message: String, throwable: Throwable? = null)
    fun d(message: String, throwable: Throwable? = null)
    fun i(message: String, throwable: Throwable? = null)
    fun w(message: String, throwable: Throwable? = null)
    fun e(message: String, throwable: Throwable? = null)
}

object DefaultSmplrAlarmLogger : SmplrAlarmLogger { /* Timber-backed */ }

object SmplrAlarmLoggerHolder {
    @Volatile
    var logger: SmplrAlarmLogger = DefaultSmplrAlarmLogger
}
```

- `SmplrAlarmAPI` accepts a `SmplrAlarmLogger` (defaulting to `DefaultSmplrAlarmLogger`) and registers it with `SmplrAlarmLoggerHolder`.
- Receivers (`AlarmReceiver`, `RebootReceiver`, `TimeChangeReceiver`), `AlarmService`, and key extensions (`Extensions+Calendar`, `Extensions+Notifications`) log via `SmplrAlarmLoggerHolder.logger`.
- All internal Timber usages have been removed from the library module; Timber is now only used inside `DefaultSmplrAlarmLogger`.

### 4.2 Benefits

- Host apps can:
  - Plug in their own logging framework (e.g. Kermit, Napier, custom analytics).
  - Capture structured error/diagnostic events for:
    - Alarm creation/update/removal.
    - Alarm firing and scheduling.
    - Boot/time-change recovery.
    - Notification creation errors.

- The library can continue to default to Timber in its own sample app, while staying agnostic for library consumers.

---

## 5. Summary (current state)

-- The legacy `AlarmNotificationRepository` + `AlarmNotificationDatabase` stack has been fully removed; `RoomAlarmStore` is the single storage implementation shipped by the library.
-- All call sites have been migrated to `AlarmStore` + `AlarmSchedulerImpl` + `AlarmDefinition` / `NotificationConfig`.
-- `second` and `millis` are part of `AlarmDefinition` and `AlarmDefinitionEntity`, and are used by the time-calculation helpers and `AlarmService`.
-- `nextTriggerTime` is now computed and persisted on every schedule/renew/reschedule. Reboot and time-change flows use this to drive conservative rescheduling.
-- Behavior depending on serialized arbitrary `Intent`s has been removed. Notifications and actions are driven by `NotificationItem` plus `NotificationTargetDescriptor` descriptors in `NotificationConfig`.
-- Logging is now routed through a pluggable `SmplrAlarmLogger` abstraction with a default Timber-backed implementation.

- This document should be updated as we:

- - Refine semantics for `rescheduleTomorrow`/`renew` if needed.
- - Add tests and (optional) pruning helpers that use `nextTriggerTime`.
