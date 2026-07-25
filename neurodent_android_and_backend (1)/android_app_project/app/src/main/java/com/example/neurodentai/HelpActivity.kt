package com.example.neurodentai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)
        supportActionBar?.hide()

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val btnHelp = findViewById<Button>(R.id.btnHelp)

        backButton.setOnClickListener { finish() }

        btnHelp.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:") // restricts to email clients
                putExtra(Intent.EXTRA_EMAIL, arrayOf("support@neurodentai.com"))
                putExtra(Intent.EXTRA_SUBJECT, "NeuroDent AI - Support Request")
                putExtra(Intent.EXTRA_TEXT, "Hello NeuroDent AI Support Team,\n\nI need assistance with: \n\nDoctor Username: \n\nDevice info: Android App")
            }

            try {
                // Launch the chooser to allow the user to select their email client (Gmail, Outlook, etc.)
                startActivity(Intent.createChooser(emailIntent, "Send Email Support request..."))
            } catch (e: Exception) {
                Toast.makeText(this, "No email client installed on your device", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
