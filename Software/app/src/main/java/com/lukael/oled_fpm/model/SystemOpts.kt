package com.lukael.oled_fpm.model

import org.opencv.core.Mat

data class SystemOpts(
        val nObj: IntArray,
        val wNA: Mat) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return true
    }

    override fun hashCode(): Int {
        var result = nObj.contentHashCode()
        result = 31 * result + wNA.hashCode()
        return result
    }
}