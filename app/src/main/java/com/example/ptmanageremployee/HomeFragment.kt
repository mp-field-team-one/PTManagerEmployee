@file:Suppress("HardcodedStringLiteral")

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
import com.example.ptmanageremployee.data.relativeTime
import com.example.ptmanageremployee.data.shiftTimeRange
import com.example.ptmanageremployee.data.toUserMessage
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class HomeFragment : Fragment() {

    /** 오늘 근무 카드가 가리키는 근무 ID. 없으면 -1. */
    private var todayShiftId: Long = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tv_greeting).text = getString(R.string.home_greeting, TokenStore.name ?: getString(R.string.home_default_user))

        view.findViewById<View>(R.id.btn_checkin).setOnClickListener {
            startActivity(shiftIntent(CheckInActivity::class.java))
        }
        view.findViewById<View>(R.id.card_today).setOnClickListener {
            if (todayShiftId > 0) startActivity(shiftIntent(ShiftDetailActivity::class.java))
        }
        view.findViewById<View>(R.id.card_week).setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottom_nav)
                ?.selectedItemId = R.id.nav_schedule
        }
        view.findViewById<View>(R.id.btn_bell).setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }
        view.findViewById<View>(R.id.card_notice).setOnClickListener {
            startActivity(Intent(requireContext(), NoticeListActivity::class.java))
        }
        view.findViewById<View>(R.id.tv_swap_status).setOnClickListener {
            startActivity(Intent(requireContext(), SwapListActivity::class.java))
        }
        view.findViewById<View>(R.id.card_handover).setOnClickListener {
            startActivity(Intent(requireContext(), HandoverListActivity::class.java))
        }
        view.findViewById<View>(R.id.card_swap_req).setOnClickListener {
            startActivity(Intent(requireContext(), SwapListActivity::class.java))
        }

        loadToday(view)
        loadDashboard(view)
    }

    /** 이번 주 근무 요약, 대타 지원 현황, 최신 소식, 알림 뱃지를 로드한다. */
    private fun loadDashboard(view: View) {
        lifecycleScope.launch {
            listOf(
                async { loadWeekSummary(view) },
                async { loadSwapStatus(view) },
                async { loadLatestNotice(view) },
                async { loadLatestHandover(view) },
                async { loadOpenSwapRequest(view) },
                async { loadBellBadge(view) },
            ).awaitAll()
        }
    }

    /** 이번 주(월~일) 내 근무 건수와 총 근무 시간을 계산해 표시한다. */
    private suspend fun loadWeekSummary(view: View) {
        val today = LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val sunday = monday.plusDays(6)
        val summaryView = view.findViewById<TextView>(R.id.tv_week_summary)
        val shifts = runCatching {
            Network.api.getShifts(employeeId = "me", from = monday.toString(), to = sunday.toString())
        }.getOrNull() ?: run { summaryView.text = "—"; return }

        var minutes = 0L
        for (s in shifts) minutes += shiftMinutes(s.startTime, s.endTime)
        val h = minutes / 60
        val m = minutes % 60
        val hoursText = if (m == 0L) "${h}시간" else "${h}시간 ${m}분"
        summaryView.text = "근무 ${shifts.size}건 · $hoursText ›"
    }

    /** startTime/endTime("HH:mm:ss") 사이 분. 종료가 시작보다 이르면 자정 넘김으로 보고 +24h. */
    private fun shiftMinutes(start: String?, end: String?): Long {
        val s = runCatching { LocalTime.parse(start) }.getOrNull() ?: return 0
        val e = runCatching { LocalTime.parse(end) }.getOrNull() ?: return 0
        val diff = java.time.Duration.between(s, e).toMinutes()
        return if (diff < 0) diff + 24 * 60 else diff
    }

    /** 내가 지원한 대타 중 대기 중(PENDING) 건수를 요약 줄로 노출한다. 없으면 숨김. */
    private suspend fun loadSwapStatus(view: View) {
        val statusView = view.findViewById<TextView>(R.id.tv_swap_status)
        val count = runCatching {
            Network.api.getMySwapApplications(status = "PENDING").size
        }.getOrDefault(0)
        if (count > 0) {
            statusView.text = "대타 지원 ${count}건 대기 중 ›"
            statusView.visibility = View.VISIBLE
        } else {
            statusView.visibility = View.GONE
        }
    }

    /** 최신 공지 1건의 제목·내용을 공지 카드에 표시한다. */
    private suspend fun loadLatestNotice(view: View) {
        val titleView = view.findViewById<TextView>(R.id.tv_notice_title)
        val bodyView = view.findViewById<TextView>(R.id.tv_notice_body)
        val notice = runCatching {
            Network.api.getNotices(workplaceId = TokenStore.workplaceId, size = 1).content.firstOrNull()
        }.getOrNull()
        view.findViewById<TextView>(R.id.tv_notice_time).text = relativeTime(notice?.createdAt)
        if (notice == null) {
            titleView.text = getString(R.string.home_no_notice)
            bodyView.text = ""
        } else {
            titleView.text = notice.title ?: getString(R.string.common_no_title)
            bodyView.text = notice.body ?: ""
        }
    }

    /** 최신 인수인계 1건의 제목·내용을 표시한다(공지 카드와 동일 형식). */
    private suspend fun loadLatestHandover(view: View) {
        val titleView = view.findViewById<TextView>(R.id.tv_handover_title)
        val contentView = view.findViewById<TextView>(R.id.tv_handover_content)
        val handover = runCatching {
            Network.api.getHandovers(workplaceId = TokenStore.workplaceId)
                .maxByOrNull { it.createdAt ?: "" }
        }.getOrNull()
        view.findViewById<TextView>(R.id.tv_handover_time).text = relativeTime(handover?.createdAt)
        if (handover == null) {
            titleView.text = getString(R.string.home_no_handover)
            contentView.text = ""
        } else {
            titleView.text = handover.title ?: getString(R.string.common_no_title)
            contentView.text = handover.content ?: ""
        }
    }

    /** 다른 직원의 열린 대타요청(view=open) 중 가장 최근 1건을 표시한다. */
    private suspend fun loadOpenSwapRequest(view: View) {
        val titleView = view.findViewById<TextView>(R.id.tv_swap_req_title)
        val subView = view.findViewById<TextView>(R.id.tv_swap_req_sub)
        val req = runCatching {
            Network.api.getSwapRequests(workplaceId = TokenStore.workplaceId, view = "open")
                .maxByOrNull { it.createdAt ?: "" }
        }.getOrNull()
        view.findViewById<TextView>(R.id.tv_swap_req_time).text = relativeTime(req?.createdAt)
        if (req == null) {
            titleView.text = getString(R.string.home_no_swap_request)
            subView.text = ""
        } else {
            val shift = req.shift
            titleView.text = if (shift != null)
                "${shift.workDate ?: ""} ${shiftTimeRange(shift.startTime, shift.endTime)}".trim()
            else getString(R.string.home_swap_request_number, req.id)
            subView.text = req.reason ?: getString(R.string.home_no_reason)
        }
    }

    /** 안 읽은 알림 개수(GET /api/notifications/unread-count)로 종 아이콘 빨간 점을 표시한다. */
    private suspend fun loadBellBadge(view: View) {
        val count = runCatching { Network.api.getNotificationUnreadCount().count }.getOrDefault(0)
        view.findViewById<View>(R.id.bell_dot).visibility =
            if (count > 0) View.VISIBLE else View.GONE
    }

    private fun loadToday(view: View) {
        val today = LocalDate.now().toString()
        val labelView = view.findViewById<TextView>(R.id.tv_today_label)
        val timeView = view.findViewById<TextView>(R.id.tv_today_time)
        val withView = view.findViewById<TextView>(R.id.tv_today_with)
        val chevronView = view.findViewById<View>(R.id.tv_today_chevron)
        lifecycleScope.launch {
            try {
                val shifts = Network.api.getShifts(employeeId = "me", from = today, to = today)
                val shift: ShiftDto? = shifts.firstOrNull()
                if (shift == null) {
                    labelView.text = getString(R.string.home_no_shift_label)
                    timeView.text = getString(R.string.home_no_shift_time)
                    withView.text = getString(R.string.home_no_shift_desc)
                    todayShiftId = -1
                    chevronView.visibility = View.GONE
                } else {
                    todayShiftId = shift.id
                    labelView.text = getString(R.string.home_today_shift)
                    timeView.text = shiftTimeRange(shift.startTime, shift.endTime)
                    withView.text = shift.checkedInAt?.let { getString(R.string.home_checkin_done) } ?: getString(R.string.home_before_checkin)
                    chevronView.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), e.toUserMessage(), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun shiftIntent(target: Class<*>): Intent =
        Intent(requireContext(), target).putExtra(Extras.SHIFT_ID, todayShiftId)
}
