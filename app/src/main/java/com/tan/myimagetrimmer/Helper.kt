package com.tan.myimagetrimmer

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.media.MediaActionSound
import android.util.DisplayMetrics
import android.util.TypedValue
import android.widget.TextView
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.media.AudioManager

const val ZERO = 0
const val ONE = 1
const val TWO = 2

object Helper {

    const val LOG_TAG = "MyLog"
    const val ALPHA_1 = ONE.toFloat()
    const val ALPHA_0_4 = 0.4F
    const val QUALITY_100 = 100
    const val IMAGE_TEXTURE_MAX_SIZE = 2048

    const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    const val READ_STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE

    const val REQUEST_CAMERA_PERMISSION = ONE
    const val REQUEST_READ_STORAGE_PERMISSION = 3
    const val REQUEST_HEADER_IMAGE = 4

    val displayMetrics: DisplayMetrics = Resources.getSystem().displayMetrics

    private const val MAIN_VIEW_BASE_HEIGHT = 568F
    private const val CAMERA_SHUTTER_SOUND_NAME = MediaActionSound.SHUTTER_CLICK

    private val cameraShutterSound = MediaActionSound().apply { load(CAMERA_SHUTTER_SOUND_NAME) }

    private enum class TextSize(val value: Float) {
        S(14F),
        M(17F),
        XXXL(26F),
    }

    private fun TextView.scaleTextSize() {
        val height = when (id) {
            R.id.txt_template
            -> TextSize.S.value
            R.id.txt_no_image_main
            -> TextSize.XXXL.value
            else    -> TextSize.M.value
        }
        setTextSize(TypedValue.COMPLEX_UNIT_PX, getScaledHeight(height))
    }

    private fun getScaledHeight(base: Float): Float = if (base == ZERO.toFloat()) base else displayMetrics.heightPixels * (base / MAIN_VIEW_BASE_HEIGHT)

    fun scaleTextViewSize(tv: TextView) = tv.scaleTextSize()

    fun restartApp(activity: Activity) {
        activity.startActivity(Intent(activity, ActivitySplash::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        activity.finishAfterTransition()
    }

    fun getCroppedImage(srcBmp: Bitmap, zoomedRect: RectF): Bitmap {
        val startX = (srcBmp.width * zoomedRect.left).toInt()
        val endX = (srcBmp.width * zoomedRect.right).toInt()
        val startY = (srcBmp.height * zoomedRect.top).toInt()
        val endY = (srcBmp.height * zoomedRect.bottom).toInt()
        return Bitmap.createBitmap(srcBmp, startX, startY, endX - startX, endY - startY)
    }

    fun decodeSampledBitmapFromFile(
        imagePath: String,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap {
        // First decode with inJustDecodeBounds=true to check dimensions
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(imagePath, this)

            // Calculate inSampleSize
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false

            BitmapFactory.decodeFile(imagePath, this)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = ONE

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / TWO
            val halfWidth: Int = width / TWO

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width smaller than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight || halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= TWO
            }
        }

        return inSampleSize
    }

    fun rotateBitmap(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(
            source, ZERO, ZERO, source.width, source.height,
            matrix, true
        )
    }

    fun playCameraShutterSound(activity: Activity) {
        val audioMgr = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        when (audioMgr.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                cameraShutterSound.play(CAMERA_SHUTTER_SOUND_NAME)
            }
            AudioManager.RINGER_MODE_SILENT -> {}
            AudioManager.RINGER_MODE_VIBRATE -> {}
        }
    }
}