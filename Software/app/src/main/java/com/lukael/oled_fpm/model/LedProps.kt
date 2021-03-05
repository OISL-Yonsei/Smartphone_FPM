package com.lukael.oled_fpm.model

import com.lukael.oled_fpm.constant.*
import org.opencv.core.CvType
import org.opencv.core.Mat
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.sin
import kotlin.math.asin
import kotlin.math.atan





class LedProps(firstFile: String) {
    private val litCenH = 16
    private val litCenV = 16


    val diaLed = firstFile.substring(firstFile.length - 10, firstFile.length - 8).toInt()
    var nled = 0 // calculated at countNLED()

    val litCoordinate = Array(32) { IntArray(32) }
    val sinThetaV = Array(32) { DoubleArray(32) }
    val sinThetaH = Array(32) { DoubleArray(32) }
    val illuminationNA = Array(32) { DoubleArray(32) }

    fun updateNLED(nImg: Int, imgCenter: DoubleArray) {
        /** total number of LEDs used in the experiment  */

        // corresponding angles for each LEDs
        val vvLed = Mat(32,32, CvType.CV_32F)
        val hhLed =  Mat(32,32, CvType.CV_32F)
        val rrLed =  Mat(32,32, CvType.CV_32F)
        val x0 =  Mat(32,32, CvType.CV_32F) // from rightmost position
        val y0 =  Mat(32,32, CvType.CV_32F) // from topmost position
        val dd =  Mat(32,32, CvType.CV_32F)

        nled = 0
        for (i in 0..31) {
            for (j in 0..31) {
                hhLed.put(i, j, (j + 1 - litCenH).toDouble())
                vvLed.put(i, j, (i + 1 - litCenV).toDouble())
                rrLed.put(i, j, sqrt(vvLed.get(i,j)[0].pow(2.0) + hhLed.get(i,j)[0].pow(2.0)))
                litCoordinate[i][j] = if (rrLed.get(i,j)[0] < diaLed / 2.0) 1 else 0
                // total number of LEDs used in the experiment
                nled += litCoordinate[i][j]
                // index of LEDs used in the experiment

                /**
                 * spacing between neighboring LEDs
                 */
                // 4mm
                x0.put(i, j, hhLed.get(i,j)[0] * dsLed) // from rightmost postion
                y0.put(i, j, vvLed.get(i,j)[0] * dsLed) //from topmost postion# corresponding angles for each LEDs
                dd.put(i, j, sqrt((y0.get(i,j)[0] - imgCenter[0]).pow(2.0) + (x0.get(i,j)[0] - imgCenter[1]).pow(2.0) + zLed.pow(2.0)))
                sinThetaV[i][j] = (y0.get(i,j)[0] - imgCenter[0]) / dd.get(i,j)[0]
                sinThetaV[i][j] = sin(asin(sinThetaV[i][j])-(atan(imgCenter[0]/ffl)))
                sinThetaH[i][j] = (x0.get(i,j)[0] - imgCenter[1]) / dd.get(i,j)[0]
                sinThetaH[i][j] = sin(asin(sinThetaH[i][j])-(atan(imgCenter[1]/ffl)))
                illuminationNA[i][j] = sqrt(sinThetaV[i][j].pow(2.0) + sinThetaH[i][j].pow(2.0))
            }
        }



    }
}