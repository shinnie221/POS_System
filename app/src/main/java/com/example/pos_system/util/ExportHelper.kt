package com.example.pos_system.util

import android.content.Context
import com.example.pos_system.data.local.database.entity.SalesEntity
import com.example.pos_system.data.local.database.Converters
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.UnitValue
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ExportHelper(private val context: Context) {

    private fun getInternalCacheFile(fileName: String, suffix: String): File {
        val cachePath = File(context.cacheDir, "reports")
        if (!cachePath.exists()) cachePath.mkdirs()
        return File(cachePath, "$fileName$suffix")
    }

    private fun calculateAnalytics(currentSales: List<SalesEntity>, allSales: List<SalesEntity>, fileName: String): ReportData {
        val totalRevenue = currentSales.sumOf { it.totalAmount }
        var cash = 0.0; var eWallet = 0.0; var delivery = 0.0
        val dailyTotals = mutableMapOf<String, Double>()
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        currentSales.forEach { sale ->
            when (sale.paymentType.uppercase()) {
                "CASH" -> cash += sale.totalAmount
                "E-WALLET" -> eWallet += sale.totalAmount
                "DELIVERY" -> delivery += sale.totalAmount
            }
            val dateKey = df.format(Date(sale.timestamp))
            dailyTotals[dateKey] = dailyTotals.getOrDefault(dateKey, 0.0) + sale.totalAmount
        }

        var comparisonText = "N/A"
        if (currentSales.isNotEmpty()) {
            val cal = Calendar.getInstance().apply { timeInMillis = currentSales[0].timestamp }

            if (fileName.contains("Monthly", ignoreCase = true)) {
                val targetMonth = cal.get(Calendar.MONTH)
                val targetYear = cal.get(Calendar.YEAR)
                val prevCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, targetYear)
                    set(Calendar.MONTH, targetMonth)
                    add(Calendar.MONTH, -1)
                }
                val prevTotal = allSales.filter {
                    val sCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    sCal.get(Calendar.YEAR) == prevCal.get(Calendar.YEAR) &&
                            sCal.get(Calendar.MONTH) == prevCal.get(Calendar.MONTH)
                }.sumOf { it.totalAmount }

                if (prevTotal > 0) {
                    val diff = ((totalRevenue - prevTotal) / prevTotal) * 100
                    comparisonText = "${if (diff >= 0) "+" else ""}${String.format("%.1f", diff)}% vs Last Month"
                } else {
                    comparisonText = "No Last Month Data"
                }
            } else if (fileName.contains("Yearly", ignoreCase = true)) {
                val targetYear = cal.get(Calendar.YEAR)
                val prevYearTotal = allSales.filter {
                    val sCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                    sCal.get(Calendar.YEAR) == (targetYear - 1)
                }.sumOf { it.totalAmount }

                if (prevYearTotal > 0) {
                    val diff = ((totalRevenue - prevYearTotal) / prevYearTotal) * 100
                    comparisonText = "${if (diff >= 0) "+" else ""}${String.format("%.1f", diff)}% vs Last Year"
                } else {
                    comparisonText = "No Last Year Data"
                }
            }
        }

        return ReportData(
            revenue = totalRevenue,
            transactions = currentSales.size,
            cash = cash,
            eWallet = eWallet,
            delivery = delivery,
            comparison = comparisonText,
            peakDay = dailyTotals.maxByOrNull { it.value }?.key ?: "N/A"
        )
    }

    fun exportToExcel(currentSales: List<SalesEntity>, allSales: List<SalesEntity>, fileName: String): String? {
        return try {
            val data = calculateAnalytics(currentSales, allSales, fileName)
            val converter = Converters()

            val totalDiscount = currentSales.sumOf { sale ->
                val items = converter.toCartItemList(sale.itemsJson)
                val originalPrice = items.sumOf { it.item.itemPrice * it.quantity }
                (originalPrice - sale.totalAmount).coerceAtLeast(0.0)
            }

            val file = getInternalCacheFile(fileName, ".xlsx")
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Report")

            var r = 0
            sheet.createRow(r++).createCell(0).setCellValue("COLFI - $fileName")
            r++

            sheet.createRow(r++).createCell(0).setCellValue("1. SALES SUMMARY")
            sheet.createRow(r++).apply { createCell(0).setCellValue("Total Sales:"); createCell(1).setCellValue("RM ${String.format("%.2f", data.revenue)}") }
            sheet.createRow(r++).apply { createCell(0).setCellValue("Total Discount:"); createCell(1).setCellValue("RM ${String.format("%.2f", totalDiscount)}") }
            sheet.createRow(r++).apply { createCell(0).setCellValue("Transactions:"); createCell(1).setCellValue(data.transactions.toDouble()) }
            sheet.createRow(r++).apply { createCell(0).setCellValue("Comparison:"); createCell(1).setCellValue(data.comparison) }
            sheet.createRow(r++).apply { createCell(0).setCellValue("Peak Sales Day:"); createCell(1).setCellValue(data.peakDay) }
            r++

            sheet.createRow(r++).createCell(0).setCellValue("2. PAYMENT BREAKDOWN")
            sheet.createRow(r++).apply { createCell(0).setCellValue("Cash:"); createCell(1).setCellValue("RM ${String.format("%.2f", data.cash)}") }
            sheet.createRow(r++).apply { createCell(0).setCellValue("E-Wallet:"); createCell(1).setCellValue("RM ${String.format("%.2f", data.eWallet)}") }
            sheet.createRow(r++).apply { createCell(0).setCellValue("Delivery:"); createCell(1).setCellValue("RM ${String.format("%.2f", data.delivery)}") }
            r++

            sheet.createRow(r++).createCell(0).setCellValue("3. TRANSACTION DETAILS")
            val headerRow = sheet.createRow(r++)
            headerRow.createCell(0).setCellValue("Order ID")
            headerRow.createCell(1).setCellValue("Payment")
            headerRow.createCell(2).setCellValue("Amount (RM)")
            headerRow.createCell(3).setCellValue("Date")

            val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            currentSales.forEach { sale ->
                val row = sheet.createRow(r++)
                row.createCell(0).setCellValue(sale.id.takeLast(6).uppercase())
                row.createCell(1).setCellValue(sale.paymentType)
                row.createCell(2).setCellValue(sale.totalAmount)
                row.createCell(3).setCellValue(df.format(Date(sale.timestamp)))
            }

            FileOutputStream(file).use { workbook.write(it) }
            workbook.close()
            file.absolutePath
        } catch (e: Exception) { null }
    }

    fun exportToPdf(currentSales: List<SalesEntity>, allSales: List<SalesEntity>, fileName: String): String? {
        return try {
            val data = calculateAnalytics(currentSales, allSales, fileName)
            val converter = Converters()

            val totalDiscount = currentSales.sumOf { sale ->
                val items = converter.toCartItemList(sale.itemsJson)
                val originalPrice = items.sumOf { it.item.itemPrice * it.quantity }
                (originalPrice - sale.totalAmount).coerceAtLeast(0.0)
            }

            val file = getInternalCacheFile(fileName, ".pdf")
            val writer = PdfWriter(FileOutputStream(file))
            val pdf = PdfDocument(writer)
            val document = Document(pdf)

            document.add(Paragraph("COLFI - $fileName").setBold().setFontSize(16f).setTextAlignment(TextAlignment.CENTER))
            document.add(Paragraph("Generated on: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}").setFontSize(10f).setTextAlignment(TextAlignment.CENTER))

            document.add(Paragraph("\n1. SALES SUMMARY").setBold())
            document.add(Paragraph("Total Sales: RM ${String.format("%.2f", data.revenue)}"))
            document.add(Paragraph("Total Discount: RM ${String.format("%.2f", totalDiscount)}"))
            document.add(Paragraph("Transactions: ${data.transactions}"))
            document.add(Paragraph("Comparison: ${data.comparison}"))
            document.add(Paragraph("Peak Sales Day: ${data.peakDay}"))

            document.add(Paragraph("\n2. PAYMENT BREAKDOWN").setBold())
            val pTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
            pTable.addCell("Cash")
            pTable.addCell("RM ${String.format("%.2f", data.cash)}")
            pTable.addCell("E-Wallet")
            pTable.addCell("RM ${String.format("%.2f", data.eWallet)}")
            pTable.addCell("Delivery")
            pTable.addCell("RM ${String.format("%.2f", data.delivery)}")
            document.add(pTable)

            document.add(Paragraph("\n3. TRANSACTION DETAILS").setBold())
            val table = Table(UnitValue.createPercentArray(floatArrayOf(2f, 2f, 2f, 3f))).useAllAvailableWidth()
            table.addHeaderCell("ID")
            table.addHeaderCell("Payment")
            table.addHeaderCell("Price")
            table.addHeaderCell("Date")

            // Updated date format to include dd/MM/yyyy (Full Year)
            val df = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            currentSales.forEach { sale ->
                table.addCell(sale.id.takeLast(4).uppercase())
                table.addCell(sale.paymentType)
                table.addCell("RM ${String.format("%.2f", sale.totalAmount)}")
                table.addCell(df.format(Date(sale.timestamp)))
            }
            document.add(table)

            document.close()
            file.absolutePath
        } catch (e: Exception) { null }
    }

    private data class ReportData(
        val revenue: Double,
        val transactions: Int,
        val cash: Double,
        val eWallet: Double,
        val delivery: Double,
        val comparison: String,
        val peakDay: String
    )
}