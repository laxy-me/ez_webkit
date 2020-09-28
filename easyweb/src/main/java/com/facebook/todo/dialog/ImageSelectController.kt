package com.facebook.todo.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.Keep
import com.facebook.todo.R

@Keep
class ImageSelectController(private val mContext: Context) :
    SmartDialog.BaseCustomViewController() {
    private var onGetPhotoClickListener: OnGetPhotoClickListener? = null
    fun setOnGetPhotoClickListener(l: OnGetPhotoClickListener) {
        onGetPhotoClickListener = l
    }

    @Keep
    interface OnGetPhotoClickListener {
        fun takePhoto(dialog: SmartDialog)
        fun takeFromGallery(dialog: SmartDialog)
    }

    override fun onCreateView(): View {
        val view = LayoutInflater.from(mContext).inflate(R.layout.dialog_upload_image, null)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return view
    }

    override fun onInitView(view: View, dialog: SmartDialog) {
        view.findViewById<TextView>(R.id.takePhoneFromGallery).setOnClickListener {
            onGetPhotoClickListener?.takeFromGallery(dialog)
            if (onGetPhotoClickListener == null) {
                dialog.dismiss()
            }
        }
        view.findViewById<TextView>(R.id.takePhoneFromCamera).setOnClickListener {
            onGetPhotoClickListener?.takePhoto(dialog)
            if (onGetPhotoClickListener == null) {
                dialog.dismiss()
            }
        }
        view.findViewById<TextView>(R.id.takePhoneCancel).setOnClickListener { dialog.dismiss() }
    }
}
