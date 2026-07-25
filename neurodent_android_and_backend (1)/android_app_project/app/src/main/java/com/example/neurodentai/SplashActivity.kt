package com.example.neurodentai // Update with your actual package name

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Hide action bar for full screen splash
        supportActionBar?.hide()

        // Wait 2 seconds and move to next screen
        Handler(Looper.getMainLooper()).postDelayed({
            // TODO: Initialize Supabase here or in Application class
            
            // Navigate to Login Activity
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }, 2000)
    }
}
