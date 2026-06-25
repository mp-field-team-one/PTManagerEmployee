package com.example.ptmanageremployee

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class ShiftDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shift_detail)
        findViewById<View>(R.id.shift_root).applySystemBarInsets()

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_sub_request).setOnClickListener {
            startActivity(Intent(this, SubRequestActivity::class.java))
        }
    }
}
