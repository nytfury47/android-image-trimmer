package com.tan.myimagetrimmer

import android.app.Activity
import android.graphics.PointF
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.otaliastudios.cameraview.*
import kotlinx.android.synthetic.main.activity_camera_view.*
import android.content.Intent
import com.tan.myimagetrimmer.data.DataModel
import java.io.File

class ActivityCameraView : AppCompatActivity() {

    private lateinit var camera: CameraView
    private var cameraOrientation = ZERO
    private var pictureOrientation = ZERO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_view)
        root_layout.setBackgroundColor(ContextCompat.getColor(this, R.color.colorMainBG))

        if (savedInstanceState != null) {
            Helper.restartApp(this)
            return
        }

        listOf(txt_title, txt_tap).apply {
            for (tv in this) Helper.scaleTextViewSize(tv)
        }

        btn_close.setOnClickListener { finish() }
        btn_camera_shutter.setOnClickListener { takePicture() }
        btn_camera_shutter.isEnabled = false

        if (DataModel.topImageID != ZERO) {
            view_crop_area_container.visibility = View.VISIBLE
        }

        camera = camera_view
        camera.setLifecycleOwner(this)
        camera.addCameraListener(object : CameraListener() {
            override fun onCameraOpened(options: CameraOptions) {
                cameraOpened()
            }
            override fun onPictureTaken(result: PictureResult) {
                pictureTaken(result)
            }
            override fun onAutoFocusStart(point: PointF) {
                //
            }
            override fun onCameraError(exception: CameraException) {
                Log.d(Helper.LOG_TAG, exception.toString())
            }
            override fun onOrientationChanged(orientation: Int) {
                cameraOrientation = when (orientation) {
                    90      -> 270
                    270     -> 90
                    else    -> orientation
                }
            }
        })
    }

    // Custom Methods

    private fun takePicture() {
        pictureOrientation = cameraOrientation
        camera.takePicture()
    }

    private fun cameraOpened() {
        updateViews()
    }

    private fun pictureTaken(result: PictureResult) {
        Helper.playCameraShutterSound(this)
        updateViews(false)

        val imageID = DataModel.topImageID
        val imageName = DataModel.createImageName()
        val imageFile = File("${DataModel.imagesDir}/$imageName")
        val imageInfo = DataModel.imageInfoList[imageID]
        imageInfo.imageName = imageName
        imageInfo.orientation = pictureOrientation
        DataModel.updateImageInfo(imageInfo)

        result.toFile(imageFile) {
            Log.d(Helper.LOG_TAG, "${imageFile.absolutePath} created.")
            setResult(Activity.RESULT_OK, Intent())
            finish()
        }
    }

    private fun updateViews(enable: Boolean = true) {
        progressBar?.visibility = View.GONE

        val alpha = if (enable) Helper.ALPHA_1 else Helper.ALPHA_0_4
        btn_close.isEnabled = enable
        btn_camera_shutter.alpha = alpha
        btn_camera_shutter.isEnabled = enable
        img_camera_shutter.alpha = alpha
        txt_tap.alpha = alpha
    }
}