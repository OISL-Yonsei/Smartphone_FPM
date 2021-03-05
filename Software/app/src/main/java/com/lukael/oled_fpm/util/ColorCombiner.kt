package com.lukael.oled_fpm.util

import com.lukael.oled_fpm.constant.THRESHOLD_PERCENT
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.resize
import java.util.*
import kotlin.math.ceil

class ColorCombiner {
    fun combineBGR(rgb_inputs: Array<Mat?>): Mat {
        val redImg = rgb_inputs[0]
        val greenImg = rgb_inputs[1]
        val blueImg = rgb_inputs[2]

        val m = blueImg!!.rows()
        val n = blueImg!!.cols()
        val scaleSize = Size(m.toDouble(),n.toDouble())

        if(redImg!!.rows() < m)
            resize(redImg, redImg   , scaleSize, 0.0, 0.0, Imgproc.INTER_CUBIC)
        if(greenImg!!.rows() < m)
            resize(greenImg, greenImg, scaleSize, 0.0, 0.0, Imgproc.INTER_CUBIC)

//        val m = redImg!!.rows()
//        val n = redImg.cols()

        //Log.d("redImg[72,94]", redImg!!.get(72, 94)[0].toString() + "")
        //Log.d("greenImg[72,94]", greenImg!!.get(72, 94)[0].toString() + "")
        //Log.d("blueImg[72,94]", blueImg!!.get(72,94)[0].toString() + "")

        val sortedRed = redImg.reshape(1, 1).clone()
        val sortedGreen = greenImg!!.reshape(1, 1).clone() // clone...
        val sortedBlue = blueImg!!.reshape(1, 1).clone()

        Core.sort(sortedRed, sortedRed, Core.SORT_ASCENDING)
        Core.sort(sortedGreen, sortedGreen, Core.SORT_ASCENDING)
        Core.sort(sortedBlue, sortedBlue, Core.SORT_ASCENDING)

        val redThreshold = sortedRed[0, ceil(THRESHOLD_PERCENT * m * n).toInt()][0]
        val greenThreshold = sortedGreen[0, ceil(THRESHOLD_PERCENT * m * n).toInt()][0]
        val blueThreshold = sortedBlue[0, ceil(THRESHOLD_PERCENT * m * n).toInt()][0]

        //Log.d("THRESHOLDS", "$redThreshold,$greenThreshold,$blueThreshold")

        // images upon red threshold
        val redCutUp = Mat()
        val greenCutUp = Mat()
        val blueCutUp = Mat()
        val redCutDown = Mat()
        val greenCutDown = Mat()
        val blueCutDown = Mat()

        Imgproc.threshold(redImg, redCutUp, redThreshold, 1.0, Imgproc.THRESH_BINARY)
        Imgproc.threshold(greenImg, greenCutUp, greenThreshold, 1.0, Imgproc.THRESH_BINARY)
        Imgproc.threshold(blueImg, blueCutUp, blueThreshold, 1.0, Imgproc.THRESH_BINARY)
        Imgproc.threshold(redImg, redCutDown, redThreshold, 1.0, Imgproc.THRESH_BINARY_INV)
        Imgproc.threshold(greenImg, greenCutDown, greenThreshold, 1.0, Imgproc.THRESH_BINARY_INV)
        Imgproc.threshold(blueImg, blueCutDown, blueThreshold, 1.0, Imgproc.THRESH_BINARY_INV)

        // normImages calculation
        val normRed = Mat()
        val normGreen = Mat()
        val normBlue = Mat()

        //Norm_Red=RedImg*(RedImg<Red_threshold) * Green_threshold/Red_threshold
        // +(RedImg>Red_threshold)*Green_threshold
        Core.multiply(redImg, redCutDown, normRed)
        Core.multiply(normRed, Scalar(greenThreshold / redThreshold), normRed)
        val addToNormRed = Mat()
        Core.multiply(redCutUp, Scalar(greenThreshold), addToNormRed)
        Core.add(normRed, addToNormRed, normRed)

        //Norm_Green=GreenImg*(GreenImg<Green_threshold)
        // +Green_threshold*(GreenImg>Green_threshold)
        Core.multiply(greenImg, greenCutDown, normGreen)
        val addToNormGreen = Mat()
        Core.multiply(greenCutUp, Scalar(greenThreshold), addToNormGreen)
        Core.add(normGreen, addToNormGreen, normGreen)

        //Norm_Blue=BlueImg*(BlueImg<Blue_threshold) * Green_threshold/Blue_threshold
        // +(BlueImg>Blue_threshold)*Green_threshold
        Core.multiply(blueImg, blueCutDown, normBlue)
        Core.multiply(normBlue, Scalar(greenThreshold / blueThreshold), normBlue)
        val addToNormBlue = Mat()
        Core.multiply(blueCutUp, Scalar(greenThreshold), addToNormBlue)
        Core.add(normBlue, addToNormBlue, normBlue)

        // merge three planes (bgr)
        val normRGB = Mat(redImg.rows(), redImg.cols(), CvType.CV_32SC3)
        val planes = ArrayList<Mat>()
        planes.add(normRed)
        planes.add(normGreen)
        planes.add(normBlue)
        Core.merge(planes, normRGB)
        return normRGB
    }
}