package com.example.pos_system.ui.sales // Package name should match your folder structure

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pos_system.R
import com.example.pos_system.data.local.database.Converters
import com.example.pos_system.data.local.database.entity.SalesEntity
import com.example.pos_system.ui.salesimport.ReportType
import com.example.pos_system.ui.salesimport.SalesReportsViewModel
import com.example.pos_system.util.ExportHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportsScreen(
    onBack: () -> Unit,
    viewModel: SalesReportsViewModel = viewModel()
) {
    val context = LocalContext.current
    val exportHelper = remember { ExportHelper(context) }

    val reportType by viewModel.reportType.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val sales by viewModel.filteredSales.collectAsState()

    var selectedOrderForDetail by remember { mutableStateOf<SalesEntity?>(null) }
    var saleToDelete by remember { mutableStateOf<SalesEntity?>(null) }
    var showMonthMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val years = (2024..2026).toList()

    // Corrected sumOf import logic
    val totalRevenue = sales.sumOf { it.totalAmount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Reports", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    // Export Actions
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                    DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Export to Excel (.xlsx)") },
                            onClick = {
                                showExportMenu = false
                                val path = exportHelper.exportToExcel(sales, "Sales_Report_${System.currentTimeMillis()}")
                                Toast.makeText(context, if (path != null) "Saved to Downloads" else "Export Failed", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export to PDF (.pdf)") },
                            onClick = {
                                showExportMenu = false
                                val path = exportHelper.exportToPdf(sales, "Sales_Report_${System.currentTimeMillis()}")
                                Toast.makeText(context, if (path != null) "Saved to Downloads" else "Export Failed", Toast.LENGTH_SHORT).show()
                            }
                        )
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
            // Tabs
            TabRow(selectedTabIndex = reportType.ordinal, contentColor = Color(0xFFD2B48C)) {
                ReportType.entries.forEach { type ->
                    Tab(
                        selected = reportType == type,
                        onClick = { viewModel.setReportType(type) },
                        text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            // Dropdowns for Monthly/Yearly
            if (reportType != ReportType.DAILY) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (reportType == ReportType.MONTHLY) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(onClick = { showMonthMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(months[selectedMonth])
                                Icon(Icons.Default.KeyboardArrowDown, null)
                            }
                            DropdownMenu(expanded = showMonthMenu, onDismissRequest = { showMonthMenu = false }) {
                                months.forEachIndexed { index, name ->
                                    DropdownMenuItem(text = { Text(name) }, onClick = { viewModel.setMonth(index); showMonthMenu = false })
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(onClick = { showYearMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedYear.toString())
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                        DropdownMenu(expanded = showYearMenu, onDismissRequest = { showYearMenu = false }) {
                            years.forEach { year ->
                                DropdownMenuItem(text = { Text(year.toString()) }, onClick = { viewModel.setYear(year); showYearMenu = false })
                            }
                        }
                    }
                }
            }

            // Revenue Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD2B48C)),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL REVENUE", color = Color.White.copy(0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("RM ${String.format("%.2f", totalRevenue)}", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            // List
            if (sales.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sales found", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(sales) { index, sale ->
                        // FIXED: Use java.util.Date
                        val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(sale.timestamp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOrderForDetail = sale },
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            ListItem(
                                headlineContent = { Text("Order #${String.format("%02d", sales.size - index)}", fontWeight = FontWeight.Bold) },
                                supportingContent = { Text(timeString) },
                                trailingContent = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("RM ${String.format("%.2f", sale.totalAmount)}", color = Color(0xFFD2B48C), fontWeight = FontWeight.Bold)
                                        if (reportType == ReportType.DAILY) {
                                            IconButton(onClick = { saleToDelete = sale }) {
                                                Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Dialogs
        selectedOrderForDetail?.let { OrderDetailDialog(it) { selectedOrderForDetail = null } }

        saleToDelete?.let { sale ->
            AlertDialog(
                onDismissRequest = { saleToDelete = null },
                title = { Text("Delete Sale") },
                text = { Text("Delete this transaction permanently?") },
                confirmButton = {
                    Button(onClick = { viewModel.deleteSale(sale); saleToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                        Text("Delete")
                    }
                },
                dismissButton = { TextButton(onClick = { saleToDelete = null }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
fun OrderDetailDialog(sale: SalesEntity, onDismiss: () -> Unit) {
    val items = Converters().toCartItemList(sale.itemsJson)
    val dateStr = SimpleDateFormat("yyyy年M月d日 'UTC+8' HH:mm:ss", Locale.CHINESE).format(Date(sale.timestamp))

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = Color(0xFFD2B48C)) } },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.receipt), null, Modifier.size(24.dp), Color(0xFFD2B48C))
                Spacer(Modifier.width(8.dp))
                Text("Order Detail")
            }
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(dateStr, fontSize = 12.sp, color = Color.Gray)
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                items.forEach { cartItem ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${cartItem.quantity}x ${cartItem.item.itemName}", Modifier.weight(1f), fontSize = 14.sp)
                        Text("RM ${String.format("%.2f", cartItem.totalPrice)}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Paid", fontWeight = FontWeight.Bold)
                    Text("RM ${String.format("%.2f", sale.totalAmount)}", fontWeight = FontWeight.ExtraBold, color = Color(0xFFD2B48C))
                }
            }
        }
    )
}