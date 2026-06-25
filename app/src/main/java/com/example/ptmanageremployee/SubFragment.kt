package com.example.ptmanageremployee

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class SubFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_sub, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val open = view.findViewById<View>(R.id.panel_open)
        val mine = view.findViewById<View>(R.id.panel_mine)
        val tabOpen = view.findViewById<View>(R.id.tab_open)
        val tabMine = view.findViewById<View>(R.id.tab_mine)
        fun select(openSel: Boolean) {
            tabOpen.setBackgroundResource(if (openSel) R.drawable.bg_segment_active else 0)
            tabMine.setBackgroundResource(if (openSel) 0 else R.drawable.bg_segment_active)
            open.visibility = if (openSel) View.VISIBLE else View.GONE
            mine.visibility = if (openSel) View.GONE else View.VISIBLE
        }
        tabOpen.setOnClickListener { select(true) }
        tabMine.setOnClickListener { select(false) }
        select(true)

        view.findViewById<View>(R.id.btn_create_sub).setOnClickListener {
            startActivity(Intent(requireContext(), SubRequestActivity::class.java))
        }
    }
}
