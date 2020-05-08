package com.lax.ezweb.dialog

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog
import java.util.*

/**
 * 统一普通弹框
 */
@Keep
class SmartDialog private constructor(private val mActivity: Activity?) {

    private var mBuilder: AlertDialog.Builder? = null
    private var mAlertDialog: AlertDialog? = null

    private var mTitleText: String? = null
    private var mMessageText: String? = null
    private var mCustomViewController: BaseCustomViewController? = null
    private val mCustomView: View? = null

    private var mWidthScale: Float = 0.toFloat()
    private var mHeightScale: Float = 0.toFloat()
    private var mWindowGravity: Int = 0
    private var mWindowAnim: Int = 0
    private var mSoftInputMode: Int = 0

    private var mOnCancelListener: OnCancelListener? = null
    private var mDismissListener: OnDismissListener? = null

    private var mPositiveId: Int = 0
    private var mNegativeId: Int = 0
    private var mPositiveListener: OnClickListener? = null
    private var mNegativeListener: OnClickListener? = null

    private var mCancelableOnTouchOutside: Boolean = false

    interface OnClickListener {
        fun onClick(dialog: DialogInterface)
    }

    interface OnCancelListener {
        fun onCancel(dialog: DialogInterface)
    }

    interface OnDismissListener {
        fun onDismiss(dialog: DialogInterface)
    }

    init {
        init()
    }

    private fun init() {
        mTitleText = null

        mMessageText = null

        mWidthScale = 0f
        mHeightScale = 0f

        mPositiveId = -1
        mNegativeId = -1
        mPositiveListener = null
        mNegativeListener = null
        mOnCancelListener = null
        mDismissListener = null

        mCancelableOnTouchOutside = true
        mCustomViewController = null

        mWindowGravity = -1
        mWindowAnim = -1
        mSoftInputMode = -1
    }

    private fun scaleDialog() {
        if (mCustomViewController == null) {
            return
        }

        if (mWidthScale == 0f && mHeightScale == 0f) {
            return
        }

        val displayMetrics = DisplayMetrics()
        mActivity!!.windowManager.defaultDisplay.getMetrics(displayMetrics)

        val width = if (mWidthScale == 0f)
            ViewGroup.LayoutParams.MATCH_PARENT
        else
            (displayMetrics.widthPixels * mWidthScale).toInt()
        val height = if (mHeightScale == 0f)
            ViewGroup.LayoutParams.WRAP_CONTENT
        else
            (displayMetrics.heightPixels * mHeightScale).toInt()

        mAlertDialog!!.window!!.setLayout(width, height)
    }

    fun setOnDismissListener(onDismissListener: OnDismissListener): SmartDialog {
        mDismissListener = onDismissListener
        return this
    }

    fun setCustomViewController(customViewController: BaseCustomViewController): SmartDialog {
        mCustomViewController = customViewController
        return this
    }

    fun setPositive(textId: Int, listener: OnClickListener): SmartDialog {
        mPositiveId = textId
        mPositiveListener = listener
        return this
    }

    fun setPositive(textId: Int): SmartDialog {
        mPositiveId = textId
        return this
    }

    fun setWindowAnim(windowAnim: Int): SmartDialog {
        mWindowAnim = windowAnim
        return this
    }

    fun setWindowGravity(windowGravity: Int): SmartDialog {
        mWindowGravity = windowGravity
        return this
    }

    fun setSoftInputMode(softInputMode: Int): SmartDialog {
        mSoftInputMode = softInputMode
        return this
    }

    fun setNegative(textId: Int, listener: OnClickListener): SmartDialog {
        mNegativeId = textId
        mNegativeListener = listener
        return this
    }

    fun setCancelableOnTouchOutside(cancelable: Boolean): SmartDialog {
        mCancelableOnTouchOutside = cancelable
        return this
    }

    fun setCancelListener(cancelListener: OnCancelListener): SmartDialog {
        mOnCancelListener = cancelListener
        return this
    }

    fun setMessage(messageRes: Int): SmartDialog {
        mMessageText = mActivity!!.getText(messageRes).toString()
        return this
    }

    fun setMessage(message: String?): SmartDialog {
        mMessageText = message
        return this
    }

    fun setTitle(titleId: Int): SmartDialog {
        mTitleText = mActivity!!.getText(titleId).toString()
        return this
    }

    fun setTitle(title: String): SmartDialog {
        mTitleText = title
        return this
    }

    fun setWidthScale(widthScale: Float): SmartDialog {
        mWidthScale = widthScale
        return this
    }

    fun setHeightScale(heightScale: Float): SmartDialog {
        mHeightScale = heightScale
        return this
    }

    fun show() {
        if (mBuilder == null) { // solo dialog, the builder is already existed.
            mBuilder = AlertDialog.Builder(mActivity!!)
        }

        if (mAlertDialog != null && mAlertDialog!!.isShowing) {
            mAlertDialog!!.dismiss()
            mAlertDialog = null
        }

        setup()

        if (mAlertDialog != null && !mActivity!!.isFinishing) {
            mAlertDialog!!.show()
            mAlertDialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
            scaleDialog()
        }
    }

