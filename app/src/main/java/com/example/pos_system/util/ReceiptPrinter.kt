package com.example.pos_system.util

import android.content.Context
import android.widget.Toast
import com.example.pos_system.data.local.database.entity.SalesEntity
import com.example.pos_system.data.local.database.Converters
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import java.text.SimpleDateFormat
import java.util.*

class ReceiptPrinter(private val context: Context) {

    fun printSale(sale: SalesEntity) {
        try {
            val connection = BluetoothPrintersConnections.selectFirstPaired()

            if (connection == null) {
                Toast.makeText(context, "No paired printer found!", Toast.LENGTH_SHORT).show()
                return
            }

            val items = Converters().toCartItemList(sale.itemsJson)
            val createdDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(sale.timestamp))
            val printedDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())

            // Calculate Original Total (Subtotal)
            val subtotal = items.sumOf { it.item.itemPrice * it.quantity }

            // Initialize Printer: 203 DPI, 48mm width, 32 characters
            val printer = EscPosPrinter(connection, 203, 48f, 32)

            var receiptText =
                "[C]<b><font size='big'>COLFI</font></b>\n\n" +      // Row 1: Shop Name
                        "[C]<font size='small'>G-07, Wisma New Asia, \nJalan Raja Chulan, Bukit Ceylon,"+ // Row 2: Address
                        "[C]50200 Kuala Lumpur</font>\n" +
                        "[L]Order No: #${sale.id}\n" +                     // Row 3: Order Number
                        "[L]Created: $createdDate\n" +                    // Row 4: Created Date
                        "[L]Printed: $printedDate\n" +                    // Row 5: Printed Date
                        "[L]\n" +                                         // Row 6: Blank
                        "[L]<b>Product</b> [C]<b>Qty</b> [R]<b>Price</b>\n" + // Row 7: Header
                        "[C]--------------------------------\n"

            // Row 8: Item List
            items.forEach { cartItem ->
                // 1. Separate the Base Name from the Modifiers
                // Example: "Nutty White (Hot, Oatmilk)" -> Base: "Nutty White", Modifiers: "Hot", "Oatmilk"
                val rawName = cartItem.item.itemName
                val baseName: String
                val modifiers: List<String>
                if (rawName.contains("(") && rawName.contains(")")) {
                baseName = rawName.substringBefore(" (")
                modifiers = rawName.substringAfter("(").substringBefore(")").split(", ")
            } else {
                baseName = rawName
                modifiers = emptyList()
            }

                // 2. Print the Base Product Name (Left) and Price (Right)
                val displayName = if (baseName.length > 16) baseName.substring(0, 14) + ".." else baseName

                receiptText += "[L]${displayName} [C]${cartItem.quantity} [R]${String.format("%.2f", cartItem.totalPrice)}\n"

        // 3. Print Modifiers on new rows (Indented below the name)
                modifiers.forEach { modifier ->
                    receiptText += "[L]  + $modifier\n"
                }
            }


            receiptText +=
                "[L]\n" +                                         // Row 9: Blank
                        "[L]Subtotal [R]RM ${String.format("%.2f", subtotal)}\n" + // Row 10: Subtotal
                        "[L]\n" +                                         // Row 11: Blank
                        "[L]<b>Amount paid</b> [R]<b>RM ${String.format("%.2f", sale.totalAmount)}</b>\n" + // Row 12: Final
                        "[C]--------------------------------\n" +
                        "[C]<font size='small'>CNA ENTERPRISE (748393039)</font>\n" + // Row 13: Business Info
                        "[L]\n" +
                        "[L]\n"

            printer.printFormattedTextAndCut(receiptText)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Printer Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}