package com.lukael.oled_fpm.util

import android.util.Log
class LineDebugger(startTime: Long) {
    var prevTime = startTime // initialize

    private fun getLineNumber(): Int = Thread.currentThread().stackTrace[4].lineNumber

    fun timeFactorTest() { // for debugging
        val curTime = System.currentTimeMillis()
        Log.d("LINE CHECKING", "time elapsed until line: " + getLineNumber() + ": " + (curTime - prevTime))
        prevTime = curTime
    }
}