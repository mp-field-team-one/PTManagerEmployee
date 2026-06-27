package com.example.ptmanageremployee

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployee.data.CheckInRequest
import com.example.ptmanageremployee.data.Extras
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.shiftTimeRange
import com.example.ptmanageremployee.data.toUserMessage
import kotlinx.coroutines.launch

class CheckInActivity : AppCompatActivity() {

    private var shiftId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_checkin)
        findViewById<View>(R.id.checkin_root).applySystemBarInsets()

        shiftId = intent.getLongExtra(Extras.SHIFT_ID, -1)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        if (shiftId > 0) loadShift()

        findViewById<View>(R.id.btn_checkin_confirm).setOnClickListener { btn ->
            if (shiftId <= 0) {
                Toast.makeText(this, "오늘 예정된 근무가 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btn.isEnabled = false
            lifecycleScope.launch {
                try {
                    // QR 스캐너 미연동 단계 — 서버가 QR 토큰을 무시하므로 플레이스홀더를 전송한다.
                    Network.api.checkIn(shiftId, CheckInRequest(qrToken = "app-checkin"))
                    Toast.makeText(this@CheckInActivity, "출근 처리되었습니다", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@CheckInActivity, e.toUserMessage(), Toast.LENGTH_SHORT).show()
                    btn.isEnabled = true
                }
            }
        }
    }

    private fun loadShift() {
        lifecycleScope.launch {
            try {
                val shift = Network.api.getShift(shiftId)
                findViewById<TextView>(R.id.tv_checkin_shift).text =
                    "${shift.workDate ?: ""} · ${shiftTimeRange(shift.startTime, shift.endTime)} 근무"
            } catch (_: Exception) {
                // 표시용 정보 로드 실패는 무시하고 체크인은 진행 가능하게 둔다.
            }
        }
    }
}
