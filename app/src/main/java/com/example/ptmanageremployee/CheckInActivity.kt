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
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

/**
 * 매장 QR을 스캔해 출근/퇴근을 기록한다.
 * 근무 상태에 따라 모드가 자동 전환된다: 출근 전→출근, 출근함→퇴근, 퇴근함→완료.
 */
class CheckInActivity : AppCompatActivity() {

    private enum class Mode { CHECK_IN, CHECK_OUT, DONE }

    private var shiftId: Long = -1
    private var mode: Mode = Mode.CHECK_IN

    private val scanLauncher = registerForActivityResult(ScanContract()) { result ->
        val token = result.contents
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "QR 스캔이 취소되었습니다.", Toast.LENGTH_SHORT).show()
        } else {
            submit(token)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_checkin)
        findViewById<View>(R.id.checkin_root).applySystemBarInsets()

        shiftId = intent.getLongExtra(Extras.SHIFT_ID, -1)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        if (shiftId > 0) loadShift()

        findViewById<View>(R.id.btn_checkin_confirm).setOnClickListener {
            if (shiftId <= 0) {
                Toast.makeText(this, "오늘 예정된 근무가 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (mode == Mode.DONE) return@setOnClickListener
            val prompt = if (mode == Mode.CHECK_IN) "매장 출근 QR을 스캔하세요" else "매장 QR을 스캔해 퇴근하세요"
            scanLauncher.launch(
                ScanOptions()
                    .setPrompt(prompt)
                    .setBeepEnabled(false)
                    .setOrientationLocked(false),
            )
        }
    }

    private fun submit(qrToken: String) {
        lifecycleScope.launch {
            try {
                if (mode == Mode.CHECK_IN) {
                    Network.api.checkIn(shiftId, CheckInRequest(qrToken = qrToken))
                    Toast.makeText(this@CheckInActivity, "출근 처리되었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Network.api.checkOut(shiftId, CheckInRequest(qrToken = qrToken))
                    Toast.makeText(this@CheckInActivity, "퇴근 처리되었습니다", Toast.LENGTH_SHORT).show()
                }
                loadShift() // 상태 갱신 → 버튼 모드 전환(출근 후엔 퇴근 버튼으로)
            } catch (e: Exception) {
                Toast.makeText(this@CheckInActivity, e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadShift() {
        val button = findViewById<TextView>(R.id.btn_checkin_confirm)
        lifecycleScope.launch {
            try {
                val shift = Network.api.getShift(shiftId)
                findViewById<TextView>(R.id.tv_checkin_shift).text =
                    "${shift.workDate ?: ""} · ${shiftTimeRange(shift.startTime, shift.endTime)} 근무"
                mode = when {
                    shift.checkedOutAt != null -> Mode.DONE
                    shift.checkedInAt != null -> Mode.CHECK_OUT
                    else -> Mode.CHECK_IN
                }
                button.text = when (mode) {
                    Mode.CHECK_IN -> "출근 체크인"
                    Mode.CHECK_OUT -> "퇴근 체크아웃"
                    Mode.DONE -> "근무 완료"
                }
                button.isEnabled = mode != Mode.DONE
            } catch (_: Exception) {
                // 표시용 정보 로드 실패는 무시하고 기본(출근) 모드로 진행 가능하게 둔다.
            }
        }
    }
}
