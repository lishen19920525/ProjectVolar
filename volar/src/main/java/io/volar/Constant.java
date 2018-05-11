package io.volar;

/**
 * Created by LiShen on 2017/11/27.
 * Constants
 */

public final class Constant {
    public static final String DEFAULT_LOG_TAG = "Volar";
    public static final String NETWORK_MODULE_NAME = "com.glowgeniuses.android.athena.network.configuration.NetworkModule";
    public static final long DEFAULT_CONNECT_TIMEOUT = 15 * 1000;
    public static final long DEFAULT_READ_TIMEOUT = 45 * 1000;
    public static final long DEFAULT_WRITE_TIMEOUT = 45 * 1000;
    public static final int MSG_MAX_WHAT = 10000;

    static final class Method {
        static final int GET = 1;
        static final int POST = 2;
        static final int PUT = 3;
        static final int DELETE = 4;
        static final int HEAD = 5;
        static final int PATCH = 6;
    }

    public static final class ContentType {
        public static final String CONTENT_TYPE = "Content-Type";
        public static final String IMAGE_PNG = "image/png; charset=utf-8";
        public static final String TEXT_PLAIN = "text/plain; charset=utf-8";
        public static final String TEXT_XML = "text/xml; charset=utf-8";
        public static final String JSON = "application/json; charset=utf-8";
        public static final String OCTET_STREAM = "application/octet-stream";
        public static final String FORM_URLENCODED = "application/x-www-form-urlencoded";
        public static final String MULTI_PART = "multipart/form-data";
    }

    static final class ErrorMessages {
        static final String NETWORK_ERROR = "Something goes wrong with the network";
        static final String DATA_PARSE_FAILURE = "Data parsing failure";
        static final String SERVER_NO_RESPONSE = "Failed to connect to the server";
        static final String OTHER_ERROR = "Other problems";
    }
}