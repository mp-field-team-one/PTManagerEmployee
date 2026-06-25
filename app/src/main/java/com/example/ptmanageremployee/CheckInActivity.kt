package com.example.ptmanageremployee

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class CheckInActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_checkin)
        findViewById<View>(R.id.checkin_root).applySystemBarInsets()

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_checkin_confirm).setOnClickListener {
            Toast.makeText(this, "출근 처리되었습니다", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
