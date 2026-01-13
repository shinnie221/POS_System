package com.example.pos_system.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pos_system.R
import com.example.pos_system.navigation.BottomNavigationBar

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToSales: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(currentRoute = "profile", onNavigate = { route ->
                if (route == "home") onNavigateToHome()
                if (route == "cart") onNavigateToCart()
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFFDF8F3)).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(Modifier.size(100.dp), shape = CircleShape, color = Color(0xFFD2B48C)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, Modifier.size(60.dp), Color.White)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(text = viewModel.userName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4E342E))

            Spacer(Modifier.height(40.dp))

            Text(text = "MANAGEMENT", modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.ExtraBold, color = Color.Gray, fontSize = 12.sp)

            Spacer(Modifier.height(12.dp))

            // Menu Management Button
            Card(
                onClick = onNavigateToMenu,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(id = R.drawable.restaurantmenu), null, Modifier.size(32.dp), Color(0xFFD2B48C))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Menu Management", fontWeight = FontWeight.Bold, color = Color(0xFF4E342E))
                        Text("Add items and categories", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            // Sales Reports Button
            Card(
                onClick = onNavigateToSales,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(id = R.drawable.barchart), null, Modifier.size(32.dp), Color(0xFFD2B48C))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("Sales Reports", fontWeight = FontWeight.Bold, color = Color(0xFF4E342E))
                        Text("View history and revenue", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.logout(onLogout) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD2B48C))
            ) {
                Text("Logout", fontWeight = FontWeight.Bold)
            }
        }
    }
}