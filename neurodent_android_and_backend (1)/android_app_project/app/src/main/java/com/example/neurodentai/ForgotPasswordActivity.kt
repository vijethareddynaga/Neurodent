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

class ForgotPasswordActivity : AppCompatActivity() {

    private val client = NetworkConfig.client

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        supportActionBar?.hide()

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val emailInput = findViewById<EditText>(R.id.emailEditText)
        val newPasswordInput = findViewById<EditText>(R.id.newPasswordEditText)
        val confirmPasswordInput = findViewById<EditText>(R.id.confirmPasswordEditText)
        val resetButton = findViewById<Button>(R.id.resetButton)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        backButton.setOnClickListener { finish() }

        resetButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val newPassword = newPasswordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all password fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            resetButton.visibility = View.INVISIBLE

            val jsonObj = JSONObject().apply {
                put("username", email)
                put("new_password", newPassword)
            }

            val body = jsonObj.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("${NetworkConfig.BASE_URL}/reset-password")
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        resetButton.visibility = View.VISIBLE
                        Toast.makeText(this@ForgotPasswordActivity, "Error resetting password: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    val responseData = response.body?.string()
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        resetButton.visibility = View.VISIBLE
                        if (response.isSuccessful) {
                            Toast.makeText(this@ForgotPasswordActivity, "Password reset successfully", Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            val errorMsg = try {
                                val errorJson = JSONObject(responseData ?: "")
                                errorJson.optString("detail", "Reset failed")
                            } catch (e: Exception) {
                                "Reset failed"
                            }
                            Toast.makeText(this@ForgotPasswordActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            })
        }
    }
}
