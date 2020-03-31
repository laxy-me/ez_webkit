package com.lax.ezweb.ssl

import java.math.BigInteger
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey

import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * @author YangGuangda
 * @date 2017/11/7
 */

class TrustAllManager : X509TrustManager {
    companion object {
        /**
         * 证书中的公钥
         */
        private const val PUB_KEY = ""
    }

    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
        checkServer(chain, authType);
    }

    @Throws(CertificateException::class)
    private fun checkServer(chain: Array<X509Certificate>?, authType: String?) {
        if (chain == null) {
            throw IllegalArgumentException("checkServerTrusted:x509Certificate array isnull")
        }

        if (chain.isEmpty()) {
            throw IllegalArgumentException("checkServerTrusted: X509Certificate is empty")
        }

        if (!(null != authType && "RSA".equals(authType, ignoreCase = true))) {
            throw CertificateException("checkServerTrusted: AuthType is not RSA")
        }

        // Perform customary SSL/TLS checks
        try {
            val tmf = TrustManagerFactory.getInstance("X509")
            tmf.init(null as KeyStore?)
            for (trustManager in tmf.trustManagers) {
                (trustManager as X509TrustManager).checkServerTrusted(chain, authType)
            }
        } catch (e: Exception) {
            throw CertificateException(e)
        }

        // Hack ahead: BigInteger and toString(). We know a DER encoded Public Key begins
        // with 0×30 (ASN.1 SEQUENCE and CONSTRUCTED), so there is no leading 0×00 to drop.
        val pubkey = chain[0].publicKey as RSAPublicKey

        val encoded = BigInteger(1 /* positive */, pubkey.encoded).toString(16)
        // Pin it!
        val expected = PUB_KEY.equals(encoded, ignoreCase = true)

        if (!expected) {
            throw CertificateException(
                "checkServerTrusted: Expected public key: "
                        + PUB_KEY + ", got public key:" + encoded
            )
        }
    }

    override fun getAcceptedIssuers(): Array<X509Certificate?> {
        return arrayOfNulls(0)
    }
}
