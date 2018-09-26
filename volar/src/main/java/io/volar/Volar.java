package io.volar;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import java.lang.ref.SoftReference;
import java.util.concurrent.TimeUnit;

import io.volar.configuration.NetworkConfiguration;
import io.volar.https.SslSocketFactoryHelper;
import io.volar.https.SslSocketFactoryParams;
import io.volar.https.TrustAllHostnameVerifier;
import io.volar.util.LOG;
import okhttp3.Call;
import okhttp3.OkHttpClient;

/**
 * Created by LiShen on 2017/11/26.
 * Http framework based on OkHttp
 */

public final class Volar {
    private volatile static Volar volar;

    private NetworkConfiguration configuration;
    private OkHttpClient okHttpClient;
    private MainHandler mainHandler;
    private WorkHandler workHandler;
    private HandlerThread workThread;

    private Volar(NetworkConfiguration customConfiguration) {
        if (customConfiguration == null) {
            this.configuration = new NetworkConfiguration.Builder().build();
        } else {
            this.configuration = customConfiguration;
        }
        okHttpClient = generateOkHttpClient(configuration);
    }

    /**
     * Generate a OkHttpClient through custom configuration
     *
     * @param configuration
     * @return
     */
    OkHttpClient generateOkHttpClient(NetworkConfiguration configuration) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(configuration.getConnectTimeout(), TimeUnit.MILLISECONDS);
        builder.readTimeout(configuration.getReadTimeout(), TimeUnit.MILLISECONDS);
        builder.writeTimeout(configuration.getWriteTimeout(), TimeUnit.MILLISECONDS);
        builder.cache(configuration.getCache());
        builder.retryOnConnectionFailure(configuration.isRetryOnConnectionFailure());
        builder.followRedirects(configuration.isFollowRedirects());
        builder.followSslRedirects(configuration.isFollowSslRedirects());
        builder.proxy(configuration.getProxy());
        if (configuration.getDns() != null) {
            builder.dns(configuration.getDns());
        }
        if (configuration.getCookieJar() != null) {
            builder.cookieJar(configuration.getCookieJar());
        }
        if (configuration.getSslSocketFactoryParams() != null
                && configuration.getSslSocketFactoryParams().getSslSocketFactory() != null
                && configuration.getSslSocketFactoryParams().getX509TrustManager() != null) {
            builder.sslSocketFactory(configuration.getSslSocketFactoryParams().getSslSocketFactory(),
                    configuration.getSslSocketFactoryParams().getX509TrustManager());
        }
        if (configuration.getHostnameVerifier() != null) {
            builder.hostnameVerifier(configuration.getHostnameVerifier());
        }
        if (configuration.isTrustAllHttps()) {
            SslSocketFactoryParams sslSocketFactoryParams = SslSocketFactoryHelper.getTrustAllSslSocketFactory();
            if (sslSocketFactoryParams.getX509TrustManager() != null && sslSocketFactoryParams.getSslSocketFactory() != null) {
                builder.sslSocketFactory(sslSocketFactoryParams.getSslSocketFactory(), sslSocketFactoryParams.getX509TrustManager());
                builder.hostnameVerifier(new TrustAllHostnameVerifier());
            }
        }
        return builder.build();
    }

    /**
     * Init, must call it before {@link #getDefault()}
     *
     * @param customConfiguration configuration
     */
    public static Volar init(NetworkConfiguration customConfiguration) {
        if (volar == null) {
            synchronized (Volar.class) {
                if (volar == null) {
                    volar = new Volar(customConfiguration);
                }
            }
        }
        return volar;
    }

    /**
     * Get a volar singleton, must call it after {@link #init(NetworkConfiguration)}
     *
     * @return volar
     */
    public static Volar getDefault() {
        return init(null);
    }

    /**
     * Get OkHttpClient
     *
     * @return okHttpClient
     */
    OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    /**
     * Get configuration
     *
     * @return configuration
     */
    public NetworkConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Get separate configuration builder
     *
     * @return builder
     */
    public NetworkConfiguration.Builder getSeparateConfigurationBuilder() {
        return configuration.newBuilder();
    }

    /**
     * Cancel all calls
     */
    public void cancelAllCalls() {
        getOkHttpClient().dispatcher().cancelAll();
    }

    /**
     * Cancel call
     *
     * @param tag tag
     */
    public void cancelCall(Object tag) {
        if (tag != null) {
            for (Call call : getOkHttpClient().dispatcher().queuedCalls()) {
                if (tag.equals(call.request().tag())) {
                    call.cancel();
                }
            }
            for (Call call : getOkHttpClient().dispatcher().runningCalls()) {
                if (tag.equals(call.request().tag())) {
                    call.cancel();
                }
            }
        }
    }

    /**
     * Get
     *
     * @param url url
     * @return request builder
     */
    public HttpRequest.HttpRequestBuilder GET(String url) {
        return new HttpRequest.HttpRequestBuilder(url, HttpConstant.Method.GET);
    }

    /**
     * Post
     *
     * @param url url
     * @return request builder
     */
    public HttpRequest.HttpRequestBuilder POST(String url) {
        return new HttpRequest.HttpRequestBuilder(url, HttpConstant.Method.POST);
    }

    /**
     * Put
     *
     * @param url url
     * @return request builder
     */
    public HttpRequest.HttpRequestBuilder PUT(String url) {
        return new HttpRequest.HttpRequestBuilder(url, HttpConstant.Method.PUT);
    }

    /**
     * Delete
     *
     * @param url url
     * @return request builder
     */
    public HttpRequest.HttpRequestBuilder DELETE(String url) {
        return new HttpRequest.HttpRequestBuilder(url, HttpConstant.Method.DELETE);
    }

    /**
     * Head
     *
     * @param url url
     * @return request builder
     */
    public HttpRequest.HttpRequestBuilder HEAD(String url) {
        return new HttpRequest.HttpRequestBuilder(url, HttpConstant.Method.HEAD);
    }

    /**
     * Patch
     *
     * @param url url
     * @return request builder
     */
    public HttpRequest.HttpRequestBuilder PATCH(String url) {
        return new HttpRequest.HttpRequestBuilder(url, HttpConstant.Method.PATCH);
    }

    /**
     * Download
     *
     * @param url url
     * @return download request
     */
    public DownloadRequest.DownloadRequestBuilder DOWNLOAD(String url) {
        return new DownloadRequest.DownloadRequestBuilder(url);
    }

    /**
     * Main handler
     *
     * @return handler
     */
    MainHandler getMainHandler() {
        if (mainHandler == null || mainHandler.reference.get() == null) {
            mainHandler = new MainHandler(this, Looper.getMainLooper());
        }
        return mainHandler;
    }

    /**
     * Work handler
     *
     * @return handler
     */
    WorkHandler getWorkHandler() {
        if (workThread == null || !workThread.isAlive() || workHandler == null || workHandler.reference.get() == null) {
            workHandler = new WorkHandler(this, getWorkThread().getLooper());
        }
        return workHandler;
    }

    /**
     * Work thread
     *
     * @return thread
     */
    private HandlerThread getWorkThread() {
        if (workThread == null || !workThread.isAlive() || workHandler == null || workHandler.reference.get() == null) {
            workThread = new HandlerThread("volar", Process.THREAD_PRIORITY_BACKGROUND);
            workThread.start();
        }
        return workThread;
    }

    private void handleMainMessage(Message msg) {

    }

    private void handleWorkMessage(Message msg) {

    }

    /**
     * Logger
     *
     * @param content content
     */
    void log(String content) {
        if (getConfiguration().isLogEnabled()) {
            switch (getConfiguration().getLogLevel()) {
                case V:
                    LOG.v(getConfiguration().getLogTag(), content);
                    break;
                default:
                case I:
                    LOG.i(getConfiguration().getLogTag(), content);
                    break;
                case D:
                    LOG.d(getConfiguration().getLogTag(), content);
                    break;
                case W:
                    LOG.w(getConfiguration().getLogTag(), content);
                    break;
                case E:
                    LOG.e(getConfiguration().getLogTag(), content);
                    break;
            }
        }
    }

    /**
     * Main thread handler
     */
    static final class MainHandler extends Handler {
        private final SoftReference<Volar> reference;

        private MainHandler(Volar volar, Looper Looper) {
            super(Looper);
            reference = new SoftReference<>(volar);
        }

        @Override
        public void handleMessage(Message msg) {
            Volar volar = reference.get();
            if (volar != null && msg != null) {
                volar.handleMainMessage(msg);
            }
        }
    }

    /**
     * Work thread handler
     */
    static final class WorkHandler extends Handler {
        private final SoftReference<Volar> reference;

        private WorkHandler(Volar volar, Looper Looper) {
            super(Looper);
            reference = new SoftReference<>(volar);
        }

        @Override
        public void handleMessage(Message msg) {
            Volar volar = reference.get();
            if (volar != null && msg != null) {
                volar.handleWorkMessage(msg);
            }
        }
    }
}