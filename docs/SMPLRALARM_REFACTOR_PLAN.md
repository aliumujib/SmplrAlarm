# SmplrAlarm Refactor Plan: Storage & Scheduling

## 1. Current Library Structure

### 1.1 Top-Level API Entry Points

- **`SmplrAlarm.kt`**
  - `smplrAlarmSet(context, block)` → builds `SmplrAlarmAPI` and calls `setAlarm()`.
  - `smplrAlarmCancel(context, block)` → builds `SmplrAlarmAPI` and calls `removeAlarm()`.
  - `smplrAlarmUpdate(context, block)` → builds `SmplrAlarmAPI` and calls `updateAlarm()`.
  - `smplrAlarmRenewMissingAlarms(context)` → uses `SmplrAlarmAPI.renewMissingAlarms()`.
  - `smplrAlarmChangeOrRequestListener(context, listener)` → builds `SmplrAlarmListRequestAPI` and wires a JSON-based alarm-list listener.
  - `channel { ... }` / `alarmNotification { ... }` → DSL builders for notification/channel models.

**Role:** Public façade for consumers. Hard-wires the concrete implementation (`SmplrAlarmAPI` + its internal Room/SharedPreferences + `AlarmService`). There is no way to substitute storage or scheduling behavior.

---

### 1.2 Core API: `apis/`

#### 1.2.1 `SmplrAlarmAPI`

- **Fields:**
  - Alarm parameters: `hour`, `min`, `weekdays: List<WeekDays>`, `isActive`.
  - Identity & intents: `requestCode`, `contentIntent`, `receiverIntent`, `alarmReceivedIntent`.
  - Notification data: `notificationChannel: NotificationChannelItem?`, `notification: NotificationItem?`.
  - Observation: `requestAPI: SmplrAlarmListRequestAPI?`.
  - Metadata: `metadata: Map<String, String>?`.
  - Services: `alarmService: AlarmService`, `alarmNotificationRepository: AlarmNotificationRepository`.

- **DSL setters:** `hour {}`, `min {}`, `second {}`, `millis {}`, `requestCode {}`, `contentIntent {}`, `receiverIntent {}`, `alarmReceivedIntent {}`, `notificationChannel {}`, `notification {}`, `weekdays { WeekDaysAPI }`, `isActive {}`, `requestAPI {}`, `metadata {}`.

- **Core behavior:**
  - `setAlarm()`
    - Validates time (`isAlarmValid`).
    - Assigns `requestCode` (currentTimeMillis-based if not overridden).
    - Constructs `AlarmNotification` (domain object representing an alarm, including notification data and intents).
    - On `Dispatchers.IO`:
      - Persists alarm via `AlarmNotificationRepository.insertAlarmNotification(...)`.
      - Updates an in-memory list `alarmNotification` (from `SmplrAlarmReceiverObjects`).
      - Calls `requestAPI?.requestAlarmList()` to push JSON to listeners.
    - Schedules OS alarm via `AlarmService.setAlarm(requestCode, hour, min, second, millis, weekdays)`.

  - `renewMissingAlarms()`
    - Reads all alarms from `AlarmNotificationRepository`.
    - For each `isActive` alarm where `!alarmService.alarmExist(id)`, calls `alarmService.renewAlarm(alarmNotification)`.

  - `updateAlarm()`
    - Cancels existing OS alarm via `AlarmService.cancelAlarm(requestCode)`.
    - Loads existing `AlarmNotification` from `AlarmNotificationRepository.getAlarmNotification(requestCode)`.
    - Computes updated values: `hour`, `min`, `second`, `millis`, `weekdays`, `isActive`, `metadata`.
    - Persists via `updateAlarmNotification(...)` and optional `updateNotification(...)`.
    - If still active, schedules via `AlarmService.setAlarm(...)`.
    - Notifies JSON listeners via `requestAPI?.requestAlarmList()`.

  - `removeAlarm()`
    - Cancels OS alarm.
    - Deletes from `AlarmNotificationRepository`.
    - Notifies listeners.

**Role:** God-class DSL that orchestrates: building alarms, persisting them (Room + SharedPreferences), and talking to `AlarmService`. Also responsible for “alarm list change” signaling.

---

#### 1.2.2 `SmplrAlarmListRequestAPI`

- Holds `alarmListChangeOrRequestedListener: (String) -> Unit`.
- Can call `requestAlarmList()` → queries `AlarmNotificationRepository.getAllAlarmNotifications()`, maps to a JSON structure with `alarmItems` (requestId, hour, minute, second, millis, weekdays, isActive, metadata) and delivers it to the listener.

**Role:** Converts internal Room/SharedPreferences state into a JSON snapshot and change stream.

---

#### 1.2.3 `WeekDaysAPI`, `AlarmNotificationAPI`, `ChannelManagerAPI`

- **`WeekDaysAPI`**
  - DSL to build `List<WeekDays>` from method calls like `monday()`, `tuesday()` etc.

- **`AlarmNotificationAPI` / `ChannelManagerAPI`**
  - DSL wrappers that construct `NotificationItem` and `NotificationChannelItem` models.

**Role:** UI-friendly DSL for consumers configuring alarms and notifications.

---

