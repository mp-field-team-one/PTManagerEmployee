package com.example.ptmanageremployee

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.ptmanageremployee.data.Network
import com.example.ptmanageremployee.data.NotificationSettingUpdate
import com.example.ptmanageremployee.data.Push
import com.example.ptmanageremployee.data.TokenStore
import com.example.ptmanageremployee.data.toUserMessage
import com.example.ptmanageremployee.data.won
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

class MyFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_my, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<TextView>(R.id.tv_my_name).text = "${TokenStore.name ?: "사용자"}님"
        val subView = view.findViewById<TextView>(R.id.tv_my_sub)
        subView.text = TokenStore.email ?: "알바"
        // 소속 매장이 있으면 매장명을 함께 표시한다(GET /api/workplaces/{id}).
        val workplaceId = TokenStore.workplaceId
        if (workplaceId > 0) {
            lifecycleScope.launch {
                runCatching { Network.api.getWorkplace(workplaceId) }.getOrNull()?.let { wp ->
                    subView.text = listOfNotNull(wp.name, "알바").joinToString(" · ")
                }
            }
        }

        loadWeekSummary(view)

        view.findViewById<View>(R.id.row_profile).setOnClickListener {
            startActivity(Intent(requireContext(), ProfileEditActivity::class.java))
        }
        view.findViewById<View>(R.id.row_members).setOnClickListener {
            startActivity(Intent(requireContext(), MembersActivity::class.java))
        }
        view.findViewById<View>(R.id.row_noti).setOnClickListener { openNotificationSetting() }
        view.findViewById<View>(R.id.row_logout).setOnClickListener {
            // 서버 로그아웃은 베스트 에포트로 호출하고, 로컬 세션을 비운 뒤 로그인 화면으로.
            lifecycleScope.launch {
                // 이 기기의 FCM 토큰을 서버에서 먼저 제거(로그인 상태에서 호출).
                runCatching { Push.currentToken()?.let { Network.api.deleteDeviceToken(it) } }
                runCatching { Network.api.logout() }
                Push.invalidateLocalToken()
                TokenStore.clear()
                val i = Intent(requireContext(), LoginActivity::class.java)
                i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(i)
            }
        }
    }

    /** 이번 주(월~일) 근무 건수 · 출근 횟수 · 예상 급여를 요약 카드에 채운다. */
    private fun loadWeekSummary(view: View) {
        val today = LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val sunday = monday.plusDays(6)
        val yearMonth = today.toString().substring(0, 7)
        lifecycleScope.launch {
            val shifts = runCatching {
                Network.api.getShifts(employeeId = "me", from = monday.toString(), to = sunday.toString())
            }.getOrNull() ?: return@launch
            val wage = runCatching { Network.api.getMyPayroll(yearMonth).hourlyWage }.getOrDefault(0)
            val minutes = shifts.sumOf { shiftMinutes(it.startTime, it.endTime) }

            view.findViewById<TextView>(R.id.tv_sum_shifts).text = "${shifts.size}건"
            view.findViewById<TextView>(R.id.tv_sum_checkin).text =
                "${shifts.count { it.checkedInAt != null }}회"
            view.findViewById<TextView>(R.id.tv_sum_pay).text = won(minutes * wage / 60)
        }
    }

    /** startTime/endTime("HH:mm:ss") 사이 분. 종료가 시작보다 이르면 자정 넘김으로 보고 +24h. */
    private fun shiftMinutes(start: String?, end: String?): Long {
        val s = runCatching { LocalTime.parse(start) }.getOrNull() ?: return 0
        val e = runCatching { LocalTime.parse(end) }.getOrNull() ?: return 0
        val diff = java.time.Duration.between(s, e).toMinutes()
        return if (diff < 0) diff + 24 * 60 else diff
    }

    /** 알림 카테고리 on/off 를 불러와 다중 선택 다이얼로그로 수정한다. */
    private fun openNotificationSetting() {
        lifecycleScope.launch {
            val s = runCatching { Network.api.getNotificationSetting() }.getOrNull()
            if (s == null) {
                Toast.makeText(requireContext(), "알림 설정을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val labels = arrayOf("대타 알림", "공지 알림", "출퇴근 알림", "가입 신청 알림")
            val checked = booleanArrayOf(
                s.swapEnabled, s.noticeEnabled, s.attendanceEnabled, s.joinRequestEnabled,
            )
            AlertDialog.Builder(requireContext())
                .setTitle("알림 설정")
                .setMultiChoiceItems(labels, checked) { _, which, isChecked -> checked[which] = isChecked }
                .setPositiveButton("저장") { _, _ ->
                    lifecycleScope.launch {
                        runCatching {
                            Network.api.updateNotificationSetting(
                                NotificationSettingUpdate(checked[0], checked[1], checked[2], checked[3]),
                            )
                        }.onSuccess {
                            Toast.makeText(requireContext(), "알림 설정을 저장했어요", Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(requireContext(), it.toUserMessage(), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }
}
