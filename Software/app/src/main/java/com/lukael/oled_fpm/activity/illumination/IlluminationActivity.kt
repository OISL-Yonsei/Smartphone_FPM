/**
 * @author Jaewoo Lukael Jung
 * @email lukael.jung@yonsei.ac.kr
 * @create date 2019-12-10 21:49:26
 * @modify date 2019-12-10 21:49:26
 * @desc Display main function
 */
package com.lukael.oled_fpm.activity.illumination

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.lukael.oled_fpm.R
import com.lukael.oled_fpm.activity.illumination.captureoption.CaptureMode
import com.lukael.oled_fpm.activity.reconstruct.ConfirmActivity
import com.lukael.oled_fpm.camera.CameraControllerV2WithoutPreview
import com.lukael.oled_fpm.model.CaptureSetting
import kotlinx.android.synthetic.main.action_bar_no_backbutton.*
import java.io.File
import java.lang.Math.pow
import java.util.*
import kotlin.math.*


class IlluminationActivity : AppCompatActivity() {
    lateinit var ccv2WithoutPreview: CameraControllerV2WithoutPreview // camera preview
    lateinit var captureSetting: CaptureSetting // capture setting passed from before activity

    // xpos and ypos list for illumination dots
    inner class Pos(val xPos: Float, val yPos : Float, val radius : Float)
    var posList = ArrayList<Pos>()

    private var firstFileToScan: String? = null // 0th file name, for passing

    //Requesting permission.
    private val permissions: Unit
        get() {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //Requesting permission.
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        toolbar_title

        initSettings() // 1) get capture settings & check permissions
        makePosList() // 2) Generate point grid (xPosList and yPosList)
        initCamera() // 3) initialize camera

        // 4) start illumination
        val m = MyView(this)
        setContentView(m)
    }

    // 1)
    private fun initSettings(){
        captureSetting = intent.getSerializableExtra("capture_setting") as CaptureSetting // Get setting data
        permissions // check for permissions
    }

    // 2)
    private fun makePosList(){
        val pixel_distance = 0.04857

        if(captureSetting.illuminationMode == 0) {

            // makes position list
            val halfRow = captureSetting.dotsInRow / 2
            for (i in -halfRow..halfRow) {
                for (j in -halfRow..halfRow) {
                    if (i * i + j * j < halfRow * halfRow) {
                        var pointdist = hypot(j * captureSetting.stepSize.toDouble(),i * captureSetting.stepSize.toDouble())
                        var light_distance = hypot(pointdist*pixel_distance,captureSetting.sampleheight.toDouble())
                        var gain = pow(light_distance/captureSetting.sampleheight,2.0)
                        var radius = round(captureSetting.dotRadius / sin(atan((captureSetting.sampleheight/(pointdist*pixel_distance))))*gain)
                        posList.add(Pos((j * captureSetting.stepSize).toFloat(), (i * captureSetting.stepSize).toFloat(), radius.toFloat()))
                    }
                }
            }
            Log.e("dotsInRow", captureSetting.dotsInRow.toString())
            Log.e("halfRow", halfRow.toString())
            Log.e("posnum", posList.size.toString())
        }

        if(captureSetting.illuminationMode == 1) {
            val halfRow = captureSetting.dotsInRow / 2

            var uniform_x_pos = 0f
            var uniform_y_pos = 0f

            for (i in -halfRow..halfRow) {
                for (j in -halfRow..halfRow) {
                    if (i * i + j * j < halfRow * halfRow) {
                        uniform_x_pos = round(captureSetting.sampleheight/pixel_distance* asin(sin((captureSetting.stepSize*pixel_distance)/captureSetting.sampleheight)*j)).toFloat()
                        uniform_y_pos = round(captureSetting.sampleheight/pixel_distance* asin(sin((captureSetting.stepSize*pixel_distance)/captureSetting.sampleheight)*i)).toFloat()
                        var pointdist = hypot(uniform_x_pos.toDouble(),uniform_y_pos.toDouble())
                        var light_distance = hypot(pointdist*pixel_distance,captureSetting.sampleheight.toDouble())
                        var gain = pow(light_distance/captureSetting.sampleheight,2.0)
                        var radius = round(captureSetting.dotRadius / sin(atan((captureSetting.sampleheight/(pointdist*pixel_distance))))*gain)
                        posList.add(Pos(uniform_x_pos, uniform_y_pos, radius.toFloat()))
                    }
                }
            }
        }

        if(captureSetting.illuminationMode == 2) {

            var pos_radius = intArrayOf(10,30,60,106,175)
            var circle_dot_number = intArrayOf(4,12,12,16,16)
            var non_uniform_x_pos = 0f
            var non_uniform_y_pos = 0f

            for (i in 0..4) {
                for (j in 0..circle_dot_number[i]-1)
                {
                    non_uniform_x_pos = round(pos_radius[i] * cos(2 * 3.14 * j / circle_dot_number[i])).toFloat();
                    non_uniform_y_pos = round(pos_radius[i] * sin(2 * 3.14 * j / circle_dot_number[i])).toFloat();
                    var pointdist = hypot(non_uniform_x_pos.toDouble(),non_uniform_y_pos.toDouble())
                    var light_distance = hypot(pointdist*pixel_distance,captureSetting.sampleheight.toDouble())
                    var gain = pow(light_distance/captureSetting.sampleheight,2.0)
                    var radius = round(captureSetting.dotRadius / sin(atan((captureSetting.sampleheight/(pointdist*pixel_distance))))*gain)
                    posList.add(Pos(non_uniform_y_pos, non_uniform_x_pos,radius.toFloat()))
                }
            }
            Log.e("posnum", posList.size.toString())
        }
    }

