package com.lukael.oled_fpm.activity.reconstruct

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.*
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.lukael.oled_fpm.R
import com.lukael.oled_fpm.activity.MainActivity
import com.lukael.oled_fpm.constant.*
import com.lukael.oled_fpm.model.HashmapData
import com.lukael.oled_fpm.model.LedProps
import com.lukael.oled_fpm.model.SystemOpts
import com.lukael.oled_fpm.util.Calculator
import com.lukael.oled_fpm.util.FileSaver
import com.lukael.oled_fpm.util.FourierTransformer
import com.lukael.oled_fpm.util.LineDebugger
import io.reactivex.Flowable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function3
import io.reactivex.functions.Function9
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.action_bar.*
import kotlinx.android.synthetic.main.activity_reconstruct.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class MonoReconstructActivity : AppCompatActivity(), ReconstructActivity {

    /** on satrt **/
    companion object { init { OpenCVLoader.initDebug() } } // load and init openCV
    private lateinit var ledProps : LedProps // about led
    private val startTime = System.currentTimeMillis()
    private var lineDebugger = LineDebugger(startTime) // debugger

    /** thread messaging **/
    private var fullTask: Disposable ?= null // rxjava handling

    var message: Message? = null
    private var handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.arg1) {
                SHOW_RESULT -> showResult()
                PROGRESS_1 -> changeProgressUI(1)
                PROGRESS_2 -> changeProgressUI(2)
                PROGRESS_3 -> changeProgressUI(3)
                else -> reconstruct_message.text = msg.obj.toString()
            }
        }
    }


    // Sequence size
    private var parallelsize = 5
    private var sequencesize = 10

    /** data for function input & output **/
    private var reconstructInputData = arrayOfNulls<HashmapData>(parallelsize)
    private var preProcessOutputData = arrayOfNulls<HashmapData>(parallelsize)

//    /** system parameters for processing **/
//    private var nImg = 0
//    private lateinit var du: DoubleArray
//    private lateinit var illuminationNA: DoubleArray
//    private var freqUV: Array<DoubleArray>? = null
//    private var freqUVCal: Array<DoubleArray>? = null
//    private var freqUVDesign: Array<DoubleArray>? = null
//    private lateinit var dfi: IntArray


//    /** variables needed for each rgb object **/
//    // results - before normalizing
//    private var objRComplex = arrayOfNulls<Mat>(2)
//    private var objR = Mat()
//    // results - after normalizing
//    private var ampResults = Mat()
//    private var orgResults = Mat()
//    private var phaseResult: Mat? = null

    /** system parameters for processing **/
    private var nImg = 0
    private lateinit var du: Array<DoubleArray>
    private lateinit var illuminationNA: Array<DoubleArray>
    private var freqUV: Array<Array<DoubleArray>>? = null
    private var freqUVCal: Array<Array<DoubleArray>>? = null
    private var freqUVDesign: Array<Array<DoubleArray>>? = null
    private lateinit var dfi: Array<IntArray>
    private var curIs: Array<Mat?>? = null

    /** variables needed for each rgb object **/
    // results - before normalizing
    private val objRComplex = Array(parallelsize) { arrayOfNulls<Mat>(parallelsize) }
    private val objR = arrayOfNulls<Mat>(parallelsize)
    // results - after normalizing
    private var ampResults = arrayOfNulls<Mat>(parallelsize)
    private var orgResults = arrayOfNulls<Mat>(parallelsize)
    private var phaseResult: Mat? = null

    /** Mats for display **/
    private var orgObjAmp: Mat? = null
    private var resObjAmp: Mat? = null
    private var resObjPhase: Mat? = null
    private var orgOBitmap: Bitmap? = null
    private var resultOBitmap: Bitmap? = null
    private var resultOBitmapAngle : Bitmap? = null // displaying bitmaps
    private val fileSaver = FileSaver()
    private val fourierTransformer = FourierTransformer()
    private val calculator = Calculator()

    /** about image **/
    private val imgNCent = DoubleArray(2)
    private val imgCenter = DoubleArray(2)

    /** about file **/
    private var fileStart : String ?= null
    private var fileEnd : String ?= null
    private var ampFileName : String ?= null
    private var phaseFileName : String ?= null
    private lateinit var root : File

    private var nStart = IntArray(2)
    private var nStart_x = IntArray(sequencesize)
    private var nStart_y = IntArray(sequencesize)

    //Requesting permission.
    private val permissions: Unit
        get() {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                //Requesting permission.
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reconstruct)
        permissions// permission checking

        getPassedInfo() // passed information from CropImageActivity
        initUI() // initialize UI

        initSettings() // settings (debugger, nImg and other things)
        loadimages()
        startFullTask_para() // thread processing & reconstructing
        // ALL sequential code using for
        /*
        for (i in 0 until parallelsize) {
            preProcessOutputData[0] = preProcessMono(0, nStart_x[i], nStart_y[i], 0)
            message = handler.obtainMessage()
            message?.arg1 = PROGRESS_2
            handler.sendMessage(message!!)
            message = handler.obtainMessage()
            message?.arg1 = 0
            message?.obj = "Configuring Reconstruct..."
            handler.sendMessage(message!!)
            reconstructInputData[0] = configureReconstruct(preProcessOutputData[0]!!)
            fpmFunc(reconstructInputData[0])
        }
        orgObjAmp = orgResults[0]
        resObjAmp = ampResults[0]
        progress_layout.visibility = View.INVISIBLE
        message = handler.obtainMessage()
        message?.arg1 = SHOW_RESULT
        message?.arg2 = 3
        handler.sendMessage(message!!)
         */
    }

    override fun getPassedInfo() {
        /** get information passed from CropImageActivity **/
        if (nStartGiven[0] == -1) { // if there is some input values that must be used
            nStart[0] = intent.getIntExtra("nStart[0]", 0)
            nStart[1] = intent.getIntExtra("nStart[1]", 0)
        } else {
            nStart[0] = nStartGiven[0]
            nStart[1] = nStartGiven[1]
        }
        // Position calculator
        for (i in 0 until sequencesize) {
            nStart_x[i] = nStart[0] + (i * 200)
            nStart_y[i] = nStart[1] + (i * 200)
        }

        // input and output filenames
        val fullFilepath = intent.extras!!.getString("filepath")
        val firstFile = File(fullFilepath!!)
        val fileName = firstFile.name

        //init ledProps
        ledProps = LedProps(fileName)

        fileStart = fileName.substring(0, fileName.length - 8) // get timestamp only
        fileEnd = ".jpg" // jpg
    }

    override fun initUI(){
        /** initialize UI **/
        supportActionBar?.displayOptions  = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.action_bar)

        // init buttons
        back_button.setOnClickListener{
            toHome()
        }
        roi_button.setOnClickListener{
            finish()
        }
        before_after_button.setOnTouchListener { _: View?, event: MotionEvent ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> showBefore()
                MotionEvent.ACTION_UP -> showAfter()
            }
            true
        }
        save_button.setOnClickListener { saveImage() }

        // first ui change for preprocess
        message = handler.obtainMessage()
        message?.arg1 = PROGRESS_1
        handler.sendMessage(message!!)
    }

    override fun initSettings(){
        /** set line debugger, permission, and other reconstruct settings **/
        lineDebugger = LineDebugger(startTime)
        root = Environment.getExternalStorageDirectory()

        // information about saving file
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREAN).format(Date())
        ampFileName = fileStart + "_" + timeStamp + "_" + nStart[0].toString() + "_" + nStart[1].toString() + "_amp.jpg"
        phaseFileName = fileStart + "_" + timeStamp + "_" + nStart[0].toString() + "_" + nStart[1].toString() + "_phase.jpg"

        // count number of images into nImg
        countNImg()

        // initialization for arrays related with nLed
//        ledProps.updateNLED(nImg, imgCenter)
//        freqUV = Array(ledProps.nled) { DoubleArray(2) }
//        freqUVCal = Array(ledProps.nled) { DoubleArray(2) }
//        freqUVDesign = Array(ledProps.nled) { DoubleArray(2) }
//        dfi = IntArray(ledProps.nled)
//        illuminationNA = DoubleArray(nImg)
        curIs = arrayOfNulls<Mat>(nImg)
