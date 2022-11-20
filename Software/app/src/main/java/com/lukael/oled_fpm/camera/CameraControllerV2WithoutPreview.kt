package com.lukael.oled_fpm.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.RggbChannelVector
import android.hardware.camera2.params.TonemapCurve
import android.media.AudioManager
import android.media.Image
import android.media.ImageReader
import android.media.ToneGenerator
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.lukael.oled_fpm.activity.illumination.captureoption.CaptureMode
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * @author Jaewoo Lukael Jung
 * @email lukael.jung@yonsei.ac.kr
 * @create date 2019-12-23 17:23:42
 * @modify date 2019-12-23 17:23:42
 * @desc Camera control code
 * @ref https://zatackcoder.com/android-camera-2-api-example-without-preview/
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class CameraControllerV2WithoutPreview(private val context: Context,
                                       private val imgCnt: Int,
                                       private val diaLed: Int) {
    private var mCameraId: String? = null
    private var mCaptureSession: CameraCaptureSession? = null
    private var mCameraDevice: CameraDevice? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var imageReader: ImageReader? = null
    private var file: File? = null

    private val exposure: Long = 0
    private var timeStamp: String? = null
    private val mCameraOpenCloseLock = Semaphore(1)

    fun openCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        setUpCameraOutputs()
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            startBackgroundThread()
            Log.e(TAG, "THREAD STARTED!")
            manager.openCamera(mCameraId!!, mStateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    private fun setUpCameraOutputs() {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)

                // front camera setting
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    continue
                }
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        ?: continue

                // For still image captures, we use the largest available size.
                val largest = Collections.max(Arrays.asList(*map.getOutputSizes(ImageFormat.JPEG)), CompareSizesByArea())
                imageReader = ImageReader.newInstance(largest.width, largest.height, ImageFormat.JPEG,  /*maxImages*/2)
                imageReader!!.setOnImageAvailableListener(mOnImageAvailableListener, backgroundHandler)
                Log.d("image available listener made", "made!")
                mCameraId = cameraId
                return
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        } catch (e: NullPointerException) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            e.printStackTrace()
        }
    }

    private val mStateCallback: CameraDevice.StateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(cameraDevice: CameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release()
            mCameraDevice = cameraDevice
            createCameraCaptureSession()
        }

        override fun onDisconnected(cameraDevice: CameraDevice) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
        }

        override fun onError(cameraDevice: CameraDevice, error: Int) {
            mCameraOpenCloseLock.release()
            cameraDevice.close()
            mCameraDevice = null
        }
    }

    private val mOnImageAvailableListener: ImageReader.OnImageAvailableListener = object : ImageReader.OnImageAvailableListener {
        private var curImgCnt = 0
        override fun onImageAvailable(reader: ImageReader) {
            // test beep
            val toneGen1 = ToneGenerator(AudioManager.STREAM_MUSIC, 50)
            toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP, 20)
            Log.d(TAG, "ImageAvailable")
            //startBackgroundThread();
            Log.d("current image count", (curImgCnt + 1).toString() + "/" + imgCnt.toString())
            Log.d("available save file", file!!.name)
            try {
                backgroundHandler!!.post(ImageSaver(reader.acquireNextImage(), file))
                //Thread.sleep(500);// take picture
            } catch (e: Exception) {
                e.printStackTrace()
            }
            //stopBackgroundThread();
            curImgCnt++
            if (curImgCnt == imgCnt) {
                Log.d(TAG, "Image count reached")
                try {
                    Thread.sleep(3000) // wait for last image to be saved
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                stopBackgroundThread()
            }
        }
    }

    fun closeCamera() {
        try {
            mCameraOpenCloseLock.acquire()
            if (null != mCaptureSession) {
                mCaptureSession!!.close()
                mCaptureSession = null
            }
            if (null != mCameraDevice) {
                mCameraDevice!!.close()
                mCameraDevice = null
            }
            if (null != imageReader) {
                imageReader!!.close()
                imageReader = null
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            mCameraOpenCloseLock.release()
        }
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground")
        backgroundThread!!.start()
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        //try{Thread.sleep(100000);}catch(Exception e){e.printStackTrace();}
        backgroundThread!!.quitSafely()
        try {
            backgroundThread!!.join()
            backgroundThread = null
            backgroundHandler = null
            Log.e(TAG, "CLOSED!")
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun createCameraCaptureSession() {
        try {

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice!!.createCaptureSession(Arrays.asList(imageReader!!.surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession
                        }

                        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                            Log.d(TAG, "Configuration Failed")
                        }
                    }, null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun takePicture(cnt: Int, exp: Long, captureMode: CaptureMode, rgbIter: Int): File? {
        file = getOutputMediaFile(cnt, captureMode, rgbIter)
        try {
            if (null == mCameraDevice) {
                // return null;
            }

            // This is the CaptureRequest.Builder that we use to take a picture.
            val captureBuilder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReader!!.surface)

            captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF)
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
            // white balance part
            captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF) // auto off
            captureBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, CaptureRequest.CONTROL_EFFECT_MODE_OFF)
            captureBuilder.set(CaptureRequest.CONTROL_POST_RAW_SENSITIVITY_BOOST, 100)
            captureBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, CaptureRequest.CONTROL_SCENE_MODE_DISABLED)
            captureBuilder.set(CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE,CaptureRequest.COLOR_CORRECTION_ABERRATION_MODE_OFF) // white balance
            captureBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX) // mode set
            captureBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, computeTemperature()) // white balance
            captureBuilder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, transformer()) // white balance
            // KC
            captureBuilder.set(CaptureRequest.BLACK_LEVEL_LOCK, false) // white balance
            captureBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE,CaptureRequest.CONTROL_EFFECT_MODE_OFF)
            captureBuilder.set(CaptureRequest.DISTORTION_CORRECTION_MODE,CaptureRequest.DISTORTION_CORRECTION_MODE_OFF)
            captureBuilder.set(CaptureRequest.EDGE_MODE,CaptureRequest.EDGE_MODE_OFF)
            captureBuilder.set(CaptureRequest.HOT_PIXEL_MODE,CaptureRequest.HOT_PIXEL_MODE_OFF)
            captureBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF)
            captureBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,CaptureRequest.NOISE_REDUCTION_MODE_MINIMAL)
            captureBuilder.set(CaptureRequest.SHADING_MODE,CaptureRequest.SHADING_MODE_OFF)
            captureBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,CaptureRequest.STATISTICS_FACE_DETECT_MODE_OFF)
            captureBuilder.set(CaptureRequest.TONEMAP_MODE,CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE)
            captureBuilder.set(CaptureRequest.TONEMAP_CURVE,toneCurve())
            // exposure part
            captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, 600) // 695, 300:a50
            captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exp*1000)
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, 100.toByte())

            val CaptureCallback: CameraCaptureSession.CaptureCallback = object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                    Log.e("CAPTURED", "captured")
