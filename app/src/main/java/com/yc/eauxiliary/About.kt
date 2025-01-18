package com.yc.eauxiliary

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class About : AppCompatActivity() {

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        updateStatusBarTextColor(window)

        // 适配导航栏小横条
        makeNavigationBarTransparentAndKeepSpace()

        val pageTitle = findViewById<TextView>(R.id.pageTitle)
        // 更新标题栏并添加动画效果
        pageTitle.animate().alpha(0f).setDuration(250).withEndAction {
            pageTitle.text = "关于"
            pageTitle.animate().alpha(1f).setDuration(100).start()
        }.start()
        val backArrow = findViewById<ImageView>(R.id.backArrow)
        backArrow?.visibility = View.VISIBLE

        // 获取应用版本
        val versionName = packageManager.getPackageInfo(packageName, 0).versionName

        // 获取构建时间
        val buildTime = getString(R.string.build_time)

        // 更新 TextView
        val versionTextView: TextView = findViewById(R.id.value1)
        val buildTimeTextView: TextView = findViewById(R.id.time)

        versionTextView.text = versionName
        buildTimeTextView.text = buildTime

        // 获取并判断用户状态
        fetchAndDisplayUserStatus()
    }

    private fun fetchAndDisplayUserStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            val studentName = getStudentName(this@About)
            val isActivated =
                SecureStorageUtils.isActivated(this@About) // 使用 ActivationUtils 获取激活状态
            val statusText = when {
                isUserWhitelisted(studentName) -> "内定访问 功能全部开放"
                isUserBlacklisted(studentName) -> "不是给你用的哈一边玩去"
                !isActivated -> "未激活"
                else -> "已激活"
            }

            // 更新UI
            withContext(Dispatchers.Main) {
                val jihuoTextView = findViewById<TextView>(R.id.jihuoa)
                jihuoTextView.text = statusText
            }
        }
    }

    // 检查用户是否在黑名单内的函数
    private fun isUserBlacklisted(username: String?): Boolean {
        val blacklistedUsers = mutableListOf("黑名单位置", "null")
        return blacklistedUsers.contains(username)
    }

    // 检查用户是否在白名单内的函数
    private fun isUserWhitelisted(username: String?): Boolean {
        val whitelistedUsers = mutableListOf("白名单位置")
        return whitelistedUsers.contains(username)
    }


    // 获取用户名的函数
    private fun getStudentName(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("student_name", null)
    }

    fun yinsixieyi(view: View) {
        val intent = Intent(this, EULA::class.java)
        intent.putExtra("source", "about")
        startActivity(intent)
    }

    @SuppressLint("SetTextI18n")
    fun onBackArrowClick(view: View) {
        // 恢复标题栏并添加动画效果
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}
