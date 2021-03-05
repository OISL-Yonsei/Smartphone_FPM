package com.lukael.oled_fpm.activity.test

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.lukael.oled_fpm.R
import kotlinx.android.synthetic.main.activity_reconstruct.*
import kotlinx.android.synthetic.main.activity_select.*
import kotlinx.android.synthetic.main.activity_test.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs

class TestActivity : AppCompatActivity() {
    companion object { init { OpenCVLoader.initDebug() } }

    val root = Environment.getExternalStorageDirectory()
    val filepath = "$root/Pictures/OLED_RGB/rgb_tst.jpg"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)
        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar!!.setCustomView(R.layout.action_bar_no_backbutton)

        showRGBChannels()
    }
    fun showRGBChannels(){
        Log.e("HMM", filepath)
        val image = Imgcodecs.imread(filepath, Imgcodecs.IMREAD_COLOR) // grayscale makes it blurry
        Log.e("size", image.rows().toString() + ", " + image.cols().toString() + ", " + image.channels().toString())

        val redImage = Mat(image.rows(), image.cols(), CvType.CV_32F)
        val greenImage = Mat(image.rows(), image.cols(), CvType.CV_32F)
        val blueImage = Mat(image.rows(), image.cols(), CvType.CV_32F)

        Core.extractChannel(image, redImage, 0)
        Core.extractChannel(image, greenImage, 1)
        Core.extractChannel(image, blueImage, 2)

        Log.e("size", redImage.rows().toString() + ", " + redImage.cols().toString())

        // result object amplitude
        val redBitmap = Bitmap.createBitmap(redImage.cols(), redImage.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(redImage, redBitmap)
        rgb_red.setImageBitmap(redBitmap)
        // result object amplitude
        val greenBitmap = Bitmap.createBitmap(greenImage.cols(), greenImage.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(greenImage, greenBitmap)
        rgb_green.setImageBitmap(greenBitmap)
        // result object amplitude
        val blueBitmap = Bitmap.createBitmap(blueImage.cols(), blueImage.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(blueImage, blueBitmap)
        rgb_blue.setImageBitmap(blueBitmap)
    }
}
