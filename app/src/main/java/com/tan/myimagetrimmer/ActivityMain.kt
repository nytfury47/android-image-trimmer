package com.tan.myimagetrimmer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import android.os.Handler

class ActivityMain : AppCompatActivity() {

    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        title = getString(R.string.app_name)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        doubleBackToExitPressedOnce = true
        Toast.makeText(this, R.string.toast_a_backToExit, Toast.LENGTH_SHORT).show()
        Handler().postDelayed( { doubleBackToExitPressedOnce = false }, DELAY_EXIT)
    }

    companion object {
        private const val DELAY_EXIT = 2000L
    }
}