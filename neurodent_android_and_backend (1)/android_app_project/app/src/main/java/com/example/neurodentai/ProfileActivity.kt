package com.example.neurodentai

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.IOException

class ProfileActivity : AppCompatActivity() {

    private val client = NetworkConfig.client
    private var doctorId: Int = -1
    private var doctorName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        supportActionBar?.hide()

        doctorId = intent.getIntExtra("DOCTOR_ID", -1)
        doctorName = intent.getStringExtra("DOCTOR_NAME") ?: ""

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val tvProfileFullName = findViewById<TextView>(R.id.tvProfileFullName)
        val tvProfileEmail = findViewById<TextView>(R.id.tvProfileEmail)
        val btnDeleteAccount = findViewById<LinearLayout>(R.id.btnDeleteAccount)

        backButton.setOnClickListener { finish() }

        // Format names nicely
        val displayName = if (doctorName.contains("@")) {
            doctorName.substringBefore("@").replaceFirstChar { it.uppercase() }
        } else {
            doctorName.replaceFirstChar { it.uppercase() }
        }

        tvProfileFullName.text = "Dr. $displayName"
        tvProfileEmail.text = doctorName

        btnDeleteAccount.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Delete Account Permanently")
                .setMessage("Are you sure you want to permanently delete your account? All your patients and reports will be permanently lost.")
                .setPositiveButton("Delete Permanently") { _, _ ->
                    deleteAccount(doctorId)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun deleteAccount(userId: Int) {
        val request = Request.Builder()
            .url("http://10.170.241.109:8000/users/$userId")
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Failed to delete account: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ProfileActivity, "Account deleted successfully", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@ProfileActivity, "Error deleting account", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