### 1.3 Services: Scheduling Layer

#### 1.3.1 `AlarmService`

- Wraps `AlarmManager` and `PendingIntent` creation.
- Core methods:
  - `setAlarm(requestCode, hour, min, second, millis, weekDays, receiverIntent?)`
    - Checks `canScheduleExactAlarms()` on Android 12+.
    - Computes next trigger time (`Calendar.getTimeExactForAlarmInMilliseconds(hour, min, second, millis, weekDays)`).
    - Constructs `AlarmClockInfo` with an "open app" intent (via `ActivateAppReceiver`).
    - Schedules via `alarmManager.setAlarmClock(...)` with a broadcast `AlarmReceiver` as the actual alarm.
  - `alarmExist(requestCode)` uses `PendingIntent.getBroadcast(..., FLAG_NO_CREATE)`.
  - `cancelAlarm(requestCode)` cancels via AlarmManager.

**Role:** Pure scheduling executor (interaction with Android `AlarmManager`).

---

### 1.4 Repository & Storage: `repository/`

#### 1.4.1 `AlarmNotificationRepository`

- Depends on:
  - `AlarmNotificationDatabase` (Room DB instance).
  - `SharedPreferences` for storing serialized `Intent`s.

- Responsibilities:
  - `insertAlarmNotification(AlarmNotification)`:
    - Serializes `contentIntent`, `fullScreenIntent`, `alarmReceivedIntent`, button intents, and `notificationDismissedIntent` to SharedPreferences as URIs + JSON bundles.
    - Inserts 3 related entities into Room: `AlarmNotificationEntity`, `NotificationChannelEntity`, `NotificationEntity`.
  - `updateAlarmNotification(...)`:
    - Updates time/weekday/isActive/metadata in `AlarmNotificationEntity`.
  - `getAlarmNotification(id)`:
    - Loads composite row from Room, reconstructs `AlarmNotification` by:
      - Reading intents from SharedPreferences.
      - Converting `NotificationEntity` to `NotificationItem` (restoring button intents and dismiss intents).
  - `getAllAlarmNotifications()`:
    - Same as above but for all rows.
  - `deleteAlarmNotification(id)`, `deactivateSingleAlarmNotification(id)`, `deleteAlarmsBeforeNow()`, `updateNotification(id, NotificationItem)`.
  - Internal helpers:
    - `saveIntent(...)` and `retrieveIntent(...)` round-trip intents through URIs and JSON extras.

**Role:** Central concrete storage implementation (Room + SharedPreferences) and gateway via which all alarms/notifications are persisted and reconstructed.

---

#### 1.4.2 Room Layer

- `AlarmNotificationDatabase`
  - Room database instance exposing DAOs:
    - `DaoAlarmNotification` → CRUD over `AlarmNotificationEntity`.
    - `DaoNotificationChannel` → CRUD over `NotificationChannelEntity`.
    - `DaoNotification` → CRUD over `NotificationEntity`.

- `entity/` classes
  - `AlarmNotificationEntity` – primary key `alarmNotificationId`, `hour`, `min`, `second`, `millis`, `activeDaysJson`, `isActive`, `metadata`.
  - `NotificationEntity` – notification visual data (icons, text, etc.).
  - `NotificationChannelEntity` – channel metadata.

- `relations/` combine entities into a composite `AlarmNotification` row.

**Role:** Concrete persistence schema for alarms + notification UI. Not abstracted behind interfaces.

---

### 1.5 Models: `models/`

- `AlarmItem` / `ActiveAlarmList`, `ActiveWeekDays`, `WeekDays`
  - Scheduling and “which days” representations.
- `NotificationItem`, `NotificationChannelItem`
  - Data classes for building notifications and channels.
- `ReceiverModels` (`AlarmNotification`, etc.)
  - Composite domain models that hold everything needed by receivers and `AlarmService`.

**Role:** Data layer for both storage and runtime scheduling.

---

### 1.6 Receivers: `receivers/`

- `AlarmReceiver`
  - BroadcastReceiver that reacts when the OS alarm fires.
  - Reads the alarm ID from extras (`SMPLR_ALARM_RECEIVER_INTENT_ID`).
  - Fetches the corresponding `AlarmNotification` via `AlarmNotificationRepository`.
  - Shows notification and/or triggers alarmReceivedIntent, full-screen intent, etc.

- `ActivateAppReceiver`
  - Handles the “open app” action associated with `AlarmClockInfo`.

- `AlarmNotification` (data class) and helper extractors
  - Converters between Room entities and `AlarmNotification` domain object.

- `SmplrAlarmReceiverObjects`
  - Holds static lists / objects used by receivers (e.g., in-memory cache `alarmNotification`).

**Role:** Bridge between scheduled OS alarm events and your app: either show notification or dispatch to action intents.

---

### 1.7 Extensions: `extensions/`

- Time helpers like `getTimeExactForAlarmInMilliseconds(...)`.
- Converters between weekdays list ↔ JSON, NotificationEntity ↔ NotificationItem, `metadata` ↔ JSON.

**Role:** Utility glue to keep core classes smaller (though the logic is still conceptually part of the storage/scheduling design).

---

## 2. Interaction Tree (How Pieces Fit Together)

