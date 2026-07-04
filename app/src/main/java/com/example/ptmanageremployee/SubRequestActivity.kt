package com.example.ptmanageremployee

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployee.data.CreateSwapRequest
import com.example.ptmanageremployee.data.Extras
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.shiftTimeRange
import com.example.ptmanageremployee.data.toUserMessage
import kotlinx.coroutines.launch

class SubRequestActivity : AppCompatActivity() {

    private var shiftId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sub_request)
        findViewById<View>(R.id.sub_root).applySystemBarInsets()

        shiftId = intent.getLongExtra(Extras.SHIFT_ID, -1)
        if (shiftId > 0) loadSummary()

        val chipAll = findViewById<TextView>(R.id.chip_all)
        val chipPick = findViewById<TextView>(R.id.chip_pick)
        selectChip(chipAll, chipPick)
        chipAll.setOnClickListener { selectChip(chipAll, chipPick) }
        chipPick.setOnClickListener { selectChip(chipPick, chipAll) }

        findViewById<View>(R.id.btn_cancel).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_send).setOnClickListener { btn ->
            if (shiftId <= 0) {
                Toast.makeText(this, "대상 근무가 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val reasonText = findViewById<EditText>(R.id.input_reason).text.toString().trim()
            val reason = reasonText.ifBlank { "대타요청합니다." }
            btn.isEnabled = false
            lifecycleScope.launch {
                try {
                    Network.api.createSwapRequest(CreateSwapRequest(shiftId, reason))
                    Toast.makeText(this@SubRequestActivity, "대타요청을 보냈어요", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@SubRequestActivity, e.toUserMessage(), Toast.LENGTH_SHORT).show()
                    btn.isEnabled = true
                }
            }
        }
    }

    private fun loadSummary() {
        lifecycleScope.launch {
            try {
                val shift = Network.api.getShift(shiftId)
                findViewById<TextView>(R.id.tv_shift_summary).text =
                    "${shift.workDate ?: ""} ${shiftTimeRange(shift.startTime, shift.endTime)}"
            } catch (_: Exception) {
            }
        }
    }

    private fun selectChip(selected: TextView, other: TextView) {
        selected.setBackgroundResource(R.drawable.bg_chip_selected)
        selected.setTextColor(ContextCompat.getColor(this, R.color.brand_blue))
        other.setBackgroundResource(R.drawable.bg_chip_outline)
        other.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
    }
}
