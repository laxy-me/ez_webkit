package com.lax.ezweb.ssl

import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

/**
 * @author YangGuangda
 * @date 2017/2/28
 */

internal object TrustManager {
    private val sTrustManager: X509TrustManager = TrustAllManager()

    // Create a trust manager that does not validate certificate chains
    // Install the all-trusting trust manager
    // Create an ssl socket factory with our all-trusting manager
    val unsafeOkHttpClient: SSLSocketFactory
        get() {
            try {
                val trustAllCerts = arrayOf(sTrustManager)
                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(
                    null, trustAllCerts,
                    java.security.SecureRandom()
                )
                return sslContext
                    .socketFactory
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
}