```
SmplrAlarm.kt
  ├─ smplrAlarmSet(context) { ... }
  │    └─ SmplrAlarmAPI
  │         ├─ AlarmService
  │         ├─ AlarmNotificationRepository
  │         └─ SmplrAlarmListRequestAPI? (observer)
  ├─ smplrAlarmCancel(context) { ... } → SmplrAlarmAPI.removeAlarm()
  ├─ smplrAlarmUpdate(context) { ... } → SmplrAlarmAPI.updateAlarm()
  ├─ smplrAlarmRenewMissingAlarms(context) → SmplrAlarmAPI.renewMissingAlarms()
  └─ smplrAlarmChangeOrRequestListener(context, listener)
         └─ SmplrAlarmListRequestAPI
               └─ AlarmNotificationRepository.getAllAlarmNotifications() → JSON

SmplrAlarmAPI.setAlarm()
  ├─ build AlarmNotification
  ├─ AlarmNotificationRepository.insertAlarmNotification()
  ├─ requestAPI?.requestAlarmList()
  └─ AlarmService.setAlarm(...)

AlarmService.setAlarm(...)
  ├─ compute next time via Calendar + getTimeExactForAlarmInMilliseconds()
  ├─ create open-app PendingIntent via ActivateAppReceiver
  └─ schedule AlarmManager.setAlarmClock(..., AlarmReceiver)

AlarmReceiver.onReceive()
  ├─ read alarmNotificationId from extras
  ├─ AlarmNotificationRepository.getAlarmNotification(id)
  └─ show notification, run intents

AlarmNotificationRepository
  ├─ uses Room DB (AlarmNotificationDatabase + DAOs)
  └─ uses SharedPreferences to persist all Intents as URIs + JSON

SmplrAlarmListRequestAPI
  ├─ builds AlarmItem list from AlarmNotificationRepository.getAllAlarmNotifications()
  └─ serializes to JSON and calls alarmListChangeOrRequestedListener
```

FocusModes currently sits **on top of** the JSON stream, translating `AlarmItem`+`infoPairs` into `SmplrAlarmData` and `FocusModeAlarmInfo`.

---

## 3. Improvement Plan by Class / Layer

Below is a step-by-step plan to move toward a design where the library either:
- uses its own "ROM" storage (current behavior, but behind interfaces), or
- allows consumers (like FocusModes) to bring their own storage & health model.

### 3.1 `SmplrAlarm.kt` (Top-Level API)

**Current issues:**
- Hard-wires the concrete implementation (`SmplrAlarmAPI` with internal Room/SharedPreferences store).
- No way to inject alternate storage or scheduler.

**Planned improvements:**
- Introduce new overloads that accept interfaces:

  - `AlarmStore` – abstract persistence.
  - `AlarmScheduler` – abstract scheduling (wraps `AlarmManager`).
  - `AlarmListObserver` – abstract list-change notification.

- Keep existing functions as **ROM mode** defaults using the current Room+SP implementation.
- New signatures (for BYO mode):

  ```kotlin
  fun smplrAlarmSet(
      context: Context,
      store: AlarmStore,
      scheduler: AlarmScheduler,
      observer: AlarmListObserver? = null,
      block: SmplrAlarmAPI.() -> Unit,
  ): Int
  ```

- Document both modes clearly in KDoc:
  - Mode A: "Internal storage & JSON list" (backwards compatible).
  - Mode B: "External store - advanced users" (FocusModes path).

---

### 3.2 `SmplrAlarmAPI`

**Current issues:**
- Knows about **everything**: Room, SharedPreferences, AlarmService, JSON observer.
- Directly constructs `AlarmNotificationRepository` and `AlarmService`.
- Persists & schedules in the same method (`setAlarm`, `updateAlarm`, `removeAlarm`).
- Uses weakly-typed `infoPairs` and serializes inside the library.

**Planned improvements:**
- Refactor constructor to depend on abstractions:

  ```kotlin
  class SmplrAlarmAPI(
      private val context: Context,
      private val store: AlarmStore,
      private val scheduler: AlarmScheduler,
      private val observer: AlarmListObserver? = null,
      private val idGenerator: AlarmIdGenerator = DefaultAlarmIdGenerator,
  )
  ```

- Split responsibilities:
  - **Alarm definition building** (DSL) stays here.
  - **Persistence** delegated to `store`:

    ```kotlin
    store.insert(notification: AlarmDefinition)
    store.update(...)
    store.delete(id)
    ```

  - **Scheduling** delegated to `scheduler`:

    ```kotlin
    scheduler.schedule(id, timeSpec, weekdays)
    scheduler.cancel(id)
    scheduler.exists(id)
    scheduler.renew(definition)
    ```

  - **List notifications** delegated to `observer`:

    ```kotlin
    observer?.onAlarmListChanged(store.getAll())
    ```

- Replace `AlarmNotification` + `infoPairs` coupling with a smaller, focused domain type:

  ```kotlin
  data class AlarmDefinition(
      val id: Int,
      val hour: Int,
      val minute: Int,
      val second: Int = 0,                 // NEW: second-level precision
      val millis: Int = 0,                 // NEW: millisecond precision
      val weekdays: List<WeekDays>,
      val isActive: Boolean,
      val nextTriggerTime: Long? = null,   // NEW: cached next trigger time (epoch millis)
      val metadata: Map<String, String>,   // or Any via serializer
      val notificationConfig: NotificationConfig?,
  )
  ```

