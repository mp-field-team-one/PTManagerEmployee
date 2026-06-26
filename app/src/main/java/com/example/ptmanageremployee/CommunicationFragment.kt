package com.example.ptmanageremployee

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class CommunicationFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_communication, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val chipNotice = view.findViewById<View>(R.id.chip_notice)
        val chipMessenger = view.findViewById<View>(R.id.chip_messenger)
        val panelNotice = view.findViewById<View>(R.id.panel_notice)
        val panelMessenger = view.findViewById<View>(R.id.panel_messenger)

        fun select(noticeSel: Boolean) {
            chipNotice.setBackgroundResource(if (noticeSel) R.drawable.bg_pill_active else R.drawable.bg_pill)
            chipMessenger.setBackgroundResource(if (noticeSel) R.drawable.bg_pill else R.drawable.bg_pill_active)
            (chipNotice as? android.widget.TextView)?.setTextColor(
                resources.getColor(if (noticeSel) R.color.white else R.color.text_tertiary, null)
            )
            (chipMessenger as? android.widget.TextView)?.setTextColor(
                resources.getColor(if (noticeSel) R.color.text_tertiary else R.color.white, null)
            )
            panelNotice.visibility = if (noticeSel) View.VISIBLE else View.GONE
            panelMessenger.visibility = if (noticeSel) View.GONE else View.VISIBLE
        }
        chipNotice.setOnClickListener { select(true) }
        chipMessenger.setOnClickListener { select(false) }
        select(true)

        val openNotice = { _: View ->
            startActivity(Intent(requireContext(), NoticeDetailActivity::class.java))
        }
        view.findViewById<View>(R.id.notice_1).setOnClickListener(openNotice)
        view.findViewById<View>(R.id.notice_2).setOnClickListener(openNotice)
        view.findViewById<View>(R.id.notice_3).setOnClickListener(openNotice)
    }
}
