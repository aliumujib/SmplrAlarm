# SmplrAlarm

An Android convenience library to make setting alarms way **simpler** than it is.

[![](https://jitpack.io/v/aliumujib/SmplrAlarm.svg)](https://jitpack.io/#aliumujib/SmplrAlarm)
![](https://img.shields.io/badge/Kotlin-2.0-brightgreen)
![](https://img.shields.io/badge/Minimum%20Api-24-green)
![](https://img.shields.io/badge/License-MIT-green)

SmplrAlarm manages all the necessary modules to set a proper alarm using native Android libraries, provides an API interface powered by Kotlin DSL, and makes setting an alarm as simple as:

```kotlin
smplrAlarmSet(applicationContext) {
    hour { 8 }
    min { 30 }
}
```

## What's New in v3.0.0

- **Suspend-based API** -- all top-level functions (`smplrAlarmSet`, `smplrAlarmUpdate`, `smplrAlarmCancel`) are now `suspend` functions
- **`AlarmDefinition` + `RoomAlarmStore`** architecture -- replaced legacy Room + SharedPreferences with a clean, serializable model
- **`NotificationTargetDescriptor`** -- serializable sealed class hierarchy replaces raw `Intent` storage for notification actions and activation targets
- **`SmplrAlarmEnvironment`** -- global configuration for pluggable `AlarmStore`, `AlarmScheduler`, `SmplrAlarmLogger`, and `AlarmIdGenerator`
- **Second and millisecond precision** -- `second {}` and `millis {}` DSL setters for sub-minute alarm timing
- **kotlinx.serialization** -- replaced Moshi for all serialization
- **Pluggable logging** -- `SmplrAlarmLogger` abstraction with default Timber-backed implementation
- **Critical bug fix** -- fixed `deleteAlarmsBeforeNow()` which was incorrectly deleting all alarms on device reboot
- **Compose sample app** -- fully rewritten from XML/Fragments to Jetpack Compose
- **Kotlin DSL build files** -- all `build.gradle` migrated to `build.gradle.kts` with version catalogs

## Installation

### Gradle (Kotlin DSL)

**Step 1.** Add the JitPack repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**Step 2.** Add the dependency to your module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.aliumujib:SmplrAlarm:v3.0.0")
}
```

### Gradle (Groovy DSL)

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        maven { url 'https://jitpack.io' }
    }
}

// build.gradle
dependencies {
    implementation 'com.github.aliumujib:SmplrAlarm:v3.0.0'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.aliumujib</groupId>
    <artifactId>SmplrAlarm</artifactId>
    <version>v3.0.0</version>
</dependency>
```

## How to Use

### Setting an Alarm

All SmplrAlarm requires to set an alarm is an hour and a minute. All top-level functions are `suspend`, so call them from a coroutine scope:

```kotlin
viewModelScope.launch {
    val requestCode = smplrAlarmSet(applicationContext) {
        hour { 8 }
        min { 30 }
    }
    // requestCode is the unique alarm ID
}
```

### Setting an Alarm with Second/Millisecond Precision

```kotlin
smplrAlarmSet(applicationContext) {
    hour { 8 }
    min { 30 }
    second { 45 }
    millis { 500 }
}
```

### Repeating Alarm

Set the weekdays you want the alarm to repeat on:

```kotlin
smplrAlarmSet(applicationContext) {
    hour { 8 }
    min { 30 }
    weekdays {
        monday()
        wednesday()
        friday()
    }
}
```

### Alarm with Notification

When providing a `notification {}`, you **must** also provide a `notificationChannel {}`:

```kotlin
smplrAlarmSet(applicationContext) {
    hour { 8 }
    min { 30 }
    notificationChannel {
        channel {
            channelId { "my_alarm_channel" }
            name { "My Alarms" }
            description { "Alarm notifications" }
            importance { NotificationManager.IMPORTANCE_HIGH }
            showBadge { false }
        }
    }
    notification {
        alarmNotification {
            smallIcon { R.drawable.ic_alarm }
            title { "Wake up!" }
            message { "Time to start the day" }
            bigText { "Time to start the day" }
            autoCancel { true }
        }
    }
}
```

### Notification with Action Buttons

Create intents and wrap them in target descriptors:

```kotlin
val snoozeIntent = Intent(applicationContext, ActionReceiver::class.java).apply {
    action = "ACTION_SNOOZE"
}

val dismissIntent = Intent(applicationContext, ActionReceiver::class.java).apply {
    action = "ACTION_DISMISS"
}

smplrAlarmSet(applicationContext) {
    hour { 8 }
    min { 30 }
    notificationChannel {
        channel {
            channelId { "my_alarm_channel" }
            name { "My Alarms" }
            description { "Alarm notifications" }
            importance { NotificationManager.IMPORTANCE_HIGH }
            showBadge { false }
        }
    }
    notification {
        alarmNotification {
            smallIcon { R.drawable.ic_alarm }
            title { "Alarm ringing" }
            message { "Alarm ringing" }
            bigText { "Alarm ringing" }
            autoCancel { true }
            firstButtonText { "Snooze" }
            secondButtonText { "Dismiss" }
            firstButtonTarget { broadcastTargetFromIntent(snoozeIntent) }
            secondButtonTarget { broadcastTargetFromIntent(dismissIntent) }
        }
    }
}
```

### Activation Targets

SmplrAlarm uses `NotificationTargetDescriptor` to describe what happens when events fire. Three types are available:

- **`screenTargetFromIntent(intent)`** -- opens an Activity
- **`broadcastTargetFromIntent(intent)`** -- sends a broadcast
- **`serviceTargetFromIntent(intent)`** -- starts a Service

```kotlin
val onClickIntent = Intent(applicationContext, MainActivity::class.java)
val fullScreenIntent = Intent(applicationContext, LockScreenAlarmActivity::class.java)
val alarmReceivedIntent = Intent(applicationContext, AlarmBroadcastReceiver::class.java)

smplrAlarmSet(applicationContext) {
    hour { 8 }
    min { 30 }
    contentTarget { screenTargetFromIntent(onClickIntent) }
    fullScreenTarget { screenTargetFromIntent(fullScreenIntent) }
    alarmReceivedTarget { broadcastTargetFromIntent(alarmReceivedIntent) }
    // ... notification config ...
}
```

### Custom Request Code

By default, SmplrAlarm generates a unique ID. You can supply your own:

```kotlin
smplrAlarmSet(applicationContext) {
    requestCode { myCustomId }
    hour { 8 }
    min { 30 }
}
```

### Updating an Alarm

```kotlin
smplrAlarmUpdate(applicationContext) {
    requestCode { existingAlarmId }
    hour { 9 }
    min { 0 }
    weekdays {
        monday()
        friday()
    }
    isActive { true }
}
```

### Updating a Notification

```kotlin
smplrAlarmUpdate(applicationContext) {
    requestCode { existingAlarmId }
    notificationChannel {
        channel {
            channelId { "my_alarm_channel" }
            name { "My Alarms" }
            description { "Alarm notifications" }
            importance { NotificationManager.IMPORTANCE_HIGH }
            showBadge { true }
        }
    }
    notification {
        NotificationItem(
            smallIcon = R.drawable.ic_changed,
            title = "Updated alarm",
            message = "Updated alarm",
            bigText = "Updated alarm",
        )
    }
}
```

### Cancelling an Alarm

```kotlin
smplrAlarmCancel(applicationContext) {
    requestCode { existingAlarmId }
}
```

### Renewing Missing Alarms

After a reboot or app update, call this to reschedule any alarms that lost their OS-level scheduling:

```kotlin
smplrAlarmRenewMissingAlarms(applicationContext)
```

### Info Pairs (Metadata)

Attach arbitrary key-value metadata to an alarm:

```kotlin
smplrAlarmSet(applicationContext) {
    hour { 8 }
    min { 30 }
    infoPairs {
        listOf(
            "note" to "Take your pills",
            "snoozeCount" to "0"
        )
    }
}
```

### Getting Notification IDs in Receivers

When the alarm fires, your BroadcastReceiver can retrieve the alarm ID:

```kotlin
class AlarmBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val requestId = intent.getIntExtra(SmplrAlarmAPI.SMPLR_ALARM_REQUEST_ID, -1)
        // Handle the alarm...
    }
}
```

For notification action buttons:

```kotlin
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(SmplrAlarmAPI.SMPLR_ALARM_NOTIFICATION_ID, -1)
        // Cancel notification, snooze, etc.
    }
}
```

## Advanced: Custom Environment

For apps that need full control, configure `SmplrAlarmEnvironment` at startup:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        SmplrAlarmEnvironment.init(
            SmplrAlarmConfig(
                storeFactory = { context -> RoomAlarmStore(context) },
                idGenerator = MyCustomIdGenerator,
                logger = MyCustomLogger,
                alarmTimeCalculator = DefaultAlarmTimeCalculator(),
            )
        )
    }
}
```

### Pluggable Logger

Implement `SmplrAlarmLogger` to route library logs to your own framework:

```kotlin
object MyLogger : SmplrAlarmLogger {
    override fun v(message: String, throwable: Throwable?) { /* ... */ }
    override fun d(message: String, throwable: Throwable?) { /* ... */ }
    override fun i(message: String, throwable: Throwable?) { /* ... */ }
    override fun w(message: String, throwable: Throwable?) { /* ... */ }
    override fun e(message: String, throwable: Throwable?) { /* ... */ }
}
```

### Observable Alarm Store

`RoomAlarmStore` implements `ObservableAlarmStore`, exposing a `Flow` of all alarms:

```kotlin
val store = RoomAlarmStore(context)
store.alarmsFlow.collect { alarms ->
    // React to alarm changes
}
```

## Architecture

```
SmplrAlarmAPI (DSL entry point)
    |
    +-- AlarmStore (insert/update/delete/get/getAll)
    |       +-- RoomAlarmStore (default, Room + kotlinx.serialization)
    |
    +-- AlarmScheduler (schedule/cancel/renew)
    |       +-- AlarmSchedulerImpl -> AlarmService (AlarmManager)
    |
    +-- AlarmDefinition (core model)
    |       +-- NotificationConfig
    |       |       +-- NotificationChannelItem
    |       |       +-- NotificationItem
    |       |       +-- NotificationTargetDescriptor (Screen/Broadcast/Service)
    |       +-- WeekDays
    |       +-- metadata: Map<String, String>
    |
    +-- SmplrAlarmEnvironment (global config)
            +-- SmplrAlarmLogger
            +-- AlarmIdGenerator
            +-- AlarmTimeCalculator
```

## Migration from v2.x

| v2.x | v3.0.0 |
|---|---|
| `smplrAlarmSet(context) { ... }` (non-suspend) | `smplrAlarmSet(context) { ... }` (suspend, call from coroutine) |
| `intent { ... }` / `contentIntent { ... }` | `contentTarget { screenTargetFromIntent(intent) }` |
| `receiverIntent { ... }` | `fullScreenTarget { screenTargetFromIntent(intent) }` |
| `alarmReceivedIntent { ... }` | `alarmReceivedTarget { broadcastTargetFromIntent(intent) }` |
| `firstButtonIntent { intent }` | `firstButtonTarget { broadcastTargetFromIntent(intent) }` |
| `secondButtonIntent { intent }` | `secondButtonTarget { broadcastTargetFromIntent(intent) }` |
| `notificationDismissedIntent { intent }` | `dismissTarget { broadcastTargetFromIntent(intent) }` |
| `smplrAlarmChangeOrRequestListener { json -> }` | Use `RoomAlarmStore.alarmsFlow` or `AlarmStore.getAll()` |
| Moshi for serialization | kotlinx.serialization |
| `AlarmNotificationRepository` | `AlarmStore` / `RoomAlarmStore` |

## ChangeLog

### [3.0.0] 2025

- Complete architecture overhaul: `AlarmDefinition` + `RoomAlarmStore` replaces legacy storage
- All top-level DSL functions are now `suspend`
- `NotificationTargetDescriptor` sealed hierarchy for serializable activation targets
- `SmplrAlarmEnvironment` for global pluggable configuration
- Second and millisecond precision support
- Pluggable `SmplrAlarmLogger` abstraction
- Pluggable `AlarmIdGenerator` and `AlarmTimeCalculator`
- `ObservableAlarmStore` with Flow-based observation
- Critical bug fix: `deleteAlarmsBeforeNow()` no longer deletes all alarms on reboot
- Removed JSON listener API (`SmplrAlarmListRequestAPI`)
- Replaced Moshi with kotlinx.serialization
- Sample app rewritten in Jetpack Compose
- Build system migrated to Kotlin DSL with version catalogs
- AGP 8.5.0, Kotlin 2.0.0, compileSdk/targetSdk 35, Room 2.6.1

### [2.1.1] 20.04.2022

- Fixed the problem with on click notification intent
- Name change: On click notification intent name was changed from intent to contentIntent
- Proguard rule update: keep class names under -> de.coldtea.smplr.smplralarm.models

### [2.1.0] 03.03.2022

- Dependency upgrades: Kotlin 1.6.10, Gradle 7.0.4, Room 2.4.2, Moshi 1.13.0

### [2.0.2] 11.12.2021

- Android 12 integration, Java 11, Kotlin 1.5.31, compileSdkVersion 31
- Updatable notifications, muted notification sounds

### [v1.3.0] 12.11.2021

- Added `notificationDismissedIntent`

### [v1.2.0] 31.10.2021

- Added `notificationReceivedIntent`
- Fixed JSON conversion error for full screen intent extras

### [v1.1.0] 10.07.2021

- Added `infoPairs` for metadata storage
- Full screen intents return request ID

### [v1.0.0] 02.06.2021

- Initial release: Set, Update, Delete alarm with notifications and intents

## License

```
MIT License

Copyright (c) 2020 Yasar Naci Gunduz
Copyright (c) 2025 Abdul-Mujeeb Aliu

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
