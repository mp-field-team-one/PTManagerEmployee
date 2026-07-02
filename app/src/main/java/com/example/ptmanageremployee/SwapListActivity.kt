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
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployee.data.Extras
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.SwapApplicationDto
import com.example.ptmanageremployee.data.SwapRequestDto
import com.example.ptmanageremployee.data.TokenStore
import com.example.ptmanageremployee.data.shiftTimeRange
import com.example.ptmanageremployee.data.toUserMessage
import kotlinx.coroutines.launch

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
            chips.forEach { chip ->
                val on = chip === active
                chip.setBackgroundResource(if (on) R.drawable.bg_pill_active else R.drawable.bg_pill)
                chip.setTextColor(
                    ContextCompat.getColor(this, if (on) R.color.white else R.color.text_tertiary)
                )
            }
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
        for (i in container.childCount - 1 downTo 0) {
            if (container.getChildAt(i).id != R.id.tv_swap_empty) container.removeViewAt(i)
        }
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        lifecycleScope.launch {
            try {
                when (tab) {
                    Tab.OPEN -> renderRequests(
                        Network.api.getSwapRequests(workplaceId, view = "open"), forOpen = true,
                    )
                    Tab.MINE -> renderRequests(
                        Network.api.getSwapRequests(workplaceId, view = "mine"), forOpen = false,
                    )
                    Tab.APPLIED -> renderApplications(Network.api.getMySwapApplications())
                }
            } catch (e: Exception) {
                Toast.makeText(this@SwapListActivity, e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderRequests(list: List<SwapRequestDto>, forOpen: Boolean) {
        val container = findViewById<LinearLayout>(R.id.swap_container)
        val empty = findViewById<TextView>(R.id.tv_swap_empty)
        if (list.isEmpty()) { empty.visibility = View.VISIBLE; return }
        empty.visibility = View.GONE
        val inflater = LayoutInflater.from(this)
        list.forEach { req ->
            val row = inflater.inflate(R.layout.item_swap, container, false)
            row.findViewById<TextView>(R.id.tv_title).text = shiftTitle(req)
            row.findViewById<TextView>(R.id.tv_sub).text = req.reason ?: "사유 없음"
            val badge = row.findViewById<TextView>(R.id.tv_badge)
            if (forOpen) {
                badge.text = "지원 가능"
                badge.setBackgroundResource(R.drawable.bg_badge_pending)
            } else {
                badge.text = statusLabel(req.status)
                badge.setBackgroundResource(statusBadge(req.status))
            }
            row.setOnClickListener { openDetail(req.id) }
            container.addView(row)
        }
    }

    private fun renderApplications(list: List<SwapApplicationDto>) {
        val container = findViewById<LinearLayout>(R.id.swap_container)
        val empty = findViewById<TextView>(R.id.tv_swap_empty)
        if (list.isEmpty()) { empty.visibility = View.VISIBLE; return }
        empty.visibility = View.GONE
        val inflater = LayoutInflater.from(this)
        list.forEach { app ->
            val row = inflater.inflate(R.layout.item_swap, container, false)
            row.findViewById<TextView>(R.id.tv_title).text = "대타 지원 #${app.swapRequestId ?: app.id}"
            row.findViewById<TextView>(R.id.tv_sub).text = "지원 상태 · ${statusLabel(app.status)}"
            val badge = row.findViewById<TextView>(R.id.tv_badge)
            badge.text = statusLabel(app.status)
            badge.setBackgroundResource(statusBadge(app.status))
            app.swapRequestId?.let { id -> row.setOnClickListener { openDetail(id) } }
            container.addView(row)
        }
    }

    private fun openDetail(swapRequestId: Long) {
        startActivity(
            Intent(this, SwapDetailActivity::class.java)
                .putExtra(Extras.SWAP_REQUEST_ID, swapRequestId)
        )
    }

    private fun shiftTitle(req: SwapRequestDto): String {
        val shift = req.shift
        return if (shift != null) {
            "${shift.workDate ?: ""} ${shiftTimeRange(shift.startTime, shift.endTime)}".trim()
        } else {
            "대타 요청 #${req.id}"
        }
    }

    private fun statusLabel(status: String?): String = when (status) {
        "PENDING" -> "대기 중"
        "APPROVED" -> "승인"
        "REJECTED" -> "거절"
        else -> status ?: ""
    }

    private fun statusBadge(status: String?): Int = when (status) {
        "APPROVED" -> R.drawable.bg_badge_approved
        "REJECTED" -> R.drawable.bg_badge_rejected
        else -> R.drawable.bg_badge_pending
    }
}
