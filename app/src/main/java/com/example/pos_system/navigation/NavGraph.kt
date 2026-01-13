package com.example.pos_system.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pos_system.ui.auth.*
import com.example.pos_system.ui.cart.*
import com.example.pos_system.ui.home.*
import com.example.pos_system.ui.profile.*
import com.example.pos_system.ui.sales.SalesReportsScreen

@Composable
fun POSNavGraph() {
    val navController = rememberNavController()

    // Shared CartViewModel to persist items when switching between Home and Cart
    val cartViewModel: CartViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        // --- Login Screen ---
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        // --- Register Screen ---
        composable("register") {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate("home") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // --- Home Screen ---
        composable("home") {
            HomeScreen(
                onNavigateToCart = { navController.navigate("cart") },
                onNavigateToSettings = { navController.navigate("profile") },
                onNavigateToSales = { navController.navigate("sales_reports") },
                cartViewModel = cartViewModel
            )
        }

        // --- Cart Screen ---
        composable("cart") {
            CartScreen(
                cartViewModel = cartViewModel,
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToProfile = { navController.navigate("profile") }
            )
        }

        // --- Profile Screen ---
        composable("profile") {
            ProfileScreen(
                onNavigateToHome = { navController.navigate("home") },
                onNavigateToCart = { navController.navigate("cart") },
                onNavigateToMenu = { navController.navigate("add_menu") },
                onNavigateToSales = { navController.navigate("sales_reports") },
                onLogout = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // --- Management: Add Menu ---
        composable("add_menu") {
            AddMenuScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // --- Reports: Sales Reports ---
        composable("sales_reports") {
            SalesReportsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}