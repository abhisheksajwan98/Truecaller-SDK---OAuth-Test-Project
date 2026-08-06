package com.example.testoauth.ui.login

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.testoauth.R

class SignedInSuccessfulActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signed_in_successful);
        val name = intent.getStringExtra("name");
        findViewById<TextView>(R.id.textViewName).text = "Welcome, $name!"
    }
}
