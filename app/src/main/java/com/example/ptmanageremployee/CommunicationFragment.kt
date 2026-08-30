package com.example.ptmanageremployee

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.ptmanageremployee.data.Extras
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.SwapRequestDto
import com.example.ptmanageremployee.data.TokenStore
import com.example.ptmanageremployee.data.handoverCategoryLabel
import com.example.ptmanageremployee.data.handoverMeta
import com.example.ptmanageremployee.data.noticeMeta
import com.example.ptmanageremployee.data.shiftTitle
import com.example.ptmanageremployee.data.swapStatusBadge
import com.example.ptmanageremployee.data.swapStatusLabel

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

    private fun loadNotices(view: View) {
        val workplaceId = TokenStore.workplaceId
        val panel = view.findViewById<LinearLayout>(R.id.panel_notice)
        val empty = view.findViewById<TextView>(R.id.tv_notice_empty)
        panel.removeAllExcept(R.id.tv_notice_empty)
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        launchApi {
            val notices = Network.api.getNotices(workplaceId, page = 0, size = 50).content
            // 공지 탭 진입 시 읽음 처리(레드 닷 해제).
            runCatching { Network.api.markNoticesRead() }
            empty.visibility = if (notices.isEmpty()) View.VISIBLE else View.GONE
            notices.take(previewCount).forEach { notice ->
                panel.addItem(R.layout.item_notice) { card ->
                    card.text(R.id.tv_title, notice.title ?: "(제목 없음)")
                    card.findViewById<TextView>(R.id.tv_content).apply {
                        maxLines = 1
                        text = firstLine(notice.body)
                    }
                    card.text(R.id.tv_meta, noticeMeta(notice))
                    card.setOnClickListener {
                        startActivity(
                            Intent(requireContext(), NoticeDetailActivity::class.java)
                                .putExtra(Extras.NOTICE_ID, notice.id)
                        )
                    }
                }
            }
        }
    }

    private fun loadHandovers(view: View) {
        val workplaceId = TokenStore.workplaceId
        val panel = view.findViewById<LinearLayout>(R.id.panel_handover)
        val empty = view.findViewById<TextView>(R.id.tv_handover_empty)
        panel.removeAllExcept(R.id.tv_handover_empty)
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        launchApi {
            val notes = Network.api.getHandovers(workplaceId, null)
            empty.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
            notes.take(previewCount).forEach { note ->
                panel.addItem(R.layout.item_handover) { card ->
                    card.text(R.id.tv_category, handoverCategoryLabel(note.category))
                    card.text(R.id.tv_title, note.title)
                    card.findViewById<TextView>(R.id.tv_content).apply {
                        maxLines = 1
                        text = firstLine(note.content)
                    }
                    card.text(R.id.tv_meta, handoverMeta(note))
                    // 미리보기라 삭제는 목록 화면에서만 — btn_delete 는 기본 gone 유지.
                    card.setOnClickListener {
                        startActivity(Intent(requireContext(), HandoverListActivity::class.java))
                    }
                }
            }
        }
    }

    private fun loadSwaps(view: View) {
        val workplaceId = TokenStore.workplaceId
        val panel = view.findViewById<LinearLayout>(R.id.panel_swap)
        val empty = view.findViewById<TextView>(R.id.tv_swap_empty)
        panel.removeAllExcept(R.id.tv_swap_empty)
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        launchApi {
            // 다른 직원의 대타요청과 내가 신청한 대타요청을 각각 최근 1건씩 노출한다.
            val open = Network.api.getSwapRequests(workplaceId, view = "open")
            val mine = Network.api.getSwapRequests(workplaceId, view = "mine")
            if (open.isEmpty() && mine.isEmpty()) {
                empty.visibility = View.VISIBLE
                return@launchApi
            }
            empty.visibility = View.GONE
            open.firstOrNull()?.let {
                addSwapCaption(panel, "다른 직원의 대타요청")
                // 지원 가능한(다른 직원의) 대타 요청. 탭 → 상세(지원).
                addSwapCard(panel, it, "지원 가능", badgeBg = null)
            }
            mine.firstOrNull()?.let {
                addSwapCaption(panel, "내가 신청한 대타요청")
                // 내가 신청한 대타 요청은 진행 상태를 배지로 보여준다.
                addSwapCard(panel, it, swapStatusLabel(it.status), swapStatusBadge(it.status))
            }
        }
    }

    /** 대타 요청 카드 1건. 탭 → 상세. */
    private fun addSwapCard(panel: LinearLayout, req: SwapRequestDto, badgeText: String, badgeBg: Int?) {
        panel.addItem(R.layout.item_swap) { row ->
            row.text(R.id.tv_title, shiftTitle(req))
            row.text(R.id.tv_sub, req.reason ?: "사유 없음")
            val badge = row.findViewById<TextView>(R.id.tv_badge)
            badge.text = badgeText
            badgeBg?.let { badge.setBackgroundResource(it) }
            row.setOnClickListener { openSwapDetail(req.id) }
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

    /** 미리보기 한 줄용: 줄바꿈을 공백으로 접어 첫 줄만(넘치면 …) 보이게 한다. */
    private fun firstLine(text: String?): String =
        (text ?: "").replace("\n", " ").replace("\r", " ").trim()
}
