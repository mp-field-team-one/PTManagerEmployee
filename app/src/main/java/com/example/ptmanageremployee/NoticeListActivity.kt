package com.example.ptmanageremployee

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.ptmanageremployee.data.Extras
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.TokenStore
import com.example.ptmanageremployee.data.noticeMeta

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
        container.removeAllExcept(R.id.tv_notice_empty)
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        launchApi {
            val notices = Network.api.getNotices(workplaceId, page = 0, size = 100).content
            // 목록 진입 시 읽음 처리(레드 닷 해제).
            runCatching { Network.api.markNoticesRead() }
            empty.visibility = if (notices.isEmpty()) View.VISIBLE else View.GONE
            notices.forEach { notice ->
                container.addItem(R.layout.item_notice) { card ->
                    card.text(R.id.tv_title, notice.title ?: "(제목 없음)")
                    card.text(R.id.tv_content, notice.body)
                    card.text(R.id.tv_meta, noticeMeta(notice))
                    card.setOnClickListener {
                        startActivity(
                            Intent(this@NoticeListActivity, NoticeDetailActivity::class.java)
                                .putExtra(Extras.NOTICE_ID, notice.id)
                        )
                    }
                }
            }
        }
    }
}
