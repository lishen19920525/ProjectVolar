package io.volar.https;

import android.annotation.SuppressLint;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Created by LiShen on 2017/11/28.
 * Combined X509 trust manager
 */

public class CombinedX509TrustManager implements X509TrustManager {
    private X509TrustManager defaultTrustManager;
    private X509TrustManager localTrustManager;

    public CombinedX509TrustManager(X509TrustManager localTrustManager) throws NoSuchAlgorithmException, KeyStoreException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);
        defaultTrustManager = SslSocketFactoryHelper.chooseX509TrustManager(trustManagerFactory.getTrustManagers());
        this.localTrustManager = localTrustManager;
    }


    @SuppressLint("TrustAllX509TrustManager")
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            defaultTrustManager.checkServerTrusted(chain, authType);
        } catch (CertificateException ce) {
            localTrustManager.checkServerTrusted(chain, authType);
        }
    }


    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}