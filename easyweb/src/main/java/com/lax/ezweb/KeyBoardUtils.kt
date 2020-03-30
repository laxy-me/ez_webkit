package com.lax.ezweb

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

/**
 * 键盘控制类.
 */
object KeyBoardUtils {

    fun closeKeyboard(context: Context, view: View) {
        val imm = context.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun closeOrOpenKeyBoard(context: Context) {
        val inputMethodManager = context.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    fun openKeyBoard(context: Context, view: View) {
        val inputMethodManager = context.applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(view, 0)
    }

    /**
     * 根据EditText所在坐标和用户点击的坐标相对比，来判断是否隐藏键盘，因为当用户点击EditText时则不能隐藏
     *
     * @param v     View
     * @param event MotionEvent
     * @return boolean
     */
    fun isShouldHideKeyboard(v: View?, event: MotionEvent): Boolean {
        return if (v != null && v is EditText) {
            isOutside(event, v)
        } else false
        // 如果焦点不是EditText则忽略，这个发生在视图刚绘制完，第一个焦点不在EditText上，和用户用轨迹球选择其他的焦点
    }

    fun isOutside(ev: MotionEvent, v: View): Boolean {
        val l = intArrayOf(0, 0)
        v.getLocationInWindow(l)
        val left = l[0]
        val top = l[1]
        val bottom = top + v.height
        val right = left + v.width
        return !(ev.x > left && ev.x < right
                && ev.y > top && ev.y < bottom)
    }
}
