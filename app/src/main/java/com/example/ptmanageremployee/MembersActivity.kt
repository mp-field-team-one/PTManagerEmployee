package com.example.ptmanageremployee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.TokenStore
import com.example.ptmanageremployee.data.UserDto
import com.example.ptmanageremployee.data.toUserMessage
import kotlinx.coroutines.launch

class MembersActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_members)
        findViewById<View>(R.id.members_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        loadMembers()
    }

    private fun loadMembers() {
        val workplaceId = TokenStore.workplaceId
        val container = findViewById<LinearLayout>(R.id.members_container)
        val countLabel = findViewById<TextView>(R.id.tv_members_count)
        if (workplaceId <= 0) {
            countLabel.text = "소속된 매장이 없습니다."
            return
        }
        lifecycleScope.launch {
            try {
                val members = Network.api.getMembers(workplaceId)
                countLabel.text = "멤버 ${members.size}명"
                val inflater = LayoutInflater.from(this@MembersActivity)
                members.forEach { member ->
                    val row = inflater.inflate(R.layout.item_member, container, false)
                    row.findViewById<TextView>(R.id.tv_name).text = member.name ?: "이름 없음"
                    row.findViewById<TextView>(R.id.tv_sub).text = roleLabel(member)
                    val tag = row.findViewById<TextView>(R.id.tv_tag)
                    tag.visibility = if (member.id == TokenStore.userId) View.VISIBLE else View.GONE
                    container.addView(row)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MembersActivity, e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun roleLabel(user: UserDto): String = when (user.role) {
        "EMPLOYER" -> "사장님"
        "EMPLOYEE" -> "알바"
        else -> user.email ?: ""
    }
}
