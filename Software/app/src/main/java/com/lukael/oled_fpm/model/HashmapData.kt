package com.lukael.oled_fpm.model

import org.opencv.core.Mat
import java.util.*

// data used for reconstruct

class HashmapData {
    private val strings = HashMap<String, String>()
    private val string1DArrays = HashMap<String, Array<String?>>()
    private val integers = HashMap<String, Int>()
    private val doubles = HashMap<String, Double>()
    private val longs = HashMap<String, Long>()
    private val double1DArrays = HashMap<String, DoubleArray>()
    private val double2DArrays = HashMap<String, Array<DoubleArray>>()
    private val int1DArrays = HashMap<String, IntArray>()
    private val mat1DArrays = HashMap<String, Array<Mat?>>()
    private val mats = HashMap<String, Mat>()

    fun putString(key: String, value: String) { strings[key] = value }
    fun putInt(key: String, value: Int) { integers[key] = value }
    fun putDouble(key: String, value: Double) { doubles[key] = value }
    fun putLong(key: String, value: Long) { longs[key] = value }
    fun putDouble1DArray(key: String, value: DoubleArray) { double1DArrays[key] = value }
    fun putDouble2DArray(key: String, value: Array<DoubleArray>) { double2DArrays[key] = value }
    fun putInt1DArray(key: String, value: IntArray) { int1DArrays[key] = value }
    fun putMat1DArray(key: String, value: Array<Mat?>) { mat1DArrays[key] = value }
    fun putString1DArray(key: String, value: Array<String?>) { string1DArrays[key] = value }
    fun putMat(key: String, value: Mat) { mats[key] = value }

    fun getString(key: String?): String? { return strings[key] }
    fun getInt(key: String): Int? = integers[key]
    fun getDouble(key: String?): Double? = doubles[key]
    fun getLong(key: String?): Long? = longs[key]
    fun getDouble1DArray(key: String?): DoubleArray? = double1DArrays[key]
    fun getDouble2DArray(key: String?): Array<DoubleArray>? = double2DArrays[key]
    fun getString1DArray(key: String?): Array<String?>? = string1DArrays[key]
    fun getInt1DArray(key: String?): IntArray? = int1DArrays[key]
    fun getMat1DArray(key: String?): Array<Mat?>? = mat1DArrays[key]
    fun getMat(key: String?): Mat? = mats[key]
}