package com.example.ptmanageremployee

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.TokenStore
import kotlinx.coroutines.launch

class MyFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_my, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tv_my_name).text = "${TokenStore.name ?: "사용자"}님"
        view.findViewById<TextView>(R.id.tv_my_sub).text = TokenStore.email ?: "알바"

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
            // 서버 로그아웃은 베스트 에포트로 호출하고, 로컬 세션을 비운 뒤 로그인 화면으로.
            lifecycleScope.launch {
                runCatching { Network.api.logout() }
                TokenStore.clear()
                val i = Intent(requireContext(), LoginActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(i)
            }
        }
    }
}
