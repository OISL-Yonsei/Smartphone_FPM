package com.lukael.oled_fpm.util

import org.opencv.core.*
import java.util.*

class FourierTransformer {
    fun fftShift(src: Array<Mat?>): Array<Mat?> {
        val dst = arrayOfNulls<Mat>(2)
        dst[0] = src[0]!!.submat(Rect(0, 0, src[0]!!.cols() and -2, src[0]!!.rows() and -2)).clone()
        dst[1] = src[1]!!.submat(Rect(0, 0, src[1]!!.cols() and -2, src[1]!!.rows() and -2)).clone()
        val cx = dst[0]!!.cols() / 2
        val cy = dst[0]!!.rows() / 2
        val q0 = Mat(dst[0], Rect(0, 0, cx, cy)) // Top-Left - Create a ROI per quadrant
        val q1 = Mat(dst[0], Rect(cx, 0, cx, cy)) // Top-Right
        val q2 = Mat(dst[0], Rect(0, cy, cx, cy)) // Bottom-Left
        val q3 = Mat(dst[0], Rect(cx, cy, cx, cy)) // Bottom-Right
        val tmp1 = Mat() // swap quadrants (Top-Left with Bottom-Right)
        q0.copyTo(tmp1)
        q3.copyTo(q0)
        tmp1.copyTo(q3)
        q1.copyTo(tmp1) // swap quadrant (Top-Right with Bottom-Left)
        q2.copyTo(q1)
        tmp1.copyTo(q2)
        val p0 = Mat(dst[1], Rect(0, 0, cx, cy)) // Top-Left - Create a ROI per quadrant
        val p1 = Mat(dst[1], Rect(cx, 0, cx, cy)) // Top-Right
        val p2 = Mat(dst[1], Rect(0, cy, cx, cy)) // Bottom-Left
        val p3 = Mat(dst[1], Rect(cx, cy, cx, cy)) // Bottom-Right
        val tmp2 = Mat() // swap quadrants (Top-Left with Bottom-Right)
        p0.copyTo(tmp2)
        p3.copyTo(p0)
        tmp2.copyTo(p3)
        p1.copyTo(tmp2) // swap quadrant (Top-Right with Bottom-Left)
        p2.copyTo(p1)
        tmp2.copyTo(p2)
        return dst
    }

    fun ifftShift(src: Array<Mat?>): Array<Mat?> {
        val dst = arrayOfNulls<Mat>(2)
        dst[0] = src[0]!!.submat(Rect(0, 0, src[0]!!.cols() and -2, src[0]!!.rows() and -2)).clone()
        dst[1] = src[1]!!.submat(Rect(0, 0, src[1]!!.cols() and -2, src[1]!!.rows() and -2)).clone() // odd rows
        val cx = dst[0]!!.cols() / 2
        val cy = dst[0]!!.rows() / 2
        val q0 = Mat(dst[0], Rect(0, 0, cx, cy)) // Top-Left - Create a ROI per quadrant
        val q1 = Mat(dst[0], Rect(cx, 0, cx, cy)) // Top-Right
        val q2 = Mat(dst[0], Rect(0, cy, cx, cy)) // Bottom-Left
        val q3 = Mat(dst[0], Rect(cx, cy, cx, cy)) // Bottom-Right
        val tmp1 = Mat() // swap quadrants (Top-Left with Bottom-Right)
        q0.copyTo(tmp1)
        q3.copyTo(q0)
        tmp1.copyTo(q3)
        q1.copyTo(tmp1) // swap quadrant (Top-Right with Bottom-Left)
        q2.copyTo(q1)
        tmp1.copyTo(q2)
        val p0 = Mat(dst[1], Rect(0, 0, cx, cy)) // Top-Left - Create a ROI per quadrant
        val p1 = Mat(dst[1], Rect(cx, 0, cx, cy)) // Top-Right
        val p2 = Mat(dst[1], Rect(0, cy, cx, cy)) // Bottom-Left
        val p3 = Mat(dst[1], Rect(cx, cy, cx, cy)) // Bottom-Right
        val tmp2 = Mat() // swap quadrants (Top-Left with Bottom-Right)
        p0.copyTo(tmp2)
        p3.copyTo(p0)
        tmp2.copyTo(p3)
        p1.copyTo(tmp2) // swap quadrant (Top-Right with Bottom-Left)
        p2.copyTo(p1)
        tmp2.copyTo(p2)
        return dst
    }

    fun fftShiftComplex(src: Array<Mat?>): Array<Mat?> { // return complex, simple dft
        val result = arrayOfNulls<Mat>(2)
        val paddedRe = Mat()
        val paddedIm = Mat()
        val m = Core.getOptimalDFTSize(src[0]!!.rows())
        val n = Core.getOptimalDFTSize(src[0]!!.cols())
        Core.copyMakeBorder(src[0], paddedRe, 0, m - src[0]!!.rows(), 0, n - src[0]!!.cols(), Core.BORDER_CONSTANT, Scalar.all(0.0))
        Core.copyMakeBorder(src[1], paddedIm, 0, m - src[1]!!.rows(), 0, n - src[1]!!.cols(), Core.BORDER_CONSTANT, Scalar.all(0.0))
        val planes: MutableList<Mat> = ArrayList()
        paddedRe.convertTo(paddedRe, CvType.CV_32FC1)
        paddedIm.convertTo(paddedIm, CvType.CV_32FC1)
        planes.add(paddedRe)
        planes.add(paddedIm)
        val complexI = Mat()
        Core.merge(planes, complexI) // Add to the expanded another plane with zeros
        Core.dft(complexI, complexI) // this way the result may fit in the source matrix
        // compute the magnitude and switch to logarithmic scale
        // => log(1 + sqrt(Re(DFT(I))^2 + Im(DFT(I))^2))
        Core.split(complexI, planes)
        val re = planes[0] // re
        val im = planes[1] // im
        result[0] = re
        result[1] = im
        return result
    }

    fun ifftShiftComplex(src: Array<Mat?>): Array<Mat?> {
        val result = arrayOfNulls<Mat>(2)
        val planes: List<Mat> = ArrayList()
        val srcArray = ArrayList<Mat>()
        srcArray.add(src[0]!!)
        srcArray.add(src[1]!!)
        val complexI = Mat()
        Core.merge(srcArray, complexI)
        Core.idft(complexI, complexI)
        Core.split(complexI, planes) // planes.get(0) = Re(DFT(I)
        result[0] = Mat()
        result[1] = Mat()
        result[0] = planes[0]
        result[1] = planes[1]
        return result
    }

}