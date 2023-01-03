package com.lukael.oled_fpm.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class FileSaver {
    fun saveBmpImage(context: Context, bmp: Bitmap, filename: String) {
        var out: FileOutputStream? = null
        val sd = File(Environment.getExternalStorageDirectory().toString() + "/Pictures/FPM_RESULT")
        var success = true
        if (!sd.exists()) {
            success = sd.mkdir()
        }
        if (success) {
            val dest = File(sd, filename)
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
//
//    fun saveTempImage(context: Context, tempFile: String, filename: String) {
//        val inputStream = FileInputStream(tempFile)
//        var outputStream: FileOutputStream? = null
//        try {
//            val sd = File(Environment.getExternalStorageDirectory().toString() + "/Pictures/FPM_RESULT")
//            var success = true
//            if (!sd.exists()) {
//                success = sd.mkdir()
//            }
//            if (success) {
//                val dest = File(sd, filename)
//                outputStream = FileOutputStream(dest)
//                // Transfer bytes from in to out
//                // Transfer bytes from in to out
//                val buf = ByteArray(1024)
//                var len: Int
//                while (inputStream.read(buf).also { len = it } > 0) {
//                    outputStream.write(buf, 0, len)
//                }
//
//                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(dest))
//                mediaScanIntent.data = Uri.fromFile(dest)
//                context.sendBroadcast(mediaScanIntent)
//                Toast.makeText(context, "file saved", Toast.LENGTH_SHORT).show()
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        } finally {
//            inputStream.close()
//            outputStream?.close()
//        }
//    }
}