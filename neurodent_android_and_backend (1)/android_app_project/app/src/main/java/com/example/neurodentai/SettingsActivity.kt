package com.example.neurodentai

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private var doctorId: Int = -1
    private var doctorName: String = "simats"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.hide()

        doctorId = intent.getIntExtra("DOCTOR_ID", -1)
        val nameExtra = intent.getStringExtra("DOCTOR_NAME")
        if (nameExtra != null && nameExtra.isNotEmpty()) {
            doctorName = nameExtra
        }

        val tvSettingsUsername = findViewById<TextView>(R.id.tvSettingsUsername)
        tvSettingsUsername.text = "Dr. $doctorName"

        // Setup bottom navigation clicks
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navUpload = findViewById<LinearLayout>(R.id.navUpload)
        val navReports = findViewById<LinearLayout>(R.id.navReports)

        navHome.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java).apply {
                putExtra("DOCTOR_ID", doctorId)
                putExtra("DOCTOR_NAME", doctorName)
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        navUpload.setOnClickListener {
            val intent = Intent(this, UploadActivity::class.java).apply {
                putExtra("DOCTOR_ID", doctorId)
                putExtra("DOCTOR_NAME", doctorName)
            }
            startActivity(intent)
            finish()
        }

        navReports.setOnClickListener {
            val intent = Intent(this, ReportsActivity::class.java).apply {
                putExtra("DOCTOR_ID", doctorId)
                putExtra("DOCTOR_NAME", doctorName)
            }
            startActivity(intent)
            finish()
        }

        // Setup settings menu clicks
        val btnSettingProfile = findViewById<LinearLayout>(R.id.btnSettingProfile)
        val btnSettingPrivacy = findViewById<LinearLayout>(R.id.btnSettingPrivacy)
        val btnSettingPassword = findViewById<LinearLayout>(R.id.btnSettingPassword)
        val btnSettingHelp = findViewById<LinearLayout>(R.id.btnSettingHelp)
        val btnSettingLogout = findViewById<LinearLayout>(R.id.btnSettingLogout)

        btnSettingProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java).apply {
                putExtra("DOCTOR_ID", doctorId)
                putExtra("DOCTOR_NAME", doctorName)
            }
            startActivity(intent)
        }

        btnSettingPrivacy.setOnClickListener {
            val intent = Intent(this, PrivacyActivity::class.java)
            startActivity(intent)
        }

        btnSettingPassword.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java).apply {
                putExtra("DOCTOR_NAME", doctorName)
            }
            startActivity(intent)
        }

        btnSettingHelp.setOnClickListener {
            val intent = Intent(this, HelpActivity::class.java)
            startActivity(intent)
        }

        btnSettingLogout.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
