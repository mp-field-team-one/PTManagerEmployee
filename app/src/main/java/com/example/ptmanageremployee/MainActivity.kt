package com.example.ptmanageremployee

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.Push
import com.example.ptmanageremployee.data.TokenStore
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val home by lazy { HomeFragment() }
    private val schedule by lazy { ScheduleFragment() }
    private val communication by lazy { CommunicationFragment() }
    private val my by lazy { MyFragment() }

    private val notifPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val host = findViewById<View>(R.id.nav_host)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            host.updatePadding(top = bars.top)
            bottomNav.updatePadding(bottom = bars.bottom)
            insets
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> show(home)
                R.id.nav_schedule -> show(schedule)
                R.id.nav_communication -> {
                    show(communication)
                    // 공지 탭에 들어가면 읽음 처리되므로 레드닷 제거.
                    bottomNav.removeBadge(R.id.nav_communication)
                }
                R.id.nav_my -> show(my)
                else -> return@setOnItemSelectedListener false
            }
            true
        }
        if (savedInstanceState == null) bottomNav.selectedItemId = R.id.nav_home
        loadNoticeBadge(bottomNav)

        // 알림 권한(Android 13+) 요청 후 FCM 토큰을 백엔드에 등록한다.
        requestNotifPermission()
        Push.registerCurrentToken()
    }

    private fun requestNotifPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** 미확인 공지 여부(GET /api/notices/unread)를 소통 탭 레드닷으로 표시한다. */
    private fun loadNoticeBadge(bottomNav: BottomNavigationView) {
        val workplaceId = TokenStore.workplaceId
        if (workplaceId <= 0) return
        lifecycleScope.launch {
            val hasUnread = runCatching {
                Network.api.getNoticeUnread(workplaceId).hasUnread
            }.getOrDefault(false)
            if (hasUnread && bottomNav.selectedItemId != R.id.nav_communication) {
                bottomNav.getOrCreateBadge(R.id.nav_communication)
            }
        }
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host, fragment)
            .commit()
    }
}
