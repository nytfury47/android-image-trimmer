package com.tan.myimagetrimmer

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_trim.*
import java.io.File
import java.io.FileOutputStream
import android.graphics.drawable.BitmapDrawable
import android.view.View
import com.tan.myimagetrimmer.data.DataModel
import com.tan.myimagetrimmer.service.OnClearFromRecentService
import com.tan.myimagetrimmer.util.Exif
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ActivityTrimView : AppCompatActivity() {

    private var operationInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trim)

        if (savedInstanceState != null) {
            Helper.restartApp(this)
            return
        }

        OnClearFromRecentService.getInstance()?.setTrimActivity(this)

        Helper.scaleTextViewSize(txt_title_trim)
        Helper.scaleTextViewSize(txt_save_trim)

        btn_close_trim.setOnClickListener { onBackPressed() }
        btn_save_trim.setOnClickListener { saveCropArea() }

        progressBar.visibility = View.INVISIBLE

        val imageID = DataModel.topImageID
        val cropViewHeight = if (imageID == ZERO) Helper.displayMetrics.widthPixels else Helper.displayMetrics.widthPixels / TWO
        view_crop_area.layoutParams.height = cropViewHeight

        val imageInfo = DataModel.imageInfoList[imageID]
        val imagePath = DataModel.getImagePath(imageID)
        val image = if (imagePath.isEmpty()) null else Helper.decodeSampledBitmapFromFile(imagePath, Helper.IMAGE_TEXTURE_MAX_SIZE, Helper.IMAGE_TEXTURE_MAX_SIZE)
        // EXIF rotation
        val orientedImage = Exif.rotateBitmap(imagePath, image)
        val rotatedBitmap =
            if (imageInfo.orientation == ZERO) orientedImage
            else Helper.rotateBitmap(orientedImage, imageInfo.orientation.toFloat())

        imageView.tag = imageID
        imageView.setImageBitmap(rotatedBitmap)

        if (imageInfo.hasCropArea) {
            imageView.setZoom(imageInfo.zoomScale, imageInfo.scrollPosition.x, imageInfo.scrollPosition.y)
        }
    }

    override fun onBackPressed() {
        if (operationInProgress) return
        cancelTrimming()
        super.onBackPressed()
    }

    // Custom Methods

    fun cancelTrimming() {
        val imageID = DataModel.topImageID
        val imageInfo = DataModel.imageInfoList[imageID]
        val isNewImage = intent.extras?.getBoolean("isNewImage") as Boolean

        // cancel trimming
        if (!imageInfo.hasCropArea) {
            if (imageID == ZERO && !isNewImage) {
                Log.d(Helper.LOG_TAG, "imageID is ZERO && !isNewImage")
            } else {
                // Delete image
                val file = File(DataModel.getImagePath(imageID))
                Log.d(Helper.LOG_TAG, "Delete header image: $file")
                file.delete()
                // reset image info
                imageInfo.imageName = ""
                DataModel.updateImageInfo(imageInfo)
            }
        }
    }

    private fun saveCropArea() {
        if (operationInProgress) return

        operationInProgress = true
        progressBar.visibility = View.VISIBLE

        GlobalScope.launch {
            val zoomScale = imageView.currentZoom
            val zoomedRect = imageView.zoomedRect
            val scrollPosition = imageView.scrollPosition
            val imageID = DataModel.topImageID
            val imageInfo = DataModel.imageInfoList[imageID]

            // create and use small-size version of image
            val image = (imageView.drawable as BitmapDrawable).bitmap
            val imageName = imageInfo.imageName.substringBeforeLast(".")
            val imageFile = "${DataModel.imagesDir}/${imageName}_small.jpg"
            val cropViewWidth = Helper.displayMetrics.widthPixels
            val cropViewHeight = if (imageID == ZERO) cropViewWidth else cropViewWidth / TWO
            val croppedImage = Helper.getCroppedImage(image, zoomedRect)
            val scaledImage = Bitmap.createScaledBitmap(croppedImage, cropViewWidth, cropViewHeight, false)
            FileOutputStream(imageFile).use { scaledImage.compress(Bitmap.CompressFormat.JPEG, Helper.QUALITY_100, it) }

            imageInfo.hasCropArea = true
            imageInfo.zoomScale = zoomScale
            imageInfo.zoomedRect = zoomedRect
            imageInfo.scrollPosition = scrollPosition

            DataModel.updateImageInfo(imageInfo)
            ActivityMain.isNewImageSet = true

            launch(Dispatchers.Main) {
                operationInProgress = false
                progressBar.visibility = View.INVISIBLE
                finish()
            }
        }
    }
}
