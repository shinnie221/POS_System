// C:/Users/shinn/StudioProjects/POS_System/app/src/main/java/com/example/pos_system/MainActivity.kt

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

        // 1. Access the Repositories through the Application class
        val appModule = (application as POSSystem).appModule

        // 2. Start Real-Time Listeners for all modules
        appModule.categoryRepository.startRealTimeSync()
        appModule.itemRepository.startRealTimeSync()
        appModule.salesRepository.startRealTimeSync()

        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    POSNavGraph()
                }
            }
        }
    }
}