package com.example.pos_system.ui.home

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pos_system.R
import com.example.pos_system.data.local.database.entity.ItemEntity
import com.example.pos_system.data.model.CartItem
import com.example.pos_system.data.model.Item
import com.example.pos_system.navigation.BottomNavigationBar
import com.example.pos_system.ui.cart.CartViewModel

@Composable
fun HomeScreen(
    onNavigateToSales: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCart: () -> Unit,
    homeViewModel: HomeViewModel = viewModel(),
    cartViewModel: CartViewModel
) {
    val uiState by homeViewModel.uiState.collectAsState()
    var selectedItemForPopup by remember { mutableStateOf<ItemEntity?>(null) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp >= configuration.screenHeightDp

    Scaffold(
        bottomBar = {
            if (!isLandscape) {
                BottomNavigationBar(
                    currentRoute = "home",
                    onNavigate = { route ->
                        when (route) {
                            "cart" -> onNavigateToCart()
                            "profile" -> onNavigateToSettings()
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFDF8F3))
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (isLandscape) {
                    NavigationSidebar(
                        onCartClick = onNavigateToCart,
                        onSalesClick = onNavigateToSales,
                        onSettingsClick = onNavigateToSettings
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    HomeHeader(title = "POS Menu")

                    if (isLandscape) {
                        LandscapeContent(uiState, homeViewModel) { selectedItemForPopup = it }
                    } else {
                        PortraitContent(uiState, homeViewModel) { selectedItemForPopup = it }
                    }
                }
            }

            selectedItemForPopup?.let { item ->
                ItemSelectionPopUp(
                    itemEntity = item,
                    onDismiss = { selectedItemForPopup = null },
                    onConfirm = { cartItem ->
                        cartViewModel.addToCart(cartItem)
                        selectedItemForPopup = null
                    }
                )
            }
        }
    }
}

@Composable
fun ItemSelectionPopUp(
    itemEntity: ItemEntity,
    onDismiss: () -> Unit,
    onConfirm: (CartItem) -> Unit
) {
    // 1. Selection States
    var isIceSelected by remember { mutableStateOf(false) }
    var isHotSelected by remember { mutableStateOf(true) }
    var isOatmilkSelected by remember { mutableStateOf(false) }
    var quantity by remember { mutableIntStateOf(1) } // Default to 1

    // 2. Pricing Logic
    val iceCharge = if (isIceSelected && itemEntity.itemType == "milk") 1.0 else 0.0
    val oatmilkCharge = if (isOatmilkSelected && itemEntity.itemType == "milk") 2.0 else 0.0

    val unitPrice = itemEntity.price + iceCharge + oatmilkCharge
    val totalPrice = unitPrice * quantity

    val dialogBackground = Color(0xFFFDF5E6) // Cream Background

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBackground,
        shape = RoundedCornerShape(28.dp),
        confirmButton = {
            Button(
                onClick = {
                    val selections = mutableListOf<String>()
                    selections.add(if (isIceSelected) "Ice" else "Hot")
                    if (isOatmilkSelected) selections.add("Oatmilk")

                    val cartItem = CartItem(
                        item = Item(
                            itemId = itemEntity.id,
                            itemName = "${itemEntity.name} (${selections.joinToString(", ")})",
                            itemPrice = unitPrice, // Save the unit price with options
                            categoryId = itemEntity.categoryId
                        ),
                        quantity = quantity // Pass the selected quantity
                    )
                    onConfirm(cartItem)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF5E6BE)),
                modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 32.dp),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text("OK", color = Color(0xFF4E342E), fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                text = itemEntity.name,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = Color(0xFF4E342E)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Selection List Area
                Column(
                    modifier = Modifier.width(240.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SelectionRow(
                        label = "Hot",
                        isSelected = isHotSelected,
                        onCheckedChange = {
                            isHotSelected = true
                            isIceSelected = false
                        }
                    )

                    SelectionRow(
                        label = "Ice",
                        priceLabel = if (itemEntity.itemType == "milk") "+ RM1" else null,
                        isSelected = isIceSelected,
                        onCheckedChange = {
                            isIceSelected = true
                            isHotSelected = false
                        }
                    )

                    if (itemEntity.itemType == "milk") {
                        SelectionRow(
                            label = "Oatmilk",
                            priceLabel = "+ RM2",
                            isSelected = isOatmilkSelected,
                            onCheckedChange = { isOatmilkSelected = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- QUANTITY ROW ---
                Row(
                    modifier = Modifier.width(200.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (quantity > 1) quantity-- },
                        modifier = Modifier.background(Color(0xFFE0E0E0), RoundedCornerShape(8.dp)).size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, tint = Color(0xFF4E342E))
                    }

                    Text(
                        text = quantity.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4E342E)
                    )

                    IconButton(
                        onClick = { quantity++ },
                        modifier = Modifier.background(Color(0xFFE0E0E0), RoundedCornerShape(8.dp)).size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF4E342E))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Total Price Display
                Text(
                    text = "RM ${String.format("%.2f", totalPrice)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFD2B48C)
                )
            }
        }
    )
}

@Composable
fun SelectionRow(
    label: String,
    isSelected: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    priceLabel: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isSelected) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Square Checkbox
        Box(
            modifier = Modifier
                .size(26.dp)
                .border(2.dp, Color(0xFF4E342E), RoundedCornerShape(2.dp))
                .background(if (isSelected) Color(0xFFD3D3D3) else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF4E342E)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = label,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4E342E),
            modifier = Modifier.weight(1f)
        )

        if (priceLabel != null) {
            Text(
                text = priceLabel,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF4E342E)
            )
        }
    }
}