    fun dismiss() {
        if (mAlertDialog != null && mAlertDialog!!.isShowing) {
            mAlertDialog!!.dismiss()
        }
    }

    private fun setup() {
        if (mCustomViewController != null) {
            val view = mCustomViewController!!.onCreateView()
            mBuilder!!.setView(view)
            mBuilder!!.setPositiveButton(null, null)
            mBuilder!!.setNegativeButton(null, null)
            mBuilder!!.setMessage(null)
            mBuilder!!.setTitle(null)
            mCustomViewController!!.onInitView(view, this)
            mCustomViewController!!.finishViewInit()
        } else {
            mBuilder!!.setMessage(mMessageText)
            mBuilder!!.setTitle(mTitleText)
            mBuilder!!.setView(null)

            if (mPositiveId != -1) {
                mBuilder!!.setPositiveButton(mPositiveId) { dialog, _ ->
                    if (mPositiveListener != null) {
                        mPositiveListener!!.onClick(dialog)
                    } else {
                        dialog.dismiss()
                    }
                }
            }
            if (mNegativeId != -1) {
                mBuilder!!.setNegativeButton(mNegativeId) { dialog, _ ->
                    if (mNegativeListener != null) {
                        mNegativeListener!!.onClick(dialog)
                    } else {
                        dialog.dismiss()
                    }
                }
            }
        } // else

        mBuilder!!.setOnCancelListener { dialog ->
            if (mOnCancelListener != null) {
                mOnCancelListener!!.onCancel(dialog)
            } else if (!mCancelableOnTouchOutside) {
                // finish current page when not allow user to cancel on touch outside
                mActivity?.finish()
            }
        }
        mBuilder!!.setOnDismissListener { dialog ->
            if (mDismissListener != null) {
                mDismissListener!!.onDismiss(dialog)
            }
        }

        mAlertDialog = mBuilder!!.create()
        mAlertDialog!!.setCanceledOnTouchOutside(mCancelableOnTouchOutside)
        mAlertDialog!!.setCancelable(mCancelableOnTouchOutside)

        if (mWindowGravity != -1) {
            val params = mAlertDialog!!.window!!.attributes
            params.gravity = mWindowGravity
            mAlertDialog!!.window!!.attributes = params
        }

        if (mWindowAnim != -1) {
            val params = mAlertDialog!!.window!!.attributes
            params.windowAnimations = mWindowAnim
            mAlertDialog!!.window!!.attributes = params
        }

        if (mSoftInputMode != -1) {
            mAlertDialog!!.window!!.setSoftInputMode(mSoftInputMode)
        }

        if (mCustomViewController != null) {
            mAlertDialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            if (mWidthScale == 0f) {
                mWidthScale = 0.85f // default width scale for custom view
            }
        }
    }

    abstract class BaseCustomViewController {
        var isViewInitialized: Boolean = false
            private set

        abstract fun onCreateView(): View

        abstract fun onInitView(view: View, dialog: SmartDialog)

        internal fun finishViewInit() {
            isViewInitialized = true
        }
    }

    @Keep
    companion object {

        private val mListMap = HashMap<String, List<SmartDialog>>()

        @JvmStatic
        @JvmOverloads
        fun solo(activity: Activity, msg: String? = null): SmartDialog {
            val key = getKey(activity)
            val dialogList = mListMap[key]
            val dialog: SmartDialog
            dialog = if (dialogList != null && dialogList.isNotEmpty()) {
                dialogList[0]
            } else {
                with(activity)
            }
            dialog.init()
            dialog.setMessage(msg)
            return dialog
        }

        @JvmStatic
        fun solo(activity: Activity, msgRes: Int): SmartDialog {
            return solo(activity, activity.getText(msgRes).toString())
        }

        @JvmStatic
        @JvmOverloads
        fun with(activity: Activity, msg: String? = null): SmartDialog {
            val dialog = SmartDialog(activity)
            addMap(activity, dialog)
            dialog.setMessage(msg)
            return dialog
        }

        @JvmStatic
        fun with(activity: Activity, msgRes: Int): SmartDialog {
            return with(activity, activity.getText(msgRes).toString())
        }

        @JvmStatic
        private fun getKey(activity: Activity): String {
            return activity.hashCode().toString()
        }

        @JvmStatic
        private fun addMap(activity: Activity, dialog: SmartDialog) {
            val key = getKey(activity)
            var dialogList: LinkedList<SmartDialog>? = mListMap[key] as LinkedList<SmartDialog>?
            if (dialogList == null) {
                dialogList = LinkedList()
            }
            dialogList.add(dialog)
            mListMap[key] = dialogList
        }

        @JvmStatic
        fun dismiss(activity: Activity) {
            val key = getKey(activity)
            val dialogList = mListMap[key]
            if (dialogList != null) {
                for (dialog in dialogList) {
                    dialog.dismiss()
                }
                mListMap.remove(key)
            }
        }
    }
}
