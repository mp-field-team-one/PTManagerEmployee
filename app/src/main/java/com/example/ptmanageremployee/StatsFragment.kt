package com.example.ptmanageremployee

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.WeeklyCost
import com.example.ptmanageremployee.data.won
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

/** 직원 본인의 이번 달 급여(실근태 기준)를 보여주는 통계 탭. */
class StatsFragment : Fragment() {

    // 주차별 막대: 인덱스 0~3 = 1주~4주
    private val barIds = intArrayOf(R.id.bar_w1, R.id.bar_w2, R.id.bar_w3, R.id.bar_w4)
    private val weekLabelIds = intArrayOf(R.id.tv_w1, R.id.tv_w2, R.id.tv_w3, R.id.tv_w4)

    // 현재 보고 있는 월. 우측 상단 셀렉터로 변경한다.
    private var selected: YearMonth = YearMonth.now()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.tv_month_selector).setOnClickListener { pickMonth(view) }
        loadStats(view)
    }

    private fun loadStats(view: View) {
        val yearMonth = selected.toString() // 예: 2026-07
        view.findViewById<TextView>(R.id.tv_month_selector).text = "${selected.monthValue}월 ▾"
        view.findViewById<TextView>(R.id.tv_pay_label).text = "${selected.monthValue}월 급여"

        // 현재 월일 때만 이번 주를 강조한다. 지난 달은 강조 없음(-1).
        val activeWeek = if (selected == YearMonth.now()) currentWeekIndex(LocalDate.now()) else -1
        lifecycleScope.launch {
            val pay = runCatching { Network.api.getMyPayroll(yearMonth) }.getOrNull() ?: return@launch
            val hours = pay.workedMinutes / 60
            val mins = pay.workedMinutes % 60
            val hoursText = if (mins == 0L) "${hours}시간" else "${hours}시간 ${mins}분"

            view.findViewById<TextView>(R.id.tv_pay_amount).text = won(pay.amount)
            view.findViewById<TextView>(R.id.tv_pay_sub).text =
                "$hoursText 근무 · 시급 ${won(pay.hourlyWage.toLong())}"
            view.findViewById<TextView>(R.id.tv_worked_hours).text = hoursText
            view.findViewById<TextView>(R.id.tv_hourly_wage).text = won(pay.hourlyWage.toLong())
            renderWeeklyChart(view, pay.weeks, activeWeek)
        }
    }

    /** 최근 12개월 중 하나를 골라 급여를 다시 로드한다. */
    private fun pickMonth(view: View) {
        val now = YearMonth.now()
        val months = (0 until 12).map { now.minusMonths(it.toLong()) }
        val labels = months.map { "${it.year}년 ${it.monthValue}월" }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("월 선택")
            .setItems(labels) { _, which ->
                selected = months[which]
                loadStats(view)
            }
            .show()
    }

    private fun renderWeeklyChart(view: View, weeks: List<WeeklyCost>, activeWeek: Int) {
        // week 값으로 정렬해 1~4주 순서를 보장한다.
        val amounts = LongArray(4)
        weeks.forEach { if (it.week in 1..4) amounts[it.week - 1] = it.amount }
        val max = amounts.max().coerceAtLeast(1L)
        val density = resources.displayMetrics.density

        for (i in 0..3) {
            val heightDp = MIN_BAR_DP + (MAX_BAR_DP - MIN_BAR_DP) * (amounts[i].toFloat() / max)
            val bar = view.findViewById<View>(barIds[i])
            bar.layoutParams = bar.layoutParams.apply { height = (heightDp * density).toInt() }
            val active = i == activeWeek
            bar.setBackgroundResource(if (active) R.drawable.bg_bar_active else R.drawable.bg_bar)
            view.findViewById<TextView>(weekLabelIds[i]).setTextColor(
                ContextCompat.getColor(requireContext(), if (active) R.color.brand_blue else R.color.text_hint),
            )
        }
    }

    /** 백엔드와 동일한 버킷: 1–7→0, 8–14→1, 15–21→2, 22–말일→3 */
    private fun currentWeekIndex(date: LocalDate): Int =
        ((date.dayOfMonth - 1) / 7).coerceAtMost(3)

    companion object {
        private const val MIN_BAR_DP = 6f
        private const val MAX_BAR_DP = 68f
    }
}
