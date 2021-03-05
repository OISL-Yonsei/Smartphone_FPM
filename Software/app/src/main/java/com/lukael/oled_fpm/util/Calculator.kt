package com.lukael.oled_fpm.util

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import java.util.*
import kotlin.math.ceil

class Calculator {
    fun argSort(a: DoubleArray, ascending: Boolean): Array<Int?> {
        val indexes = arrayOfNulls<Int>(a.size)
        for (i in indexes.indices) indexes[i] = i
        Arrays.sort(indexes) { i1: Int?, i2: Int? -> (if (ascending) 1 else -1) * a[i1!!].compareTo(a[i2!!]) }
        return indexes
    }

    fun myRange(a: Int): Mat {
        val result = Mat(1,a,CvType.CV_32F)
        for (i in 0 until a) result.put(0,i, i.toDouble())
        return result
    }

    fun myArrange(a: Double, b: Double): Mat {
        val count = ceil(b - a).toInt()
        val result = Mat(1, count, CvType.CV_32F)
        for (i in 0 until count) {
            result.put(0,i, a + i)
        }
        return result
    }

    fun myArrange(a: Double, b: Double, step: Double): Mat {
        val count = ceil((b - a) / step).toInt()
        val result = Mat(1, count, CvType.CV_32F)
        for (i in 0 until count) {
            result.put(0,i, a + i * step)
        }
        return result
    }

    fun myMul(a: DoubleArray, scale: Double): DoubleArray {
        for (i in a.indices) a[i] *= scale
        return a
    }

    fun myMeshGrid(a: Mat, b: Mat): Array<Mat?> {
        val results = arrayOfNulls<Mat>(2)
        val res1 = Mat(b.cols(), a.cols(), CvType.CV_32F)
        val res2 = Mat(b.cols(), a.cols(), CvType.CV_32F)
        // res1에서는 j줄에 j를 집어넣고, res2에서는 i줄에 i를 집어넣기.
        for (j in 0 until a.cols()) { res1.col(j).setTo(Scalar(a.get(0,j)[0])) }
        for (i in 0 until b.cols()) { res2.row(i).setTo(Scalar(b.get(0,i)[0])) }
        results[0] = res1
        results[1] = res2
        return results
    }

    fun maxValue(doubles: DoubleArray): Double {
        var max = doubles[0]
        for (aDouble in doubles) {
            if (aDouble > max) max = aDouble
        }
        return max
    }
}