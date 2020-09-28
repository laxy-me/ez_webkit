package com.facebook.todo.tools

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.IOException

class ImageUtil {
    companion object {
        @JvmStatic
        fun compressImageToBase64(urlPath: String): String? {
            return bitmapToBase64(compressScale(urlPath))
        }

        /**
         * 图片按比例大小压缩方法
         *
         * @param srcPath （根据路径获取图片并压缩）
         * @return
         */
        @JvmStatic
        fun compressScale(srcPath: String): Bitmap? {

            val newOpts = BitmapFactory.Options()
            // 开始读入图片，此时把options.inJustDecodeBounds 设回true了
            newOpts.inJustDecodeBounds = true
            var bitmap = BitmapFactory.decodeFile(srcPath, newOpts)// 此时返回bm为空

            newOpts.inJustDecodeBounds = false
            val w = newOpts.outWidth
            val h = newOpts.outHeight
            // 现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
            val hh = 480f// 这里设置高度为800f
            val ww = 320f// 这里设置宽度为480f
            // 缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
            var be = 1// be=1表示不缩放
            if (w > h && w > ww) {// 如果宽度大的话根据宽度固定大小缩放
                be = (newOpts.outWidth / ww).toInt()
            } else if (w < h && h > hh) {// 如果高度高的话根据宽度固定大小缩放
                be = (newOpts.outHeight / hh).toInt()
            }
            if (be <= 0)
                be = 1
            newOpts.inSampleSize = be// 设置缩放比例
            // 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
            bitmap = BitmapFactory.decodeFile(srcPath, newOpts)
            return reviewPicRotate(
                bitmap,
                srcPath
            )
        }

        /**
         * 获取图片文件的信息，是否旋转了90度，如果是则反转
         *
         * @param bitmap 需要旋转的图片
         * @param path   图片的路径
         */
        @JvmStatic
        fun reviewPicRotate(bitmap: Bitmap?, path: String): Bitmap? {
            var b = bitmap
            bitmap?.let {
                val degree =
                    getPicRotate(path)
                if (degree != 0) {
                    val m = Matrix()
                    m.setRotate(degree.toFloat()) // 旋转angle度
                    b = Bitmap.createBitmap(it, 0, 0, it.width, it.height, m, true)// 从新生成图片
                }
                return b
            }
            return b
        }

        /**
         * 读取图片文件旋转的角度
         *
         * @param path 图片绝对路径
         * @return 图片旋转的角度
         */
        @JvmStatic
        fun getPicRotate(path: String): Int {
            var degree = 0
            try {
                val exifInterface = ExifInterface(path)
                val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> degree = 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> degree = 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> degree = 270
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return degree
        }

        /**
         * bitmap转为base64
         *
         * @param bitmap
         * @return
         */
        @JvmStatic
        fun bitmapToBase64(bitmap: Bitmap?): String? {
            var result: String? = null
            var baos: ByteArrayOutputStream? = null
            try {
                if (bitmap != null) {
                    baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)

                    baos.flush()
                    baos.close()

                    val bitmapBytes = baos.toByteArray()
                    result = Base64.encodeToString(bitmapBytes, Base64.NO_WRAP)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                try {
                    if (baos != null) {
                        baos.flush()
                        baos.close()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                return result
            }
        }
    }
}
