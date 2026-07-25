package com.example.neurodentai

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ReportsActivity : AppCompatActivity() {

    private val client = NetworkConfig.client
    private var doctorId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)
        supportActionBar?.hide()

        doctorId = intent.getIntExtra("DOCTOR_ID", -1)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navUpload = findViewById<LinearLayout>(R.id.navUpload)

        backButton.setOnClickListener { finish() }

        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        navHome.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        navUpload.setOnClickListener {
            val intent = Intent(this, UploadActivity::class.java)
            intent.putExtra("DOCTOR_ID", doctorId)
            startActivity(intent)
            finish()
        }

        navProfile.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.putExtra("DOCTOR_ID", doctorId)
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (doctorId != -1) {
            fetchReports()
        }
    }

    private fun fetchReports() {
        val request = Request.Builder()
            .url("${NetworkConfig.BASE_URL}/patients/$doctorId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ReportsActivity, "Failed to load reports: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                runOnUiThread {
                    if (response.isSuccessful && responseData != null) {
                        try {
                            val reports = JSONArray(responseData)
                            val tvReportsCount = findViewById<TextView>(R.id.tvReportsCount)
                            tvReportsCount.text = "${reports.length()} Patient Reports Available"

                            val layoutReportsList = findViewById<LinearLayout>(R.id.layoutReportsList)
                            layoutReportsList.removeAllViews()

                            for (i in 0 until reports.length()) {
                                val item = reports.getJSONObject(i)
                                val view = LayoutInflater.from(this@ReportsActivity)
                                    .inflate(R.layout.item_recent_activity, layoutReportsList, false)

                                val tvPatientId = view.findViewById<TextView>(R.id.tvPatientId)
                                val tvNerveDamage = view.findViewById<TextView>(R.id.tvNerveDamage)
                                val tvRiskLevel = view.findViewById<TextView>(R.id.tvRiskLevel)
                                val iconContainer = view.findViewById<FrameLayout>(R.id.iconContainer)
                                val iconImage = view.findViewById<ImageView>(R.id.iconImage)

                                tvPatientId.text = item.optString("patient_id_display", "Patient")
                                tvNerveDamage.text = "Nerve damage: ${item.optDouble("nerve_damage", 0.0)}%"

                                val riskLevel = item.optString("risk_level", "Low Risk")
                                tvRiskLevel.text = riskLevel

                                if (riskLevel == "High Risk") {
                                    tvRiskLevel.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                                    tvRiskLevel.setBackgroundResource(R.drawable.bg_badge_red)
                                    iconContainer.setBackgroundResource(R.drawable.bg_icon_red)
                                    iconImage.setImageResource(R.drawable.ic_warning)
                                } else if (riskLevel == "Medium Risk") {
                                    tvRiskLevel.setTextColor(android.graphics.Color.parseColor("#D97706"))
                                    tvRiskLevel.setBackgroundResource(R.drawable.bg_badge_red)
                                    iconContainer.setBackgroundResource(R.drawable.bg_icon_red)
                                    iconImage.setImageResource(R.drawable.ic_warning)
                                } else {
                                    tvRiskLevel.setTextColor(android.graphics.Color.parseColor("#16A34A"))
                                    tvRiskLevel.setBackgroundResource(R.drawable.bg_badge_green)
                                    iconContainer.setBackgroundResource(R.drawable.bg_icon_blue)
                                    iconImage.setImageResource(R.drawable.ic_people)
                                }

                                val btnDelete = view.findViewById<android.widget.ImageButton>(R.id.btnDelete)

                                view.setOnClickListener {
                                    val intent = Intent(this@ReportsActivity, ResultActivity::class.java).apply {
                                        putExtra("PERCENTAGE", item.optDouble("nerve_damage", 0.0))
                                        putExtra("PATIENT_NAME", item.optString("name", "Anonymous"))
                                        putExtra("PATIENT_AGE", item.optInt("age", 0))
                                        putExtra("PATIENT_GENDER", item.optString("gender", "N/A"))
                                        putExtra("PATIENT_DISPLAY_ID", item.optString("patient_id_display", ""))
                                        putExtra("PATIENT_DB_ID", item.optInt("id", -1))
                                    }
                                    startActivity(intent)
                                }

                                btnDelete.setOnClickListener {
                                    deletePatient(item.optInt("id", -1))
                                }

                                layoutReportsList.addView(view)
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@ReportsActivity, "Error parsing reports", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun deletePatient(patientId: Int) {
        val request = Request.Builder()
            .url("${NetworkConfig.BASE_URL}/patients/$patientId")
            .delete()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ReportsActivity, "Failed to connect", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ReportsActivity, "Report deleted", Toast.LENGTH_SHORT).show()
                        fetchReports() // Refresh list
                    } else {
                        Toast.makeText(this@ReportsActivity, "Error deleting report", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }
}