    // 3)
    private fun initCamera(){
        // initialize camera
        val rgbMaxIter = if (captureSetting.captureMode == CaptureMode.RGBReconstruction) 3 else 1 // take rgb or only r|g|b
        ccv2WithoutPreview = CameraControllerV2WithoutPreview(applicationContext, rgbMaxIter * posList.size + 1, captureSetting.dotsInRow-1)
        ccv2WithoutPreview.openCamera()
        Log.d("picture # to be taken", (captureSetting.dotsInRow * captureSetting.dotsInRow + 1).toString())
    }

    // 4)
    internal inner class MyView(context: Context?) : View(context) {
        // current information initialization
        private val colors = intArrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.WHITE)
        private var paint = Paint()
        private var step = 0 // step in r|g|b each
        private var iterCount = 0 //
        private var rgbIter = 0 // r(0) -> g(1) -> b(2)
        var exposureTime_var = captureSetting.exposureTime

        inner class DotStatus (var color : Int, var cx : Float, var cy : Float, var radius : Float, var distance : Float)
        private var dotStatus = DotStatus (
                colors[3],
                captureSetting.centerX.toFloat(),
                captureSetting.centerY.toFloat(),
                200f,
                0f) // initialization (white big dot)

        // change illumination in canvas onDraw
        @SuppressLint("DrawAllocation")
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            setBackgroundColor(Color.BLACK)
            canvas.drawColor(Color.BLACK)

            // capture images
            Log.d("STEP", step.toString() + "/" + posList.size)

            // check if done capturing
            if (step == posList.size + 1) { // if there is next color
                if (captureSetting.captureMode == CaptureMode.RGBReconstruction && rgbIter < 2) { // FIXME
                    rgbIter++
                    step = 1
                } else { // if done capturing
                    mediaScan() // scan 0th file into images
                    goToNextActivity() // and proceed to next activity
                }
            }