// Complete the Sidebar which was cut off previously
@Composable
fun NavigationSidebar(onCartClick: () -> Unit, onSalesClick: () -> Unit, onSettingsClick: () -> Unit) {
    Column(
        modifier = Modifier.width(80.dp).fillMaxHeight().background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        IconButton(onClick = {}) {
            Icon(Icons.Default.Home, contentDescription = "Home", tint = Color(0xFFD2B48C))
        }
        IconButton(onClick = onCartClick) {
            Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color.Gray)
        }
        IconButton(onClick = onSalesClick) {
            Icon(Icons.Default.List, contentDescription = "Sales", tint = Color.Gray)
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Gray)
        }
    }
}


@Composable
fun SelectionSquare(label: String, isSelected: Boolean, subLabel: String? = null, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFFD2B48C).copy(alpha = 0.1f) else Color.Transparent)
            .border(2.dp, if (isSelected) Color(0xFFD2B48C) else Color.LightGray, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFFD2B48C) else Color.Black)
            if (subLabel != null) {
                Text(subLabel, fontSize = 10.sp, color = if (isSelected) Color(0xFFD2B48C) else Color.Gray)
            }
        }
    }
}

@Composable
fun POSItemCard(item: ItemEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.colfi),
                contentDescription = null,
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.width(16.dp))
            Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

@Composable
fun PortraitContent(uiState: HomeUiState, vm: HomeViewModel, onItemClick: (ItemEntity) -> Unit) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.width(90.dp).fillMaxHeight().background(Color.White).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            uiState.categories.forEach { cat ->
                CategoryItem(name = cat.name, isSelected = uiState.selectedCategoryId == cat.id, onClick = { vm.selectCategory(cat.id) })
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            ItemsGrid(items = uiState.items, columns = 1, onItemClick = onItemClick)
        }
    }
}

@Composable
fun LandscapeContent(uiState: HomeUiState, vm: HomeViewModel, onItemClick: (ItemEntity) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            uiState.categories.forEach { cat ->
                Text(
                    text = cat.name,
                    modifier = Modifier.clickable { vm.selectCategory(cat.id) },
                    color = if (uiState.selectedCategoryId == cat.id) Color(0xFFD2B48C) else Color.Gray,
                    fontWeight = FontWeight.Bold, fontSize = 18.sp
                )
            }
        }
        Box(modifier = Modifier.weight(1f)) {
            ItemsGrid(items = uiState.items, columns = 1, onItemClick = onItemClick)
        }
    }
}

@Composable
fun ItemsGrid(items: List<ItemEntity>, columns: Int, onItemClick: (ItemEntity) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item -> POSItemCard(item = item, onClick = { onItemClick(item) }) }
    }
}

@Composable
fun HomeHeader(title: String) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(text = "— COLFi —", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD2B48C))
    }
}

@Composable
fun CategoryItem(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = name, fontSize = 14.sp, color = if (isSelected) Color(0xFFD2B48C) else Color.Black, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
        if (isSelected) Box(Modifier.width(40.dp).height(2.dp).background(Color(0xFFD2B48C)))
    }
}
