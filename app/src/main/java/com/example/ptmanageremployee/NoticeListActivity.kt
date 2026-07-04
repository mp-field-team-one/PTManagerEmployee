package com.example.ptmanageremployee

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployee.data.Extras
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.NoticeDto
import com.example.ptmanageremployee.data.TokenStore
import com.example.ptmanageremployee.data.toUserMessage
import kotlinx.coroutines.launch

/** 직원용 공지 전체 목록(읽기 전용). 탭 → 상세. */
class NoticeListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_notice_list)
        findViewById<View>(R.id.notice_list_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadNotices()
    }

    private fun loadNotices() {
        val workplaceId = TokenStore.workplaceId
        val container = findViewById<LinearLayout>(R.id.notice_container)
        val empty = findViewById<TextView>(R.id.tv_notice_empty)
        for (i in container.childCount - 1 downTo 0) {
            if (container.getChildAt(i).id != R.id.tv_notice_empty) container.removeViewAt(i)
        }
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        lifecycleScope.launch {
            try {
                val notices = Network.api.getNotices(workplaceId, page = 0, size = 100).content
                // 목록 진입 시 읽음 처리(레드 닷 해제).
                runCatching { Network.api.markNoticesRead() }
                empty.visibility = if (notices.isEmpty()) View.VISIBLE else View.GONE
                val inflater = LayoutInflater.from(this@NoticeListActivity)
                notices.forEach { notice ->
                    val card = inflater.inflate(R.layout.item_notice, container, false)
                    card.findViewById<TextView>(R.id.tv_title).text = notice.title ?: "(제목 없음)"
                    card.findViewById<TextView>(R.id.tv_content).text = notice.body ?: ""
                    card.findViewById<TextView>(R.id.tv_meta).text = noticeMeta(notice)
                    card.setOnClickListener {
                        startActivity(
                            Intent(this@NoticeListActivity, NoticeDetailActivity::class.java)
                                .putExtra(Extras.NOTICE_ID, notice.id)
                        )
                    }
                    container.addView(card)
                }
            } catch (e: Exception) {
                Toast.makeText(this@NoticeListActivity, e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun noticeMeta(notice: NoticeDto): String {
        val author = notice.authorName ?: "사장님"
        val date = notice.createdAt?.take(10) ?: ""
        return listOf(author, date).filter { it.isNotBlank() }.joinToString(" · ")
    }
}
