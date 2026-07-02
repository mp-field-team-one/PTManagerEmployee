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
import com.example.ptmanageremployee.data.Extras
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.ShiftDto
import com.example.ptmanageremployee.data.TokenStore
import com.example.ptmanageremployee.data.shiftTimeRange
import com.example.ptmanageremployee.data.toUserMessage
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeFragment : Fragment() {

    /** 오늘 근무 카드가 가리키는 근무 ID. 없으면 -1. */
    private var todayShiftId: Long = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tv_greeting).text = "안녕하세요, ${TokenStore.name ?: "사용자"}님"

        view.findViewById<View>(R.id.btn_checkin).setOnClickListener {
            startActivity(shiftIntent(CheckInActivity::class.java))
        }
        view.findViewById<View>(R.id.card_today).setOnClickListener {
            if (todayShiftId > 0) startActivity(shiftIntent(ShiftDetailActivity::class.java))
        }
        view.findViewById<View>(R.id.btn_all_schedule).setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottom_nav)
                ?.selectedItemId = R.id.nav_schedule
        }
        view.findViewById<View>(R.id.btn_bell).setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }
        view.findViewById<View>(R.id.btn_sub_request).setOnClickListener {
            startActivity(shiftIntent(SubRequestActivity::class.java))
        }
        view.findViewById<View>(R.id.btn_swap_list).setOnClickListener {
            startActivity(Intent(requireContext(), SwapListActivity::class.java))
        }

        loadToday(view)
        loadBellBadge(view)
    }

    /** 안 읽은 알림 개수(GET /api/notifications/unread-count)로 종 아이콘 빨간 점을 표시한다. */
    private fun loadBellBadge(view: View) {
        lifecycleScope.launch {
            val count = runCatching { Network.api.getNotificationUnreadCount().count }.getOrDefault(0)
            view.findViewById<View>(R.id.bell_dot).visibility =
                if (count > 0) View.VISIBLE else View.GONE
        }
    }

    private fun loadToday(view: View) {
        val today = LocalDate.now().toString()
        val labelView = view.findViewById<TextView>(R.id.tv_today_label)
        val timeView = view.findViewById<TextView>(R.id.tv_today_time)
        val withView = view.findViewById<TextView>(R.id.tv_today_with)
        lifecycleScope.launch {
            try {
                val shifts = Network.api.getShifts(employeeId = "me", from = today, to = today)
                val shift: ShiftDto? = shifts.firstOrNull()
                if (shift == null) {
                    labelView.text = "오늘 근무 없음"
                    timeView.text = "—"
                    withView.text = "오늘은 예정된 근무가 없어요."
                    todayShiftId = -1
                } else {
                    todayShiftId = shift.id
                    labelView.text = "오늘 근무"
                    timeView.text = shiftTimeRange(shift.startTime, shift.endTime)
                    withView.text = shift.checkedInAt?.let { "출근 완료" } ?: "아직 출근 전이에요."
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shiftIntent(target: Class<*>): Intent =
        Intent(requireContext(), target).putExtra(Extras.SHIFT_ID, todayShiftId)
}
