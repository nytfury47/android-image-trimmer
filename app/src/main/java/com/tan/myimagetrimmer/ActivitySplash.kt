package com.tan.myimagetrimmer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_splash.*

class ActivitySplash : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }

    override fun onStart() {
        super.onStart()
        animateLogo(textView)
    }

    private fun animateLogo(tv: TextView, fadeIn: Boolean = true) {
        val anim = AlphaAnimation(if (fadeIn) 0f else 1f, if (fadeIn) 1f else 0f)
        anim.interpolator = AccelerateInterpolator()
        anim.duration = LENGTH_FADE

        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationEnd(animation: Animation) {
                if (fadeIn) {
                    Handler().postDelayed({ animateLogo(tv, false) }, LENGTH_SPLASH)
                } else {
                    tv.visibility = View.GONE
                    startActivity(Intent(this@ActivitySplash, ActivityMain::class.java))
                    finish()
                }
            }
            override fun onAnimationRepeat(animation: Animation) {}
            override fun onAnimationStart(animation: Animation) {}
        })

        tv.startAnimation(anim)
    }

    companion object {
        private const val LENGTH_SPLASH = 2000L
        private const val LENGTH_FADE = 1000L
    }
}