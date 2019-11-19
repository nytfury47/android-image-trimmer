package com.tan.myimagetrimmer.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.tan.myimagetrimmer.ActivityTrimView
import com.tan.myimagetrimmer.Helper

class OnClearFromRecentService : Service() {

    private var mTrimActivity: ActivityTrimView? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(Helper.LOG_TAG, "Service started.")
        instance = this
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(Helper.LOG_TAG, "Service destroyed.")
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        Log.d(Helper.LOG_TAG, "Task removed.")
        mTrimActivity?.cancelTrimming()
        stopSelf()
    }

    // Custom Methods

    fun setTrimActivity(activity: ActivityTrimView) {
        mTrimActivity = activity
    }

    companion object {
        private var instance: OnClearFromRecentService? = null

        fun getInstance(): OnClearFromRecentService? {
            return instance
        }
    }
}