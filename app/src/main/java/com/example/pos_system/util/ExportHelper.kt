package com.example.pos_system.util

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class ExportHelper(private val context: Context) {

    private fun createOutputStream(fileName: String, mimeType: String): OutputStream? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)?.let { uri ->
                resolver.openOutputStream(uri)
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file)
        }
    }

    fun exportToExcel(sales: List<SalesEntity>, fileName: String): String? {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Sales Report")

        // Create header row
        val headerRow = sheet.createRow(0)
        headerRow.createCell(0).setCellValue("Order ID")
        headerRow.createCell(1).setCellValue("Timestamp")
        headerRow.createCell(2).setCellValue("Total Amount (RM)")
        headerRow.createCell(3).setCellValue("Items")

        // Populate data rows
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sales.forEachIndexed { index, sale ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(sale.id.toString())
            row.createCell(1).setCellValue(dateFormat.format(Date(sale.timestamp)))
            row.createCell(2).setCellValue(String.format("%.2f", sale.totalAmount))
            row.createCell(3).setCellValue(sale.itemsJson) // You might want to format this better
        }

        return try {
            val outputStream = createOutputStream("$fileName.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            outputStream?.use {
                workbook.write(it)
            }
            workbook.close()
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun exportToPdf(sales: List<SalesEntity>, fileName: String): String? {
        return try {
            val outputStream = createOutputStream("$fileName.pdf", "application/pdf")
            outputStream?.use {
                val writer = PdfWriter(it)
                val pdf = PdfDocument(writer)
                val document = Document(pdf)

                // Title
                document.add(Paragraph("Sales Report").setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(18f))
                document.add(Paragraph(SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())).setTextAlignment(TextAlignment.CENTER).setFontSize(10f))

                // Table
                val table = Table(UnitValue.createPercentArray(floatArrayOf(1f, 3f, 2f)))
                    .useAllAvailableWidth()
                    .setMarginTop(20f)

                // Table Headers
                table.addHeaderCell(Paragraph("ID").setBold())
                table.addHeaderCell(Paragraph("Timestamp").setBold())
                table.addHeaderCell(Paragraph("Amount (RM)").setBold().setTextAlignment(TextAlignment.RIGHT))

                // Table Body
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                sales.forEach { sale ->
                    table.addCell(Paragraph(sale.id.toString()))
                    table.addCell(Paragraph(dateFormat.format(Date(sale.timestamp))))
                    table.addCell(Paragraph(String.format("%.2f", sale.totalAmount)).setTextAlignment(TextAlignment.RIGHT))
                }

                document.add(table)
                document.close()
            }
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
