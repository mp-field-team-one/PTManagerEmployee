package com.example.ptmanageremployee

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class JoinStoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_join_store)
        findViewById<View>(R.id.join_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        val pendingState = findViewById<View>(R.id.state_pending)
        val inputState = findViewById<View>(R.id.state_input)
        findViewById<View>(R.id.btn_submit_code).setOnClickListener {
            inputState.visibility = View.GONE
            pendingState.visibility = View.VISIBLE
        }
        findViewById<View>(R.id.btn_enter_app).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finishAffinity()
        }
    }
}
