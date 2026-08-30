package com.example.ptmanageremployee

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.TokenStore
import com.example.ptmanageremployee.data.UserDto

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
        launchApi {
            val members = Network.api.getMembers(workplaceId)
            countLabel.text = "멤버 ${members.size}명"
            members.forEach { member ->
                container.addItem(R.layout.item_member) { row ->
                    row.text(R.id.tv_name, member.name ?: "이름 없음")
                    row.text(R.id.tv_sub, roleLabel(member))
                    val tag = row.findViewById<TextView>(R.id.tv_tag)
                    tag.visibility = if (member.id == TokenStore.userId) View.VISIBLE else View.GONE
                }
            }
        }
    }

    private fun roleLabel(user: UserDto): String = when (user.role) {
        "EMPLOYER" -> "사장님"
        "EMPLOYEE" -> "알바"
        else -> user.email ?: ""
    }
}
