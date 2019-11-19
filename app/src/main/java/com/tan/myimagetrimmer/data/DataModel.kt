package com.tan.myimagetrimmer.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.PointF
import android.graphics.RectF
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tan.myimagetrimmer.ONE
import com.tan.myimagetrimmer.ZERO
import java.io.File

object DataModel {

    private const val SHARED_PREFS_NAME = "AIT42_Preferences"
    private const val MODE = Context.MODE_PRIVATE
    private const val IMG_DIR_NAME = "images"

    private lateinit var kPrefs: SharedPreferences

    // App-specific preferences and their default values
    private val kTopImageID = Pair("TopImageID", ZERO)
    private val kImageInfoList = Pair("ImageInfoList", "")

    fun init(context: Context) {
        kPrefs = context.getSharedPreferences(SHARED_PREFS_NAME, MODE)
        // make images folder to save data
        imagesDir = File(context.filesDir, IMG_DIR_NAME).apply { if (!exists()) mkdirs() }.absolutePath
    }

    /**
     * SharedPreferences extension function, so we won't need to call edit() and apply()
     * ourselves on every SharedPreferences operation.
     */
    private inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
        val editor = edit()
        operation(editor)
        editor.apply()
    }

    lateinit var imagesDir: String
        private set

    var topImageID: Int
        // custom getter to get a preference of a desired type, with a predefined default value
        get() = kPrefs.getInt(kTopImageID.first, kTopImageID.second)
        // custom setter to save a preference back to preferences file
        set(value) = kPrefs.edit { it.putInt(kTopImageID.first, value) }

    var imageInfoList: ArrayList<ImageInfo>
        get() {
            val json = kPrefs.getString(kImageInfoList.first, kImageInfoList.second)
            val collectionType = object : TypeToken<ArrayList<ImageInfo>>(){}.type
            return  if (json.isNullOrEmpty()) arrayListOf(ImageInfo(), ImageInfo(), ImageInfo())
            else Gson().fromJson(json, collectionType) as ArrayList<ImageInfo>
        }
        set(value) = kPrefs.edit {
            val json = Gson().toJson(value)
            it.putString(kImageInfoList.first, json)
        }

    val imageNameList: ArrayList<String>
        get() {
            val list = arrayListOf<String>()
            for (info in imageInfoList) list.add(info.imageName)
            return list
        }

    fun getImagePath(index: Int): String {
        val imageName = imageNameList[index]
        return  if (imageName.isEmpty()) ""
        else "$imagesDir/$imageName"
    }

    fun updateImageInfo(item: ImageInfo) {
        val list = imageInfoList
        list[topImageID] = item
        imageInfoList = list
    }

    fun createImageName(): String {
        var num = ZERO
        var imageName: String

        while (true) {
            imageName = "Img_$num.jpg"
            val imagePath = "$imagesDir/$imageName"
            if (!File(imagePath).exists()) break
            num++
        }

        return imageName
    }

//    fun clear() {
//        File(imagesDir).deleteRecursively()
//        kPrefs.edit { it.clear() }
//    }

    /**
     * Instrument Data Class
     */
    data class ImageInfo(var imageName: String = "", var hasCropArea: Boolean = false, var zoomScale: Float = ONE.toFloat(),
                         var zoomedRect: RectF = RectF(), var scrollPosition: PointF = PointF(), var orientation: Int = ZERO)
}