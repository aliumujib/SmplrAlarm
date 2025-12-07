package de.coldtea.smplr.alarm.alarms

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.coldtea.smplr.alarm.R
import de.coldtea.smplr.alarm.alarms.models.WeekInfo
import de.coldtea.smplr.alarm.extensions.nowPlus
import de.coldtea.smplr.smplralarm.models.NotificationItem
import de.coldtea.smplr.smplralarm.smplrAlarmUpdate
import java.util.Calendar

@Composable
fun AlarmScreen(alarmViewModel: AlarmViewModel = viewModel()) {
    val context = LocalContext.current
    val defaultTime = Calendar.getInstance().nowPlus(1)

    val scheduleState by alarmViewModel.scheduleState.collectAsState()

    // State for UI elements
    var hour by remember { mutableStateOf(defaultTime.first.toString()) }
    var minute by remember { mutableStateOf(defaultTime.second.toString()) }
    var second by remember { mutableStateOf("0") }
    var millis by remember { mutableStateOf("0") }
    var alarmId by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }

    val weekDays = remember {
        mutableStateMapOf(
            "SUNDAY" to true, "MONDAY" to false, "TUESDAY" to false,
            "WEDNESDAY" to false, "THURSDAY" to false, "FRIDAY" to false, "SATURDAY" to false
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Repeating alarm", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        // Time Input (HH:MM:SS.mmm)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = hour,
                onValueChange = { hour = it },
                label = { Text("HH") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
            )
            Text(":", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 8.dp))
            OutlinedTextField(
                value = minute,
                onValueChange = { minute = it },
                label = { Text("MM") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
            )
            Text(":", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 8.dp))
            OutlinedTextField(
                value = second,
                onValueChange = { second = it },
                label = { Text("SS") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
            )
            Text(".", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 4.dp))
            OutlinedTextField(
                value = millis,
                onValueChange = { millis = it },
                label = { Text("ms") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Weekday Checkboxes
        WeekDaySelector(weekDays)
        Spacer(modifier = Modifier.height(16.dp))

        // Alarm ID
        OutlinedTextField(
            value = alarmId,
            onValueChange = { alarmId = it },
            label = { Text("Alarm ID (for update/cancel)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Is Active")
            Checkbox(checked = isActive, onCheckedChange = { isActive = it })
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        val buttonModifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        val weekInfo = weekDays.toWeekInfo()
        val secondInt = second.toIntOrNull() ?: 0
        val millisInt = millis.toIntOrNull() ?: 0

        Button(onClick = {
            alarmViewModel.setFullScreenIntentAlarm(
                hour.toInt(),
                minute.toInt(),
                secondInt,
                millisInt,
                weekInfo,
                context.applicationContext
            )
        }, modifier = buttonModifier) {
            Text("Set FullScreen Intent Alarm")
        }

        Button(onClick = {
            alarmViewModel.setNotificationAlarm(
                hour.toInt(),
                minute.toInt(),
                secondInt,
                millisInt,
                weekInfo,
                context.applicationContext
            )
        }, modifier = buttonModifier) {
            Text("Set Notification Alarm")
        }

        Button(onClick = {
            alarmViewModel.setNoNotificationAlarm(
                hour.toInt(),
                minute.toInt(),
                secondInt,
                millisInt,
                weekInfo,
                context.applicationContext
            )
        }, modifier = buttonModifier) {
            Text("Set No Notification Alarm")
        }

        Button(onClick = {
            if (alarmId.isNotEmpty()) {
                alarmViewModel.updateAlarm(alarmId.toInt(), hour.toInt(), minute.toInt(), weekInfo, isActive, context.applicationContext)
                Toast.makeText(context, "Alarm $alarmId updated!", Toast.LENGTH_SHORT).show()
            }
        }, modifier = buttonModifier) {
            Text("Update Alarm")
        }

        Button(onClick = {
            if (alarmId.isNotEmpty()) {
                alarmViewModel.updateNotification(alarmId.toInt(), context.applicationContext)
                Toast.makeText(context, "Notification for $alarmId updated!", Toast.LENGTH_SHORT).show()
            }
        }, modifier = buttonModifier) {
            Text("Update Notification")
        }

        Button(onClick = {
            if (alarmId.isNotEmpty()) {
                alarmViewModel.cancelAlarm(alarmId.toInt(), context.applicationContext)
                Toast.makeText(context, "Alarm $alarmId cancelled!", Toast.LENGTH_SHORT).show()
            }
        }, modifier = buttonModifier) {
            Text("Cancel Alarm")
        }

        LaunchedEffect(scheduleState) {
            when (val state = scheduleState) {
                is AlarmViewModel.AlarmScheduleState.Success -> {
                    alarmId = state.requestCode.toString()
                    toastAlarm(context, hour, minute, weekDays.toWeekInfo())
                    alarmViewModel.clearScheduleState()
                }
                is AlarmViewModel.AlarmScheduleState.Error -> {
                    Toast.makeText(
                        context,
                        "Failed to schedule alarm: ${state.throwable.message ?: "Unknown error"}",
                        Toast.LENGTH_SHORT
                    ).show()
                    alarmViewModel.clearScheduleState()
                }
                else -> Unit
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun WeekDaySelector(weekDays: MutableMap<String, Boolean>) {
    Column {
        Row {
            WeekDayCheckBox("Mon", weekDays)
            WeekDayCheckBox("Tue", weekDays)
            WeekDayCheckBox("Wed", weekDays)
        }
        Row {
            WeekDayCheckBox("Thu", weekDays)
            WeekDayCheckBox("Fri", weekDays)
            WeekDayCheckBox("Sat", weekDays)
        }
        Row {
            WeekDayCheckBox("Sun", weekDays)
        }
    }
}

@Composable
fun RowScope.WeekDayCheckBox(day: String, weekDays: MutableMap<String, Boolean>) {
    val key = when(day){
        "Mon" -> "MONDAY"
        "Tue" -> "TUESDAY"
        "Wed" -> "WEDNESDAY"
        "Thu" -> "THURSDAY"
        "Fri" -> "FRIDAY"
        "Sat" -> "SATURDAY"
        "Sun" -> "SUNDAY"
        else -> ""
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f)
    ) {
        Checkbox(
            checked = weekDays[key] ?: false,
            onCheckedChange = { weekDays[key] = it }
        )
        Text(day)
    }
}

private fun MutableMap<String, Boolean>.toWeekInfo(): WeekInfo =
    WeekInfo(
        this["MONDAY"] ?: false,
        this["TUESDAY"] ?: false,
        this["WEDNESDAY"] ?: false,
        this["THURSDAY"] ?: false,
        this["FRIDAY"] ?: false,
        this["SATURDAY"] ?: false,
        this["SUNDAY"] ?: false
    )


private fun toastAlarm(context: Context, hour: String, minute: String, weekInfo: WeekInfo) {
    var toastText = "$hour:$minute"
    if (weekInfo.monday) toastText = toastText.plus(" Monday")
    if (weekInfo.tuesday) toastText = toastText.plus(" Tuesday")
    if (weekInfo.wednesday) toastText = toastText.plus(" Wednesday")
    if (weekInfo.thursday) toastText = toastText.plus(" Thursday")
    if (weekInfo.friday) toastText = toastText.plus(" Friday")
    if (weekInfo.saturday) toastText = toastText.plus(" Saturday")
    if (weekInfo.sunday) toastText = toastText.plus(" Sunday")

    Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    // We can't fully preview since it depends on a ViewModel.
    // A more advanced setup would involve providing a fake ViewModel for the preview.
    AlarmScreen()
}
