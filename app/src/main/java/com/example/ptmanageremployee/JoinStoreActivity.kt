package com.example.ptmanageremployee

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployee.data.CreateJoinRequest
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.TokenStore
import com.example.ptmanageremployee.data.toUserMessage
import kotlinx.coroutines.launch

class JoinStoreActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_join_store)
        findViewById<View>(R.id.join_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        val pendingState = findViewById<View>(R.id.state_pending)
        val inputState = findViewById<View>(R.id.state_input)
        val codeInput = findViewById<EditText>(R.id.input_code)
        val submitBtn = findViewById<TextView>(R.id.btn_submit_code)

        submitBtn.setOnClickListener {
            val code = codeInput.text.toString().trim()
            if (code.isEmpty()) {
                toast("초대 코드를 입력해 주세요.")
                return@setOnClickListener
            }
            submitBtn.isEnabled = false
            lifecycleScope.launch {
                try {
                    Network.api.createJoinRequest(CreateJoinRequest(code))
                    inputState.visibility = View.GONE
                    pendingState.visibility = View.VISIBLE
                } catch (e: Exception) {
                    toast(e.toUserMessage())
                } finally {
                    submitBtn.isEnabled = true
                }
            }
        }

        // 승인 여부를 /me 로 확인한 뒤 메인으로 진입한다.
        findViewById<View>(R.id.btn_enter_app).setOnClickListener { btn ->
            btn.isEnabled = false
            lifecycleScope.launch {
                try {
                    val me = Network.api.me()
                    TokenStore.updateUser(me)
                    if (TokenStore.workplaceId > 0) {
                        startActivity(Intent(this@JoinStoreActivity, MainActivity::class.java))
                        finishAffinity()
                    } else {
                        toast("아직 사장님 승인 대기 중입니다.")
                        btn.isEnabled = true
                    }
                } catch (e: Exception) {
                    toast(e.toUserMessage())
                    btn.isEnabled = true
                }
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
