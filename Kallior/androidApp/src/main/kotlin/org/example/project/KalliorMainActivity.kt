package org.example.project

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.navigation.compose.rememberNavController
import org.example.project.health.AccelerometerStepForegroundService

class KalliorMainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        AccelerometerStepForegroundService.startIfNeeded(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val navController = rememberNavController()
                KalliorNavGraph(navController)
            }
        }
    }
}