//                    Log.e("RESULT", result.partialResults[0].toString() + "")
                }
            }
            mCaptureSession!!.stopRepeating()
            mCaptureSession!!.capture(captureBuilder.build(), CaptureCallback, null)
            Log.e(TAG, "capturing...$cnt") // debug
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return file
    }

    private class ImageSaver(
            /**
             * The JPEG image
             */
            private val mImage: Image,
            /**
             * The file we save the image into.
             */
            private val mFile: File?) : Runnable {

        override fun run() {
            val buffer = mImage.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())

            // 가로채?? ?�버�? 보내?? 코드
            buffer[bytes]
            mImage.close()

            // ?�??
            try {
                // file
                val output = FileOutputStream(mFile!!)
                output.write(bytes)
                output.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    }

    internal class CompareSizesByArea : Comparator<Size?> {
        override fun compare(lhs: Size?, rhs: Size?): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(lhs?.width!!.toLong() * lhs.height - rhs!!.width.toLong() * rhs.height)
        }
    }

    private fun getOutputMediaFile(cnt: Int, captureMode: CaptureMode, rgbIter: Int): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "OLED_FPM")

        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        val mediaFile: File
        // Create a media file name
        if (cnt == 0) {
            timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        }
        mediaFile = if (captureMode == CaptureMode.MonoReconstruction) { // TODO
            File(
                    mediaStorageDir.path + File.separator + "IMG_mono_" +
                            timeStamp + "_" + String.format("%02d",diaLed) + "_" + cnt.toString().padStart(3, '0')  + ".jpg")
        } else {
            val colors = arrayOf("r", "g", "b")
            if (cnt == 0) {
                File(
                        mediaStorageDir.path + File.separator + "IMG_rgb_" +
                                timeStamp + "_" + String.format("%02d",diaLed) + "_" + cnt.toString().padStart(3, '0') + ".jpg")
            } else {
                File(
                        mediaStorageDir.path + File.separator + "IMG_rgb_" +
                                timeStamp + "_" + String.format("%02d",diaLed) + "_" + colors[rgbIter] + "_" + cnt.toString().padStart(3, '0') + ".jpg")
            }
        }
        return mediaFile
    }

    companion object {
        private const val TAG = "CCV2WithoutPreview"
        private fun computeTemperature(): RggbChannelVector // use factor to get rggb
        {
//            return new RggbChannelVector(0.635f + (0.0208333f * factor), 1.0f, 1.0f, 3.7420394f + (-0.0287829f * factor));
            return RggbChannelVector(1.0f, 1.0f, 1.0f, 1.0f)
        }

        private fun transformer(): ColorSpaceTransform {
            val elementsArray = intArrayOf(1, 1, 0, 1, 0, 1,
                    0, 1, 1, 1, 0, 1,
                    0, 1, 0, 1, 1, 1)

            return ColorSpaceTransform(elementsArray)
        }

        private fun toneCurve(): TonemapCurve {
            val curveRed = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)
            val curveGreen = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)
            val curveBlue = floatArrayOf(0.0f, 0.0f, 1.0f, 1.0f)

            return TonemapCurve(curveRed, curveGreen, curveBlue)
        }
    }

    init {
        Log.e("EXPOSURE_LONG", exposure.toString())
    }
}