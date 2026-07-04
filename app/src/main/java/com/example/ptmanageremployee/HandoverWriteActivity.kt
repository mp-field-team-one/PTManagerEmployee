package com.example.ptmanageremployee

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployee.data.CreateHandoverRequest
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.TokenStore
import com.example.ptmanageremployee.data.toUserMessage
import kotlinx.coroutines.launch

/** 인수인계 노트 작성. 분류(칩) 1개 선택 + 내용. */
class HandoverWriteActivity : AppCompatActivity() {

    private var selectedCategory: String = "STOCK"

    private val chips: List<Pair<Int, String>> by lazy {
        listOf(
            R.id.chip_stock to "STOCK",
            R.id.chip_device to "DEVICE",
            R.id.chip_customer to "CUSTOMER",
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_handover_write)
        findViewById<View>(R.id.write_root).applySystemBarInsets()
        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        chips.forEach { (id, category) ->
            findViewById<TextView>(id).setOnClickListener {
                selectedCategory = category
                renderChips()
            }
        }
        renderChips()

        val titleInput = findViewById<EditText>(R.id.input_title)
        val contentInput = findViewById<EditText>(R.id.input_content)
        findViewById<View>(R.id.btn_publish).setOnClickListener { btn ->
            val title = titleInput.text.toString().trim()
            val content = contentInput.text.toString().trim()
            if (title.isEmpty()) {
                Toast.makeText(this, "제목을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (content.isEmpty()) {
                Toast.makeText(this, "내용을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val workplaceId = TokenStore.workplaceId
            if (workplaceId <= 0) {
                Toast.makeText(this, "소속 매장이 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btn.isEnabled = false
            lifecycleScope.launch {
                try {
                    Network.api.createHandover(
                        CreateHandoverRequest(workplaceId, selectedCategory, title, content),
                    )
                    Toast.makeText(this@HandoverWriteActivity, "등록했어요", Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@HandoverWriteActivity, e.toUserMessage(), Toast.LENGTH_SHORT).show()
                    btn.isEnabled = true
                }
            }
        }
    }

    private fun renderChips() {
        chips.forEach { (id, category) ->
            val selected = category == selectedCategory
            findViewById<TextView>(id).apply {
                setBackgroundResource(
                    if (selected) R.drawable.bg_chip_selected else R.drawable.bg_chip_outline,
                )
                setTextColor(getColor(if (selected) R.color.brand_blue else R.color.text_secondary))
            }
        }
    }
}
