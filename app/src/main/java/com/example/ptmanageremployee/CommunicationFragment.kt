package com.example.ptmanageremployee

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployee.data.Extras
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.NoticeDto
import com.example.ptmanageremployee.data.TokenStore
import com.example.ptmanageremployee.data.toUserMessage
import kotlinx.coroutines.launch

class CommunicationFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_communication, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.btn_handover_list).setOnClickListener {
            startActivity(Intent(requireContext(), HandoverListActivity::class.java))
        }
        loadNotices(view)
    }

    private fun loadNotices(view: View) {
        val workplaceId = TokenStore.workplaceId
        val panel = view.findViewById<LinearLayout>(R.id.panel_notice)
        val empty = view.findViewById<TextView>(R.id.tv_notice_empty)
        if (workplaceId <= 0) {
            empty.visibility = View.VISIBLE
            return
        }
        lifecycleScope.launch {
            try {
                val notices = Network.api.getNotices(workplaceId, page = 0, size = 50).content
                // 공지 탭 진입 시 읽음 처리(레드 닷 해제).
                runCatching { Network.api.markNoticesRead() }
                if (notices.isEmpty()) {
                    empty.visibility = View.VISIBLE
                    return@launch
                }
                val inflater = LayoutInflater.from(requireContext())
                notices.forEach { notice ->
                    val card = inflater.inflate(R.layout.item_notice, panel, false)
                    card.findViewById<TextView>(R.id.tv_title).text = notice.title ?: "(제목 없음)"
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

    private fun noticeMeta(notice: NoticeDto): String {
        val author = notice.authorName ?: "사장님"
        val date = notice.createdAt?.take(10) ?: ""
        return listOf(author, date).filter { it.isNotBlank() }.joinToString(" · ")
    }
}
