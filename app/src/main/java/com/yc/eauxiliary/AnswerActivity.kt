package com.yc.eauxiliary

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AnswerActivity : AppCompatActivity() {

    private lateinit var rootView: View
    private lateinit var answerTextView: TextView
    private lateinit var pagetitle: TextView
    private var cx: Int = 0
    private var cy: Int = 0
    private var finalRadius: Float = 0f

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_answer)


        rootView = findViewById(android.R.id.content)
        answerTextView = findViewById(R.id.answer_text_view)
        pagetitle = findViewById(R.id.answer_page_title)

        updateStatusBarTextColor(window)

        val title = intent.getStringExtra("TITLE") ?: "答案"
        pagetitle.text = title
        pagetitle.text = "$title" // 显示标题和日期

        // 获取传递的数据
        val answers = intent.getStringExtra("ANSWERS") ?: ""
        cx = intent.getIntExtra("CX", 0)
        cy = intent.getIntExtra("CY", 0)


        // 设置答案文本
        answerTextView.text = answers

        // 在 onLayout() 回调中启动进入动画，确保布局已经完成测量
        rootView.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View?,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                rootView.removeOnLayoutChangeListener(this)
                startEnterAnimation()
            }
        })

        // 设置返回按钮点击事件
        val backButton: ImageView = findViewById(R.id.answer_back_button)
        backButton.setOnClickListener {
            onBackPressed()
        }
    }

    private fun startEnterAnimation() {
        finalRadius = Math.hypot(rootView.width.toDouble(), rootView.height.toDouble()).toFloat()
        val circularReveal = ViewAnimationUtils.createCircularReveal(
            rootView,
            cx,
            cy,
            0f,
            finalRadius
        )
        circularReveal.duration = 600 // 动画时长
        rootView.visibility = View.VISIBLE
        circularReveal.start()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        if (finalRadius == 0f) {
            finalRadius =
                Math.hypot(rootView.width.toDouble(), rootView.height.toDouble()).toFloat()
        }

        val circularReveal = ViewAnimationUtils.createCircularReveal(
            rootView,
            cx,
            cy,
            finalRadius,
            0f
        )
        circularReveal.duration = 600 // 动画时长
        circularReveal.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                rootView.visibility = View.INVISIBLE
                super.onAnimationEnd(animation)
                finish()
                overridePendingTransition(0, R.anim.fade_out)
            }
        })
        circularReveal.start()
    }
}