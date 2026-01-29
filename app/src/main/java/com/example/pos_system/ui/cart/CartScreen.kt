package com.example.pos_system.ui.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
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

    // NEW: State for cash received calculation
    var cashReceived by remember { mutableStateOf("") }

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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                Column(Modifier.weight(1f)) {
                    LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                        items(uiState.items) { item ->
                            CartItemRow(
                                cartItem = item,
                                onIncrease = { cartViewModel.updateQuantity(item, item.quantity + 1) },
                                onDecrease = { cartViewModel.updateQuantity(item, item.quantity - 1) },
                                onRemove = { cartViewModel.removeFromCart(item) }
                            )
                        }
                    }

                    // Payment Method Selector
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Payment Method", fontWeight = FontWeight.Bold, color = Color(0xFF4E342E))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Cash", "E-Wallet", "Delivery").forEach { method ->
                                val isSelected = cartViewModel.selectedPaymentMethod == method
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        cartViewModel.setPaymentMethod(method)
                                        if (method != "Cash") cashReceived = ""
                                    },
                                    label = { Text(method) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFD2B48C),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        // NEW: Cash Calculator Input
                        if (cartViewModel.selectedPaymentMethod == "Cash") {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                OutlinedTextField(
                                    value = cashReceived,
                                    onValueChange = { if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d*$"""))) cashReceived = it },
                                    label = { Text("Cash Received") },
                                    prefix = { Text("RM ") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFD2B48C))
                                )

                                val receivedAmt = cashReceived.toDoubleOrNull() ?: 0.0
                                val changeAmt = (receivedAmt - uiState.finalPrice).coerceAtLeast(0.0)

                                if (receivedAmt > 0) {
                                    Text(
                                        text = "Change: RM ${String.format("%.2f", changeAmt)}",
                                        color = if (receivedAmt >= uiState.finalPrice) Color(0xFF4CAF50) else Color.Red,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Checkout Card
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

                    val receivedAmt = cashReceived.toDoubleOrNull() ?: 0.0
                    val canCheckout = if (cartViewModel.selectedPaymentMethod == "Cash") receivedAmt >= uiState.finalPrice else true

                    Button(
                        onClick = {
                            cartViewModel.checkout {
                                showSuccessDialog = true
                                cashReceived = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD2B48C)),
                        enabled = !cartViewModel.isCheckingOut.value && canCheckout && uiState.items.isNotEmpty()
                    ) {
                        if (cartViewModel.isCheckingOut.value) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Checkout", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showDiscountDialog) {
        DiscountDialog(
            onDismiss = { showDiscountDialog = false },
            onApply = { type, value ->
                cartViewModel.applyDiscount(type, value)
                showDiscountDialog = false
            }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            confirmButton = {
                Button(onClick = { showSuccessDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD2B48C))) { Text("OK") }
            },
            title = { Text("Success") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Icon(painterResource(id = R.drawable.tick), null, Modifier.size(64.dp), tint = Color(0xFF4CAF50))
                    Text("Payment Successful!", Modifier.padding(top = 16.dp))
                }
            }
        )
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(cartItem.item.itemName, fontWeight = FontWeight.Bold)
            }

            // Quantity Controller
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                IconButton(onClick = onDecrease) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = Color.DarkGray)
                }
                Text(
                    text = cartItem.quantity.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp),
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onIncrease) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = Color.DarkGray)
                }
            }

            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove Item", tint = Color.Red)
            }
        }
    }
}

@Composable
fun DiscountDialog(onDismiss: () -> Unit, onApply: (DiscountType, Double) -> Unit) {
    var type by remember { mutableStateOf(DiscountType.PERCENTAGE) }
    var amountValue by remember { mutableStateOf("") }
    var selectedPercentage by remember { mutableStateOf(10.0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Discount") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FilterChip(
                        selected = type == DiscountType.PERCENTAGE,
                        onClick = { type = DiscountType.PERCENTAGE },
                        label = { Text("Percentage (%)") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFD2B48C))
                    )
                    FilterChip(
                        selected = type == DiscountType.AMOUNT,
                        onClick = { type = DiscountType.AMOUNT },
                        label = { Text("Fixed (RM)") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFFD2B48C))
                    )
                }

                if (type == DiscountType.PERCENTAGE) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(10.0, 20.0).forEach { pct ->
                            OutlinedButton(
                                onClick = { selectedPercentage = pct },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selectedPercentage == pct) Color(0xFFD2B48C) else Color.Transparent,
                                    contentColor = if (selectedPercentage == pct) Color.White else Color.Black
                                )
                            ) { Text("${pct.toInt()}%") }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = amountValue,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d*$"""))) amountValue = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(type, if (type == DiscountType.PERCENTAGE) selectedPercentage else amountValue.toDoubleOrNull() ?: 0.0) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD2B48C))
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}