package com.example.pos_system.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun BottomNavigationBar(currentRoute: String, onNavigate: (String) -> Unit) {
    val items = listOf(
        Triple("Home", Icons.Default.Home, "home"),
        Triple("Cart", Icons.Default.ShoppingCart, "cart"),
        Triple("Profile", Icons.Default.Person, "profile")
    )

    NavigationBar(containerColor = Color.White) {
        items.forEach { (label, icon, route) ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = { onNavigate(route) }, // This triggers the callback
                label = { Text(label) },
                icon = { Icon(icon, contentDescription = label) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color(0xFFD2B48C),
                    selectedIconColor = Color.White,
                    selectedTextColor = Color(0xFFD2B48C),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}