package com.example.ptmanageremployee

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployee.data.CreateSwapRequest
import com.example.ptmanageremployee.data.Extras
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.shiftTimeRange
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
                toast("대상 근무가 없습니다.")
                return@setOnClickListener
            }
            val reasonText = findViewById<EditText>(R.id.input_reason).text.toString().trim()
            val reason = reasonText.ifBlank { "대타요청합니다." }
            launchApi(btn) {
                Network.api.createSwapRequest(CreateSwapRequest(shiftId, reason))
                toast("대타요청을 보냈어요")
                finish()
            }
        }
    }

    private fun loadSummary() {
        lifecycleScope.launch {
            try {
                val shift = Network.api.getShift(shiftId)
                text(R.id.tv_shift_summary, shift.workDate)
                text(R.id.tv_shift_time, shiftTimeRange(shift.startTime, shift.endTime))
            } catch (_: Exception) {
            }
        }
    }

    private fun selectChip(selected: TextView, other: TextView) {
        selected.setChipSelected(true)
        other.setChipSelected(false)
    }
}
