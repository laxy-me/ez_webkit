package com.eztd.arm.util

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object SecurityUtil {

    private const val AES_SECRET_KEY = "7634f7c34c02805afd241dec53b7fa53"

    private const val CIPHER_ALGORITHM_CBC = "AES/CBC/PKCS5Padding"

    private val iv: ByteArray?
        get() = str2Byte(AES_SECRET_KEY)

    @Throws(NoSuchAlgorithmException::class)
    fun md5Encrypt(value: String): String {
        val digester = MessageDigest.getInstance("MD5")
        digester.update(value.toByteArray())
        return convertByteArrayToHexString(
            digester.digest()
        )
    }

    private fun convertByteArrayToHexString(arrayBytes: ByteArray): String {
        val stringBuffer = StringBuffer()
        for (i in arrayBytes.indices) {
            stringBuffer.append(
                Integer.toString((arrayBytes[i].toInt() and 0xff) + 0x100, 16)
                    .substring(1)
            )
        }
        return stringBuffer.toString()
    }

    /**
     * AES算法密钥生成器.
     *
     * @return 生成的密钥 它是一个32个字符的16进制字符串.
     */
    fun keyAES(): String {
        return try {
            // Get the KeyGenerator
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(128) // 192 and 256 bits may not be available
            // Generate the secret key specs.
            val key = keyGenerator.generateKey()
            val raw = key.encoded
            byte2Str(raw)
        } catch (e: Exception) {
            ""
        }

    }

    /**
     * 使用AES算法解密字符串.
     * AES加密算法（美国国家标准局倡导的AES即将作为新标准取代DES）
     *
     * @param encrypted 要解密的字符串
     * @param rawKey    密钥字符串, 要求为一个32位(或64位，或128位)的16进制数的字符串,否则会出错.
     * 可以使用[.AESKey]方法生成一个密钥,
     * @return 解密之后的字符串
     */
    fun decryptAES(encrypted: String, rawKey: String): String? {
        val tmp = str2Byte(encrypted)
        val key = str2Byte(rawKey)
        return try {
            val sks = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM_CBC)
            cipher.init(Cipher.DECRYPT_MODE, sks, IvParameterSpec(iv))
            val decrypted = cipher.doFinal(tmp)
            String(decrypted)
        } catch (e: Exception) {
            null
        }

    }

    /**
     * 使用AES算法加密字符串.
     *
     * @param message 要加密的字符串.
     * rawKey  密钥字符串, 要求为一个32位(或64位，或128位)的16进制数的字符串,否则会出错.
     * 可以使用[.AESKey]方法生成一个密钥,
     * @return 加密之后的字符串
     * @see .AESDecrypt
     */

    fun encryptAES(message: String): String? {
        val key =
            str2Byte(AES_SECRET_KEY)
        return try {
            val sks = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM_CBC)
            cipher.init(Cipher.ENCRYPT_MODE, sks, IvParameterSpec(iv))
            val encrypted = cipher.doFinal(message.toByteArray())

            byte2Str(encrypted)
        } catch (e: Exception) {
            null
        }

    }

    fun decryptAES(encrypted: String): String? {
        val tmp = str2Byte(encrypted)
        val key =
            str2Byte(AES_SECRET_KEY)
        return try {
            val sks = SecretKeySpec(key, "AES")
            val cipher = Cipher.getInstance(CIPHER_ALGORITHM_CBC)
            cipher.init(Cipher.DECRYPT_MODE, sks, IvParameterSpec(iv))
            val decrypted = cipher.doFinal(tmp)
            String(decrypted)
        } catch (e: Exception) {
            null
        }

    }

    /**
     * 使用DES算法解密字符串.
     *
     *     AES和DES 一共有4种工作模式:
     *     1.电子密码本模式(ECB) -- 缺点是相同的明文加密成相同的密文，明文的规律带到密文。
     *     2.加密分组链接模式(CBC)
     *     3.加密反馈模式(CFB)
     *     4.输出反馈模式(OFB)四种模式
     *
     * PKCS5Padding: 填充方式
     *
     * 加密方式/工作模式/填充方式
     * DES/CBC/PKCS5Padding
     *
     * @param encrypted 要解密的字符串.
     * @param rawKey    密钥字符串, 可以为任意字符, 但最长不得超过8个字符(如最超过，后面的字符会被丢弃).
     * @return 解密之后的字符串.
     */
    fun decryptDES(encrypted: String, rawKey: String): String? {
        val arrBTmp = rawKey.toByteArray()
        val arrB = ByteArray(8) // 创建一个空的8位字节数组（默认值为0）
        var i = 0
        while (i < arrBTmp.size && i < arrB.size) {
            // 将原始字节数组转换为8位
            arrB[i] = arrBTmp[i]
            i++
        }
        return try {
            val key = SecretKeySpec(arrB, "DES")// 生成密钥
            val cipher = Cipher.getInstance("DES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key)
            String(
                cipher.doFinal(
                    str2Byte(
                        encrypted
                    )
                )
            )
        } catch (e: Exception) {
            null
        }

    }


    /**
     * Turns array of bytes into string
     *
     * @param buf Array of bytes to convert to hex string
     * @return Generated hex string
     */
    private fun byte2Str(buf: ByteArray): String {
        val sb = StringBuilder(buf.size * 2)
        var i = 0
        while (i < buf.size) {
            if (buf[i].toInt() and 0xff < 0x10) sb.append("0")

            sb.append(java.lang.Long.toString((buf[i].toInt() and 0xff).toLong(), 16))
            i++
        }
        return sb.toString()
    }

    /**
     * 将表示16进制值的字符串转换为byte数组， 和public static String byte2Str(byte[] buf)互为可逆的转换过程
     *
     * @param src 需要转换的字符串
     * @return 转换后的byte数组
     */
    private fun str2Byte(src: String): ByteArray? {
        if (src.isEmpty()) {
            return null
        }
        val encrypted = ByteArray(src.length / 2)
        for (i in 0 until src.length / 2) {
            val high = Integer.parseInt(src.substring(i * 2, i * 2 + 1), 16)
            val low = Integer.parseInt(src.substring(i * 2 + 1, i * 2 + 2), 16)

            encrypted[i] = (high * 16 + low).toByte()
        }
        return encrypted
    }
}
