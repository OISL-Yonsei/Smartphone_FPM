package com.lukael.oled_fpm.activity.capture

import com.lukael.oled_fpm.activity.illumination.captureoption.CaptureMode
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class CaptureResultViewModel(
    handle: SavedStateHandle
): ViewModel() {
    val captureMode = handle.get<CaptureMode>(CaptureResultActivity.CAPTURE_MODE)!!

    val cacheFilesLiveData: LiveData<List<String?>> get() = _cacheFilesLiveData
    private val _cacheFilesLiveData = MutableLiveData<List<String?>>(
        listOf(
            handle[CaptureResultActivity.FILE_PATH_1],
            handle[CaptureResultActivity.FILE_PATH_2]
        )
    )
}