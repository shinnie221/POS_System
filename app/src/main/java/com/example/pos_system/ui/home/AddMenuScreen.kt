package com.example.pos_system.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pos_system.data.local.database.entity.CategoryEntity
import com.example.pos_system.data.local.database.entity.ItemEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMenuScreen(
    onBack: () -> Unit,
    viewModel: AddMenuViewModel = viewModel()
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState(initial = emptyList())
    val items by viewModel.items.collectAsState(initial = emptyList())

    // State for Deletion Dialogs
    var itemToDelete by remember { mutableStateOf<ItemEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Menu Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFFDF8F3))
        ) {
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = Color.White,
                contentColor = Color(0xFFD2B48C),
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                        color = Color(0xFFD2B48C)
                    )
                }
            ) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    text = { Text("Items", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    text = { Text("Categories", fontWeight = FontWeight.Bold) }
                )
            }

            if (tabIndex == 0) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("Add New Product", style = MaterialTheme.typography.titleMedium, color = Color(0xFF4E342E))
                        Spacer(Modifier.height(8.dp))
                        AddItemForm(
                            categories = categories,
                            uiState = uiState,
                            viewModel = viewModel,
                            onAdd = { n, p, c, t -> viewModel.addItem(n, p, c, t) }
                        )
                        Spacer(Modifier.height(32.dp))
                        Text("Manage Items", style = MaterialTheme.typography.titleMedium, color = Color(0xFF4E342E))
                        Spacer(Modifier.height(8.dp))
                    }

                    // Display Items grouped by Categories (Sorted by createdAt ASC)
                    items(categories) { category ->
                        val categoryItems = items.filter { it.categoryId == category.id }
                        ExpandableCategoryItem(
                            categoryName = category.name,
                            items = categoryItems,
                            onDeleteItem = { item -> itemToDelete = item }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("Add New Category", style = MaterialTheme.typography.titleMedium, color = Color(0xFF4E342E))
                        Spacer(Modifier.height(8.dp))
                        AddCategoryForm(uiState) { name -> viewModel.addCategory(name) }
                        Spacer(Modifier.height(32.dp))
                        Text("All Categories", style = MaterialTheme.typography.titleMedium, color = Color(0xFF4E342E))
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    }

                    items(categories) { category ->
                        ListItem(
                            headlineContent = { Text(category.name, fontWeight = FontWeight.SemiBold) },
                            trailingContent = {
                                IconButton(onClick = { categoryToDelete = category }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    }
                }
            }
        }

        // --- ITEM DELETE DIALOG ---
        itemToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
                title = { Text("Delete Item") },
                text = { Text("Are you sureeeeee you want delete '${item.name}'? This action cannot be undo ohh.\n三思而后行") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteItem(item)
                            itemToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("Delete", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { itemToDelete = null }) { Text("Cancel") }
                }
            )
        }

        // --- CATEGORY DELETE DIALOG --- for
        categoryToDelete?.let { category ->
            AlertDialog(
                onDismissRequest = { categoryToDelete = null },
                title = { Text("Delete Category") },
                text = { Text("Are u sureeeee u want to delete '${category.name}'? All items inside this category will also be deleted. \n三思而后行") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteCategory(category.id)
                            categoryToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("Delete", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { categoryToDelete = null }) { Text("Cancel") }
                }
            )
        }
    }

    if (uiState is AddMenuUiState.Success) {
        LaunchedEffect(uiState) {
            viewModel.resetState()
        }
    }
}

@Composable
fun ExpandableCategoryItem(
    categoryName: String,
    items: List<ItemEntity>,
    onDeleteItem: (ItemEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = categoryName.uppercase(),
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFD2B48C),
                fontSize = 14.sp
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.Gray
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                if (items.isEmpty()) {
                    Text("No items in this category", modifier = Modifier.padding(16.dp), fontSize = 12.sp, color = Color.Gray)
                } else {
                    items.forEach { item ->
                        ListItem(
                            headlineContent = { Text(item.name, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { Text("RM ${String.format("%.2f", item.price)} | Type: ${item.itemType}") },
                            trailingContent = {
                                IconButton(onClick = { onDeleteItem(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddItemForm(
    categories: List<CategoryEntity>,
    uiState: AddMenuUiState,
    viewModel: AddMenuViewModel,
    onAdd: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    val priceError by viewModel.priceError.collectAsState()
    var selectedType by remember { mutableStateOf("water") }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    LaunchedEffect(uiState) {
        if (uiState is AddMenuUiState.Success) {
            name = ""
            price = ""
            selectedCategory = null
            selectedType = "water"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = price,
                onValueChange = {
                    if (it.isEmpty() || it.matches(Regex("""^\d*\.?\d*$"""))) {
                        price = it
                    }
                },
                label = { Text("Price (RM)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = priceError != null,
                supportingText = {
                    if (priceError != null) {
                        Text(text = priceError!!, color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                )
            )

            Text("Item Base Type:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = selectedType == "water",
                    onClick = { selectedType = "water" },
                    label = { Text("Water Based") }
                )
                Spacer(Modifier.width(12.dp))
                FilterChip(
                    selected = selectedType == "milk",
                    onClick = { selectedType = "milk" },
                    label = { Text("Milk Based") }
                )
            }

            Text("Category:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            CategoryDropdown(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            Button(
                onClick = { onAdd(name, price, selectedCategory?.id ?: "", selectedType) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD2B48C)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save Product", color = Color.White)
            }
        }
    }
}

@Composable
fun CategoryDropdown(
    categories: List<CategoryEntity>,
    selectedCategory: CategoryEntity?,
    onCategorySelected: (CategoryEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(selectedCategory?.name ?: "Select Category")
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowDown, null)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AddCategoryForm(uiState: AddMenuUiState, onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is AddMenuUiState.Success) name = ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Category Name") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = { if (name.isNotBlank()) onAdd(name) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD2B48C)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Add")
            }
        }
    }
}