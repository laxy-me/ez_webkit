package com.eztd.arm.dialog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.eztd.arm.R

class ImageSelectController(private val mContext: Context) :
    AppDialog.BaseCustomViewController() {
    private var onGetPhotoClickListener: OnGetPhotoClickListener? = null
    fun setOnGetPhotoClickListener(l: OnGetPhotoClickListener) {
        onGetPhotoClickListener = l
    }

    interface OnGetPhotoClickListener {
        fun takePhoto(dialog: AppDialog)
        fun takeFromGallery(dialog: AppDialog)
    }

    override fun onCreateView(): View {
        val view = LayoutInflater.from(mContext).inflate(R.layout.dialog_upload, null)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return view
    }

    override fun onInitView(view: View, dialog: AppDialog) {
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