- For internal/ROM mode, the store implementation can still transform `AlarmDefinition` → `AlarmNotification` + Room/SharedPreferences.
- For FocusModes/BYO mode, `metadata` can hold your strongly-typed fields (via your own serializer), and you keep AlarmDefinition in your own DB.

---

### 3.3 `AlarmService`

**Current issues:**
- Good separation: mainly concerned with AlarmManager.
- Slight coupling to specific receivers (`AlarmReceiver`, `ActivateAppReceiver`) but that’s acceptable.

**Planned improvements:**
- Formalize an `AlarmScheduler` interface:

  ```kotlin
  interface AlarmScheduler {
      fun schedule(
          id: Int,
          hour: Int,
          minute: Int,
          second: Int = 0,
          weekDays: List<WeekDays>,
      )
      fun rescheduleTomorrow(def: AlarmDefinition)
      fun renew(def: AlarmDefinition)
      fun cancel(id: Int)
      fun exists(id: Int): Boolean
  }
  ```

- Implement `AlarmSchedulerImpl` as a thin wrapper over `AlarmService`.
- Make `AlarmService` internal or private to the scheduler implementation; APIs only see `AlarmScheduler`.

---

### 3.4 `AlarmNotificationRepository` & Room Layer

**Current issues:**
- Monolithic concrete implementation:
  - Owns Room DB and SharedPreferences.
  - Knows how to persist/reconstruct intents and notification visuals.
  - Also implicitly defines the JSON alarm model (via `SmplrAlarmListRequestAPI`).
- No interface, cannot be swapped.

**Planned improvements:**
- Extract interfaces:

  ```kotlin
  interface AlarmStore {
      suspend fun insert(def: AlarmDefinition)
      suspend fun update(def: AlarmDefinition)
      suspend fun delete(id: Int)
      suspend fun get(id: Int): AlarmDefinition?
      suspend fun getAll(): List<AlarmDefinition>
  }
  ```

- Re-implement `AlarmNotificationRepository` as **`RoomAlarmStore`**, implementing `AlarmStore`.
  - Internally it can still:
    - Use Room entities for persistence.
    - Use SharedPreferences to persist intents.
    - Map between `AlarmDefinition` and current `AlarmNotification` + entity structure.

- Align cleanup semantics with the alarm deletion bug analysis:
  - **Do not** aggressively delete all alarms "before now" on every startup or migration.
  - Prefer **per-alarm deactivation** (marking `isActive = false` / clearing weekdays) when a one-time alarm fires, instead of deleting rows.
  - Optionally add a `nextTriggerTime` field (as described in `ALARM_DELETION_BUG_ANALYSIS.md`) and only trim alarms that are:
    - explicitly inactive, and
    - have `nextTriggerTime` far in the past (e.g., days/weeks),
    - or are confirmed safe to remove by the host app.
  - Expose cleanup operations via `AlarmStore` (e.g., `deactivate(id: Int)`, `pruneObsolete(before: Long)`), but leave the **policy** up to the application when running in BYO mode.

- Narrow external visibility:
  - Make Room entities and DAOs internal to the module.
  - Expose only `AlarmStore` to API layer.

- For FocusModes:
  - Add a second implementation, e.g. `FocusModeAlarmStore`, **in your app**, that maps `AlarmDefinition` to your own DB tables.
  - Use BYO overloads in `SmplrAlarm.kt` to pass that store.

---

### 3.5 `SmplrAlarmListRequestAPI`

**Current issues:**
- Emits a JSON blob representing all alarms – very library-private shape.
- Forces consumers (like FocusModes) to parse JSON and reverse-engineer meaning from string `infoPairs`.
- Requests are pull-based (`requestAlarmList()`) and push-based via the same listener, but always JSON.

**Planned improvements:**
- Introduce `AlarmListObserver` interface:

  ```kotlin
  interface AlarmListObserver {
      fun onAlarmListChanged(alarms: List<AlarmDefinition>)
  }
  ```

- In ROM/default mode:
  - Provide a built-in implementation that wraps `AlarmListObserver` → JSON:

    ```kotlin
    class JsonAlarmListObserver(
        private val inner: (String) -> Unit,
        private val json: Json = Json { ignoreUnknownKeys = true }
    ) : AlarmListObserver {
        override fun onAlarmListChanged(alarms: List<AlarmDefinition>) {
            inner(json.encodeToString(AlarmListResponse(alarms)))
        }
    }
    ```

  - `SmplrAlarmListRequestAPI` becomes a small adapter that subscribes to store changes and uses `JsonAlarmListObserver`.

- In BYO mode:
  - FocusModes can pass a different `AlarmListObserver` (e.g. storing health info, or ignoring list change events entirely).

---

### 3.6 Models & Extensions

**Current issues:**
- `AlarmNotification` and related models combine scheduling/data with notification UI.
- `infoPairs` is stringly-typed and JSON-encoded internally.
- Intents for content/fullscreen/alarm-received actions were previously persisted via SharedPreferences, which is brittle and hard to evolve.

