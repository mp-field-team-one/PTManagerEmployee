package com.example.ptmanageremployee

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.ptmanageremployee.data.Extras
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.TokenStore
import com.example.ptmanageremployee.data.handoverCategoryLabel
import com.example.ptmanageremployee.data.metaLine

/** 인수인계 노트 상세. 목록에서 넘겨받은 값을 그대로 표시한다(별도 단건 조회 API 없음). */
class HandoverDetailActivity : AppCompatActivity() {

    private var handoverId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_handover_detail)
        findViewById<View>(R.id.detail_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        handoverId = intent.getLongExtra(Extras.HANDOVER_ID, -1)
        val category = intent.getStringExtra(Extras.HANDOVER_CATEGORY)
        val title = intent.getStringExtra(Extras.HANDOVER_TITLE)
        val content = intent.getStringExtra(Extras.HANDOVER_CONTENT)
        val authorId = intent.getLongExtra(Extras.HANDOVER_AUTHOR_ID, -1)
        val authorName = intent.getStringExtra(Extras.HANDOVER_AUTHOR_NAME)
        val createdAt = intent.getStringExtra(Extras.HANDOVER_CREATED_AT)

        text(R.id.tv_category, handoverCategoryLabel(category))
        text(R.id.tv_title, title)
        text(R.id.tv_meta, metaLine(authorName, createdAt, "작성자"))
        text(R.id.tv_content, content)

        // 본인이 쓴 노트만 삭제 가능(서버도 동일 규칙).
        if (handoverId > 0 && authorId == TokenStore.userId) {
            findViewById<TextView>(R.id.btn_delete).apply {
                visibility = View.VISIBLE
                setOnClickListener { confirmDelete() }
            }
        }
    }

    private fun confirmDelete() {
        confirm("인수인계 삭제", "이 노트를 삭제할까요?", "삭제") {
            launchApi {
                Network.api.deleteHandover(handoverId)
                toast("삭제했어요")
                finish()
            }
        }
    }
}
