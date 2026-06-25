package com.example.ptmanageremployee

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SubRequestActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sub_request)
        findViewById<View>(R.id.sub_root).applySystemBarInsets()

        val chipAll = findViewById<TextView>(R.id.chip_all)
        val chipPick = findViewById<TextView>(R.id.chip_pick)
        selectChip(chipAll, chipPick)
        chipAll.setOnClickListener { selectChip(chipAll, chipPick) }
        chipPick.setOnClickListener { selectChip(chipPick, chipAll) }

        findViewById<View>(R.id.btn_cancel).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_send).setOnClickListener {
            Toast.makeText(this, "대타 요청을 보냈어요", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun selectChip(selected: TextView, other: TextView) {
        selected.setBackgroundResource(R.drawable.bg_chip_selected)
        selected.setTextColor(ContextCompat.getColor(this, R.color.brand_blue))
        other.setBackgroundResource(R.drawable.bg_chip_outline)
        other.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
    }
}
