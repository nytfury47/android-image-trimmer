package com.tan.myimagetrimmer

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.tan.myimagetrimmer.data.DataModel
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

class ActivityMain : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState != null) {
            Helper.restartApp(this)
            return
        }

        Helper.scaleTextViewSize(txt_title_main)
        Helper.scaleTextViewSize(txt_no_image_main)
        Helper.scaleTextViewSize(txt_template)

        listOf(btn_edit_header_image_1, btn_edit_header_image_2, btn_edit_header_image_3).apply {
            for (btn in this) btn.setOnClickListener { editHeaderImage(it) }
        }
        btn_crop_header_image.setOnClickListener { showCropView() }
        btn_delete_header_image.setOnClickListener { deleteHeaderImage() }

        setThumbImage()
        setSelectedImage()
    }

    override fun onStart() {
        super.onStart()

        if (isNewImageSet) {
            isNewImageSet = false
            setThumbImage()
            setSelectedImage()
        }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == Helper.REQUEST_READ_STORAGE_PERMISSION) {
                val imageData = data?.data
                val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                val cursor = contentResolver.query(imageData!!, filePathColumn, null, null, null)
                var srcImagePath: String? = null

                // Check if local image
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndexOrThrow(filePathColumn.first())
                    srcImagePath = cursor.getString(columnIndex)
                    cursor.close()
                }

                if (srcImagePath == null) {
                    // Check if from cloud (e.g. Google Photos) then save
                    srcImagePath = getImagePathFromInputStreamUri(imageData)
                } else {
                    copyLocalImage(srcImagePath)
                }

                if (srcImagePath != null) {
                    showCropView(true)
                } else {
                    Toast.makeText(this, R.string.toast_e_album_error, Toast.LENGTH_LONG).show()
                }
            } else { // Helper.REQUEST_HEADER_IMAGE
                // Image already saved in ActivityCameraView
                showCropView(true)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) { return }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            val titleID =
                if (requestCode == Helper.REQUEST_READ_STORAGE_PERMISSION) R.string.request_storage_permission
                else R.string.request_camera_permission
            val messageID =
                if (requestCode == Helper.REQUEST_READ_STORAGE_PERMISSION) R.string.request_storage_permission_rationale
                else R.string.request_camera_permission_rationale

            AlertDialog.Builder(this).apply {
                setCancelable(false)
                setTitle(titleID)
                setMessage(messageID)
                setPositiveButton(R.string.ok) { _, _ ->
                    startActivity(Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + BuildConfig.APPLICATION_ID)
                    ))
                }
                create().show()
            }
        }

        return
    }

    // Custom Methods

    private fun setThumbImage() {
        val thumbImageViews = listOf(img_thumbnail_1, img_thumbnail_2, img_thumbnail_3)

        for ((index, imageName) in DataModel.imageNameList.withIndex()) {
            val imageName2 = imageName.substringBeforeLast(".")
            val imagePath = "${DataModel.imagesDir}/${imageName2}_small.jpg"
            val image = if (imageName.isEmpty()) null else BitmapFactory.decodeFile(imagePath)
            thumbImageViews[index].setImageBitmap(image)

            if (index == ZERO) {
                img_add_1.visibility = if (imageName.isEmpty()) View.VISIBLE else View.INVISIBLE
            } else {
                thumbImageViews[index].visibility =
                    if (imageName.isEmpty()) View.INVISIBLE else View.VISIBLE
            }
        }
    }

    private fun setSelectedImage() {
        val imageID = DataModel.topImageID
        val imageInfo = DataModel.imageInfoList[imageID]
        val imageName = imageInfo.imageName.substringBeforeLast(".")
        val imagePath = "${DataModel.imagesDir}/${imageName}_small.jpg"
        val image = if (imageName.isEmpty()) null else BitmapFactory.decodeFile(imagePath)

        img_header.setImageBitmap(image)
        txt_no_image_main.visibility = if (imageName.isEmpty()) View.VISIBLE else View.INVISIBLE

        // set selected thumbnail border
        val thumbBordersViews = listOf(view_border_1, view_border_2, view_border_3)
        thumbBordersViews[imageID].visibility = View.VISIBLE
        val textColor = if (imageID == ZERO) ContextCompat.getColor(this, R.color.colorPrimary) else Color.BLACK
        txt_template.setTextColor(textColor)

        // set trim, delete buttons
        img_crop.alpha = if (imageName.isEmpty()) Helper.ALPHA_0_4 else Helper.ALPHA_1
        img_trash.alpha = img_crop.alpha
        btn_crop_header_image.isEnabled = imageName.isNotEmpty()
        btn_delete_header_image.isEnabled = btn_crop_header_image.isEnabled
    }

    private fun editHeaderImage(view: View) {
        DataModel.topImageID = view.tag.toString().toInt()
        val imageID = DataModel.topImageID

        // update image and border views
        setSelectedImage()
        listOf(view_border_1, view_border_2, view_border_3).apply {
            for ((index, v) in this.withIndex()) {
                if (index != imageID) v.visibility = View.INVISIBLE
            }
        }

        val imageName = DataModel.imageNameList[imageID]
        if (imageName.isEmpty()) {
            // show edit image alert dialog
            val dialogItems =
                arrayOf(
                    getString(R.string.alert_select_image_album),
                    getString(R.string.alert_select_image_camera),
                    getString(R.string.cancel)
                )
            AlertDialog.Builder(this).apply {
                setCancelable(false)
                setItems(dialogItems) { dialog, which ->
                    when (which) {
                        ZERO    -> dialog.dismiss().also { pickImageFromAlbum() }
                        ONE     -> dialog.dismiss().also { pickImageFromCamera() }
                        else    -> dialog.dismiss()
                    }
                }
                create().show()
            }
        }
    }

    private fun showCropView(isNewImage: Boolean = false) {
        val intent = Intent(this, ActivityTrimView::class.java)
        intent.putExtra("isNewImage", isNewImage)
        startActivity(intent)
    }

    private fun deleteHeaderImage() {
        AlertDialog.Builder(this).apply {
            setCancelable(false)
            setMessage(R.string.alert_delete_header_image)
            setPositiveButton(R.string.ok) { _, _ ->
                val imageID = DataModel.topImageID
                val imagePath = DataModel.getImagePath(imageID)
                val file = File(imagePath)

                Log.d(Helper.LOG_TAG, "Delete header image: $file")
                file.delete()

                // delete small version of file if exists
                val imageName = imagePath.substringBeforeLast(".")
                File("${imageName}_small.jpg").apply {
                    if (exists()) {
                        Log.d(Helper.LOG_TAG, "Delete header image (small): $this")
                        delete()
                    }
                }

                // reset image info
                val imageInfo = DataModel.imageInfoList[imageID]
                imageInfo.imageName = ""
                imageInfo.hasCropArea = false
                DataModel.updateImageInfo(imageInfo)

                setThumbImage()
                setSelectedImage()
            }
            setNegativeButton(R.string.cancel, null)
            create().show()
        }
    }

    @AfterPermissionGranted(Helper.REQUEST_READ_STORAGE_PERMISSION)
    private fun pickImageFromAlbum() {
        if (EasyPermissions.hasPermissions(this, Helper.READ_STORAGE_PERMISSION)) {
            val mimeTypes = arrayOf("image/jpeg", "image/png")
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            startActivityForResult(intent, Helper.REQUEST_READ_STORAGE_PERMISSION)
        } else {
            EasyPermissions.requestPermissions(
                this, getString(R.string.request_storage_permission),
                Helper.REQUEST_READ_STORAGE_PERMISSION,
                Helper.READ_STORAGE_PERMISSION)
        }
    }

    @Throws(IOException::class)
    private fun copyFile(src: File, dst: File) {
        val inChannel = FileInputStream(src).channel
        val outChannel = FileOutputStream(dst).channel
        try {
            inChannel.transferTo(ZERO.toLong(), inChannel.size(), outChannel)
        } finally {
            inChannel.close()
            outChannel.close()
        }
    }

    private fun copyLocalImage(srcImagePath: String) {
        // save (copy) image
        val imageID = DataModel.topImageID
        val imageInfo = DataModel.imageInfoList[imageID]
        val imageName = DataModel.createImageName()
        val dstImagePath = "${DataModel.imagesDir}/$imageName"
        copyFile(File(srcImagePath), File(dstImagePath))
        imageInfo.imageName = imageName
        DataModel.updateImageInfo(imageInfo)
    }

    @AfterPermissionGranted(Helper.REQUEST_CAMERA_PERMISSION)
    private fun pickImageFromCamera() {
        if (EasyPermissions.hasPermissions(this, Helper.CAMERA_PERMISSION)) {
            val intent = Intent(this, ActivityCameraView::class.java)
            startActivityForResult(intent, Helper.REQUEST_HEADER_IMAGE)
        } else {
            EasyPermissions.requestPermissions(
                this, getString(R.string.request_camera_permission),
                Helper.REQUEST_CAMERA_PERMISSION,
                Helper.CAMERA_PERMISSION)
        }
    }

    private fun getImagePathFromInputStreamUri(uri: Uri): String? {
        var inputStream: InputStream? = null
        var filePath: String? = null

        if (uri.authority != null) {
            try {
                inputStream = contentResolver.openInputStream(uri)
                val photoFile = createTemporalFileFrom(inputStream)
                filePath = photoFile?.path
            } catch (e: FileNotFoundException) {
                Log.d(Helper.LOG_TAG, e.toString())
            } catch (e: IOException) {
                Log.d(Helper.LOG_TAG, e.toString())
            } finally {
                try {
                    inputStream!!.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        return filePath
    }

    @Throws(IOException::class)
    private fun createTemporalFileFrom(inputStream: InputStream?): File? {
        var targetFile: File? = null

        if (inputStream != null) {
            val buffer = ByteArray(8 * 1024)
            val imageID = DataModel.topImageID
            val imageInfo = DataModel.imageInfoList[imageID]
            val imageName = DataModel.createImageName()
            val dstImagePath = "${DataModel.imagesDir}/$imageName"

            targetFile = File(dstImagePath)
            val outputStream = FileOutputStream(targetFile)
            while (true) {
                val read = inputStream.read(buffer)
                if (read == -1) break
                outputStream.write(buffer, 0, read)
            }
            outputStream.flush()

            imageInfo.imageName = imageName
            DataModel.updateImageInfo(imageInfo)

            try {
                outputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return targetFile
    }

    companion object {
        private const val DELAY_EXIT = 2000L

        @JvmStatic
        var isNewImageSet = false
    }
}