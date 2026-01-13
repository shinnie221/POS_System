package com.example.pos_system.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pos_system.R
import com.example.pos_system.data.model.CartItem
import com.example.pos_system.navigation.BottomNavigationBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    cartViewModel: CartViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val uiState by cartViewModel.uiState.collectAsState()
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(currentRoute = "cart", onNavigate = { route ->
                if (route == "home") onNavigateToHome()
                if (route == "profile") onNavigateToProfile()
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFFDF8F3))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My Cart", fontSize = 28.sp, fontWeight = FontWeight.Bold)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (uiState.items.isNotEmpty()) {
                        TextButton(onClick = { cartViewModel.clearCart() }) {
                            Text("Clear", color = Color.Red)
                        }
                    }
                    Button(
                        onClick = { showDiscountDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD2B48C)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(painterResource(id = R.drawable.sell), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Discount")
                    }
                }
            }

            if (uiState.items.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Your cart is empty", color = Color.Gray)
                }
            } else {
                LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    items(uiState.items) { item ->
                        CartItemRow(item) { cartViewModel.removeFromCart(item) }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", color = Color.Gray)
                            Text("RM ${String.format("%.2f", uiState.totalAmount)}")
                        }
                        if (uiState.discountApplied > 0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discount", color = Color.Red)
                                Text("- RM ${String.format("%.2f", uiState.discountApplied)}", color = Color.Red)
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("RM ${String.format("%.2f", uiState.finalPrice)}", color = Color(0xFFD2B48C), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        }
                        Button(
                            onClick = { cartViewModel.checkout { showSuccessDialog = true } },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD2B48C))
                        ) { Text("Checkout", color = Color.White) }
                    }
                }
            }
        }
    }

    if (showDiscountDialog) {
        DiscountDialog(
            onDismiss = { showDiscountDialog = false },
            onApply = { type, valDouble ->
                cartViewModel.applyDiscount(type, valDouble)
                showDiscountDialog = false
            }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            confirmButton = { Button(onClick = { showSuccessDialog = false }) { Text("OK") } },
            title = { Text("Success") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(64.dp), Color(0xFF4CAF50))
                    Text("Payment Successful!", Modifier.padding(top = 16.dp))
                }
            }
        )
    }
}

@Composable
fun CartItemRow(cartItem: CartItem, onRemove: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(cartItem.item.itemName, fontWeight = FontWeight.Bold)
                Text("Qty: ${cartItem.quantity}", fontSize = 12.sp, color = Color.Gray)
            }
            Text("RM ${String.format("%.2f", cartItem.totalPrice)}", fontWeight = FontWeight.Bold)
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
        }
    }
}

@Composable
fun DiscountDialog(onDismiss: () -> Unit, onApply: (DiscountType, Double) -> Unit) {
    var type by remember { mutableStateOf(DiscountType.PERCENTAGE) }
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Discount") },
        text = {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(selected = type == DiscountType.PERCENTAGE, onClick = { type = DiscountType.PERCENTAGE }, label = { Text("%") })
                    FilterChip(selected = type == DiscountType.AMOUNT, onClick = { type = DiscountType.AMOUNT }, label = { Text("RM") })
                }
                OutlinedTextField(
                    value = value, onValueChange = { value = it },
                    label = { Text("Value") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = { onApply(type, value.toDoubleOrNull() ?: 0.0) }) { Text("Apply") } }
    )
}