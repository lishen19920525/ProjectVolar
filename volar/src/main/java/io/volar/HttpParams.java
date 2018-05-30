package io.volar;

import android.text.TextUtils;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.List;

import io.volar.util.JSON;
import io.volar.util.arraymap.VolarArrayMap;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

/**
 * Created by LiShen on 2017/11/27.
 * Request params
 */

public final class HttpParams {
    private Headers.Builder headersBuilder;
    // priority 1
    private RequestBody requestBody;
    // priority 2
    private VolarArrayMap<String, String> formParams;
    // priority 3
    private String paramsJson;
    // priority 4
    private VolarArrayMap<String, Object> stringParams;
    public String extra;

    public HttpParams() {
        stringParams = new VolarArrayMap<>();
        formParams = new VolarArrayMap<>();
        requestBody = null;
        paramsJson = null;

        if (Volar.getDefault().getConfiguration().getCommonHeaders() != null) {
            headersBuilder = Volar.getDefault().getConfiguration().getCommonHeaders().getCommonHeaders();
        }
        if (headersBuilder == null) {
            headersBuilder = new Headers.Builder();
        }
    }

    /**
     * Add an header line containing a field name, a literal colon, and a value.
     *
     * @param line line
     */
    public void addHeader(String line) {
        if (!TextUtils.isEmpty(line))
            headersBuilder.add(line);
    }

    /**
     * Add a field with the specified value.
     *
     * @param name  field name
     * @param value value
     */
    public void addHeader(String name, String value) {
        if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value))
            headersBuilder.add(name, value);
    }

    /**
     * Set a header
     *
     * @param name  field name
     * @param value value
     */
    public void setHeader(String name, String value) {
        if (!TextUtils.isEmpty(name))
            headersBuilder.set(name, value);
    }

    /**
     * Remove a header
     *
     * @param name field name
     */
    public void removeHeader(String name) {
        if (!TextUtils.isEmpty(name))
            headersBuilder.removeAll(name);
    }

    /**
     * Set request body directly
     *
     * @param requestBody request body
     */
    public void setRequestBody(RequestBody requestBody) {
        this.requestBody = requestBody;
    }

    /**
     * Put key value params into request
     *
     * @param key   no need to URLEncode if use GET
     * @param value number value, int double long...
     */
    public <T extends Number> void put(String key, T value) {
        if (!TextUtils.isEmpty(key))
            stringParams.put(key, value);
    }

    /**
     * Put key value params into request
     *
     * @param key   no need to URLEncode if use GET
     * @param value Boolean value
     */
    public void put(String key, Boolean value) {
        if (!TextUtils.isEmpty(key))
            stringParams.put(key, value);
    }

    /**
     * Put key value params into request
     *
     * @param key   no need to URLEncode if use GET
     * @param value no need to URLEncode if use GET
     */
    public void put(String key, String value) {
        if (!TextUtils.isEmpty(key))
            stringParams.put(key, value);
    }

    /**
     * Put form key value
     *
     * @param key   key
     * @param value value
     */
    public void putFormBody(String key, String value) {
        if (!TextUtils.isEmpty(key))
            formParams.put(key, value);
    }

    /**
     * Put files and generate multi-part body
     *
     * @param files files
     */
    public void putFiles(List<FileParam> files) {
        setRequestBody(createMultiFileRequestBody(files));
    }

    /**
     * Set params json string directly
     *
     * @param paramsJsonString json string
     */
    public void setParamsJsonString(String paramsJsonString) {
        paramsJson = paramsJsonString;
    }

    /**
     * Set object params into json string
     *
     * @param params object params
     */
    public void setParamsJson(Object params) {
        if (params != null)
            setParamsJsonString(JSON.toJSONString(params));
    }

    /**
     * Get headers
     *
     * @return headers
     */
    Headers.Builder getHeadersBuilder() {
        return headersBuilder;
    }

    /**
     * Generate url with params use in GET or HEAD type
     *
     * @param url original url
     * @return url with params
     */
    String generateUrlWithParams(String url) {
        if (stringParams.size() == 0) {
            return url;
        }
        StringBuilder paramsUrl = new StringBuilder("?");
        String key;
        Object value;
        String valueStr;
        for (int i = 0; i < stringParams.keySet().size(); i++) {
            key = stringParams.keyAt(i);
            value = stringParams.get(key);
            if (value != null) {
                valueStr = String.valueOf(value);
                try {
                    key = URLEncoder.encode(key, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                try {
                    value = URLEncoder.encode(valueStr, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                paramsUrl.append(key).append("=").append(value);
                if (i != stringParams.size() - 1) {
                    paramsUrl.append("&");
                }
            }
        }
        return url + paramsUrl.toString();
    }

    /**
     * Get params json string
     *
     * @return json string
     */
    String getParamsJson() {
        return paramsJson;
    }

    /**
     * Get request body
     *
     * @return request body
     */
    RequestBody getRequestBody() {
        if (requestBody != null) {
            // already have (custom or files etc...)
            return requestBody;
        } else if (formParams.size() > 0) {
            // form body
            FormBody.Builder builder = new FormBody.Builder();
            for (String key : formParams.keySet()) {
                builder.add(key, formParams.get(key));

            }
            requestBody = builder.build();
            return requestBody;
        } else if (!TextUtils.isEmpty(paramsJson)) {
            // json string body direct
            requestBody = RequestBody.create(MediaType.parse(Constant.ContentType.JSON), paramsJson);
            return requestBody;
        } else if (stringParams.size() > 0) {
            // json string body
            paramsJson = JSON.toJSONString(stringParams);
            requestBody = RequestBody.create(MediaType.parse(Constant.ContentType.JSON), paramsJson);
            return requestBody;
        } else {
            // null params
            requestBody = RequestBody.create(MediaType.parse(Constant.ContentType.TEXT_PLAIN), "");
            return requestBody;
        }
    }

    private RequestBody createMultiFileRequestBody(List<FileParam> files) {
        if (files == null || files.isEmpty()) {
            FormBody.Builder builder = new FormBody.Builder();
            return builder.build();
        } else {
            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            for (FileParam f : files) {
                if (!TextUtils.isEmpty(f.key) && f.file != null && f.file.exists()) {
                    RequestBody requestBody = RequestBody.create(
                            MediaType.parse(guessContentType(f.file.getAbsolutePath())), f.file);
                    builder.addFormDataPart(f.key, f.file.getName(), requestBody);
                }
            }
            return builder.build();
        }
    }

    private String guessContentType(String path) {
        String contentTypeFor = null;
        try {
            contentTypeFor = URLConnection.getFileNameMap().getContentTypeFor(URLEncoder.encode(path, "utf-8"));
        } catch (Exception ignore) {
        }
        if (contentTypeFor == null) {
            contentTypeFor = Constant.ContentType.OCTET_STREAM;
        }
        return contentTypeFor;
    }

    /**
     * File param
     */
    public static class FileParam {
        public FileParam(String key, File file) {
            this.file = file;
            this.key = key;
        }

        public String key;
        public File file;
    }
}