**Planned improvements:**
- Separate models:
  - **Scheduling core**: `AlarmDefinition` (id, time, weekdays, isActive, metadata, `nextTriggerTime`).
  - **Notification decoration**: `NotificationConfig` (channel + notification visuals + **stable activation targets**).

- Represent activation targets using a **sealed, serializable descriptor model**, inspired by FocusModes' `NotificationActionTarget`:

  ```kotlin
  @Serializable
  sealed class NotificationTargetDescriptor {
      abstract val action: String?
      abstract val extras: Map<String, String>

      @Serializable
      data class ScreenTarget(
          val packageName: String,
          val activityClassName: String,
          override val action: String? = null,
          override val extras: Map<String, String> = emptyMap(),
      ) : NotificationTargetDescriptor()

      @Serializable
      data class ServiceTarget(
          val packageName: String,
          val serviceClassName: String,
          override val action: String? = null,
          override val extras: Map<String, String> = emptyMap(),
      ) : NotificationTargetDescriptor()

      @Serializable
      data class BroadcastTarget(
          val packageName: String,
          val receiverClassName: String,
          override val action: String? = null,
          override val extras: Map<String, String> = emptyMap(),
      ) : NotificationTargetDescriptor()
  }
  ```

- Extend `NotificationConfig` to carry these descriptors instead of raw `Intent`s:

  ```kotlin
  data class NotificationConfig(
      val channel: NotificationChannelItem?,
      val notification: NotificationItem?,

      val contentTarget: NotificationTargetDescriptor? = null,
      val fullScreenTarget: NotificationTargetDescriptor? = null,
      val alarmReceivedTarget: NotificationTargetDescriptor? = null,
  )
  ```

