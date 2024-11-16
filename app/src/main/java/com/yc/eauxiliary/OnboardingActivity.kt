package com.yc.eauxiliary

import DirectoryFragment
import PermissionFragment
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OnboardingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        updateStatusBarTextColor(window)

        val viewPager = findViewById<ViewPager2>(R.id.view_pager)
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        viewPager.adapter = pagerAdapter

        // 使用 TabLayoutMediator 关联 TabLayout 和 ViewPager2 (如果使用了 TabLayout)
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout) // 如果有TabLayout
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "权限获取"
                1 -> tab.text = "访问授权"
            }
        }.attach()
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = 2 // 两个步骤

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> PermissionFragment()
                1 -> DirectoryFragment()
                else -> throw IllegalArgumentException("异常出现于: $position")
            }
        }
    }
}