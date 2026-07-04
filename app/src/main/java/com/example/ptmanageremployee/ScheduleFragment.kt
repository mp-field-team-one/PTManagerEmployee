package com.example.ptmanageremployee

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployee.data.Extras
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.scheduleDateLabel
import com.example.ptmanageremployee.data.shiftTimeRange
import com.example.ptmanageremployee.data.toUserMessage
import com.example.ptmanageremployee.data.weekRangeLabel
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

class ScheduleFragment : Fragment() {

    private val cellIds = intArrayOf(
        R.id.day_cell_0, R.id.day_cell_1, R.id.day_cell_2, R.id.day_cell_3,
        R.id.day_cell_4, R.id.day_cell_5, R.id.day_cell_6,
    )
    private val numIds = intArrayOf(
        R.id.day_num_0, R.id.day_num_1, R.id.day_num_2, R.id.day_num_3,
        R.id.day_num_4, R.id.day_num_5, R.id.day_num_6,
    )

    private val weekDates = ArrayList<LocalDate>(7)
    private var anchorMonday: LocalDate = mondayOf(LocalDate.now())
    private var selectedDate: LocalDate = LocalDate.now()
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
        // 대타요청은 근무 정보(ShiftDetailActivity)의 '대타요청하기', 대타 목록은 소통 탭에서 진행한다.
        view.findViewById<View>(R.id.btn_availability).setOnClickListener {
            Toast.makeText(requireContext(), "근무 가능 시간 등록은 준비 중이에요", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.btn_prev_week).setOnClickListener { shiftWeek(view, -7) }
        view.findViewById<View>(R.id.btn_next_week).setOnClickListener { shiftWeek(view, 7) }
        view.findViewById<View>(R.id.btn_this_week).setOnClickListener {
            anchorMonday = mondayOf(LocalDate.now())
            buildWeek(view)
            selectDate(view, LocalDate.now())
        }
        buildWeek(view)
        selectDate(view, selectedDate)
    }

    /** 현재 선택된 요일(월~일 오프셋)을 유지한 채 다음/이전 주로 이동한다. */
    private fun shiftWeek(view: View, days: Long) {
        val offset = DayOfWeek.MONDAY.value.let { selectedDate.dayOfWeek.value - it }
        anchorMonday = anchorMonday.plusDays(days)
        buildWeek(view)
        selectDate(view, anchorMonday.plusDays(offset.toLong()))
    }

    /** 표시 중인 주(월요일 시작)의 7일을 계산해 날짜 숫자와 클릭 리스너를 세팅한다. */
    private fun buildWeek(view: View) {
        weekDates.clear()
        for (i in 0..6) weekDates.add(anchorMonday.plusDays(i.toLong()))
        view.findViewById<TextView>(R.id.tv_month).text = weekRangeLabel(anchorMonday)
        for (i in 0..6) {
            val date = weekDates[i]
            view.findViewById<TextView>(numIds[i]).text = date.dayOfMonth.toString()
            view.findViewById<View>(cellIds[i]).setOnClickListener { selectDate(view, date) }
        }
    }

    private fun mondayOf(date: LocalDate): LocalDate =
        date.minusDays((date.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())

    private fun selectDate(view: View, date: LocalDate) {
        selectedDate = date
        for (i in 0..6) {
            val selected = weekDates[i] == date
            val cell = view.findViewById<View>(cellIds[i])
            val num = view.findViewById<TextView>(numIds[i])
            if (selected) {
                cell.setBackgroundResource(R.drawable.bg_day_selected)
                num.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            } else {
                cell.setBackgroundResource(0)
                num.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
            }
        }
        loadMyShift(view)
    }

    private fun loadMyShift(view: View) {
        val date = selectedDate.toString()
        val card = view.findViewById<View>(R.id.item_my_shift)
        val empty = view.findViewById<TextView>(R.id.tv_shift_empty)
        val timeView = view.findViewById<TextView>(R.id.tv_my_shift_time)
        val subView = view.findViewById<TextView>(R.id.tv_my_shift_sub)
        view.findViewById<TextView>(R.id.tv_schedule_date).text = scheduleDateLabel(selectedDate)
        lifecycleScope.launch {
            try {
                // 선택한 날짜의 내 근무를 조회한다.
                val next = Network.api.getShifts(employeeId = "me", from = date, to = date)
                    .sortedBy { it.startTime }
                    .firstOrNull()
                if (next == null) {
                    card.visibility = View.GONE
                    empty.visibility = View.VISIBLE
                    nextShiftId = -1
                } else {
                    card.visibility = View.VISIBLE
                    empty.visibility = View.GONE
                    nextShiftId = next.id
                    timeView.text = shiftTimeRange(next.startTime, next.endTime)
                    val checkedIn = next.checkedInAt != null
                    subView.text = if (checkedIn) "출근 완료" else "출근 전"
                    val bg = if (checkedIn) R.color.cat_handover_bg else R.color.warn_bg
                    val fg = if (checkedIn) R.color.cat_handover else R.color.warn_orange
                    subView.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(requireContext(), bg))
                    subView.setTextColor(ContextCompat.getColor(requireContext(), fg))
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
