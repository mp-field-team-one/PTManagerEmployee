package com.example.ptmanageremployee

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployee.data.Extras
import com.example.ptmanageremployee.data.HandoverDto
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.NoticeDto
import com.example.ptmanageremployee.data.SwapRequestDto
import com.example.ptmanageremployee.data.TokenStore
import com.example.ptmanageremployee.data.handoverCategoryLabel
import com.example.ptmanageremployee.data.shiftTimeRange
import com.example.ptmanageremployee.data.toUserMessage
import kotlinx.coroutines.launch

class CommunicationFragment : Fragment() {

    // 소통 탭은 각 구역의 가장 최근 1건만 미리보기로 노출한다.
    private val previewCount = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_communication, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.btn_notice_header).setOnClickListener {
            startActivity(Intent(requireContext(), NoticeListActivity::class.java))
        }
        view.findViewById<View>(R.id.btn_handover_header).setOnClickListener {
            startActivity(Intent(requireContext(), HandoverListActivity::class.java))
        }
        view.findViewById<View>(R.id.btn_swap_header).setOnClickListener {
            startActivity(Intent(requireContext(), SwapListActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // 작성/지원 등으로 다른 화면에 다녀와도 최신 내용이 보이도록 재조회한다.
        view?.let {
            loadNotices(it)
            loadHandovers(it)
            loadSwaps(it)
        }
    }

    /** 빈 상태 뷰(emptyId)만 남기고 이전에 그린 카드를 제거한다. */
    private fun clearCards(container: LinearLayout, emptyId: Int) {
        for (i in container.childCount - 1 downTo 0) {
            if (container.getChildAt(i).id != emptyId) container.removeViewAt(i)
        }
    }

    private fun loadNotices(view: View) {
        val workplaceId = TokenStore.workplaceId
        val panel = view.findViewById<LinearLayout>(R.id.panel_notice)
        val empty = view.findViewById<TextView>(R.id.tv_notice_empty)
        clearCards(panel, R.id.tv_notice_empty)
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        lifecycleScope.launch {
            try {
                val notices = Network.api.getNotices(workplaceId, page = 0, size = 50).content
                // 공지 탭 진입 시 읽음 처리(레드 닷 해제).
                runCatching { Network.api.markNoticesRead() }
                empty.visibility = if (notices.isEmpty()) View.VISIBLE else View.GONE
                val inflater = LayoutInflater.from(requireContext())
                notices.take(previewCount).forEach { notice ->
                    val card = inflater.inflate(R.layout.item_notice, panel, false)
                    card.findViewById<TextView>(R.id.tv_title).text = notice.title ?: "(제목 없음)"
                    card.findViewById<TextView>(R.id.tv_content).apply {
                        maxLines = 1
                        text = firstLine(notice.body)
                    }
                    card.findViewById<TextView>(R.id.tv_meta).text = noticeMeta(notice)
                    card.setOnClickListener {
                        startActivity(
                            Intent(requireContext(), NoticeDetailActivity::class.java)
                                .putExtra(Extras.NOTICE_ID, notice.id)
                        )
                    }
                    panel.addView(card)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadHandovers(view: View) {
        val workplaceId = TokenStore.workplaceId
        val panel = view.findViewById<LinearLayout>(R.id.panel_handover)
        val empty = view.findViewById<TextView>(R.id.tv_handover_empty)
        clearCards(panel, R.id.tv_handover_empty)
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        lifecycleScope.launch {
            try {
                val notes = Network.api.getHandovers(workplaceId, null)
                empty.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
                val inflater = LayoutInflater.from(requireContext())
                notes.take(previewCount).forEach { note ->
                    val card = inflater.inflate(R.layout.item_handover, panel, false)
                    card.findViewById<TextView>(R.id.tv_category).text = handoverCategoryLabel(note.category)
                    card.findViewById<TextView>(R.id.tv_title).text = note.title ?: ""
                    card.findViewById<TextView>(R.id.tv_content).apply {
                        maxLines = 1
                        text = firstLine(note.content)
                    }
                    card.findViewById<TextView>(R.id.tv_meta).text = handoverMeta(note)
                    // 미리보기라 삭제는 목록 화면에서만 — btn_delete 는 기본 gone 유지.
                    panel.addView(card)
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSwaps(view: View) {
        val workplaceId = TokenStore.workplaceId
        val panel = view.findViewById<LinearLayout>(R.id.panel_swap)
        val empty = view.findViewById<TextView>(R.id.tv_swap_empty)
        clearCards(panel, R.id.tv_swap_empty)
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        lifecycleScope.launch {
            try {
                // 다른 직원의 대타요청과 내가 신청한 대타요청을 각각 최근 1건씩 노출한다.
                val open = Network.api.getSwapRequests(workplaceId, view = "open")
                val mine = Network.api.getSwapRequests(workplaceId, view = "mine")
                if (open.isEmpty() && mine.isEmpty()) {
                    empty.visibility = View.VISIBLE
                    return@launch
                }
                empty.visibility = View.GONE
                open.firstOrNull()?.let {
                    addSwapCaption(panel, "다른 직원의 대타요청")
                    renderOpenSwaps(panel, listOf(it))
                }
                mine.firstOrNull()?.let {
                    addSwapCaption(panel, "내가 신청한 대타요청")
                    renderMineSwaps(panel, listOf(it))
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** 지원 가능한(다른 직원의) 대타 요청 카드. 탭 → 상세(지원). */
    private fun renderOpenSwaps(panel: LinearLayout, requests: List<SwapRequestDto>) {
        val inflater = LayoutInflater.from(requireContext())
        requests.take(previewCount).forEach { req ->
            val row = inflater.inflate(R.layout.item_swap, panel, false)
            row.findViewById<TextView>(R.id.tv_title).text = shiftTitle(req)
            row.findViewById<TextView>(R.id.tv_sub).text = req.reason ?: "사유 없음"
            row.findViewById<TextView>(R.id.tv_badge).text = "지원 가능"
            row.setOnClickListener { openSwapDetail(req.id) }
            panel.addView(row)
        }
    }

    /** 내가 신청한 대타 요청의 진행 현황 카드(상태 배지). 탭 → 상세. */
    private fun renderMineSwaps(panel: LinearLayout, requests: List<SwapRequestDto>) {
        val inflater = LayoutInflater.from(requireContext())
        requests.take(previewCount).forEach { req ->
            val row = inflater.inflate(R.layout.item_swap, panel, false)
            row.findViewById<TextView>(R.id.tv_title).text = shiftTitle(req)
            row.findViewById<TextView>(R.id.tv_sub).text = req.reason ?: "사유 없음"
            val badge = row.findViewById<TextView>(R.id.tv_badge)
            badge.text = swapStatusLabel(req.status)
            badge.setBackgroundResource(swapStatusBadge(req.status))
            row.setOnClickListener { openSwapDetail(req.id) }
            panel.addView(row)
        }
    }

    private fun openSwapDetail(swapRequestId: Long) {
        startActivity(
            Intent(requireContext(), SwapDetailActivity::class.java)
                .putExtra(Extras.SWAP_REQUEST_ID, swapRequestId)
        )
    }

    /** 구역 안내 캡션(예: "내가 신청한 대타 진행 현황")을 패널에 추가한다. */
    private fun addSwapCaption(panel: LinearLayout, text: String) {
        val caption = TextView(requireContext()).apply {
            this.text = text
            setTextColor(ContextCompat.getColor(requireContext(), R.color.cat_swap))
            textSize = 12f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            val top = (14 * resources.displayMetrics.density).toInt()
            setPadding(0, top, 0, 0)
        }
        panel.addView(caption)
    }

    private fun swapStatusLabel(status: String?): String = when (status) {
        "PENDING" -> "대기 중"
        "APPROVED" -> "승인"
        "REJECTED" -> "거절"
        else -> status ?: ""
    }

    private fun swapStatusBadge(status: String?): Int = when (status) {
        "APPROVED" -> R.drawable.bg_badge_approved
        "REJECTED" -> R.drawable.bg_badge_rejected
        else -> R.drawable.bg_badge_pending
    }

    /** 미리보기 한 줄용: 줄바꿈을 공백으로 접어 첫 줄만(넘치면 …) 보이게 한다. */
    private fun firstLine(text: String?): String =
        (text ?: "").replace("\n", " ").replace("\r", " ").trim()

    private fun noticeMeta(notice: NoticeDto): String {
        val author = notice.authorName ?: "사장님"
        val date = notice.createdAt?.take(10) ?: ""
        return listOf(author, date).filter { it.isNotBlank() }.joinToString(" · ")
    }

    private fun handoverMeta(note: HandoverDto): String {
        val author = note.authorName ?: "작성자"
        val date = note.createdAt?.take(10) ?: ""
        return listOf(author, date).filter { it.isNotBlank() }.joinToString(" · ")
    }

    private fun shiftTitle(req: SwapRequestDto): String {
        val shift = req.shift
        return if (shift != null) {
            "${shift.workDate ?: ""} ${shiftTimeRange(shift.startTime, shift.endTime)}".trim()
        } else {
            "대타요청 #${req.id}"
        }
    }
}
