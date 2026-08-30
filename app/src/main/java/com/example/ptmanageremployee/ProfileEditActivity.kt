package com.example.ptmanageremployee

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.TokenStore
import com.example.ptmanageremployee.data.UpdateProfileRequest

class ProfileEditActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_edit)
        findViewById<View>(R.id.profile_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        val nameInput = findViewById<EditText>(R.id.input_name)
        nameInput.setText(TokenStore.name ?: "")

        findViewById<TextView>(R.id.btn_save).setOnClickListener { btn ->
            val name = nameInput.text.toString().trim()
            if (name.isEmpty()) {
                toast("이름을 입력해 주세요.")
                return@setOnClickListener
            }
            launchApi(btn) {
                TokenStore.updateUser(Network.api.updateProfile(UpdateProfileRequest(name)))
                toast("저장되었습니다")
                finish()
            }
        }
    }
}
