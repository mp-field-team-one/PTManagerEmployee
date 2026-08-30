package com.example.ptmanageremployee

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.ptmanageremployee.data.CreateHandoverRequest
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.TokenStore

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
                toast("제목을 입력해 주세요.")
                return@setOnClickListener
            }
            if (content.isEmpty()) {
                toast("내용을 입력해 주세요.")
                return@setOnClickListener
            }
            val workplaceId = TokenStore.workplaceId
            if (workplaceId <= 0) {
                toast("소속 매장이 없습니다.")
                return@setOnClickListener
            }
            launchApi(btn) {
                Network.api.createHandover(
                    CreateHandoverRequest(workplaceId, selectedCategory, title, content),
                )
                toast("등록했어요")
                finish()
            }
        }
    }

    private fun renderChips() {
        chips.forEach { (id, category) ->
            findViewById<TextView>(id).setChipSelected(category == selectedCategory)
        }
    }
}
