package com.lukael.oled_fpm.activity.capture

import com.lukael.oled_fpm.activity.illumination.captureoption.CaptureMode
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class CaptureResultViewModel(
    handle: SavedStateHandle
): ViewModel() {
    val cacheFile1LiveData: LiveData<String> get() = _cacheFile1LiveData
    private val _cacheFile1LiveData = handle.getLiveData<String>(CaptureResultActivity.FILE_PATH_1)

    val cacheFile2LiveData: LiveData<String> get() = _cacheFile2LiveData
    private val _cacheFile2LiveData = handle.getLiveData<String>(CaptureResultActivity.FILE_PATH_2)

    val captureMode = handle.get<CaptureMode>(CaptureResultActivity.CAPTURE_MODE)
}