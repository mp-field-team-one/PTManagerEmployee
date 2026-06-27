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
import com.example.ptmanageremployee.data.toUserMessage
import kotlinx.coroutines.launch

class NoticeDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notice_detail)
        findViewById<View>(R.id.detail_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        val noticeId = intent.getLongExtra(Extras.NOTICE_ID, -1)
        if (noticeId > 0) loadNotice(noticeId)
    }

    private fun loadNotice(noticeId: Long) {
        lifecycleScope.launch {
            try {
                val notice = Network.api.getNotice(noticeId)
                findViewById<TextView>(R.id.tv_title).text = notice.title ?: ""
                findViewById<TextView>(R.id.tv_meta).text =
                    listOfNotNull(notice.authorName, notice.createdAt?.replace("T", " ")?.take(16))
                        .joinToString(" · ")
                findViewById<TextView>(R.id.tv_body).text = notice.body ?: ""
            } catch (e: Exception) {
                Toast.makeText(this@NoticeDetailActivity, e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
