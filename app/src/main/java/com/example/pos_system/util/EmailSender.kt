package com.example.pos_system.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import javax.activation.DataHandler
import javax.activation.FileDataSource
import javax.mail.*
import javax.mail.internet.*

object EmailSender {

    // IMPORTANT: Use Google "App Passwords", not your actual login password.
    // Go to: Google Account -> Security -> 2-Step Verification -> App Passwords
    private const val SENDER_EMAIL = "shinniecheng221@gmail.com"
    private const val SENDER_PASSWORD = "wlxftdtslsbowvnk" // Your verified 16-character App Password

    /**
     * Sends an email with the report attached.
     * @param toEmail The recipient's email address.
     * @param file The report file (PDF or Excel).
     * @param subjectTitle The formatted subject (e.g., "Colfi Monthly Sales Report Oct 2024").
     */
    suspend fun sendEmailWithAttachment(
        toEmail: String,      // The main person (Visible)
        file: File,
        subjectTitle: String
    ): Boolean = withContext(Dispatchers.IO) {
        val props = Properties().apply {
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.socketFactory.port", "465")
            put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
            put("mail.smtp.auth", "true")
            put("mail.smtp.port", "465")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
            }
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(SENDER_EMAIL))

                // 1. Set the Main Recipient (Visible to everyone)
                setRecipient(Message.RecipientType.TO, InternetAddress(toEmail))

                subject = subjectTitle
            }

            // 1. Create the text body part
            val messageBodyPart = MimeBodyPart().apply {
                setText("Please find the attached sales report: ${file.name}\n\nGenerated from Colfi POS System.")
            }

            // 2. Create the attachment part
            val attachmentPart = MimeBodyPart().apply {
                val source = FileDataSource(file)
                dataHandler = DataHandler(source)
                // Use the clean filename generated in SalesReportsScreen
                fileName = file.name
            }

            // 3. Combine body and attachment
            val multipart = MimeMultipart().apply {
                addBodyPart(messageBodyPart)
                addBodyPart(attachmentPart)
            }

            message.setContent(multipart)

            // 4. Send the email
            Transport.send(message)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}