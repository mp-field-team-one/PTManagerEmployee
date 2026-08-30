package com.example.ptmanageremployee

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.NotificationDto

class NotificationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notification)
        findViewById<View>(R.id.inbox_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        loadNotifications()
    }

    private fun loadNotifications() {
        val container = findViewById<LinearLayout>(R.id.noti_container)
        val empty = findViewById<TextView>(R.id.tv_noti_empty)
        launchApi {
            val page = Network.api.getNotifications(page = 0, size = 50)
            val items = page.content
            if (items.isEmpty()) {
                empty.visibility = View.VISIBLE
                return@launchApi
            }
            items.forEach { noti ->
                container.addItem(R.layout.item_notification) { row ->
                    row.text(R.id.tv_title, titleOf(noti))
                    row.text(R.id.tv_body, noti.message)
                    row.text(R.id.tv_time, formatTime(noti.createdAt))
                }
            }
            // 인박스에 진입했으므로 전체 읽음 처리(배지 해제).
            runCatching { Network.api.markAllNotificationsRead() }
        }
    }

    private fun titleOf(n: NotificationDto): String = when (n.type) {
        "ATTENDANCE" -> "🕘 출근 알림"
        "SWAP_REQUEST" -> "🔁 대타요청"
        "SWAP_APPLICATION" -> "🙋 대타 지원"
        "SWAP_RESULT" -> "🔁 대타 결과"
        "NOTICE" -> "📢 새 공지"
        "SCHEDULE_CHANGED" -> "🗓 근무 편성 변경"
        "JOIN_REQUEST" -> "👤 가입 신청"
        else -> "🔔 알림"
    }

    /** ISO-8601 문자열에서 날짜·시간만 간단히 표시한다. */
    private fun formatTime(iso: String?): String {
        if (iso.isNullOrBlank()) return ""
        return iso.replace("T", " ").take(16)
    }
}
