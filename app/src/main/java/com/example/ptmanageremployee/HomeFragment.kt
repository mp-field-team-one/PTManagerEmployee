package com.example.ptmanageremployee

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<View>(R.id.btn_checkin).setOnClickListener {
            startActivity(Intent(requireContext(), CheckInActivity::class.java))
        }
        view.findViewById<View>(R.id.card_today).setOnClickListener {
            startActivity(Intent(requireContext(), ShiftDetailActivity::class.java))
        }
        view.findViewById<View>(R.id.btn_all_schedule).setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottom_nav)
                ?.selectedItemId = R.id.nav_schedule
        }
        view.findViewById<View>(R.id.btn_bell).setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }
        view.findViewById<View>(R.id.btn_sub_request).setOnClickListener {
            startActivity(Intent(requireContext(), SubRequestActivity::class.java))
        }
    }
}