//        du = DoubleArray(2)
        ledProps.updateNLED(nImg, imgCenter)
        freqUV = Array(parallelsize) { Array(ledProps.nled) { DoubleArray(2) } }
        freqUVCal = Array(parallelsize) { Array(ledProps.nled) { DoubleArray(2) } }
        freqUVDesign = Array(parallelsize) { Array(ledProps.nled) { DoubleArray(2) } }
        dfi = Array(parallelsize) { IntArray(ledProps.nled) }
        illuminationNA = Array(parallelsize) { DoubleArray(nImg) }
        du = Array(parallelsize) { DoubleArray(2) }
    }

    override fun countNImg() {
        nImg = 0
        // count images
        while (true) {
            val imgName = fileStart + "_" + (nImg + 1).toString().padStart(3, '0') + fileEnd
            val file = File("$root/Pictures/OLED_FPM/$imgName")
            val filepath = "$root/Pictures/OLED_FPM/$imgName"
            Log.d("FILE counting...", filepath)
            if (!file.exists()) break
            nImg++
        }
    }

    fun loadimages() {
        val tag = "load-images"

        // distortion value
        var distValue = floatArrayOf(0.0313690384695265F, -0.00126679968152714F, 0F, 0F)
        var distMat = Mat(1, 4, CvType.CV_32F)
        distMat.put(0, 0, distValue);

        var mtxValue = floatArrayOf(688.015419803622F, 0F, 1595.36915351398F, 0F, 688.013187402952F, 1215.45121729576F, 0F, 0F, 1F)
        var mtxMat = Mat(3, 3, CvType.CV_32F)
        mtxMat.put(0, 0, mtxValue);

        for (m in 0 until nImg) {
            // update ui for loading images

            message = handler.obtainMessage()
            message?.arg1 = 0
            message?.obj = "Loading Images... " + (m + 1) + "/" + nImg
            handler.sendMessage(message!!)

            // configure filename
            val imgName = fileStart + "_" + (m + 1).toString().padStart(3, '0') + fileEnd
            val filepath = "$root/Pictures/OLED_FPM/$imgName"
            Log.d(tag, filepath)

            //Reading the Image from the file, and crop it
            val curI = Imgcodecs.imread(filepath, Imgcodecs.IMREAD_COLOR) // grayscale makes it blurry
            Core.extractChannel(curI, curI, 1) // green

            // Distortion corrections
            var curIUndist = Mat(curI.rows(), curI.cols(), CvType.CV_32F)
            Calib3d.undistort(curI, curIUndist, mtxMat, distMat, mtxMat)

            curIs?.set(m, curI)
            if (m > 0 && m % 10 == 0) Runtime.getRuntime().gc() // garbage collector
        }
        Log.d(tag, "file load completed")
    }

    override fun startFullTask(){
        TODO("Not yet implemented")
    }

    fun startFullTask_para(){
        /** pre-process, configure, and reconstruct with RxJava threading **/
        // concat: execute threads in sequential order
        // zip : execute threads at once, and wait for all to finish
        val fivetask1 = Single.concat(
                // 1) preprocess
                Single.zip(
                        Single.fromCallable {
                            preProcessOutputData[0] = preProcessMono(0, nStart_x[0], nStart_y[0], 0)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[1] = preProcessMono(0, nStart_x[1], nStart_y[0], 1)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[2] = preProcessMono(0, nStart_x[2], nStart_y[0], 2)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[3] = preProcessMono(0, nStart_x[3], nStart_y[0], 3)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[4] = preProcessMono(0, nStart_x[4], nStart_y[0], 4)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            preProcessOutputData[5] = preProcessMono(0, nStart_x[5], nStart_y[5], 5)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[6] = preProcessMono(0, nStart_x[6], nStart_y[6], 6)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[7] = preProcessMono(0, nStart_x[7], nStart_y[7], 7)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[8] = preProcessMono(0, nStart_x[8], nStart_y[8], 8)
                            false
                        }.subscribeOn(Schedulers.io()),
                        */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            message = handler.obtainMessage()
                            message?.arg1 = PROGRESS_2
                            handler.sendMessage(message!!)
                            message = handler.obtainMessage()
                            message?.arg1 = 0
                            message?.obj = "Configuring Reconstruct #1 ..."
                            handler.sendMessage(message!!)
                        },
                // 2) configure reconstruction
                Single.zip(
                        Single.fromCallable {
                            reconstructInputData[0] = configureReconstruct(preProcessOutputData[0]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[1] = configureReconstruct(preProcessOutputData[1]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[2] = configureReconstruct(preProcessOutputData[2]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[3] = configureReconstruct(preProcessOutputData[3]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[4] = configureReconstruct(preProcessOutputData[4]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            reconstructInputData[5] = configureReconstruct(preProcessOutputData[5]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[6] = configureReconstruct(preProcessOutputData[6]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[7] = configureReconstruct(preProcessOutputData[7]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[8] = configureReconstruct(preProcessOutputData[8]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread()),
                // 3) reconstruct iterations
                Single.zip(
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[0])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[1])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[2])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[3])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[4])
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[5])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[6])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[7])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[8])
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            // merge rgb
//                            val colorCombiner = ColorCombiner()
//                            orgObjAmp = orgResults[0]
//                            resObjAmp = ampResults[0]
//                            progress_layout.visibility = View.INVISIBLE
//                            message = handler.obtainMessage()
//                            message?.arg1 = SHOW_RESULT
//                            message?.arg2 = 3
//                            handler.sendMessage(message!!)
                        }
        )
        val fivetask2 = Single.concat(
                // 1) preprocess
                Single.zip(
                        Single.fromCallable {
                            preProcessOutputData[0] = preProcessMono(0, nStart_x[0], nStart_y[1], 0)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[1] = preProcessMono(0, nStart_x[1], nStart_y[1], 1)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[2] = preProcessMono(0, nStart_x[2], nStart_y[1], 2)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[3] = preProcessMono(0, nStart_x[3], nStart_y[1], 3)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[4] = preProcessMono(0, nStart_x[4], nStart_y[1], 4)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            preProcessOutputData[5] = preProcessMono(0, nStart_x[5], nStart_y[5], 5)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[6] = preProcessMono(0, nStart_x[6], nStart_y[6], 6)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[7] = preProcessMono(0, nStart_x[7], nStart_y[7], 7)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[8] = preProcessMono(0, nStart_x[8], nStart_y[8], 8)
                            false
                        }.subscribeOn(Schedulers.io()),
                        */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            message = handler.obtainMessage()
                            message?.arg1 = PROGRESS_2
                            handler.sendMessage(message!!)
                            message = handler.obtainMessage()
                            message?.arg1 = 0
                            message?.obj = "Configuring Reconstruct #2 ..."
                            handler.sendMessage(message!!)
                        },
                // 2) configure reconstruction
                Single.zip(
                        Single.fromCallable {
                            reconstructInputData[0] = configureReconstruct(preProcessOutputData[0]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[1] = configureReconstruct(preProcessOutputData[1]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[2] = configureReconstruct(preProcessOutputData[2]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[3] = configureReconstruct(preProcessOutputData[3]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[4] = configureReconstruct(preProcessOutputData[4]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            reconstructInputData[5] = configureReconstruct(preProcessOutputData[5]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[6] = configureReconstruct(preProcessOutputData[6]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[7] = configureReconstruct(preProcessOutputData[7]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[8] = configureReconstruct(preProcessOutputData[8]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread()),
                // 3) reconstruct iterations
                Single.zip(
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[0])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[1])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[2])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[3])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[4])
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[5])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[6])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[7])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[8])
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            // merge rgb
//                            val colorCombiner = ColorCombiner()
//                            orgObjAmp = orgResults[0]
//                            resObjAmp = ampResults[0]
//                            progress_layout.visibility = View.INVISIBLE
//                            message = handler.obtainMessage()
//                            message?.arg1 = SHOW_RESULT
//                            message?.arg2 = 3
//                            handler.sendMessage(message!!)
                        }
        )
        val fivetask3 = Single.concat(
                // 1) preprocess
                Single.zip(
                        Single.fromCallable {
                            preProcessOutputData[0] = preProcessMono(0, nStart_x[0], nStart_y[2], 0)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[1] = preProcessMono(0, nStart_x[1], nStart_y[2], 1)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[2] = preProcessMono(0, nStart_x[2], nStart_y[2], 2)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[3] = preProcessMono(0, nStart_x[3], nStart_y[2], 3)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[4] = preProcessMono(0, nStart_x[4], nStart_y[2], 4)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            preProcessOutputData[5] = preProcessMono(0, nStart_x[5], nStart_y[5], 5)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[6] = preProcessMono(0, nStart_x[6], nStart_y[6], 6)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[7] = preProcessMono(0, nStart_x[7], nStart_y[7], 7)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[8] = preProcessMono(0, nStart_x[8], nStart_y[8], 8)
                            false
                        }.subscribeOn(Schedulers.io()),
                        */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            message = handler.obtainMessage()
                            message?.arg1 = PROGRESS_2
                            handler.sendMessage(message!!)
                            message = handler.obtainMessage()
                            message?.arg1 = 0
                            message?.obj = "Configuring Reconstruct #3 ..."
                            handler.sendMessage(message!!)
                        },
                // 2) configure reconstruction
                Single.zip(
                        Single.fromCallable {
                            reconstructInputData[0] = configureReconstruct(preProcessOutputData[0]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[1] = configureReconstruct(preProcessOutputData[1]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[2] = configureReconstruct(preProcessOutputData[2]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[3] = configureReconstruct(preProcessOutputData[3]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[4] = configureReconstruct(preProcessOutputData[4]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            reconstructInputData[5] = configureReconstruct(preProcessOutputData[5]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[6] = configureReconstruct(preProcessOutputData[6]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[7] = configureReconstruct(preProcessOutputData[7]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[8] = configureReconstruct(preProcessOutputData[8]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread()),
                // 3) reconstruct iterations
                Single.zip(
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[0])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[1])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[2])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[3])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[4])
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[5])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[6])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[7])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[8])
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            // merge rgb
//                            val colorCombiner = ColorCombiner()
//                            orgObjAmp = orgResults[0]
//                            resObjAmp = ampResults[0]
//                            progress_layout.visibility = View.INVISIBLE
//                            message = handler.obtainMessage()
//                            message?.arg1 = SHOW_RESULT
//                            message?.arg2 = 3
//                            handler.sendMessage(message!!)
                        }
        )
        val fivetask4 = Single.concat(
                // 1) preprocess
                Single.zip(
                        Single.fromCallable {
                            preProcessOutputData[0] = preProcessMono(0, nStart_x[0], nStart_y[3], 0)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[1] = preProcessMono(0, nStart_x[1], nStart_y[3], 1)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[2] = preProcessMono(0, nStart_x[2], nStart_y[3], 2)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[3] = preProcessMono(0, nStart_x[3], nStart_y[3], 3)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[4] = preProcessMono(0, nStart_x[4], nStart_y[3], 4)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            preProcessOutputData[5] = preProcessMono(0, nStart_x[5], nStart_y[5], 5)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[6] = preProcessMono(0, nStart_x[6], nStart_y[6], 6)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[7] = preProcessMono(0, nStart_x[7], nStart_y[7], 7)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[8] = preProcessMono(0, nStart_x[8], nStart_y[8], 8)
                            false
                        }.subscribeOn(Schedulers.io()),
                        */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            message = handler.obtainMessage()
                            message?.arg1 = PROGRESS_2
                            handler.sendMessage(message!!)
                            message = handler.obtainMessage()
                            message?.arg1 = 0
                            message?.obj = "Configuring Reconstruct #4 ..."
                            handler.sendMessage(message!!)
                        },
                // 2) configure reconstruction
                Single.zip(
                        Single.fromCallable {
                            reconstructInputData[0] = configureReconstruct(preProcessOutputData[0]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[1] = configureReconstruct(preProcessOutputData[1]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[2] = configureReconstruct(preProcessOutputData[2]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[3] = configureReconstruct(preProcessOutputData[3]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[4] = configureReconstruct(preProcessOutputData[4]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            reconstructInputData[5] = configureReconstruct(preProcessOutputData[5]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[6] = configureReconstruct(preProcessOutputData[6]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[7] = configureReconstruct(preProcessOutputData[7]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[8] = configureReconstruct(preProcessOutputData[8]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread()),
                // 3) reconstruct iterations
                Single.zip(
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[0])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[1])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[2])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[3])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[4])
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[5])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[6])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[7])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[8])
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            // merge rgb
//                            val colorCombiner = ColorCombiner()
//                            orgObjAmp = orgResults[0]
//                            resObjAmp = ampResults[0]
//                            progress_layout.visibility = View.INVISIBLE
//                            message = handler.obtainMessage()
//                            message?.arg1 = SHOW_RESULT
//                            message?.arg2 = 3
//                            handler.sendMessage(message!!)
                        }
        )
        val fivetask5 = Single.concat(
                // 1) preprocess
                Single.zip(
                        Single.fromCallable {
                            preProcessOutputData[0] = preProcessMono(0, nStart_x[0], nStart_y[4], 0)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[1] = preProcessMono(0, nStart_x[1], nStart_y[4], 1)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[2] = preProcessMono(0, nStart_x[2], nStart_y[4], 2)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[3] = preProcessMono(0, nStart_x[3], nStart_y[4], 3)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[4] = preProcessMono(0, nStart_x[4], nStart_y[4], 4)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            preProcessOutputData[5] = preProcessMono(0, nStart_x[5], nStart_y[5], 5)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[6] = preProcessMono(0, nStart_x[6], nStart_y[6], 6)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[7] = preProcessMono(0, nStart_x[7], nStart_y[7], 7)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[8] = preProcessMono(0, nStart_x[8], nStart_y[8], 8)
                            false
                        }.subscribeOn(Schedulers.io()),
                        */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            message = handler.obtainMessage()
                            message?.arg1 = PROGRESS_2
                            handler.sendMessage(message!!)
                            message = handler.obtainMessage()
                            message?.arg1 = 0
                            message?.obj = "Configuring Reconstruct #5 ..."
                            handler.sendMessage(message!!)
                        },
                // 2) configure reconstruction
                Single.zip(
                        Single.fromCallable {
                            reconstructInputData[0] = configureReconstruct(preProcessOutputData[0]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[1] = configureReconstruct(preProcessOutputData[1]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[2] = configureReconstruct(preProcessOutputData[2]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[3] = configureReconstruct(preProcessOutputData[3]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[4] = configureReconstruct(preProcessOutputData[4]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            reconstructInputData[5] = configureReconstruct(preProcessOutputData[5]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[6] = configureReconstruct(preProcessOutputData[6]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[7] = configureReconstruct(preProcessOutputData[7]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[8] = configureReconstruct(preProcessOutputData[8]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread()),
                // 3) reconstruct iterations
                Single.zip(
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[0])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[1])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[2])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[3])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[4])
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[5])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[6])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[7])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[8])
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            // merge rgb
//                            val colorCombiner = ColorCombiner()
//                            orgObjAmp = orgResults[0]
//                            resObjAmp = ampResults[0]
//                            progress_layout.visibility = View.INVISIBLE
//                            message = handler.obtainMessage()
//                            message?.arg1 = SHOW_RESULT
//                            message?.arg2 = 3
//                            handler.sendMessage(message!!)
                        }
        )

        val fivetask6 = Single.concat(
                // 1) preprocess
                Single.zip(
                        Single.fromCallable {
                            preProcessOutputData[0] = preProcessMono(0, nStart_x[0], nStart_y[5], 0)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[1] = preProcessMono(0, nStart_x[1], nStart_y[5], 1)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[2] = preProcessMono(0, nStart_x[2], nStart_y[5], 2)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[3] = preProcessMono(0, nStart_x[3], nStart_y[5], 3)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[4] = preProcessMono(0, nStart_x[4], nStart_y[5], 4)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            preProcessOutputData[5] = preProcessMono(0, nStart_x[5], nStart_y[5], 5)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[6] = preProcessMono(0, nStart_x[6], nStart_y[6], 6)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[7] = preProcessMono(0, nStart_x[7], nStart_y[7], 7)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[8] = preProcessMono(0, nStart_x[8], nStart_y[8], 8)
                            false
                        }.subscribeOn(Schedulers.io()),
                        */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            message = handler.obtainMessage()
                            message?.arg1 = PROGRESS_2
                            handler.sendMessage(message!!)
                            message = handler.obtainMessage()
                            message?.arg1 = 0
                            message?.obj = "Configuring Reconstruct #6 ..."
                            handler.sendMessage(message!!)
                        },
                // 2) configure reconstruction
                Single.zip(
                        Single.fromCallable {
                            reconstructInputData[0] = configureReconstruct(preProcessOutputData[0]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[1] = configureReconstruct(preProcessOutputData[1]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[2] = configureReconstruct(preProcessOutputData[2]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[3] = configureReconstruct(preProcessOutputData[3]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[4] = configureReconstruct(preProcessOutputData[4]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            reconstructInputData[5] = configureReconstruct(preProcessOutputData[5]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[6] = configureReconstruct(preProcessOutputData[6]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[7] = configureReconstruct(preProcessOutputData[7]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[8] = configureReconstruct(preProcessOutputData[8]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread()),
                // 3) reconstruct iterations
                Single.zip(
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[0])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[1])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[2])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[3])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[4])
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[5])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[6])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[7])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[8])
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            // merge rgb
//                            val colorCombiner = ColorCombiner()
//                            orgObjAmp = orgResults[0]
//                            resObjAmp = ampResults[0]
//                            progress_layout.visibility = View.INVISIBLE
//                            message = handler.obtainMessage()
//                            message?.arg1 = SHOW_RESULT
//                            message?.arg2 = 3
//                            handler.sendMessage(message!!)
                        }
        )
        val fivetask7 = Single.concat(
                // 1) preprocess
                Single.zip(
                        Single.fromCallable {
                            preProcessOutputData[0] = preProcessMono(0, nStart_x[0], nStart_y[6], 0)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[1] = preProcessMono(0, nStart_x[1], nStart_y[6], 1)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[2] = preProcessMono(0, nStart_x[2], nStart_y[6], 2)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[3] = preProcessMono(0, nStart_x[3], nStart_y[6], 3)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[4] = preProcessMono(0, nStart_x[4], nStart_y[6], 4)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            preProcessOutputData[5] = preProcessMono(0, nStart_x[5], nStart_y[5], 5)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[6] = preProcessMono(0, nStart_x[6], nStart_y[6], 6)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[7] = preProcessMono(0, nStart_x[7], nStart_y[7], 7)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[8] = preProcessMono(0, nStart_x[8], nStart_y[8], 8)
                            false
                        }.subscribeOn(Schedulers.io()),
                        */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            message = handler.obtainMessage()
                            message?.arg1 = PROGRESS_2
                            handler.sendMessage(message!!)
                            message = handler.obtainMessage()
                            message?.arg1 = 0
                            message?.obj = "Configuring Reconstruct #7 ..."
                            handler.sendMessage(message!!)
                        },
                // 2) configure reconstruction
                Single.zip(
                        Single.fromCallable {
                            reconstructInputData[0] = configureReconstruct(preProcessOutputData[0]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[1] = configureReconstruct(preProcessOutputData[1]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[2] = configureReconstruct(preProcessOutputData[2]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[3] = configureReconstruct(preProcessOutputData[3]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[4] = configureReconstruct(preProcessOutputData[4]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            reconstructInputData[5] = configureReconstruct(preProcessOutputData[5]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[6] = configureReconstruct(preProcessOutputData[6]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[7] = configureReconstruct(preProcessOutputData[7]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[8] = configureReconstruct(preProcessOutputData[8]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread()),
                // 3) reconstruct iterations
                Single.zip(
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[0])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[1])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[2])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[3])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[4])
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[5])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[6])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[7])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[8])
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            // merge rgb
//                            val colorCombiner = ColorCombiner()
                            orgObjAmp = orgResults[0]
                            resObjAmp = ampResults[0]
                            progress_layout.visibility = View.INVISIBLE
                            message = handler.obtainMessage()
                            message?.arg1 = SHOW_RESULT
                            message?.arg2 = 3
                            handler.sendMessage(message!!)
                        }
        )
        val fivetask8 = Single.concat(
                // 1) preprocess
                Single.zip(
                        Single.fromCallable {
                            preProcessOutputData[0] = preProcessMono(0, nStart_x[0], nStart_y[7], 0)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[1] = preProcessMono(0, nStart_x[1], nStart_y[7], 1)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[2] = preProcessMono(0, nStart_x[2], nStart_y[7], 2)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[3] = preProcessMono(0, nStart_x[3], nStart_y[7], 3)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[4] = preProcessMono(0, nStart_x[4], nStart_y[7], 4)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            preProcessOutputData[5] = preProcessMono(0, nStart_x[5], nStart_y[5], 5)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[6] = preProcessMono(0, nStart_x[6], nStart_y[6], 6)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[7] = preProcessMono(0, nStart_x[7], nStart_y[7], 7)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[8] = preProcessMono(0, nStart_x[8], nStart_y[8], 8)
                            false
                        }.subscribeOn(Schedulers.io()),
                        */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            message = handler.obtainMessage()
                            message?.arg1 = PROGRESS_2
                            handler.sendMessage(message!!)
                            message = handler.obtainMessage()
                            message?.arg1 = 0
                            message?.obj = "Configuring Reconstruct #8 ..."
                            handler.sendMessage(message!!)
                        },
                // 2) configure reconstruction
                Single.zip(
                        Single.fromCallable {
                            reconstructInputData[0] = configureReconstruct(preProcessOutputData[0]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[1] = configureReconstruct(preProcessOutputData[1]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[2] = configureReconstruct(preProcessOutputData[2]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[3] = configureReconstruct(preProcessOutputData[3]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[4] = configureReconstruct(preProcessOutputData[4]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            reconstructInputData[5] = configureReconstruct(preProcessOutputData[5]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[6] = configureReconstruct(preProcessOutputData[6]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[7] = configureReconstruct(preProcessOutputData[7]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[8] = configureReconstruct(preProcessOutputData[8]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread()),
                // 3) reconstruct iterations
                Single.zip(
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[0])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[1])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[2])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[3])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[4])
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[5])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[6])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[7])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[8])
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            // merge rgb
//                            val colorCombiner = ColorCombiner()
//                            orgObjAmp = orgResults[0]
//                            resObjAmp = ampResults[0]
//                            progress_layout.visibility = View.INVISIBLE
//                            message = handler.obtainMessage()
//                            message?.arg1 = SHOW_RESULT
//                            message?.arg2 = 3
//                            handler.sendMessage(message!!)
                        }
        )
        val fivetask9 = Single.concat(
                // 1) preprocess
                Single.zip(
                        Single.fromCallable {
                            preProcessOutputData[0] = preProcessMono(0, nStart_x[0], nStart_y[8], 0)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[1] = preProcessMono(0, nStart_x[1], nStart_y[8], 1)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[2] = preProcessMono(0, nStart_x[2], nStart_y[8], 2)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[3] = preProcessMono(0, nStart_x[3], nStart_y[8], 3)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[4] = preProcessMono(0, nStart_x[4], nStart_y[8], 4)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            preProcessOutputData[5] = preProcessMono(0, nStart_x[5], nStart_y[5], 5)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[6] = preProcessMono(0, nStart_x[6], nStart_y[6], 6)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[7] = preProcessMono(0, nStart_x[7], nStart_y[7], 7)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[8] = preProcessMono(0, nStart_x[8], nStart_y[8], 8)
                            false
                        }.subscribeOn(Schedulers.io()),
                        */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            message = handler.obtainMessage()
                            message?.arg1 = PROGRESS_2
                            handler.sendMessage(message!!)
                            message = handler.obtainMessage()
                            message?.arg1 = 0
                            message?.obj = "Configuring Reconstruct #9 ..."
                            handler.sendMessage(message!!)
                        },
                // 2) configure reconstruction
                Single.zip(
                        Single.fromCallable {
                            reconstructInputData[0] = configureReconstruct(preProcessOutputData[0]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[1] = configureReconstruct(preProcessOutputData[1]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[2] = configureReconstruct(preProcessOutputData[2]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[3] = configureReconstruct(preProcessOutputData[3]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[4] = configureReconstruct(preProcessOutputData[4]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            reconstructInputData[5] = configureReconstruct(preProcessOutputData[5]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[6] = configureReconstruct(preProcessOutputData[6]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[7] = configureReconstruct(preProcessOutputData[7]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[8] = configureReconstruct(preProcessOutputData[8]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread()),
                // 3) reconstruct iterations
                Single.zip(
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[0])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[1])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[2])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[3])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[4])
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[5])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[6])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[7])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[8])
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            // merge rgb
//                            val colorCombiner = ColorCombiner()
//                            orgObjAmp = orgResults[0]
//                            resObjAmp = ampResults[0]
//                            progress_layout.visibility = View.INVISIBLE
//                            message = handler.obtainMessage()
//                            message?.arg1 = SHOW_RESULT
//                            message?.arg2 = 3
//                            handler.sendMessage(message!!)
                        }
        )
        val fivetask10 = Single.concat(
                // 1) preprocess
                Single.zip(
                        Single.fromCallable {
                            preProcessOutputData[0] = preProcessMono(0, nStart_x[0], nStart_y[9], 0)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[1] = preProcessMono(0, nStart_x[1], nStart_y[9], 1)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[2] = preProcessMono(0, nStart_x[2], nStart_y[9], 2)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[3] = preProcessMono(0, nStart_x[3], nStart_y[9], 3)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[4] = preProcessMono(0, nStart_x[4], nStart_y[9], 4)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            preProcessOutputData[5] = preProcessMono(0, nStart_x[5], nStart_y[5], 5)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[6] = preProcessMono(0, nStart_x[6], nStart_y[6], 6)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[7] = preProcessMono(0, nStart_x[7], nStart_y[7], 7)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            preProcessOutputData[8] = preProcessMono(0, nStart_x[8], nStart_y[8], 8)
                            false
                        }.subscribeOn(Schedulers.io()),
                        */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            message = handler.obtainMessage()
                            message?.arg1 = PROGRESS_2
                            handler.sendMessage(message!!)
                            message = handler.obtainMessage()
                            message?.arg1 = 0
                            message?.obj = "Configuring Reconstruct #10 ..."
                            handler.sendMessage(message!!)
                        },
                // 2) configure reconstruction
                Single.zip(
                        Single.fromCallable {
                            reconstructInputData[0] = configureReconstruct(preProcessOutputData[0]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[1] = configureReconstruct(preProcessOutputData[1]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[2] = configureReconstruct(preProcessOutputData[2]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[3] = configureReconstruct(preProcessOutputData[3]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[4] = configureReconstruct(preProcessOutputData[4]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            reconstructInputData[5] = configureReconstruct(preProcessOutputData[5]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[6] = configureReconstruct(preProcessOutputData[6]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[7] = configureReconstruct(preProcessOutputData[7]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            reconstructInputData[8] = configureReconstruct(preProcessOutputData[8]!!)
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread()),
                // 3) reconstruct iterations
                Single.zip(
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[0])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[1])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[2])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[3])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[4])
                            false
                        }.subscribeOn(Schedulers.io()),
                        /*
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[5])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[6])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[7])
                            false
                        }.subscribeOn(Schedulers.io()),
                        Single.fromCallable {
                            fpmFunc(reconstructInputData[8])
                            false
                        }.subscribeOn(Schedulers.io()),
                         */
                        io.reactivex.functions.Function5 { _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false })
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSuccess {
                            // merge rgb
//                            val colorCombiner = ColorCombiner()
                            orgObjAmp = orgResults[0]
                            resObjAmp = ampResults[0]
                            progress_layout.visibility = View.INVISIBLE
                            message = handler.obtainMessage()
                            message?.arg1 = SHOW_RESULT
                            message?.arg2 = 3
                            handler.sendMessage(message!!)
                        }
        )

//        val loadtask = Flowable.fromCallable {
//            loadimages()
//            false
//        }
//
//        val fiveTasks = Flowable.zip(
//                loadtask.subscribeOn(Schedulers.io()),
//                fivetask1.subscribeOn(Schedulers.io()),
//                fivetask2.subscribeOn(Schedulers.io()),
//                fivetask3.subscribeOn(Schedulers.io()),
//                fivetask4.subscribeOn(Schedulers.io()),
//                fivetask5.subscribeOn(Schedulers.io()),
//                io.reactivex.functions.Function6{ _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean?, _: Boolean? -> false }
//        )

        val patchTask = Flowable.concatArray(
                fivetask1.subscribeOn(Schedulers.io()),
                fivetask2.subscribeOn(Schedulers.io()),
                fivetask3.subscribeOn(Schedulers.io()),
                fivetask4.subscribeOn(Schedulers.io()),
                fivetask5.subscribeOn(Schedulers.io()),
                fivetask6.subscribeOn(Schedulers.io()),
                fivetask7.subscribeOn(Schedulers.io()))
//                fivetask8.subscribeOn(Schedulers.io()),
//                fivetask9.subscribeOn(Schedulers.io()),
//                fivetask10.subscribeOn(Schedulers.io()))
//
        fullTask = patchTask.subscribe{}
//        fullTask = fiveTasks.subscribe{}

    }

    override fun changeProgressUI(progress: Int) {
        when (progress) {
            1 -> {
                progress_1_circle!!.setImageDrawable(getDrawable(R.drawable.progress_icon_yellow))
                progress_2_circle!!.setImageDrawable(getDrawable(R.drawable.progress_icon_gray))
                progress_3_circle!!.setImageDrawable(getDrawable(R.drawable.progress_icon_gray))
                progress_1_text!!.setTextColor(getColor(R.color.myBlack))
                progress_2_text!!.setTextColor(getColor(R.color.myGray))
                progress_3_text!!.setTextColor(getColor(R.color.myGray))
            }
            2 -> {
                progress_1_circle!!.setImageDrawable(getDrawable(R.drawable.progress_icon_green))
                progress_2_circle!!.setImageDrawable(getDrawable(R.drawable.progress_icon_yellow))
                progress_3_circle!!.setImageDrawable(getDrawable(R.drawable.progress_icon_gray))
                progress_1_text!!.setTextColor(getColor(R.color.myGray))
                progress_2_text!!.setTextColor(getColor(R.color.myBlack))
                progress_3_text!!.setTextColor(getColor(R.color.myGray))
            }
            3 -> {
                progress_1_circle!!.setImageDrawable(getDrawable(R.drawable.progress_icon_green))
                progress_2_circle!!.setImageDrawable(getDrawable(R.drawable.progress_icon_green))
                progress_3_circle!!.setImageDrawable(getDrawable(R.drawable.progress_icon_yellow))
                progress_1_text!!.setTextColor(getColor(R.color.myGray))
                progress_2_text!!.setTextColor(getColor(R.color.myGray))
                progress_3_text!!.setTextColor(getColor(R.color.myBlack))
            }
        }
    }

    override fun showResult() {
        // result object amplitude
        Core.normalize(resObjAmp, resObjAmp, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC3) // Transform the matrix with float values
        resultOBitmap = Bitmap.createBitmap(resObjAmp!!.rows(), resObjAmp!!.cols(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resObjAmp, resultOBitmap)
        imageView_obj_amp.setImageBitmap(resultOBitmap)

        // result object phase
        Core.normalize(resObjPhase, resObjPhase, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC1) // Transform the matrix with float values
        resultOBitmapAngle = Bitmap.createBitmap(resObjPhase!!.rows(), resObjPhase!!.cols(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resObjPhase, resultOBitmapAngle)
        imageView_obj_phase.setImageBitmap(resultOBitmapAngle)
        result_buttons.visibility = View.VISIBLE
    }

    override fun saveImage() {
        fileSaver.saveBmpImage(applicationContext, resultOBitmap!!, ampFileName)
        fileSaver.saveBmpImage(applicationContext, resultOBitmapAngle!!, phaseFileName)
    }

    private fun saveImageauto(amp: Mat, phase: Mat, crop_x: Int, crop_y: Int) {
        // result object amplitude
        Core.normalize(amp, amp, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC3) // Transform the matrix with float values
        var resultOBitmapauto = Bitmap.createBitmap(amp!!.rows(), amp!!.cols(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(amp, resultOBitmapauto)

        // result object phase
        Core.normalize(phase, phase, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC1) // Transform the matrix with float values
        var resultOBitmapAngleauto = Bitmap.createBitmap(phase!!.rows(), phase!!.cols(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(phase, resultOBitmapAngleauto)

        var timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREAN).format(Date())
        var ampFileNameauto = fileStart + "_" + timeStamp + "_" + crop_x.toString() + "_" + crop_y.toString() + "_amp.jpg"
        var phaseFileNameauto = fileStart + "_" + timeStamp + "_" + crop_x.toString() + "_" + crop_y.toString() + "_phase.jpg"

        fileSaver.saveBmpImage(applicationContext, resultOBitmapauto!!, ampFileNameauto)
        fileSaver.saveBmpImage(applicationContext, resultOBitmapAngleauto!!, phaseFileNameauto)
    }

    override fun showBefore() {
        // original object amplitude
        Core.normalize(orgObjAmp, orgObjAmp, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC1) // Transform the matrix with float values
        orgOBitmap = Bitmap.createBitmap(orgObjAmp!!.rows(), orgObjAmp!!.cols(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(orgObjAmp, orgOBitmap)
        imageView_obj_amp.setImageBitmap(orgOBitmap)
        before_after_button.text = resources.getString(R.string.after)
    }

    override fun showAfter() {
        // result object amplitude
        Core.normalize(resObjAmp, resObjAmp, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC1) // Transform the matrix with float values
        resultOBitmap = Bitmap.createBitmap(resObjAmp!!.rows(), resObjAmp!!.cols(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(resObjAmp, resultOBitmap)
        imageView_obj_amp.setImageBitmap(resultOBitmap)
        before_after_button.text = resources.getString(R.string.before)
    }

    override fun preProcess(iRGB: Int): HashmapData {
        TODO("Not yet implemented")
    }

    fun preProcessMono(iRGB: Int, crop_x: Int, crop_y: Int, index: Int): HashmapData {
        /** pre-process image **/
        val tag = "pre-process"

//        // distortion value
//        var distValue = floatArrayOf(0.0313690384695265F, -0.00126679968152714F, 0F, 0F)
//        var distMat = Mat(1, 4, CvType.CV_32F)
//        distMat.put(0, 0, distValue);
//
//        var mtxValue = floatArrayOf(688.015419803622F, 0F, 1595.36915351398F, 0F, 688.013187402952F, 1215.45121729576F, 0F, 0F, 1F)
//        var mtxMat = Mat(3, 3, CvType.CV_32F)
//        mtxMat.put(0, 0, mtxValue);

        // outputs
        val preProcessOutputData = HashmapData()
        val ibk = Mat(2, nImg, CvType.CV_32F)
        val cropImages = arrayOfNulls<Mat>(nImg)


        // read in all images into the memory first
        Log.d(tag, "loading the images...")
        for (m in 0 until nImg) {
            // update ui for loading images

//            message = handler.obtainMessage()
//            message?.arg1 = 0
//            message?.obj = "Loading Images... " + (m + 1) + "/" + nImg
//            handler.sendMessage(message!!)

//            // configure filename
//            val imgName = fileStart + "_" + (m + 1).toString().padStart(3, '0') + fileEnd
//            val filepath = "$root/Pictures/OLED_FPM/$imgName"
//            Log.d(tag, filepath)
//
//            //Reading the Image from the file, and crop it
//            val curI = Imgcodecs.imread(filepath, Imgcodecs.IMREAD_COLOR) // grayscale makes it blurry
//            Core.extractChannel(curI, curI, 1) // green
//
//            // Distortion corrections
//            var curIUndist = Mat(curI.rows(), curI.cols(), CvType.CV_32F)
//            Calib3d.undistort(curI, curIUndist, mtxMat, distMat, mtxMat)

            val rectCrop = Rect(crop_y - 1, crop_x - 1, Np, Np)
            val cropImage = Mat(curIs?.get(m), rectCrop)
            cropImage.convertTo(cropImage, CvType.CV_32F)
            Core.multiply(cropImage, Scalar(256.0), cropImage) // for jpg image only

            cropImages[m] = cropImage

            // specify background region for background noise estimation

//            val cropI1 = curIs?.get(m)?.submat(Rect(0, 0, 100, 100))
//            var bk1 = Core.sumElems(cropImage).`val`[0]
            var bk1 = 0.0
//
//            val cropI2 = curIs?.get(m)?.submat(curIs!![m]?.cols()?.minus(200)?.let { Rect(it, curIs!![m]?.rows() - 200, 100, 100) })
//            var bk2 = Core.sumElems(cropImage).`val`[0]
            var bk2 = 0.0

            ibk.put(0, m, bk1)
            ibk.put(1, m, bk2)

            if (m > 0 && m % 10 == 0) Runtime.getRuntime().gc() // garbage collector
        }
        Log.d(tag, "file load completed")

        // return result
        preProcessOutputData.putMat1DArray("cropImages", cropImages)
        preProcessOutputData.putMat("ibk", ibk)
        preProcessOutputData.putInt("nImg", nImg)
        preProcessOutputData.putInt("iRGB", iRGB)
        preProcessOutputData.putInt("crop_x", crop_x)
        preProcessOutputData.putInt("crop_y", crop_y)
        preProcessOutputData.putInt("index", index)
        return preProcessOutputData
    }

    override fun configureReconstruct(preprocessedData: HashmapData): HashmapData {
        /** reconstruct configuration */
        /** Calculates useful system parameters that define how the data is processed
         * lambda: wavelength of light
         * NA: Numerical Aperture
         * mag: magnification
         * dpix_c: size of camera pixels on sensro
         * NsampR: size of the image region to be processed in [row, col] (that is, (y,x))
         */
        // outputs
        val reconstructInputData = HashmapData()
        val defineSystemInput = HashmapData()

        // get input data
        val cropImages = preprocessedData.getMat1DArray("cropImages")!!
        val ibk = preprocessedData.getMat("ibk")!!
        val nImg = preprocessedData.getInt("nImg")!!
        val iRGB = preprocessedData.getInt("iRGB")!!
        val crop_x = preprocessedData.getInt("crop_x")!!
        val crop_y = preprocessedData.getInt("crop_y")!!
        val index = preprocessedData.getInt("index")!!

        // nSampleR, iBk
        val nSampleR = intArrayOf(cropImages[0]!!.rows(), cropImages[0]!!.cols())
        val iBk = DoubleArray(nImg)

        // variable matching
        nSampleR[0] = cropImages[0]!!.rows()
        nSampleR[1] = cropImages[0]!!.cols()

        // Take the mean across the two regions
        for (i in 0 until nImg) {
            var sum = 0.0
            val row = ibk.rows()
            for (j in 0 until row) {
                sum += ibk.get(j, i)[0] }
            val average = sum / row
            iBk[i] = average
        }
        if (iBk[0] > ibkThr) { // Set the first one, if it's above the threshold
            iBk[0] = ibkThr.toDouble()
        }
        for (ii in 1 until iBk.size) {
            if (iBk[ii] > ibkThr) {
                iBk[ii] = iBk[ii - 1] // If above the threshold, is brightfield; set to previous value
            }
        }

        // Background noise subtraction
        for (ii in iBk.indices) {
            Core.subtract(cropImages[ii], Scalar(2 * iBk[ii]), cropImages[ii])
            Imgproc.threshold(cropImages[ii], cropImages[ii], 0.0, Double.POSITIVE_INFINITY, Imgproc.THRESH_TOZERO)
        }

        // Process according to options
        val con = nSampleR[0] * dPixC / mag // Conversion factor(pixels / (1 / um))
        /** Set up system  */
        Log.d("iRGB", iRGB.toString() + "")

        // define system inputs
        defineSystemInput.putInt("iRGB", iRGB)
        defineSystemInput.putDouble("wavelength", wavelengths[iRGB])
        defineSystemInput.putDouble("dPixC", dPixC)
        defineSystemInput.putDouble("mag", mag)
        defineSystemInput.putInt1DArray("nSampleR", nSampleR)
        defineSystemInput.putInt("nImg", nImg)
        defineSystemInput.putInt("crop_x", crop_x)
        defineSystemInput.putInt("crop_y", crop_y)
        defineSystemInput.putInt("index", index)

        // define system
        val systemOpts = defineSystem(defineSystemInput) // variables set after this call
        val nObj = systemOpts.nObj
        val wNA = systemOpts.wNA

        //  reorder of data and parameters that will be used in reconstruction
        val idx = calculator.argSort(illuminationNA[index], true)
        val copyIlluminationNA = illuminationNA[index].clone()
        val copyI = cropImages.clone()
        val copyFreqUV = freqUV!![index].clone()
        val copyFreqUVCal = freqUVCal!![index].clone()
        val copyFreqUVDesign = freqUVDesign!![index].clone()
        val copyDFI = dfi[index].clone()
        for (i in 0 until nImg) {
            illuminationNA[index][i] = copyIlluminationNA[idx[i]!!]
            cropImages[i] = copyI[idx[i]!!]
            freqUV!![index][i] = copyFreqUV[idx[i]!!]
            freqUVCal!![index][i] = copyFreqUVCal[idx[i]!!]
            freqUVDesign!![index][i] = copyFreqUVDesign[idx[i]!!]
            dfi[index][i] = copyDFI[idx[i]!!]
        }

        // calculate O0 : initial guess for obj( in real space) % Initialize to normalized coherent sum
        val row = cropImages[0]!!.rows()
        val col = cropImages[0]!!.cols()
        val sumI = Mat.zeros(row, col, CvType.CV_32F)
        for (mat in cropImages) Core.add(sumI, mat, sumI)
        Core.divide(sumI, Scalar(cropImages.size.toDouble()), sumI)
        lineDebugger.timeFactorTest() // 1378
        val size = Size(nObj[0].toDouble(), nObj[1].toDouble())
        val o0 = Mat()
        val sqrtSumI = Mat()
        Core.sqrt(sumI, sqrtSumI)
        Imgproc.resize(sqrtSumI, o0, size, 0.0, 0.0, Imgproc.INTER_CUBIC)

        Runtime.getRuntime().gc() // garbage collector

        // return settings
        reconstructInputData.putInt("iRGB", iRGB)
        reconstructInputData.putMat("O0", o0)
        reconstructInputData.putMat("P0", wNA) // P0: initial guess for P
        reconstructInputData.putMat("Ps", wNA) // Pupil support
        reconstructInputData.putDouble("con", con)
        reconstructInputData.putInt1DArray("nObj", nObj)
        reconstructInputData.putMat1DArray("I", cropImages)
        reconstructInputData.putInt("maxIteration", maxIter)
        reconstructInputData.putInt("crop_x", crop_x)
        reconstructInputData.putInt("crop_y", crop_y)
        reconstructInputData.putInt("index", index)
        return reconstructInputData
    }

    override fun defineSystem(defineSystemInput: HashmapData): SystemOpts {
        /** defineSystem is called from configureReconstruct. this updates systemOpts **/
        /** calculates w_NA and nObj **/
        /** updates imgNCent, imgCenter, freqUVDesign, freqUVCal, freqUV, illumNA, du **/
        val tag = "defineSystem"

        // get input values
        val iRGB = defineSystemInput.getInt("iRGB")!!
        val mag = defineSystemInput.getDouble("mag")!!
        val dPixC = defineSystemInput.getDouble("dPixC")!!
        val nSampleR = defineSystemInput.getInt1DArray("nSampleR")!!
        val crop_x = defineSystemInput.getInt("crop_x")!!
        val crop_y = defineSystemInput.getInt("crop_y")!!
        val index = defineSystemInput.getInt("index")!!

        val wNA = Mat() // init

        val wavelength = wavelengths[iRGB]
        val uMax = NA / wavelength // Maximum spatial frequency set by NA (1/um)
        val dPixM = dPixC / mag // Effective image pixel size on the object plane (um)

        val fieldOfView = DoubleArray(2)
        fieldOfView[0] = nSampleR[0] * dPixM
        fieldOfView[1] = nSampleR[1] * dPixM // FoV in the object space;

        imgNCent[0] = (crop_x - nCent[0] + Np / 2).toDouble()
        imgNCent[1] = (crop_y - nCent[1] + Np / 2).toDouble()

        imgCenter[0] = imgNCent[0] * dPixM
        imgCenter[1] = imgNCent[1] * dPixM

        // LED Arrays
        val sinThetaVUsed = DoubleArray(ledProps.nled)
        val sinThetaHUsed = DoubleArray(ledProps.nled)
        val illuminationNaUsed = DoubleArray(ledProps.nled)

        //corresponding spatial freq for each LEDs
        var countLED = 0
        var ledCol = 0
        var ledRow = 0
        for (j in 0..31) {
            for (i in 0..31) {
                // for smartphone capture
                ledCol = i
                ledRow = 31-j

                // for test set
                //ledCol = 31-j
                //ledRow = 31-i

                if (ledProps.litCoordinate[ledCol][ledRow] == 1) {
                    illuminationNaUsed[countLED] = ledProps.illuminationNA[ledCol][ledRow]
                    sinThetaVUsed[countLED] = ledProps.sinThetaV[ledCol][ledRow]
                    sinThetaHUsed[countLED] = ledProps.sinThetaH[ledCol][ledRow]
                    countLED += 1
                }
            }
        }
        for (i in 0 until ledProps.nled) {
            freqUVDesign!![index][i][0] = sinThetaHUsed[i] / wavelength
            freqUVDesign!![index][i][1] = sinThetaVUsed[i] / wavelength
            freqUVCal!![index][i][0] = sinThetaHUsed[i] / wavelength
            freqUVCal!![index][i][1] = sinThetaVUsed[i] / wavelength
        }

        // Figure out which illumination to use
        // parameters
        freqUV!![index] = freqUVCal!![index]
        for (i in 0 until nImg) {
            Log.d(tag, illuminationNA.size.toString() + ", " + freqUV!!.size)
            illuminationNA[index][i] = sqrt(freqUV!![index][i][0].pow(2.0) + freqUV!![index][i][1].pow(2.0)) * wavelength
        }
        for (k in 0 until ledProps.nled) {
            if (illuminationNaUsed[k] <= NA) dfi[index][k] = 0 else dfi[index][k] = 1
        }
        if (nSampleR[0] % 2 == 1) { // Row
            du[index][0] = 1 / dPixM / (nSampleR[0] - 1)
        } else {
            du[index][0] = 1 / fieldOfView[1] // Sampling size at Fourier plane is always = 1 / FoV;
        }
        if (nSampleR[1] % 2 == 1) { // Col
            du[index][1] = 1 / dPixM / (nSampleR[1] - 1)
        } else {
            du[index][1] = 1 / fieldOfView[1]
        }

        val umIdx = Mat(1, 2, CvType.CV_32F)
        umIdx.put(0, 0, uMax / du[index][0])
        umIdx.put(0, 1, uMax / du[index][1]) //max spatial freq/sample size at Fourier plane = number of samples in Fourier space (?)

        val n = Mat()
        val m = Mat()
        Core.subtract(
                calculator.myRange(nSampleR[0]),
                Scalar(((nSampleR[0] + 1) / 2.toFloat()).roundToInt().toDouble()),
                n) // vertical, row index (y) #Indices from center of window; allows szR to be rectangle (even though this is not desired)
        Core.subtract(
                calculator.myRange(nSampleR[1]),
                Scalar(((nSampleR[1] + 1) / 2.toFloat()).roundToInt().toDouble()),
                m) // horizontal, col index (x);
        val mmnn = calculator.myMeshGrid(m, n)
        // Generate cut off window by NA
        val mm = mmnn[0]
        val nn = mmnn[1] //Create grid

        // assume a circular pupil function, lpf due to finite NA
        val div1 = Mat()
        val div2 = Mat()
        val pow1 = Mat()
        val pow2 = Mat()
        val add = Mat()
        val sqrt = Mat()
        Core.divide(nn, Scalar(umIdx.get(0, 0)[0]), div1)
        Core.pow(div1, 2.0, pow1)
        Core.divide(mm, Scalar(umIdx.get(0, 1)[0]), div2)
        Core.pow(div2, 2.0, pow2)
        Core.add(pow1, pow2, add)
        Core.sqrt(add, sqrt)
        Imgproc.threshold(sqrt, wNA, 1.0, 1.0, Imgproc.THRESH_BINARY_INV)

        // maximum spatial frequency achievable based on the maximum illumination
        // angle from the LED array and NA of the objective
        val umP = calculator.maxValue(illuminationNA[index]) / wavelength + uMax
        // resolution achieved after freq post-processing
        val naS = umP * wavelength
        Log.d(tag, "synthetic NA : $naS") // 0.41630899, test ok

        // assume the max spatial freq of the original object
        // um_obj>um_p
        // assume the # of pixels of the original object
        // up-sample by 2 for intensity
        val nObj= IntArray(2)
        nObj[0] = (umP / du[index][0]).roundToInt() * 2 * 2 // Solving for the number of samples in freq space
        nObj[1] = (umP / du[index][1]).roundToInt() * 2 * 2
        Log.d(tag, umP.toString() + "," + du[index][0] + "," + du[index][1])
        Log.d(tag, nObj[0].toString() + "," + nObj[1])

        //Follows rate = 2*BW where BW=um_p (max spatial freq) and rate = N_obj/du
        //(num samples/sample size)

        // need to enforce N_obj/Np = integer to ensure no FT artifacts
        nObj[0] = ceil(nObj[0].toDouble() / nSampleR[0].toDouble()).toInt() * nSampleR[0]
        nObj[1] = ceil(nObj[1].toDouble() / nSampleR[1].toDouble()).toInt() * nSampleR[1]

        val umObj = DoubleArray(2)
        umObj[0] = du[index][0] * nObj[0] / 2
        umObj[1] = du[index][1] * nObj[1] / 2

        // sampling size of the object (=pixel size of the test image)
        val dxObj = DoubleArray(2)
        dxObj[0] = 1 / umObj[0] / 2
        dxObj[1] = 1 / umObj[1] / 2

        // spatial coordinates for object space
        val first = Mat()
        val second = Mat()

        Core.multiply(calculator.myArrange(-nObj[1] / 2 - 1.toDouble(), nObj[1] / 2.toDouble()), Scalar(dxObj[1]), first)
        Core.multiply(calculator.myArrange(-nObj[0] / 2 - 1.toDouble(), nObj[0] / 2.toDouble()), Scalar(dxObj[0]), second)
        val xy = calculator.myMeshGrid(first, second)
        val x = xy[0]
        val y = xy[1]

        // Spatial coordinates for image space
        //lineDebugger.timeFactorTest() // 1378
        Core.multiply(calculator.myArrange(-nSampleR[1] - 0.5, nSampleR[1] / 2.toDouble()), Scalar(dPixM), first)
        Core.multiply(calculator.myArrange(-nSampleR[0] - 0.5, nSampleR[0] / 2.toDouble()), Scalar(dPixM), second)
        val x0y0 = calculator.myMeshGrid(first, second)
        val x0 = x0y0[0]
        val y0 = x0y0[1]
        //lineDebugger.timeFactorTest()
        // spatial frequency coordinates
        // for object space

        val uv = calculator.myMeshGrid(
                calculator.myArrange(-umObj[1], umObj[1], du[index][1]),
                calculator.myArrange(-umObj[0], umObj[0], du[index][0]))
        val u = uv[0]
        val v = uv[1]
        //lineDebugger.timeFactorTest()

        // for image space
        val u0v0 = calculator.myMeshGrid(
                calculator.myArrange(-du[index][1] * nSampleR[1] / 2, du[index][1] * nSampleR[1] / 2, du[index][1]),
                calculator.myArrange(-du[index][0] * nSampleR[0] / 2, du[index][0] * nSampleR[0] / 2, du[index][0]))
        //lineDebugger.timeFactorTest()
        val u0 = u0v0[0]
        val v0 = u0v0[1]

        // Define k-space for these coordinates
        val uPow = Mat()
        val u0Pow = Mat()
        Core.pow(u, 2.0, uPow)
        Core.pow(u0, 2.0, u0Pow)
        val vPow = Mat()
        val v0Pow = Mat()
        Core.pow(v, 2.0, vPow)
        Core.pow(v0, 2.0, v0Pow)
        val xPow = Mat()
        val x0Pow = Mat()
        Core.pow(x, 2.0, xPow)
        Core.pow(x0, 2.0, x0Pow)
        val yPow = Mat()
        val y0Pow = Mat()
        Core.pow(y, 2.0, yPow)
        Core.pow(y0, 2.0, y0Pow)

        val k0 = Mat()
        Core.add(u0Pow, v0Pow, k0)
        Core.multiply(k0, Scalar(Math.PI * wavelength), k0) // k0

        Runtime.getRuntime().gc() // garbage collector

        return SystemOpts(nObj, wNA) // return
    }

    override fun fpmFunc(reconstructInputData: HashmapData?) {
        /** fpm Function, for iteration of reconstruct **/
        val iRGB = reconstructInputData!!.getInt("iRGB")!!
        val arrayI = reconstructInputData.getMat1DArray("I")!!
        val nObj = reconstructInputData.getInt1DArray("nObj")!!
        val crop_x = reconstructInputData.getInt("crop_x")!!
        val crop_y = reconstructInputData.getInt("crop_y")!!
        val index = reconstructInputData.getInt("index")!!


        val pupilShiftY: DoubleArray
        val pupilShiftX: DoubleArray
        var err2: Int
        // iterative phase recovery using alternating minimization sequentially on a stack of measurement I(n1 x n2 x nz).
        // It consists of 2 loops.
        // The main loop updates the reconstruction results O and P.

        // Operators & derived constants
        // size of measurement
        val nSampleR = IntArray(2)
        val cen0 = IntArray(2)
        nSampleR[0] = arrayI[0]!!.rows()
        nSampleR[1] = arrayI[0]!!.cols() // Size of real space
        val nImg = arrayI.size
        cen0[0] = (nObj[0] + 1.toFloat()).roundToInt() / 2
        cen0[1] = (nObj[1] + 1.toFloat()).roundToInt() / 2 // Coordinate of center of Fourier space

        // Operators
        // cen is in (x, y)
        // For the given image x, crop out a rectangle of NsampR(1) x NsampR(2) row, col
        // extent around center cen

        // Initialization
        // Copy some parameters
        val pup = arrayOfNulls<Mat>(2)
        pup[0] = reconstructInputData.getMat("P0")
        pup[1] = Mat.zeros(reconstructInputData.getMat("P0")!!.size(), CvType.CV_32F)
        objR[index] = reconstructInputData.getMat("O0")!!
        objRComplex[index] = arrayOfNulls(2)
        objRComplex[index][0] = objR[index]
        objRComplex[index][1] = Mat.zeros(objR[index]!!.size(), CvType.CV_32F) // add 0j to make complex number
        val objF: Array<Mat?> = fourierTransformer.fftShift(fourierTransformer.fftShiftComplex(objRComplex[index])) // real, imaginary
        val err = IntArray(reconstructInputData.getInt("maxIteration")!! + 1)
        for (i in err.indices) err[i] = 0
        var iteration: Int = -1

        // Display init
        Log.e("INIT", "|  Iteration  |   Image   |      Cost     |  LED Pos. Step |\n")
        Log.e("INIT", "+-------------+-----------+---------------+----------------+\n")
        Log.e("INIT", "|     $iteration     |    -/-    |       -       |     0      |\n")

        // Main
        // stopping criteria: when relative change in error falls below some value,
        // can change this value to speed up the process by using a larger value but
        // will trading off the reconstruction accuracy
        // error is defined by the difference b / w the measurement and the estimated
        // images in each iteration
        // Convert from 1 / um center to corresponding crop
        pupilShiftY = DoubleArray(nImg)
        pupilShiftX = DoubleArray(nImg)
        for (i in 0 until nImg) pupilShiftY[i] = (freqUV!![index][i][1] * reconstructInputData.getDouble("con")!!).roundToInt().toDouble()
        for (i in 0 until nImg) pupilShiftX[i] = (freqUV!![index][i][0] * reconstructInputData.getDouble("con")!!).roundToInt().toDouble()
        val yxMid = DoubleArray(2)
        val hfSz: Int

        // Corresponding crop regions
        yxMid[0] = floor(nObj[0].toDouble() / 2.toDouble()) + 1
        yxMid[1] = floor(nObj[1].toDouble() / 2.toDouble()) + 1
        hfSz = floor(nSampleR[1].toDouble() / 2.0).toInt()
        val cropR = Mat.zeros(nImg, 4, CvType.CV_32F)
        for (i in 0 until nImg) {
            cropR.put(i, 0, yxMid[0] + pupilShiftY[i] - hfSz - 1) // y start
            cropR.put(i, 1, yxMid[0] + pupilShiftY[i] + hfSz - 2) // y end
            cropR.put(i, 2, yxMid[1] + pupilShiftX[i] - hfSz - 1) // x start
            cropR.put(i, 3, yxMid[1] + pupilShiftX[i] + hfSz - 2) // x end
        }
        val objCrop = Array(nImg) { arrayOfNulls<Mat>(2) }
        for (i in 0 until nImg) {
            val tempRe = Mat.ones(nSampleR[0], nSampleR[1], CvType.CV_32F)
            val tempIm = Mat.zeros(nSampleR[0], nSampleR[1], CvType.CV_32F)
            val temp = arrayOfNulls<Mat>(2)
            temp[0] = tempRe
            temp[1] = tempIm
            objCrop[i] = temp
        }
        orgResults[index] = objR[index]!!.clone()
        val elapsedFpmStartIt = System.currentTimeMillis() - startTime
        Log.e("elapsed(image iteration start): ", elapsedFpmStartIt.toString())
        message = handler.obtainMessage()
        if (index == parallelsize - 1) {
            message?.arg1 = PROGRESS_3
            handler.sendMessage(message!!)
        }
        // each iteration
        while (iteration < reconstructInputData.getInt("maxIteration")!!) {
            if (index == parallelsize - 1) {
                message = handler.obtainMessage()
                message?.arg1 = 0
                message?.obj = "Iteration... " + (iteration + 1) + "/" + reconstructInputData.getInt("maxIteration")
                handler.sendMessage(message!!)
            }

            err2 = 0
            iteration += 1
            Log.d("FPMFUNC", "Evaluating image: \n")
            for (ii in 0 until nImg) {
                // Print current image number
                // Crop measured intensity
                val iMea: Mat? = arrayI[ii]
                val objFCrop = arrayOfNulls<Mat>(2)
                val objFCropP = arrayOfNulls<Mat>(2)
                var objCropP: Array<Mat?>
                var objFUp: Array<Mat?>

                // objFCrop
                objFCrop[0] = objF[0]!!.clone().submat(Rect(
                        cropR[ii, 2][0].toInt(),
                        cropR[ii, 0][0].toInt(),
                        cropR[ii, 1][0].toInt() + 1 - cropR[ii, 0][0].toInt(),
                        cropR[ii, 3][0].toInt() + 1 - cropR[ii, 2][0].toInt())) // cloned..
                objFCrop[1] = objF[1]!!.clone().submat(Rect(
                        cropR[ii, 2][0].toInt(),
                        cropR[ii, 0][0].toInt(),
                        cropR[ii, 1][0].toInt() + 1 - cropR[ii, 0][0].toInt(),
                        cropR[ii, 3][0].toInt() + 1 - cropR[ii, 2][0].toInt())) // cloned..

                // objFCropP
                objFCropP[0] = Mat(
                        cropR[ii, 1][0].toInt() + 1 - cropR[ii, 0][0].toInt(),
                        cropR[ii, 3][0].toInt() + 1 - cropR[ii, 2][0].toInt(),
                        CvType.CV_32F)
                objFCropP[1] = Mat(
                        cropR[ii, 1][0].toInt() + 1 - cropR[ii, 0][0].toInt(),
                        cropR[ii, 3][0].toInt() + 1 - cropR[ii, 2][0].toInt(),
                        CvType.CV_32F)

                // objcropP
                Core.multiply(objFCrop[0], pup[0], objFCropP[0])
                Core.multiply(objFCrop[1], pup[0], objFCropP[1])
                objCropP = fourierTransformer.ifftShiftComplex(fourierTransformer.ifftShift(objFCropP))
                // don't know why below two...
                Core.divide(objCropP[0], Scalar(40000.0), objCropP[0])
                Core.divide(objCropP[1], Scalar(40000.0), objCropP[1])

                // Objfup
                when {
                    ((dfi[index][ii] == 1) && ( illuminationNA[index][ii]<(NA*1.6))) -> {
                        //println("exceptNum $ii")
                        objFUp = objFCropP
                    }
                    dfi[index][ii] == 0 -> {
                        val add = arrayOfNulls<Mat>(2)
                        add[0] = Mat()
                        add[1] = Mat()
                        Core.add(objCropP[0], Scalar(2.220446049250313e-16), add[0])
                        Core.add(objCropP[1], Scalar(2.220446049250313e-16), add[1])
                        val div_bottom = Mat(objCropP[0]!!.rows(), objCropP[0]!!.cols(), CvType.CV_32F)
                        val div_bottom_0 = Mat()
                        val div_bottom_1 = Mat()
                        Core.pow(add[0], 2.0, div_bottom_0)
                        Core.pow(add[1], 2.0, div_bottom_1)
                        Core.add(div_bottom_0, div_bottom_1, div_bottom)
                        Core.sqrt(div_bottom, div_bottom)
                        val sqrt = Mat()
                        Core.sqrt(iMea, sqrt)
                        val mul = arrayOfNulls<Mat>(2)
                        mul[0] = Mat()
                        mul[1] = Mat()
                        Core.multiply(objCropP[0], sqrt, mul[0])
                        Core.multiply(objCropP[1], sqrt, mul[1])
                        val testSrc2 = arrayOfNulls<Mat>(2)
                        testSrc2[0] = Mat()
                        testSrc2[1] = Mat()
                        Core.divide(mul[0], div_bottom, testSrc2[0])
                        Core.divide(mul[1], div_bottom, testSrc2[1])
                        objFUp = fourierTransformer.fftShift(fourierTransformer.fftShiftComplex(testSrc2))
                    }
                    else -> {
                        // Adaptive de-noising process

                        val sorted_c_m = Mat()
                        val c_m = Mat()
                        val c_mAbs = Mat()
                        val ImeaSqrt = Mat()
                        val ObjcropPAbs = Mat()
                        Core.sqrt(iMea, ImeaSqrt)
                        val ObjcropPAbs_0 = Mat()
                        val ObjcropPAbs_1 = Mat()
                        Core.pow(objCropP[0], 2.0, ObjcropPAbs_0)
                        Core.pow(objCropP[1], 2.0, ObjcropPAbs_1)
                        Core.add(ObjcropPAbs_0, ObjcropPAbs_1, ObjcropPAbs)
                        Core.sqrt(ObjcropPAbs, ObjcropPAbs)
                        Core.subtract(ImeaSqrt, ObjcropPAbs, c_m)
                        Core.absdiff(ImeaSqrt, ObjcropPAbs, c_mAbs)
                        val c_mAbs_reshaped = c_mAbs.reshape(1, 1)
                        Core.sort(c_mAbs_reshaped, sorted_c_m, Core.SORT_ASCENDING)

                        val c_m_update = Mat()
                        val updatedI = Mat()
                        val index2 = ceil(ADAPTIVE_PERCENT * nSampleR[0] * nSampleR[1]).toInt()
                        val lowThresholdVal = sorted_c_m[0, index2][0]
                        Imgproc.threshold(c_m, c_m_update, lowThresholdVal, Double.POSITIVE_INFINITY, Imgproc.THRESH_TOZERO)
                        Core.add(c_m_update, ObjcropPAbs, updatedI)
                        val add = arrayOfNulls<Mat>(2)
                        add[0] = Mat()
                        add[1] = Mat()
                        Core.add(objCropP[0], Scalar(2.220446049250313e-16), add[0])
                        Core.add(objCropP[1], Scalar(2.220446049250313e-16), add[1])
                        val div_bottom = Mat(objCropP[0]!!.rows(), objCropP[0]!!.cols(), CvType.CV_32F)
                        val div_bottom_0 = Mat()
                        val div_bottom_1 = Mat()
                        Core.pow(add[0], 2.0, div_bottom_0)
                        Core.pow(add[1], 2.0, div_bottom_1)
                        Core.add(div_bottom_0, div_bottom_1, div_bottom)
                        Core.sqrt(div_bottom, div_bottom)
                        val mul = arrayOfNulls<Mat>(2)
                        mul[0] = Mat()
                        mul[1] = Mat()
                        Core.multiply(objCropP[0], updatedI, mul[0])
                        Core.multiply(objCropP[1], updatedI, mul[1])
                        val test_src_2 = arrayOfNulls<Mat>(2)
                        test_src_2[0] = Mat()
                        test_src_2[1] = Mat()
                        Core.divide(mul[0], div_bottom, test_src_2[0])
                        Core.divide(mul[1], div_bottom, test_src_2[1])
                        objFUp = fourierTransformer.fftShift(fourierTransformer.fftShiftComplex(test_src_2))
                    }
                }

                // Pupil Update
                var myMax: Double
                val absObjF0 = Mat(objF[0]!!.rows(), objF[0]!!.cols(), CvType.CV_32F)
                Core.pow(objF[0], 2.0, absObjF0)
                val absObjF1 = Mat(objF[0]!!.rows(), objF[0]!!.cols(), CvType.CV_32F)
                Core.pow(objF[1], 2.0, absObjF1)
                val abs_objf = Mat(objF[0]!!.rows(), objF[0]!!.cols(), CvType.CV_32F)
                Core.add(absObjF0, absObjF1, abs_objf)
                Core.sqrt(abs_objf, abs_objf)
                val mmr = Core.minMaxLoc(abs_objf)
                myMax = mmr.maxVal
                val sub_0 = Mat()
                val sub_1 = Mat()
                Core.subtract(objFUp[0], objFCropP[0], sub_0)
                Core.subtract(objFUp[1], objFCropP[1], sub_1)
                val conj_objf_crop_test_0 = objFCrop[0]
                val conj_objf_crop_test_1 = objFCrop[1]
                Core.multiply(conj_objf_crop_test_1, Scalar(-1.0), conj_objf_crop_test_1)

                // abs
                val absoluteObjfcrop = Mat(objFCrop[0]!!.rows(), objFCrop[0]!!.cols(), CvType.CV_32F)
                val absoluteObjfcrop_0 = Mat()
                val absoluteObjfcrop_1 = Mat()
                Core.pow(objFCrop[0], 2.0, absoluteObjfcrop_0)
                Core.pow(objFCrop[1], 2.0, absoluteObjfcrop_1)
                Core.add(absoluteObjfcrop_0, absoluteObjfcrop_1, absoluteObjfcrop)
                Core.sqrt(absoluteObjfcrop, absoluteObjfcrop)
                val mul_conj_abs_test_0 = Mat()
                val mul_conj_abs_test_1 = Mat()
                Core.multiply(conj_objf_crop_test_0, absoluteObjfcrop, mul_conj_abs_test_0)
                Core.multiply(conj_objf_crop_test_1, absoluteObjfcrop, mul_conj_abs_test_1)

                // complex multiplication...
                val multest_0 = Mat()
                val multest_1 = Mat()
                val re_re = Mat()
                val im_im = Mat()
                Core.multiply(mul_conj_abs_test_0, sub_0, re_re)
                Core.multiply(mul_conj_abs_test_1, sub_1, im_im)
                Core.multiply(im_im, Scalar(-1.0), im_im) // negative
                val re_im = Mat()
                val im_re = Mat()
                Core.multiply(mul_conj_abs_test_0, sub_1, re_im)
                Core.multiply(mul_conj_abs_test_1, sub_0, im_re)
                Core.add(re_re, im_im, multest_0)
                Core.add(re_im, im_re, multest_1)
                val divtest = Mat()
                Core.pow(absoluteObjfcrop, 2.0, divtest)
                Core.add(divtest, Scalar(1.0), divtest)
                val addtest_0 = Mat()
                val addtest_1 = Mat()
                Core.multiply(multest_0, reconstructInputData.getMat("Ps"), addtest_0)
                Core.multiply(multest_1, reconstructInputData.getMat("Ps"), addtest_1)
                Core.divide(addtest_0, Scalar(myMax), addtest_0)
                Core.divide(addtest_1, Scalar(myMax), addtest_1)
                Core.divide(addtest_0, divtest, addtest_0)
                Core.divide(addtest_1, divtest, addtest_1)

                // PupTest = myAddComplex(PupCopy, finalAdd);
                Core.add(pup[0], addtest_0, pup[0])
                Core.add(pup[1], addtest_1, pup[1])
                val objfcropcopy = arrayOfNulls<Mat>(2)
                objfcropcopy[0] = Mat(objF[0], Rect(
                        cropR[ii, 2][0].toInt(),
                        cropR[ii, 0][0].toInt(),
                        cropR[ii, 1][0].toInt() + 1 - cropR[ii, 0][0].toInt(),
                        cropR[ii, 3][0].toInt() + 1 - cropR[ii, 2][0].toInt()))
                objfcropcopy[1] = Mat(objF[1], Rect(
                        cropR[ii, 2][0].toInt(),
                        cropR[ii, 0][0].toInt(),
                        cropR[ii, 1][0].toInt() + 1 - cropR[ii, 0][0].toInt(),
                        cropR[ii, 3][0].toInt() + 1 - cropR[ii, 2][0].toInt()))
                val pupabsolute = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                val pupabsolute_0 = Mat()
                val pupabsolute_1 = Mat()
                Core.pow(pup[0], 2.0, pupabsolute_0)
                Core.pow(pup[1], 2.0, pupabsolute_1)
                Core.add(pupabsolute_0, pupabsolute_1, pupabsolute)
                Core.sqrt(pupabsolute, pupabsolute)
                val mul1 = arrayOfNulls<Mat>(2)
                mul1[0] = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                mul1[1] = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                Core.multiply(pup[0], pupabsolute, mul1[0])
                Core.multiply(pup[1], Scalar(-1.0), mul1[1])
                Core.multiply(mul1[1], pupabsolute, mul1[1])
                val sub = arrayOfNulls<Mat>(2)
                sub[0] = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                sub[1] = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                Core.subtract(objFUp[0], objFCropP[0], sub[0])
                Core.subtract(objFUp[1], objFCropP[1], sub[1])
                val mul2 = arrayOfNulls<Mat>(2)
                mul2[0] = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                mul2[1] = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                val re_re_2 = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                val im_im_2 = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                val re_im_2 = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                val im_re_2 = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                Core.multiply(mul1[0], sub[0], re_re_2)
                Core.multiply(mul1[1], sub[1], im_im_2)
                Core.multiply(mul1[0], sub[1], re_im_2)
                Core.multiply(mul1[1], sub[0], im_re_2)
                Core.multiply(im_im_2, Scalar(-1.0), im_im_2) // j*j=-1
                Core.add(re_re_2, im_im_2, mul2[0])
                Core.add(re_im_2, im_re_2, mul2[1])
                val myMax2 = Core.minMaxLoc(pupabsolute).maxVal
                val div1 = arrayOfNulls<Mat>(2)
                div1[0] = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                div1[1] = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                Core.divide(mul2[0], Scalar(myMax2), div1[0])
                Core.divide(mul2[1], Scalar(myMax2), div1[1])
                val abs_pup = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                val abs_0 = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                val abs_1 = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                Core.pow(pup[0], 2.0, abs_0)
                Core.pow(pup[1], 2.0, abs_1)
                Core.add(abs_0, abs_1, abs_pup) // absolute if sqrt..
                Core.add(abs_pup, Scalar(OPAlpha), abs_pup)
                val div2 = arrayOfNulls<Mat>(2)
                div2[0] = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                div2[1] = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                Core.divide(div1[0], abs_pup, div2[0])
                Core.divide(div1[1], abs_pup, div2[1])
                val result = arrayOfNulls<Mat>(2)
                result[0] = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                result[1] = Mat(pup[0]!!.rows(), pup[0]!!.cols(), CvType.CV_32F)
                Core.add(objfcropcopy[0], div2[0], result[0])
                Core.add(objfcropcopy[1], div2[1], result[1])
                val ObjF_roi_0 = Mat(objF[0], Rect(
                        cropR[ii, 2][0].toInt(),
                        cropR[ii, 0][0].toInt(),
                        cropR[ii, 1][0].toInt() + 1 - cropR[ii, 0][0].toInt(),
                        cropR[ii, 3][0].toInt() + 1 - cropR[ii, 2][0].toInt()))
                val ObjF_roi_1 = Mat(objF[1], Rect(
                        cropR[ii, 2][0].toInt(),
                        cropR[ii, 0][0].toInt(),
                        cropR[ii, 1][0].toInt() + 1 - cropR[ii, 0][0].toInt(),
                        cropR[ii, 3][0].toInt() + 1 - cropR[ii, 2][0].toInt()))
                result[0]!!.copyTo(ObjF_roi_0)
                result[1]!!.copyTo(ObjF_roi_1)

                // complex multiplication...
                val res_pup_mul_complex = arrayOfNulls<Mat>(2)
                res_pup_mul_complex[0] = Mat()
                res_pup_mul_complex[1] = Mat()
                val re_re_3 = Mat()
                val im_im_3 = Mat()
                Core.multiply(result[0], pup[0], re_re_3)
                Core.multiply(result[1], pup[1], im_im_3)
                Core.multiply(im_im_3, Scalar(-1.0), im_im_3) // negative
                val re_im_3 = Mat()
                val im_re_3 = Mat()
                Core.multiply(result[0], pup[1], re_im_3)
                Core.multiply(result[1], pup[0], im_re_3)
                Core.add(re_re_3, im_im_3, res_pup_mul_complex[0])
                Core.add(re_im_3, im_re_3, res_pup_mul_complex[1])
                val idfted = fourierTransformer.ifftShiftComplex(fourierTransformer.ifftShift(res_pup_mul_complex))
                objCrop[ii] = idfted

//                if (index == parallelsize-1 && ii % 10 == 0) Runtime.getRuntime().gc() // garbage collecter
                if (ii % 10 == 0) Runtime.getRuntime().gc() // garbage collecter
            }

            val t = System.currentTimeMillis()
            Log.d("elapsed:", (t - startTime).toString() + "")

            objRComplex[index] = fourierTransformer.ifftShiftComplex(fourierTransformer.ifftShift(objF))
            Runtime.getRuntime().gc() // garbage collector
            Log.e("FPMFUNC", "|     $iteration'     |    -/-    |   $err2   |           |\n")
        }
        Runtime.getRuntime().gc() // garbage collector

        // results into displayable objects
        val tmp_ObjAmp = Mat()
        val tmp_ObjAmp_0 = Mat()
        val tmp_ObjAmp_1 = Mat()
        Core.pow(objRComplex[index][0], 2.0, tmp_ObjAmp_0)
        Core.pow(objRComplex[index][1], 2.0, tmp_ObjAmp_1)
        Core.add(tmp_ObjAmp_0, tmp_ObjAmp_1, tmp_ObjAmp)
        Core.sqrt(tmp_ObjAmp, tmp_ObjAmp)
        ampResults[index] = tmp_ObjAmp.clone()
        var ampResultauto = tmp_ObjAmp.clone()
        if(index == 0)
        {
            resObjPhase = myAngleComplex(objRComplex[index]) // if green
            phaseResult = resObjPhase
        }
        var phaseResultauto = myAngleComplex(objRComplex[index]) // if green
        saveImageauto(ampResultauto, phaseResultauto, crop_x, crop_y)
    }

    override fun myAngleComplex(mats: Array<Mat?>): Mat {
        /** this function transfers object into its phase **/
        val re = mats[0]
        val im = mats[1]
        val rows = re!!.rows()
        val cols = re.cols()
        val result = Mat(rows, cols, CvType.CV_32F)
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                val tanValue = im!![i, j][0] / re[i, j][0]
                val angle = atan(tanValue)
                result.put(i, j, angle)
            }
        }
        return result
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.fade_out)
    }

    fun toHome() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(0, R.anim.fade_out)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (fullTask != null && !fullTask!!.isDisposed) fullTask!!.dispose()
    }
}