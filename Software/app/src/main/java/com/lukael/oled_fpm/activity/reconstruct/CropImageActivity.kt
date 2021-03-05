package com.lukael.oled_fpm.activity.reconstruct

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.lukael.oled_fpm.R
import kotlinx.android.synthetic.main.action_bar.*
import kotlinx.android.synthetic.main.activity_cropimage.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgcodecs.Imgcodecs

class CropImageActivity : AppCompatActivity() {
    companion object { init { OpenCVLoader.initDebug() } }

    private var fullFilepath: String? = null
    private var roiSelected = false
    private var mLastClickTime: Long = 0
    private var imgScale = 0f
    private var fullImage = Mat()
    private var rectOnROIWidth = 0
    private var fullImageLoc = IntArray(2)

    // original image information
    private var orgImgWidth = 0f
    private var orgImgHeight = 0f

    /** define processing ROI  */
    private val Np = 200
    private val nstart = IntArray(2)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cropimage)

        initUI()
    }
    private fun initUI(){
        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar!!.setCustomView(R.layout.action_bar)

        // get file
        val intent = intent
        fullFilepath = intent.extras!!.getString("filepath")

        //Reading the Image from the file
        fullImage = Imgcodecs.imread(fullFilepath, Imgcodecs.IMREAD_GRAYSCALE)
        val fullImageBitmap = Bitmap.createBitmap(fullImage.cols(), fullImage.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(fullImage, fullImageBitmap)
        full_image_view.setImageBitmap(fullImageBitmap)

        // calculate image scale and apply to other objects
        calcImgScale()
        setScales()

        // on touch and drag
        full_image_view.setOnTouchListener(View.OnTouchListener { _: View?, event: MotionEvent ->
            val x = event.x.toInt()
            val y = event.y.toInt()
            val movedX = x - rectOnROIWidth * 3
            val movedY = y - rectOnROIWidth * 3
            val nstartX: Int
            val nstartY: Int
            val thresX = intArrayOf(0, fullImage.rows() - Np)
            val thresY = intArrayOf(0, fullImage.cols() - Np)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    roiSelected = true // on first click
                    nstartX = ((movedY - fullImageLoc[1]) / imgScale).toInt()
                    nstartY = ((movedX - fullImageLoc[0]) / imgScale).toInt()
                    if (thresX[0] < nstartX && nstartX < thresX[1] && thresY[0] < nstartY && nstartY < thresY[1]) {
                        rect_on_ROI.x = movedX.toFloat()
                        rect_on_ROI.y = movedY.toFloat()
                        nstart[0] = nstartX
                        nstart[1] = nstartY
                    }
                    updateROI()
                }
                MotionEvent.ACTION_MOVE -> {
                    nstartX = ((movedY - fullImageLoc[1]) / imgScale).toInt()
                    nstartY = ((movedX - fullImageLoc[0]) / imgScale).toInt()
                    if (thresX[0] < nstartX && nstartX < thresX[1] && thresY[0] < nstartY && nstartY < thresY[1]) {
                        rect_on_ROI.x = movedX.toFloat()
                        rect_on_ROI.y = movedY.toFloat()
                        nstart[0] = nstartX
                        nstart[1] = nstartY
                    }
                    updateROI()
                }
                MotionEvent.ACTION_UP -> {
                    nstartX = ((movedY - fullImageLoc[1]) / imgScale).toInt()
                    nstartY = ((movedX - fullImageLoc[0]) / imgScale).toInt()
                    if (thresX[0] < nstartX && nstartX < thresX[1] && thresY[0] < nstartY && nstartY < thresY[1]) {
                        rect_on_ROI.x = movedX.toFloat()
                        rect_on_ROI.y = movedY.toFloat()
                        nstart[0] = nstartX
                        nstart[1] = nstartY
                    }
                    updateROI()
                    //Log.d("nsatrt", nstart[0].toString() + ", " + nstart[1])
                }
            }
            true
        })

        // init buttons
        confirm_button.setOnClickListener{ goToNextActivity() }
        back_button.setOnClickListener{ finish() }
    }

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
        // calc width(==height) of big rectangle on image
        rectOnROIWidth = (Np * imgScale).toInt()
        rect_on_ROI.layoutParams.height = rectOnROIWidth
        rect_on_ROI.layoutParams.width = rectOnROIWidth

        // full Image View width/height change
        full_image_view.layoutParams.width = (orgImgWidth * imgScale).toInt()
        full_image_view.layoutParams.height = (orgImgHeight * imgScale).toInt()
    }

    // update roi
    private fun updateROI() {
        val rectCrop = Rect(nstart[1], nstart[0], Np, Np)
        //Log.d("nstart2", nstart[0].toString() + ", " + nstart[1] + ", " + Np)
        val cropImage = Mat(fullImage, rectCrop)
        val cropImageBitmap = Bitmap.createBitmap(cropImage.cols(), cropImage.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(cropImage, cropImageBitmap)
        crop_image_view.setImageBitmap(cropImageBitmap)
    }

    fun goToNextActivity() {
        if (SystemClock.elapsedRealtime() - mLastClickTime < 3000) return  // in case of double click...
        if (roiSelected) { // select the processing activity with name of the file
            val isRGB = fullFilepath!!.contains("rgb") //true
            val intent: Intent
            intent = if (isRGB) Intent(applicationContext, RGBReconstructActivity::class.java) else Intent(applicationContext, MonoReconstructActivity::class.java)

            // Transfer to next activity
            intent.putExtra("nStart[0]", nstart[0])
            intent.putExtra("nStart[1]", nstart[1])
            intent.putExtra("filepath", fullFilepath)
            startActivity(intent)
            overridePendingTransition(0, R.anim.fade_out)
            mLastClickTime = SystemClock.elapsedRealtime()
        } else {
            Toast.makeText(applicationContext, "Please select an ROI(Region of Interest).", Toast.LENGTH_LONG).show()
        }
    }

    private fun finishWithoutRedirect() {
        super.finish()
        overridePendingTransition(0, R.anim.fade_out)
    }

    override fun finish() {
        val intent = Intent(applicationContext, SelectImageSetActivity::class.java)
        startActivity(intent)
        super.finish()
        overridePendingTransition(0, R.anim.fade_out)
    }
}