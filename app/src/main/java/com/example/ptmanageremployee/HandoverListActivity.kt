package com.example.ptmanageremployee

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.ptmanageremployee.data.Extras
import com.example.ptmanageremployee.data.HandoverDto
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.TokenStore
import com.example.ptmanageremployee.data.handoverCategoryLabel
import com.example.ptmanageremployee.data.handoverMeta

/** 인수인계 노트 목록. 카테고리 필터 + 작성 진입 + 본인 노트 삭제. */
class HandoverListActivity : AppCompatActivity() {

    // 선택된 카테고리 코드(null = 전체).
    private var selectedCategory: String? = null

    // 칩 뷰 id → 카테고리 코드(전체는 null).
    private val chips: List<Pair<Int, String?>> by lazy {
        listOf(
            R.id.chip_all to null,
            R.id.chip_stock to "STOCK",
            R.id.chip_device to "DEVICE",
            R.id.chip_customer to "CUSTOMER",
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_handover_list)
        findViewById<View>(R.id.handover_list_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_write).setOnClickListener {
            startActivity(Intent(this, HandoverWriteActivity::class.java))
        }
        chips.forEach { (id, category) ->
            findViewById<TextView>(id).setOnClickListener {
                selectedCategory = category
                renderChips()
                loadHandovers()
            }
        }
        renderChips()
    }

    override fun onResume() {
        super.onResume()
        // 작성/삭제 후 돌아왔을 때 갱신.
        loadHandovers()
    }

    private fun renderChips() {
        chips.forEach { (id, category) ->
            findViewById<TextView>(id).setChipSelected(category == selectedCategory)
        }
    }

    private fun loadHandovers() {
        val workplaceId = TokenStore.workplaceId
        val container = findViewById<LinearLayout>(R.id.handover_container)
        val empty = findViewById<TextView>(R.id.tv_handover_empty)
        // 기존 행 제거(빈 상태 뷰는 유지).
        container.removeAllExcept(R.id.tv_handover_empty)
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        launchApi {
            val notes = Network.api.getHandovers(workplaceId, selectedCategory)
            if (notes.isEmpty()) {
                empty.visibility = View.VISIBLE
                return@launchApi
            }
            empty.visibility = View.GONE
            notes.forEach { note ->
                container.addItem(R.layout.item_handover) { card ->
                    card.text(R.id.tv_category, handoverCategoryLabel(note.category))
                    card.text(R.id.tv_title, note.title)
                    card.text(R.id.tv_content, note.content)
                    card.text(R.id.tv_meta, handoverMeta(note))
                    val delete = card.findViewById<View>(R.id.btn_delete)
                    // 본인이 쓴 노트만 삭제 가능(서버도 동일 규칙).
                    if (note.authorId == TokenStore.userId) {
                        delete.visibility = View.VISIBLE
                        delete.setOnClickListener { confirmDelete(note) }
                    }
                    card.setOnClickListener { openDetail(note) }
                }
            }
        }
    }

    private fun openDetail(note: HandoverDto) {
        startActivity(
            Intent(this, HandoverDetailActivity::class.java)
                .putExtra(Extras.HANDOVER_ID, note.id)
                .putExtra(Extras.HANDOVER_CATEGORY, note.category)
                .putExtra(Extras.HANDOVER_TITLE, note.title)
                .putExtra(Extras.HANDOVER_CONTENT, note.content)
                .putExtra(Extras.HANDOVER_AUTHOR_ID, note.authorId ?: -1L)
                .putExtra(Extras.HANDOVER_AUTHOR_NAME, note.authorName)
                .putExtra(Extras.HANDOVER_CREATED_AT, note.createdAt)
        )
    }

    private fun confirmDelete(note: HandoverDto) {
        confirm("인수인계 삭제", "이 노트를 삭제할까요?", "삭제") {
            launchApi {
                Network.api.deleteHandover(note.id)
                toast("삭제했어요")
                loadHandovers()
            }
        }
    }
}
