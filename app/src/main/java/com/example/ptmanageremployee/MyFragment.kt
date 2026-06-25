package com.example.ptmanageremployee

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment

class MyFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_my, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.row_profile).setOnClickListener {
            startActivity(Intent(requireContext(), ProfileEditActivity::class.java))
        }
        view.findViewById<View>(R.id.row_members).setOnClickListener {
            startActivity(Intent(requireContext(), MembersActivity::class.java))
        }
        view.findViewById<View>(R.id.row_noti).setOnClickListener {
            Toast.makeText(requireContext(), "알림 설정", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.row_logout).setOnClickListener {
            val i = Intent(requireContext(), LoginActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        }
    }
}
