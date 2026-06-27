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
import com.example.ptmanageremployee.data.shiftTimeRange
import com.example.ptmanageremployee.data.toUserMessage
import kotlinx.coroutines.launch
import java.time.LocalDate

class ScheduleFragment : Fragment() {

    private var nextShiftId: Long = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_schedule, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.item_my_shift).setOnClickListener {
            if (nextShiftId > 0) {
                startActivity(
                    Intent(requireContext(), ShiftDetailActivity::class.java)
                        .putExtra(Extras.SHIFT_ID, nextShiftId)
                )
            }
        }
        loadMyShifts(view)
    }

    private fun loadMyShifts(view: View) {
        val today = LocalDate.now().toString()
        val dateView = view.findViewById<TextView>(R.id.tv_schedule_date)
        val timeView = view.findViewById<TextView>(R.id.tv_my_shift_time)
        val subView = view.findViewById<TextView>(R.id.tv_my_shift_sub)
        lifecycleScope.launch {
            try {
                // 오늘 이후의 내 근무를 조회해 가장 가까운 근무를 표시한다.
                val shifts = Network.api.getShifts(employeeId = "me", from = today)
                    .sortedWith(compareBy({ it.workDate }, { it.startTime }))
                val next = shifts.firstOrNull()
                if (next == null) {
                    dateView.text = "예정된 근무가 없습니다."
                    timeView.text = "—"
                    subView.text = ""
                    nextShiftId = -1
                } else {
                    nextShiftId = next.id
                    dateView.text = next.workDate ?: ""
                    timeView.text = shiftTimeRange(next.startTime, next.endTime)
                    subView.text = next.checkedInAt?.let { "출근 완료" } ?: "출근 전"
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
