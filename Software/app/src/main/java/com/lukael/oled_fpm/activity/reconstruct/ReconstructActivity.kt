package com.lukael.oled_fpm.activity.reconstruct

import com.lukael.oled_fpm.model.HashmapData
import com.lukael.oled_fpm.model.SystemOpts
import org.opencv.core.Mat

interface ReconstructActivity {
    fun getPassedInfo()
    fun initUI()
    fun initSettings()
    fun countNImg()
    fun startFullTask()
    fun changeProgressUI(progress: Int)
    fun showResult()
    fun saveImage()
    fun showBefore()
    fun showAfter()
    fun preProcess(iRGB: Int): HashmapData
    fun configureReconstruct(preprocessedData: HashmapData): HashmapData
    fun defineSystem(defineSystemInput: HashmapData): SystemOpts
    fun fpmFunc(reconstructInputData: HashmapData?)
    fun myAngleComplex(mats: Array<Mat?>): Mat
}