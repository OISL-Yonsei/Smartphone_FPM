package com.lukael.oled_fpm.activity.reconstruct

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.lukael.oled_fpm.R
import com.lukael.oled_fpm.constant.TARGET_BUCKET
import kotlinx.android.synthetic.main.action_bar.*
import kotlinx.android.synthetic.main.activity_selectimageset.*
import kotlinx.android.synthetic.main.item_crop.view.*
import java.io.File
import java.util.*

class SelectImageSetActivity : AppCompatActivity() {
    // references to our images
    var mMetrics: DisplayMetrics? = null
    private var adapter: ImageAdapter? = null
    private var imageCursor: Cursor? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selectimageset)
        supportActionBar!!.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar!!.setCustomView(R.layout.action_bar)

        initUI()
    }

    private fun initUI(){
        // set adapter
        adapter = ImageAdapter(this)
        image_grid_view.adapter = adapter
        image_grid_view.onItemClickListener = AdapterView.OnItemClickListener {
            _, _, arg2, _ -> adapter!!.callImageViewer(arg2) }
        mMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(mMetrics)

        back_button.setOnClickListener{ finish() }
    }

    // image adapter
    inner class ImageAdapter (private val mContext: Context) : BaseAdapter() {
        private val thumbsDataList: ArrayList<String> = ArrayList() // filename
        private val thumbsIDList: ArrayList<String> = ArrayList() // id
        init { getThumbInfo(thumbsIDList, thumbsDataList) }

        // view to hold images
        inner class CustomView(context: Context) : ConstraintLayout(context) {
            private fun init(context: Context) { View.inflate(context, R.layout.item_crop, this) }
            init { init(context) }
        }

        fun callImageViewer(selectedIndex: Int) {
            // get file info
            val fullFilepath = getImageInfo(thumbsIDList[selectedIndex])
            // Transfer to next activity
            val intent = Intent(applicationContext, ConfirmActivity::class.java)
            intent.putExtra(ConfirmActivity.FILE_PATH, fullFilepath)
            startActivity(intent)
            overridePendingTransition(0, R.anim.fade_out)
            finish()
        }

        override fun getCount() = thumbsIDList.size
        override fun getItem(position: Int) = position
        override fun getItemId(position: Int) = position.toLong()

        // create a new ImageView for each item referenced by the Adapter
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val rowWidth = mMetrics!!.widthPixels / 3
            val resultView: CustomView
            if (convertView == null) {
                resultView = CustomView(mContext)
                resultView.item_frame.layoutParams = ConstraintLayout.LayoutParams(rowWidth, rowWidth)
                resultView.item_frame.setPadding(0, 0, 0, 0)
            } else {
                resultView = convertView as CustomView
            }
            // put image into the view
            Glide.with(mContext).load(thumbsDataList[position]).centerCrop().into(resultView.crop_image) // thumbnail(0.5f) 뺐음
            val fileName = File(thumbsDataList[position]).name
            val reconstructType = if (fileName.contains("rgb")) "rgb" else "mono" //true
            resultView.name_image.text = reconstructType
            return resultView
        }

        // load thumbnails in given folder (written in Constants)
        private fun getThumbInfo(thumbsIDs: ArrayList<String>, thumbsDatas: ArrayList<String>) {
            val proj = arrayOf(MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,  // folder
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.SIZE)
            imageCursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    proj, null, null,
                    MediaStore.Images.Media._ID + " ASC") // 최신순으로 가져오기
            if (imageCursor != null && imageCursor!!.moveToFirst()) {
                var thumbsID: String
                var thumbsImageID: String?
                var thumbsData: String
                var bucketName: String
                val thumbsIDCol = imageCursor!!.getColumnIndex(MediaStore.Images.Media._ID)
                val thumbsDataCol = imageCursor!!.getColumnIndex(MediaStore.Images.Media.DATA)
                val thumbsImageIDCol = imageCursor!!.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val bucketNameCol = imageCursor!!.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                do {
                    thumbsID = imageCursor!!.getString(thumbsIDCol)
                    thumbsData = imageCursor!!.getString(thumbsDataCol)
                    thumbsImageID = imageCursor!!.getString(thumbsImageIDCol)
                    bucketName = imageCursor!!.getString(bucketNameCol)
                    val endString = thumbsData.substring(thumbsData.length - 8)
                    val compString = "_000.jpg"
                    if (thumbsImageID != null && bucketName == TARGET_BUCKET && (endString == compString)) {
                        thumbsIDs.add(thumbsID)
                        thumbsDatas.add(thumbsData)
                    }
                } while (imageCursor!!.moveToNext())
            }
            return
        }

        // get image from given thumbnail ID
        private fun getImageInfo(thumbID: String): String? {
            var imageDataPath: String? = null
            val proj = arrayOf(MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DISPLAY_NAME,
                    MediaStore.Images.Media.SIZE)
            imageCursor = managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    proj, "_ID='$thumbID'", null, null)
            if (imageCursor != null && imageCursor!!.moveToFirst()) {
                if (imageCursor!!.count > 0) {
                    val imgData = imageCursor!!.getColumnIndex(MediaStore.Images.Media.DATA)
                    imageDataPath = imageCursor!!.getString(imgData)
                }
            }
            return imageDataPath
        }
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.fade_out)
        imageCursor!!.close()
    }
}