            // if in iteration
            else when {
                step == 0 -> { // first : white big dot
                    dotStatus.color = colors[3]
                    dotStatus.cx = captureSetting.centerX.toFloat()
                    dotStatus.cy = captureSetting.centerY.toFloat()
                    dotStatus.radius = 200f
                    //dotStatus.distance = 0f
                    //var pointdist = hypot((dotStatus.cx-captureSetting.centerX).toDouble(),(dotStatus.cy-captureSetting.centerY).toDouble())
                    //dotStatus.distance = hypot(pointdist*0.0486,captureSetting.sampleheight.toDouble()).toFloat()
                    dotStatus.distance=(captureSetting.sampleheight.toDouble()*0.3).toFloat()

                }
                captureSetting.captureMode == CaptureMode.MonoReconstruction -> { // FIXME
                    dotStatus.color = colors[captureSetting.colorCode]
                    dotStatus.cx = captureSetting.centerX + (posList[step - 1].xPos)
                    dotStatus.cy = captureSetting.centerY + (posList[step - 1].yPos)
                    dotStatus.radius = posList[step - 1].radius
                    var pointdist = hypot((dotStatus.cx-captureSetting.centerX).toDouble(),(dotStatus.cy-captureSetting.centerY).toDouble())
                    dotStatus.distance = hypot(pointdist*0.0486,captureSetting.sampleheight.toDouble()).toFloat()
                }
                else -> { // rgb
                    dotStatus.color = colors[rgbIter]
                    dotStatus.cx = captureSetting.centerX + (posList[step - 1].xPos)
                    dotStatus.cy = captureSetting.centerY + (posList[step - 1].yPos)
                    dotStatus.radius = posList[step - 1].radius
                    var pointdist = hypot((dotStatus.cx-captureSetting.centerX).toDouble(),(dotStatus.cy-captureSetting.centerY).toDouble())
                    dotStatus.distance = hypot(pointdist*0.0486,captureSetting.sampleheight.toDouble()).toFloat()
                }
            }

            // draw dot with setting
            paint.color = dotStatus.color
            canvas.drawCircle( dotStatus.cx, dotStatus.cy, dotStatus.radius, paint)

            // iteration
            if (iterCount > 0 && iterCount % 3 == 0) {
                Log.d("take num", step.toString() + "")

                // beep for capture debugging
                val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 20)

                // Exposure time control
//                exposureTime_var = (captureSetting.exposureTime * pow(dotStatus.distance.toDouble()/captureSetting.sampleheight,2.0)).toLong()
                exposureTime_var = captureSetting.exposureTime
                if (step==0) {
                    exposureTime_var = exposureTime_var / 6
                }
                // make capture and save as file
                val file: File = ccv2WithoutPreview.takePicture(step, exposureTime_var, captureSetting.captureMode, rgbIter)!!
                val fName = file.absolutePath

                // select 0th file for scanning
                val endString = fName.substring(fName.length - 8)
                val compString = "_000.jpg"
                if (endString == compString) firstFileToScan = fName // scan only 0th file
                Log.d("file path", file.absolutePath)

                step++ // iterate
            }
            iterCount++ // iterate

            // if in capture iteration
            try { Thread.sleep(exposureTime_var/1000+400) } catch (e: InterruptedException) {} // delay for finishing capturing
            this.postInvalidate() // screen update
        }
    }

    // scan first file
    private fun mediaScan() {
        // image scanning
        Log.e("FIRST", firstFileToScan.toString())
        val f = File(firstFileToScan!!)
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(f))
        mediaScanIntent.data = Uri.fromFile(f)
        sendBroadcast(mediaScanIntent)
    }

    // go to activity which asks for reconstruction
    fun goToNextActivity() {
        setContentView(R.layout.activity_illumination)
        val i = Intent(applicationContext, ConfirmActivity::class.java)
        i.putExtra("filepath", firstFileToScan) // 0th file name
        startActivity(i)
        overridePendingTransition(0, R.anim.fade_out)
        finish()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.fade_out)
    }

    companion object {
        const val CAPTURE_SETTING = "capture_setting"
    }
}