package com.example.pos_system.ui.report

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.pos_system.data.local.database.Converters
import com.example.pos_system.data.local.database.entity.SalesEntity
import com.example.pos_system.util.ExportHelper
import com.example.pos_system.util.ReceiptPrinter
import com.google.firebase.auth.FirebaseAuth
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportsScreen(
    onBack: () -> Unit,
    viewModel: SalesReportsViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUserEmail = remember { FirebaseAuth.getInstance().currentUser?.email }
    val exportHelper = remember { ExportHelper(context) }
    val receiptPrinter = remember { ReceiptPrinter(context) }

    val reportType by viewModel.reportType.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val paymentFilter by viewModel.paymentFilter.collectAsState()
    val sales by viewModel.filteredSales.collectAsState()
    val allSalesForComparison by viewModel.allSales.collectAsState()
    val summary by viewModel.reportSummary.collectAsState()

    var selectedOrderForDetail by remember { mutableStateOf<SalesEntity?>(null) }
    var saleToDelete by remember { mutableStateOf<SalesEntity?>(null) }
    var showMonthMenu by remember { mutableStateOf(false) }
    var showYearMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showPaymentMenu by remember { mutableStateOf(false) }

    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val years = (2024..2026).toList()
    val paymentOptions = listOf("All", "Cash", "E-Wallet", "Delivery")

    fun handleExportAndEmail(format: String) {
        showExportMenu = false
        val reportName = when (reportType) {
            ReportType.DAILY -> "daily sales report (${SimpleDateFormat("d-M-yyyy", Locale.getDefault()).format(Date())})"
            ReportType.MONTHLY -> "monthly sales report (${months[selectedMonth]} $selectedYear)"
            ReportType.YEARLY -> "yearly sales report ($selectedYear)"
        }

        val filePath = if (format == "Excel") {
            exportHelper.exportToExcel(sales, allSalesForComparison, reportName)
        } else {
            exportHelper.exportToPdf(sales, allSalesForComparison, reportName)
        }

        if (filePath != null) {
            sendEmailWithFile(context, File(filePath), currentUserEmail)
        } else {
            Toast.makeText(context, "Export Failed", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Reports", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { showExportMenu = true }) { Icon(Icons.Default.Email, null) }
                    DropdownMenu(expanded = showExportMenu, onDismissRequest = { showExportMenu = false }) {
                        DropdownMenuItem(text = { Text("Email Excel (.xlsx)") }, onClick = { handleExportAndEmail("Excel") })
                        DropdownMenuItem(text = { Text("Email PDF (.pdf)") }, onClick = { handleExportAndEmail("PDF") })
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .background(Color(0xFFFDF8F3))) {

            TabRow(selectedTabIndex = reportType.ordinal, contentColor = Color(0xFFD2B48C)) {
                ReportType.entries.forEach { type ->
                    Tab(
                        selected = reportType == type,
                        onClick = { viewModel.setReportType(type) },
                        text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (reportType == ReportType.MONTHLY) {
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { showMonthMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(months[selectedMonth], fontSize = 11.sp)
                            Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(14.dp))
                        }
                        DropdownMenu(expanded = showMonthMenu, onDismissRequest = { showMonthMenu = false }) {
                            months.forEachIndexed { i, n -> DropdownMenuItem(text = { Text(n) }, onClick = { viewModel.setMonth(i); showMonthMenu = false }) }
                        }
                    }
                }
                if (reportType != ReportType.DAILY) {
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { showYearMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedYear.toString(), fontSize = 11.sp)
                            Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(14.dp))
                        }
                        DropdownMenu(expanded = showYearMenu, onDismissRequest = { showYearMenu = false }) {
                            years.forEach { y -> DropdownMenuItem(text = { Text(y.toString()) }, onClick = { viewModel.setYear(y); showYearMenu = false }) }
                        }
                    }
                }
                Box(Modifier.weight(1f)) {
                    OutlinedButton(onClick = { showPaymentMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(paymentFilter, fontSize = 11.sp)
                        Icon(Icons.Default.FilterList, null, Modifier.size(14.dp))
                    }
                    DropdownMenu(expanded = showPaymentMenu, onDismissRequest = { showPaymentMenu = false }) {
                        paymentOptions.forEach { o -> DropdownMenuItem(text = { Text(o) }, onClick = { viewModel.setPaymentFilter(o); showPaymentMenu = false }) }
                    }
                }
            }

            Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFD2B48C))) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TOTAL REVENUE", color = Color.White.copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("RM ${String.format("%.2f", summary.totalRevenue)}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${summary.totalTransactions} Orders", color = Color.White.copy(0.9f), fontSize = 14.sp)
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                itemsIndexed(sales) { index, sale ->
                    val dateStr = SimpleDateFormat("d/M/yyyy", Locale.getDefault()).format(Date(sale.timestamp))
                    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(sale.timestamp))
                    Card(
                        Modifier.padding(vertical = 4.dp).fillMaxWidth().clickable { selectedOrderForDetail = sale },
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        ListItem(
                            headlineContent = { Text("Order #${String.format("%02d", sales.size - index)}", fontWeight = FontWeight.Bold) },
                            supportingContent = {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (reportType != ReportType.DAILY) {
                                            Text(dateStr, fontSize = 12.sp, color = Color.Gray)
                                            Text(" | ", fontSize = 12.sp, color = Color.LightGray)
                                        }
                                        Text(timeStr, fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Text(sale.paymentType, fontSize = 11.sp, color = Color(0xFFD2B48C), fontWeight = FontWeight.Bold)
                                }
                            },
                            trailingContent = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("RM ${String.format("%.2f", sale.totalAmount)}", fontWeight = FontWeight.Bold, color = Color(0xFFD2B48C))
                                    if (reportType == ReportType.DAILY) {
                                        IconButton(onClick = { saleToDelete = sale }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        selectedOrderForDetail?.let { sale -> OrderDetailDialog(sale, { selectedOrderForDetail = null }, { receiptPrinter.printSale(it) }) }
        saleToDelete?.let { sale ->
            AlertDialog(
                onDismissRequest = { saleToDelete = null },
                title = { Text("Delete Sale") },
                text = { Text("Permanently delete this order?") },
                confirmButton = { Button(onClick = { viewModel.deleteSale(sale); saleToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Delete") } },
                dismissButton = { TextButton(onClick = { saleToDelete = null }) { Text("Cancel") } }
            )
        }
    }
}

@Composable
fun OrderDetailDialog(sale: SalesEntity, onDismiss: () -> Unit, onPrint: (SalesEntity) -> Unit) {
    val items = Converters().toCartItemList(sale.itemsJson)
    val original = items.sumOf { it.item.itemPrice * it.quantity }
    val finalAmount = sale.totalAmount
    val discount = original - finalAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row {
                TextButton(onClick = { onPrint(sale) }) { Icon(Icons.Default.Print, null, Modifier.size(18.dp)); Text(" Print") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        },
        title = { Text("Order Detail") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(SimpleDateFormat("d/M/yyyy HH:mm:ss", Locale.getDefault()).format(Date(sale.timestamp)), fontSize = 12.sp, color = Color.Gray)
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                items.forEach { Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("${it.quantity}x ${it.item.itemName}", Modifier.weight(1f), fontSize = 14.sp)
                    Text("RM ${String.format("%.2f", it.totalPrice)}", fontSize = 14.sp)
                } }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))

                PriceRow("Original Total", original)
                if (discount > 0.01) {
                    PriceRow("Total Discount", -discount, Color.Red)
                }
                PriceRow("Payment Method", 0.0, label2 = sale.paymentType)

                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Final Price", fontWeight = FontWeight.Bold)
                    Text("RM ${String.format("%.2f", finalAmount)}", color = Color(0xFFD2B48C), fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}

@Composable
fun PriceRow(label: String, amount: Double, color: Color = Color.Black, label2: String? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = Color.Gray)
        Text(label2 ?: (if (amount < 0) "- RM ${String.format("%.2f", -amount)}" else "RM ${String.format("%.2f", amount)}"), fontSize = 13.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

fun sendEmailWithFile(context: Context, file: File, recipientEmail: String?) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            if (!recipientEmail.isNullOrEmpty()) {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
            }
            putExtra(Intent.EXTRA_SUBJECT, "Sales Report: ${file.name}")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Send Email"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}