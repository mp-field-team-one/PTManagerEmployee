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
import com.example.ptmanageremployee.data.SwapApplicationDto
import com.example.ptmanageremployee.data.SwapRequestDto
import com.example.ptmanageremployee.data.TokenStore
import com.example.ptmanageremployee.data.shiftTitle
import com.example.ptmanageremployee.data.swapStatusBadge
import com.example.ptmanageremployee.data.swapStatusLabel

/**
 * 직원 대타 화면. 지원 가능(open)·내 요청(mine)·내 지원(applications) 3개 관점을
 * GET /api/swap-requests, GET /api/swap-applications/me 로 조회한다.
 */
class SwapListActivity : AppCompatActivity() {

    private enum class Tab { OPEN, MINE, APPLIED }

    private var tab = Tab.OPEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_swap_list)
        findViewById<View>(R.id.swap_list_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        val chipOpen = findViewById<TextView>(R.id.chip_open)
        val chipMine = findViewById<TextView>(R.id.chip_mine)
        val chipApplied = findViewById<TextView>(R.id.chip_applied)
        val chips = listOf(chipOpen, chipMine, chipApplied)

        fun select(sel: Tab) {
            tab = sel
            val active = when (sel) { Tab.OPEN -> chipOpen; Tab.MINE -> chipMine; Tab.APPLIED -> chipApplied }
            chips.forEach { it.setChipSelected(it === active, R.color.text_tertiary) }
            load()
        }
        chipOpen.setOnClickListener { select(Tab.OPEN) }
        chipMine.setOnClickListener { select(Tab.MINE) }
        chipApplied.setOnClickListener { select(Tab.APPLIED) }
        select(Tab.OPEN)
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val workplaceId = TokenStore.workplaceId
        val container = findViewById<LinearLayout>(R.id.swap_container)
        val empty = findViewById<TextView>(R.id.tv_swap_empty)
        container.removeAllExcept(R.id.tv_swap_empty)
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        launchApi {
            when (tab) {
                Tab.OPEN -> renderRequests(
                    Network.api.getSwapRequests(workplaceId, view = "open"), forOpen = true,
                )
                Tab.MINE -> renderRequests(
                    Network.api.getSwapRequests(workplaceId, view = "mine"), forOpen = false,
                )
                Tab.APPLIED -> renderApplications(Network.api.getMySwapApplications())
            }
        }
    }

    private fun renderRequests(list: List<SwapRequestDto>, forOpen: Boolean) = render(list) { req ->
        addRow(
            title = shiftTitle(req),
            sub = req.reason ?: "사유 없음",
            badgeText = if (forOpen) "지원 가능" else swapStatusLabel(req.status),
            badgeBg = if (forOpen) R.drawable.bg_badge_pending else swapStatusBadge(req.status),
            onClick = { openDetail(req.id) },
        )
    }

    private fun renderApplications(list: List<SwapApplicationDto>) = render(list) { app ->
        addRow(
            title = "대타 지원 #${app.swapRequestId ?: app.id}",
            sub = "지원 상태 · ${swapStatusLabel(app.status)}",
            badgeText = swapStatusLabel(app.status),
            badgeBg = swapStatusBadge(app.status),
            onClick = app.swapRequestId?.let { id -> { openDetail(id) } },
        )
    }

    /** 목록이 비면 빈 상태만 보여주고, 아니면 각 항목을 카드로 그린다. */
    private fun <T> render(list: List<T>, addCard: (T) -> Unit) {
        val empty = findViewById<TextView>(R.id.tv_swap_empty)
        empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        list.forEach(addCard)
    }

    private fun addRow(
        title: String,
        sub: String,
        badgeText: String,
        badgeBg: Int,
        onClick: (() -> Unit)?,
    ) {
        val container = findViewById<LinearLayout>(R.id.swap_container)
        container.addItem(R.layout.item_swap) { row ->
            row.text(R.id.tv_title, title)
            row.text(R.id.tv_sub, sub)
            val badge = row.findViewById<TextView>(R.id.tv_badge)
            badge.text = badgeText
            badge.setBackgroundResource(badgeBg)
            onClick?.let { row.setOnClickListener { _ -> it() } }
        }
    }

    private fun openDetail(swapRequestId: Long) {
        startActivity(
            Intent(this, SwapDetailActivity::class.java)
                .putExtra(Extras.SWAP_REQUEST_ID, swapRequestId)
        )
    }
}
