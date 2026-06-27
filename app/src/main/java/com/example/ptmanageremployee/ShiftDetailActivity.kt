package com.example.ptmanageremployee

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployee.data.Extras
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.shiftTimeRange
import com.example.ptmanageremployee.data.toUserMessage
import kotlinx.coroutines.launch

class ShiftDetailActivity : AppCompatActivity() {

    private var shiftId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_shift_detail)
        findViewById<View>(R.id.shift_root).applySystemBarInsets()

        shiftId = intent.getLongExtra(Extras.SHIFT_ID, -1)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_sub_request).setOnClickListener {
            startActivity(
                Intent(this, SubRequestActivity::class.java).putExtra(Extras.SHIFT_ID, shiftId)
            )
        }

        if (shiftId > 0) loadShift()
    }

    private fun loadShift() {
        lifecycleScope.launch {
            try {
                val shift = Network.api.getShift(shiftId)
                findViewById<TextView>(R.id.tv_date).text = shift.workDate ?: ""
                findViewById<TextView>(R.id.tv_time).text =
                    shiftTimeRange(shift.startTime, shift.endTime)
            } catch (e: Exception) {
                Toast.makeText(this@ShiftDetailActivity, e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
