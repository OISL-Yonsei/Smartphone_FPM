package com.lukael.oled_fpm.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileSaver {
    fun saveBmpImage(context: Context, bmp: Bitmap, filename: String?) {
        var out: FileOutputStream? = null
        val sd = File(Environment.getExternalStorageDirectory().toString() + "/Pictures/FPM_RESULT")
        var success = true
        if (!sd.exists()) {
            success = sd.mkdir()
        }
        if (success) {
            val dest = File(sd, filename!!)
            try {
                out = FileOutputStream(dest)
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out) // bmp is your Bitmap instance
                // PNG is a loss-less format, the compression factor (100) is ignored
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    if (out != null) {
                        out.close()
                        Log.d("close", "OK!!")
                        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(dest))
                        mediaScanIntent.data = Uri.fromFile(dest)
                        context.sendBroadcast(mediaScanIntent)
                        Toast.makeText(context, "file saved", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: IOException) {
                    Log.d("error", e.message + "Error")
                    e.printStackTrace()
                }
            }
        }
    }
}