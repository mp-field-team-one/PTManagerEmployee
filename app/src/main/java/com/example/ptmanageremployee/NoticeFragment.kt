package com.example.ptmanageremployee

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class NoticeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_notice, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val open = { _: View ->
            startActivity(Intent(requireContext(), NoticeDetailActivity::class.java))
        }
        view.findViewById<View>(R.id.notice_1).setOnClickListener(open)
        view.findViewById<View>(R.id.notice_2).setOnClickListener(open)
        view.findViewById<View>(R.id.notice_3).setOnClickListener(open)
    }
}
