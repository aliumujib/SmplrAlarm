package de.coldtea.smplr.alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import de.coldtea.smplr.alarm.alarms.AlarmScreen
import de.coldtea.smplr.alarm.ui.theme.SmplrAlarmTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            SmplrAlarmTheme {
                AlarmScreen()
            }
        }
    }

}