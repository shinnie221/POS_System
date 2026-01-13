package com.example.pos_system

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.example.pos_system.ui.navigation.POSNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                // Surface provides the background color (usually white/dark mode)
                Surface(color = MaterialTheme.colorScheme.background) {
                    POSNavGraph()
                }
            }
        }
    }
}
