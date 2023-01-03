package com.lukael.oled_fpm.activity.reconstruct

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.util.DisplayMetrics
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.lukael.oled_fpm.R
import kotlinx.android.synthetic.main.action_bar.*
import kotlinx.android.synthetic.main.activity_confirm.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import java.io.File

class ConfirmActivity : AppCompatActivity() {
    init { OpenCVLoader.initDebug() }

    private var fullFilepath: String? = null
    private val mLastClickTime: Long = 0
    private var imgScale = 0f
    private var fullImage = Mat()

    // original image information
    private var orgImgWidth = 0f
    private var orgImgHeight = 0f

    private fun calcImgScale() {
        // get phone screen width, height
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels
        // get original image width
        orgImgWidth = fullImage.cols().toFloat()
        orgImgHeight = fullImage.rows().toFloat()

        // calculate image scale
        imgScale = screenWidth / orgImgWidth
    }

    private fun setScales() {
        // full Image View width/height change
        full_image_view.layoutParams.width = (orgImgWidth * imgScale).toInt()
        full_image_view.layoutParams.height = (orgImgHeight * imgScale).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm)
        initUI()

    }

    private fun initUI(){
        // custom action bar
        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar!!.setCustomView(R.layout.action_bar)

        // get file
        fullFilepath = intent.getStringExtra(FILE_PATH)

        //Reading the Image from the file
        fullImage = Imgcodecs.imread(fullFilepath, Imgcodecs.IMREAD_GRAYSCALE)
        val fullImageBitmap = Bitmap.createBitmap(fullImage.cols(), fullImage.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(fullImage, fullImageBitmap)
        full_image_view.setImageBitmap(fullImageBitmap)

        // filename
        val firstFile = File(fullFilepath!!)
        val fileName = firstFile.name
        file_name_textView.text = fileName

        // calculate image scale and apply to other objects
        calcImgScale()
        setScales()

        // init buttons
        yes_button.setOnClickListener { if (SystemClock.elapsedRealtime() - mLastClickTime >= 3000) goToNextAtivity() }
        no_button.setOnClickListener { if (SystemClock.elapsedRealtime() - mLastClickTime >= 3000) finish() }
        back_button.setOnClickListener { finish() }
    }

    private fun goToNextAtivity() {
        val intent = Intent(applicationContext, CropImageActivity::class.java)
        // Transfer to next activity
        intent.putExtra(CropImageActivity.FILE_PATH, fullFilepath)
        startActivity(intent)
        overridePendingTransition(0, R.anim.fade_out)
        finishWitoutRedirect()
    }

    private fun finishWitoutRedirect() {
        super.finish()
        overridePendingTransition(0, R.anim.fade_out)
    }

    override fun finish() {
        val intent = Intent(applicationContext, SelectImageSetActivity::class.java)
        // Transfer to next activity
        startActivity(intent)
        super.finish()
        overridePendingTransition(0, R.anim.fade_out)
    }

    companion object {
        const val FILE_PATH = "file_path"
    }
}