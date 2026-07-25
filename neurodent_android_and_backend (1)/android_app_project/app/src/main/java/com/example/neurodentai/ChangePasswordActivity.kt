package com.example.neurodentai

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ChangePasswordActivity : AppCompatActivity() {

    private val client = NetworkConfig.client
    private var doctorName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)
        supportActionBar?.hide()

        doctorName = intent.getStringExtra("DOCTOR_NAME") ?: ""

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val newPasswordEditText = findViewById<EditText>(R.id.newPasswordEditText)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirmPasswordEditText)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        backButton.setOnClickListener { finish() }

        saveButton.setOnClickListener {
            val newPassword = newPasswordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all password fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            saveButton.visibility = View.INVISIBLE

            val jsonObj = JSONObject().apply {
                put("username", doctorName)
                put("new_password", newPassword)
            }

            val body = jsonObj.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("http://10.170.241.109:8000/reset-password")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        saveButton.visibility = View.VISIBLE
                        Toast.makeText(this@ChangePasswordActivity, "Error saving password: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseData = response.body?.string()
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        saveButton.visibility = View.VISIBLE
                        if (response.isSuccessful) {
                            Toast.makeText(this@ChangePasswordActivity, "Password updated successfully", Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            val errorMsg = try {
                                val errorJson = JSONObject(responseData ?: "")
                                errorJson.optString("detail", "Failed to update password")
                            } catch (e: Exception) {
                                "Failed to update password"
                            }
                            Toast.makeText(this@ChangePasswordActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
        }
    }
}
