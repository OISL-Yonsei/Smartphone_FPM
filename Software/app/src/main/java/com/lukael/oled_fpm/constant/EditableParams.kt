package com.lukael.oled_fpm.constant

import org.opencv.core.Scalar

/** system_parameter.py **/
val fileTypes = arrayOf("r_", "g_", "b_")
val wavelengths = listOf(0.518, 0.4764, 0.6244) // first one is used for mono
val wavelengths_color = listOf(0.6244, 0.518, 0.4764) // first one is used for mono

const val NA = 0.0895 // 0.10
const val mag = 1.56 // 3.3
const val dPixC = 1.14 // 2.4
const val dsLed = 0.0486*35e3 // 4e3
const val zLed = 24e3 // 66e3

const val n1 = 2448 // not used
const val n2 = 3264 // not used

const val Np = 200

val nStartGiven = listOf(-1,-1) // user pick, or (1210,1570)
val nCent = listOf(1224,1632)

/** combined_main.py **/
//val exceptNum = listOf(22,23,24,25,27,28,33,34)
 val exceptNum = listOf<Int>()

const val z0 = 0 // Defocus amount(um), not used

const val ibkThr = 4 // Background threshold
const val nUsed = 0 // not used (always 0... if not, need correction)
const val ffl = 9820
// opts
const val maxIter = 10
// below are not used...
const val tol = 1
const val minIter = 10
const val monotone = 1 // classified by file name
const val iters = 1
const val propMode = 0
const val OPAlpha = 10.0
const val OPBeta = 1
const val calbrateTol = 1e-1
const val saveIterResult = false
