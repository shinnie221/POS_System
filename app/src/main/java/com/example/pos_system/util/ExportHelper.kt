package com.example.pos_system.util

import android.content.Context
import com.example.pos_system.data.local.database.Converters
import com.example.pos_system.data.local.database.entity.SalesEntity
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
        if (fileName.lowercase().contains("monthly") && currentSales.isNotEmpty()) {
            val cal = Calendar.getInstance().apply { timeInMillis = currentSales[0].timestamp }
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)
            cal.add(Calendar.MONTH, -1)
            val prevMonth = cal.get(Calendar.MONTH)
            val prevYear = cal.get(Calendar.YEAR)

            val lastMonthTotal = allSales.filter {
                val sCal = Calendar.getInstance().apply { timeInMillis = it.timestamp }
                sCal.get(Calendar.MONTH) == prevMonth && sCal.get(Calendar.YEAR) == prevYear
            }.sumOf { it.totalAmount }

            if (lastMonthTotal > 0) {
                val diff = ((totalRevenue - lastMonthTotal) / lastMonthTotal) * 100
                comparisonText = "${if (diff >= 0) "+" else ""}${String.format("%.1f", diff)}% vs Last Month"
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
            val file = getInternalCacheFile(fileName, ".xlsx")
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("Summary")

            var r = 0
            sheet.createRow(r++).createCell(0).setCellValue("COLFI - $fileName")
            r++
            sheet.createRow(r++).createCell(0).setCellValue("1. SALES SUMMARY")
            sheet.createRow(r++).apply { createCell(0).setCellValue("Total Sales:"); createCell(1).setCellValue("RM ${String.format("%.2f", data.revenue)}") }
            sheet.createRow(r++).apply { createCell(0).setCellValue("Transactions:"); createCell(1).setCellValue(data.transactions.toDouble()) }
            if (fileName.lowercase().contains("monthly") || fileName.lowercase().contains("yearly")) {
                sheet.createRow(r++).apply { createCell(0).setCellValue("Sales Comparison:"); createCell(1).setCellValue(data.comparison) }
                sheet.createRow(r++).apply { createCell(0).setCellValue("Peak Day:"); createCell(1).setCellValue(data.peakDay) }
            }
            r++
            sheet.createRow(r++).createCell(0).setCellValue("2. PAYMENT BREAKDOWN")
            sheet.createRow(r++).apply { createCell(0).setCellValue("Cash:"); createCell(1).setCellValue("RM ${String.format("%.2f", data.cash)}") }
            sheet.createRow(r++).apply { createCell(0).setCellValue("E-Wallet:"); createCell(1).setCellValue("RM ${String.format("%.2f", data.eWallet)}") }
            sheet.createRow(r++).apply { createCell(0).setCellValue("Delivery:"); createCell(1).setCellValue("RM ${String.format("%.2f", data.delivery)}") }

            FileOutputStream(file).use { workbook.write(it) }
            workbook.close()
            file.absolutePath
        } catch (e: Exception) { null }
    }

    fun exportToPdf(currentSales: List<SalesEntity>, allSales: List<SalesEntity>, fileName: String): String? {
        return try {
            val data = calculateAnalytics(currentSales, allSales, fileName)
            val file = getInternalCacheFile(fileName, ".pdf")
            val writer = PdfWriter(FileOutputStream(file))
            val pdf = PdfDocument(writer)
            val document = Document(pdf)

            document.add(Paragraph("COLFI - $fileName").setBold().setFontSize(16f).setTextAlignment(TextAlignment.CENTER))
            document.add(Paragraph("Generated: ${SimpleDateFormat("dd/MM/yyyy HH:mm").format(Date())}").setFontSize(9f).setTextAlignment(TextAlignment.CENTER))

            document.add(Paragraph("\n1. SALES SUMMARY").setBold())
            document.add(Paragraph("Total Sales: RM ${String.format("%.2f", data.revenue)}"))
            document.add(Paragraph("Transactions: ${data.transactions}"))
            if (fileName.lowercase().contains("monthly") || fileName.lowercase().contains("yearly")) {
                document.add(Paragraph("Sales Comparison: ${data.comparison}"))
                document.add(Paragraph("Peak Day: ${data.peakDay}"))
            }

            document.add(Paragraph("\n2. PAYMENT BREAKDOWN").setBold())
            val pTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f))).useAllAvailableWidth()
            pTable.addCell("Cash").addCell("RM ${String.format("%.2f", data.cash)}")
            pTable.addCell("E-Wallet").addCell("RM ${String.format("%.2f", data.eWallet)}")
            pTable.addCell("Delivery").addCell("RM ${String.format("%.2f", data.delivery)}")
            document.add(pTable)

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