package com.example.ptmanageremployee

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployee.data.Extras
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.SwapRequestDetailDto
import com.example.ptmanageremployee.data.TokenStore
import com.example.ptmanageremployee.data.shiftTimeRange
import com.example.ptmanageremployee.data.toUserMessage
import kotlinx.coroutines.launch

/** 대타요청 상세. 조회(GET /api/swap-requests/{id}) + 지원(POST .../applications). */
class SwapDetailActivity : AppCompatActivity() {

    private var swapRequestId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_swap_detail)
        findViewById<View>(R.id.swap_detail_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        swapRequestId = intent.getLongExtra(Extras.SWAP_REQUEST_ID, -1)
        if (swapRequestId <= 0) {
            Toast.makeText(this, "대타요청을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        load()
    }

    private fun load() {
        lifecycleScope.launch {
            try {
                val detail = Network.api.getSwapRequest(swapRequestId)
                bind(detail)
            } catch (e: Exception) {
                Toast.makeText(this@SwapDetailActivity, e.toUserMessage(), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun bind(detail: SwapRequestDetailDto) {
        val shift = detail.shift
        findViewById<TextView>(R.id.tv_shift).text = if (shift != null) {
            "${shift.workDate ?: ""} ${shiftTimeRange(shift.startTime, shift.endTime)}".trim()
        } else {
            "대타요청 #${detail.id}"
        }
        findViewById<TextView>(R.id.tv_status).text = "상태 · ${statusLabel(detail.status)}"
        findViewById<TextView>(R.id.tv_reason).text = detail.reason ?: "사유 없음"

        val myId = TokenStore.userId
        val alreadyApplied = detail.applications?.any { it.applicantId == myId } == true
        val isMine = detail.requesterId == myId
        val applyBtn = findViewById<TextView>(R.id.btn_apply)
        when {
            detail.status != "PENDING" -> applyBtn.visibility = View.GONE
            isMine -> applyBtn.visibility = View.GONE
            alreadyApplied -> {
                applyBtn.visibility = View.VISIBLE
                applyBtn.text = "이미 지원함"
                applyBtn.isEnabled = false
            }
            else -> {
                applyBtn.visibility = View.VISIBLE
                applyBtn.text = "이 대타에 지원하기"
                applyBtn.isEnabled = true
                applyBtn.setOnClickListener { apply(applyBtn) }
            }
        }
    }

    private fun apply(btn: TextView) {
        btn.isEnabled = false
        lifecycleScope.launch {
            try {
                Network.api.applyToSwap(swapRequestId)
                Toast.makeText(this@SwapDetailActivity, "대타에 지원했어요", Toast.LENGTH_SHORT).show()
                load()
            } catch (e: Exception) {
                Toast.makeText(this@SwapDetailActivity, e.toUserMessage(), Toast.LENGTH_SHORT).show()
                btn.isEnabled = true
            }
        }
    }

    private fun statusLabel(status: String?): String = when (status) {
        "PENDING" -> "대기 중"
        "APPROVED" -> "승인됨"
        "REJECTED" -> "거절됨"
        else -> status ?: ""
    }
}