- Provide a helper to turn a `NotificationTargetDescriptor` into a `PendingIntent` at runtime (similar to FocusModes' `NotificationActionTarget.toPendingIntent`):

  ```kotlin
  internal fun NotificationTargetDescriptor.toPendingIntent(
      context: Context,
      requestCode: Int,
  ): PendingIntent { /* build Intent using className/action/extras and wrap */ }
  ```

- Move JSON/intent serialization responsibility into the **concrete store** (e.g. `RoomAlarmStore`) instead of the shared model. `RoomAlarmStore` will serialize `NotificationConfig` (including descriptors) using kotlinx.serialization.

- For metadata:
  - Define a pluggable serializer interface if you want to keep it generic:

    ```kotlin
    interface MetadataSerializer<T> {
        fun serialize(value: T): Map<String, String>
        fun deserialize(data: Map<String, String>): T?
    }
    ```

  - FocusModes can define `MetadataSerializer<FocusModeAlarmMetadata>` and attach it to their store.

---

### 3.7 Receivers & Activation Targets

**Current issues:**
- Historically, `AlarmReceiver` fetched `AlarmNotification` via `AlarmNotificationRepository` and reconstructed content/fullscreen/alarmReceived `Intent`s from SharedPreferences. This tightly coupled receivers to the legacy repo and made persistence brittle.

**Planned improvements:**
- Change `AlarmReceiver` and other receivers to depend on `AlarmStore` and **activation target descriptors** instead of concrete repository + stored Intents:
  - Resolve `AlarmDefinition` via `AlarmStore` (default: `RoomAlarmStore`, but swappable in BYO mode).
  - Use `AlarmDefinition.notificationConfig.contentTarget` / `fullScreenTarget` / `alarmReceivedTarget` and `NotificationTargetDescriptor.toPendingIntent(...)` to build `PendingIntent`s at runtime.
  - Call the existing `showNotification(...)` helper with the reconstructed `Intent`s / `PendingIntent`s.

- `ActivateAppReceiver`:
  - Read the `requestId` from extras.
  - Resolve `AlarmDefinition` via `AlarmStore`.
  - Use `contentTarget` (if present) to reconstruct the appropriate activity `Intent` and launch it. This replaces the old SharedPreferences-based contentIntent mechanism with a stable, descriptor-based model.

- Keep behavior (trigger notifications and actions) the same in ROM mode, but make the storage and intent description layers swappable and serializable.

---

## 4. Phased Refactor Steps

### Phase 1 – Introduce Interfaces (No Behavior Change)

1. **Define interfaces** in the library module:
   - `AlarmStore`
   - `AlarmScheduler`
   - `AlarmListObserver`
2. Implement `AlarmSchedulerImpl` wrapping `AlarmService`.
3. Implement `RoomAlarmStore` wrapping existing `AlarmNotificationRepository` + Room/SharedPreferences.
4. Keep old APIs wired to `RoomAlarmStore` + `AlarmSchedulerImpl` internally.

### Phase 2 – Refactor `SmplrAlarmAPI` to Use Interfaces

1. Change `SmplrAlarmAPI` constructor to accept `AlarmStore`, `AlarmScheduler`, and optional `AlarmListObserver`.
2. Update `setAlarm`, `updateAlarm`, `removeAlarm`, `renewMissingAlarms` to:
   - Build `AlarmDefinition` and delegate persistence/scheduling.
   - Notify `observer` instead of manually calling `SmplrAlarmListRequestAPI`.
3. Provide compatibility constructor used by existing top-level functions, which wires in `RoomAlarmStore` and `AlarmSchedulerImpl`.

### Phase 3 – Rebuild List Request API on Top of Interfaces

1. Re-implement `SmplrAlarmListRequestAPI` as an adapter around `AlarmStore` + `AlarmListObserver`:
   - It can still emit JSON for legacy consumers.
2. Ensure FocusModes’ `SmplrAlarmDataSource` still functions using the JSON stream initially.

### Phase 4 – Enable BYO Storage for FocusModes

1. In FocusModes app, implement `FocusModeAlarmStore`:
   - Backed by your existing scheduling tables (or a new thin table).
   - Maps `AlarmDefinition` ↔ `FocusModeTrigger` and alarm health.
2. Switch `SmplrAlarmFocusModeAlarmManager` to call the **new** `smplrAlarmSet(context, store, scheduler, ...)` overload using `FocusModeAlarmStore`.
3. Gradually stop consuming the JSON stream (`SmplrAlarmDataSource`) and instead read from your own store directly.
4. Adjust `FocusModeAlarmInfoRepository` to compute `nextStartTime`/`nextEndTime` from your store instead of SmplrAlarm’s DB.

### Phase 5 – Clean Up & Optional Simplifications

1. Deprecate direct usages of `AlarmNotificationRepository` and Room/SharedPreferences outside of `RoomAlarmStore`.
2. Optionally make `AlarmNotificationRepository` internal.
3. Tighten model boundaries (`AlarmDefinition` vs `NotificationConfig`).
4. Add documentation and diagrams describing ROM vs BYO modes.

---

## 5. Outcome

After this refactor:

- Library users can choose between:
  - **ROM mode** – drop-in, small apps use SmplrAlarm exactly as today.
  - **BYO mode** – advanced apps (like FocusModes) plug in their own `AlarmStore` and treat SmplrAlarm as a pure scheduling helper.
- FocusModes gains:
  - Single source of truth for triggers & alarms in its own domain layer.
  - Strongly-typed metadata instead of abusing `infoPairs`.
  - No reliance on JSON snapshots or polling from SmplrAlarm.
- The SmplrAlarm codebase itself becomes more modular, with clear responsibilities per class and a path to future features (e.g., additional schedulers, different persistence backends) without forking.

---

## 6. Implementation & Testing Checklist

### 6.1 Interfaces & Abstractions

- [ ] Define `AlarmDefinition` and `NotificationConfig` core models (including `second` and `nextTriggerTime`).
- [ ] Define `AlarmStore` interface (insert, update, delete, get, getAll, optional deactivate/prune APIs).
- [ ] Define `AlarmScheduler` interface and implement `AlarmSchedulerImpl` around `AlarmService`.
- [ ] Define `AlarmListObserver` interface.
- [ ] Introduce `AlarmIdGenerator` abstraction (default: time-based, but overrideable).

### 6.2 Default (ROM) Implementation

- [ ] Implement `RoomAlarmStore` on top of existing Room + SharedPreferences entities.
- [ ] Map between `AlarmDefinition`/`NotificationConfig` and current `AlarmNotification`/entities.
- [ ] Move intent serialization/deserialization fully into `RoomAlarmStore`.
- [ ] Implement non-destructive cleanup aligned with the alarm deletion bug analysis:
  - [ ] Replace blanket "delete before now" with per-alarm deactivation on fire.
  - [ ] (Optional) Add `nextTriggerTime` column and use it only for safe, targeted pruning.
  - [ ] Verify that reinstall/app restart does **not** wipe alarms unexpectedly.

- [ ] Extend Room schema and scheduling logic to support **seconds-level precision**:
  - [ ] Add `second` column (or equivalent) where needed, or derive from `nextTriggerTime`.
  - [ ] Update `AlarmService` / `AlarmSchedulerImpl` and time-calculation extensions to use seconds when computing trigger times.

### 6.3 API Refactor (`SmplrAlarmAPI` & `SmplrAlarm.kt`)

- [ ] Change `SmplrAlarmAPI` to accept `AlarmStore`, `AlarmScheduler`, `AlarmListObserver?`, `AlarmIdGenerator`.
- [ ] Update `setAlarm`, `updateAlarm`, `removeAlarm`, `renewMissingAlarms` to delegate to store/scheduler/observer.
- [ ] Keep DSL surface (`hour {}`, `min {}`, `weekdays {}`, `infoPairs {}`) intact for compatibility, but internally map to `AlarmDefinition`.
- [ ] Update top-level functions in `SmplrAlarm.kt` to:
  - [ ] Use default `RoomAlarmStore` + `AlarmSchedulerImpl` (ROM mode).
  - [ ] Provide new overloads that accept custom `AlarmStore`/`AlarmScheduler`/`AlarmListObserver` (BYO mode).

### 6.4 List Request / Observation

- [ ] Rebuild `SmplrAlarmListRequestAPI` on top of `AlarmStore` and `AlarmListObserver`.
- [ ] Implement `JsonAlarmListObserver` that wraps `AlarmListObserver` → JSON for legacy consumers.
- [ ] Ensure existing JSON-based clients continue to work without changes.

### 6.5 Receivers & Runtime Behavior

- [ ] Refactor `AlarmReceiver` (and any related receivers) to resolve `AlarmStore` via an abstraction, not directly via `AlarmNotificationRepository`.
- [ ] Ensure receivers behave identically in ROM mode.
- [ ] Add tests around reboot/time-change flows to ensure alarms are **renewed**, not silently deleted.

### 6.6 FocusModes Integration (BYO Mode)

- [ ] Implement `FocusModeAlarmStore` in the FocusModes app.
- [ ] Switch `SmplrAlarmFocusModeAlarmManager` to use the new BYO overloads with `FocusModeAlarmStore` and shared `AlarmSchedulerImpl`.
- [ ] Migrate `FocusModeAlarmInfoRepository` off the JSON stream to read from `FocusModeAlarmStore`.
- [ ] Re-run existing "missing alarms" repair logic against the new store.

### 6.7 Regression & Bug-Fix Validation

- [ ] Re-run all scenarios from `ALARM_DELETION_BUG_ANALYSIS.md`:
  - [ ] App reinstall / process restart.
  - [ ] Device reboot.
  - [ ] One-time alarm firing and deactivation.
  - [ ] Repeating alarm (weekdays) behavior.
  - [ ] Alarm update operations.
  - [ ] Alarm cancellation.
- [ ] Confirm that no alarms are unintentionally deleted; only explicitly deactivated or pruned under well-defined conditions.

### 6.8 Testing Strategy (Truth + MockK)

- **Core models & helpers** (`AlarmDefinition`, time-calculation extensions)
  - Use **Truth** assertions on computed `nextTriggerTime`, default `second = 0`, and weekday → millis mapping.

- **`AlarmSchedulerImpl`**
  - Inject a mocked `AlarmService` (MockK) and verify `setAlarm`/`cancelAlarm`/`alarmExist` are called with correct `hour`/`minute`/`second` and weekdays.

- **`RoomAlarmStore` (ROM mode)**
  - Use an in-memory Room database and real SharedPreferences in tests.
  - Verify that `insert`/`get`/`getAll` round-trip `AlarmDefinition` (including `second` and `nextTriggerTime`).
  - Verify cleanup behavior (`deactivate` vs `pruneObsolete`) respects the bug-analysis rules (no blanket delete-before-now).

- **`SmplrAlarmAPI` wiring**
  - Mock `AlarmStore`, `AlarmScheduler`, `AlarmListObserver`, `AlarmIdGenerator` with MockK.
  - Use Truth + MockK to assert that `setAlarm`/`updateAlarm`/`removeAlarm` build correct `AlarmDefinition` objects and delegate to store/scheduler/observer correctly.

- **JSON compatibility** (`SmplrAlarmListRequestAPI` + `JsonAlarmListObserver`)
  - Build `AlarmDefinition` lists, pass through `JsonAlarmListObserver`, parse JSON back, and assert with Truth that the shape matches the current `AlarmItem`/infoPairs contract.

- **BYO store (FocusModes)**
  - For `FocusModeAlarmStore`, use MockK on the FocusModes repository and Truth to verify bidirectional mapping between domain triggers and `AlarmDefinition`.


---

## 7. Current Implementation Status (Dec 2025)

This section summarizes how much of the above plan is currently implemented in the library module.

### 7.1 Architecture & Models

- **AlarmDefinition / AlarmStore / AlarmScheduler / AlarmIdGenerator**
  - **Done**: `AlarmDefinition` is the core persisted model (including `second` and `millis`).
  - **Done**: `AlarmDefinitionEntity` + `AlarmStoreDatabase` + `DaoAlarmDefinition` exist.
  - **Done**: `AlarmStore` interface is implemented by `RoomAlarmStore`.
  - **Done**: `AlarmScheduler` interface exists with `AlarmSchedulerImpl` around `AlarmService`.
  - **Done**: `AlarmIdGenerator` + `DefaultAlarmIdGenerator` are in place and used by `SmplrAlarmAPI`.
  - **Not started**: Pluggable metadata serializer abstraction (current metadata is still `Map<String, String>`).

### 7.2 Top-Level API (`SmplrAlarm.kt`)

- **Done**: `smplrAlarmSet`, `smplrAlarmUpdate`, `smplrAlarmCancel`, `smplrAlarmRenewMissingAlarms` now delegate entirely to `SmplrAlarmAPI` which works against `AlarmStore`/`AlarmScheduler`.
- **Done**: These entry points are now `suspend` functions so callers can opt into structured concurrency instead of fire-and-forget IO.
- **Partially done**: There is a constructor on `SmplrAlarmAPI` that accepts a custom `AlarmStore`, `AlarmScheduler`, and `AlarmIdGenerator`, which is the basis for BYO storage, but there are no separate top-level overloads yet that expose this directly.
- **Done**: JSON-based `smplrAlarmChangeOrRequestListener` and its API surface have been removed from the public façade.

### 7.3 SmplrAlarmAPI internals

- **Done**: `SmplrAlarmAPI` no longer touches the legacy `AlarmNotificationRepository` stack.
- **Done**: It depends on:
  - `AlarmStore` (default: `RoomAlarmStore`).
  - `AlarmScheduler` (default: `AlarmSchedulerImpl`).
  - `AlarmIdGenerator`.
  - `SmplrAlarmLogger` (pluggable, via `SmplrAlarmLoggerHolder`).
- **Done**: `setAlarm`, `updateAlarm`, `removeAlarm`, and `renewMissingAlarms` are now `suspend` and operate purely via `AlarmStore`/`AlarmScheduler`.
- **Done**: `setAlarm` enforces:
  - Valid time (hour/minute range).
  - When a `notification {}` is configured, a matching `notificationChannel {}` must also be provided.
- **Done**: `updateAlarm` enforces:
  - A valid `requestCode`.
  - Valid time setup if hour/minute overrides are supplied.
  - Matching `notificationChannel {}` when updating notifications.
- **Done**: `removeAlarm` validates `requestCode`, cancels via `AlarmService` and `AlarmScheduler`, and deletes from `AlarmStore`.
- **Done**: All alarm time paths support seconds and milliseconds end‑to‑end (API DSL → `AlarmDefinition` → calendar helpers → `AlarmService`).

### 7.4 Storage: RoomAlarmStore

- **Done**: Legacy `AlarmNotificationRepository` / multi-entity Room schema has been removed; the only shipped store is `RoomAlarmStore` over `AlarmDefinitionEntity`.
- **Done**: `RoomAlarmStore` serializes:
  - `NotificationConfig` and `NotificationItem` via kotlinx.serialization.
  - `NotificationTargetDescriptor` activation targets as part of the config.
  - `metadata` as a simple `Map<String, String>`.
- **Done**: `second` and `millis` are persisted and round‑tripped in `AlarmDefinitionEntity`.
- **Done**: `nextTriggerTime` is tracked and updated by `AlarmSchedulerImpl`.
- **Not implemented**: Explicit pruning APIs for very old / inactive definitions (only conservative reboot/time‑change policies are implemented).

### 7.5 Notification & activation model

- **Done**: `NotificationItem` and `NotificationChannelItem` are `@Serializable` and contain **no raw Intent fields**.
- **Done**: All activation targets (content, full‑screen, alarm‑received, notification buttons, dismiss) are represented by `NotificationTargetDescriptor`:
  - `ScreenTarget`
  - `ServiceTarget`
  - `BroadcastTarget`
- **Done**: Runtime conversion helpers exist in `ReceiverModels.kt`:
  - `NotificationTargetDescriptor.toIntent(context)` for descriptor → `Intent`.
  - `screenTargetFromIntent(...)`, `serviceTargetFromIntent(...)`, `broadcastTargetFromIntent(...)` for `Intent` → descriptor.
- **Done**: `AlarmNotificationAPI` builds `NotificationItem` with descriptor‑based button/dismiss targets only.
- **Done**: `Extensions+Notifications.showNotification` converts descriptors to `Intent`/`PendingIntent` at the edge, just before interacting with the Android notification APIs.
- **Partially done**: `NotificationConfig` still exists as a wrapper for channel + notification + top‑level targets; longer‑term simplification (flattening into `NotificationItem` or separate DTOs) is still optional future work.

### 7.6 Receivers & services

- **Done**: `AlarmReceiver` and `ActivateAppReceiver` use `RoomAlarmStore` + `NotificationTargetDescriptor` and do not depend on any legacy repository or stored `Intent` blobs.
- **Done**: `RebootReceiver` and `TimeChangeReceiver` both:
  - Read `AlarmDefinition` entries from `RoomAlarmStore`.
  - Re‑schedule using `AlarmSchedulerImpl` based on `isActive`, `weekdays`, and `nextTriggerTime`.
- **Done**: `AlarmService`:
  - Is `open` for testing.
  - Throws when required alarm permissions are missing instead of silently logging and returning.
  - Schedules using seconds/millis‑aware calendar helpers.

### 7.7 Logging & concurrency

- **Done**: All direct Timber usages inside the library have been replaced by `SmplrAlarmLogger` and `SmplrAlarmLoggerHolder`.
- **Done**: A public `launchIo` helper exists in `CoroutineDispatchers.kt` so receivers/background components can use the shared IO dispatcher.
- **Done**: The sample app’s `ViewModel` uses `viewModelScope` + suspend top‑level APIs instead of fire‑and‑forget coroutine scopes in the library.

### 7.8 Sample app & tests

- **Done**: Sample app now uses a Material 3‑style `SmplrAlarmTheme` and behaves correctly in dark mode.
- **Done**: Alarm UI exposes seconds and millis inputs and threads them through to `SmplrAlarmAPI`.
- **Done**: Sample app now uses descriptor‑based notification targets (via `screenTargetFromIntent` / `broadcastTargetFromIntent`).
- **Done**: Added unit tests for calendar/time helpers.
- **Done**: Added Android instrumentation tests for:
  - `RoomAlarmStore` (round‑trip of seconds/millis and definitions).
  - `AlarmSchedulerImpl` (wiring to `AlarmService`).
  - `AlarmService` (basic scheduling behavior and permission handling).
  - `AlarmReceiver` (one‑shot and repeating behavior).
- **Done**: Legacy JSON alarm list listener and related sample UI have been removed.
