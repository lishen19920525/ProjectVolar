/*
 * Copyright 2015 Glow Geniuses Studio
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.volar;

import android.os.Message;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import io.volar.callback.BaseCallback;
import io.volar.callback.JsonArrayCallback;
import io.volar.callback.JsonCallback;
import io.volar.callback.ObjectCallback;
import io.volar.callback.ObjectListCallback;
import io.volar.callback.StringCallback;
import io.volar.util.JSON;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by LiShen on 2017/11/27.
 * Http request
 */
class HttpRequest<T> {
    private String url;
    private int method;
    private HttpParams httpParams;
    private BaseCallback callback;
    private Class dataClass;
    private WeakReference<Object> tag;

    private HttpResponse<T> httpResponse;

    private int parseType;

    private long timeMilestone;
    private boolean executed = false;

    private HttpRequest(HttpRequestBuilder builder) {
        url = builder.url;
        method = builder.method;
        httpParams = builder.httpParams;
        callback = builder.callback;
        parseType = builder.parseType;
        dataClass = builder.dataClass;
        tag = new WeakReference<>(builder.tag);

        httpResponse = new HttpResponse<>();
    }

    private synchronized void execute() {
        if (executed) {
            return;
        }
        executed = true;
        Volar.getDefault().getWorkHandler().post(new Runnable() {
            @Override
            public void run() {
                executeRequest();
            }
        });
    }

    private void executeRequest() {
        String originalJsonStringBody = httpParams.getParamsJson();
        // custom filter
        if (Volar.getDefault().getConfiguration().getCustomFilter() != null) {
            httpParams = Volar.getDefault().getConfiguration().getCustomFilter().filter(httpParams);
        }

        Request.Builder requestBuilder = new Request.Builder();

        // tag
        if (tag.get() != null) {
            requestBuilder.tag(tag.get());
        }

        // method
        switch (method) {
            case HttpConstant.Method.GET:
                url = httpParams.generateUrlWithParams(url);
                requestBuilder.get();
                Volar.getDefault().log("GET URL: " + url);
                break;
            case HttpConstant.Method.DELETE:
                requestBuilder.delete(httpParams.getRequestBody());
                Volar.getDefault().log("DELETE URL: " + url);
                break;
            case HttpConstant.Method.HEAD:
                url = httpParams.generateUrlWithParams(url);
                Volar.getDefault().log("HEAD URL: " + url);
                break;
            case HttpConstant.Method.POST:
                requestBuilder.post(httpParams.getRequestBody());
                Volar.getDefault().log("POST URL: " + url);
                break;
            case HttpConstant.Method.PUT:
                requestBuilder.put(httpParams.getRequestBody());
                Volar.getDefault().log("PUT URL: " + url);
                break;
            case HttpConstant.Method.PATCH:
                requestBuilder.patch(httpParams.getRequestBody());
                Volar.getDefault().log("PATCH URL: " + url);
                break;
        }

        if (method != HttpConstant.Method.GET && method != HttpConstant.Method.HEAD && !TextUtils.isEmpty(httpParams.getParamsJson())) {
            if (Volar.getDefault().getConfiguration().isLogParamsBeforeFilter()) {
                // log original json params
                Volar.getDefault().log("REQUEST JSON PARAMS: " + originalJsonStringBody);
            } else {
                // log json params after custom filter
                Volar.getDefault().log("REQUEST JSON PARAMS: " + httpParams.getParamsJson());
            }
        }

        // url
        requestBuilder.url(url);

        // headers
        if (httpParams.getHeadersBuilder() != null) {
            Headers headers = httpParams.getHeadersBuilder().build();
            Volar.getDefault().log("REQUEST HEADERS: \n" + headers.toString());
            requestBuilder.headers(headers);
        }

        // execute request async
        Call call = Volar.getDefault().getOkHttpClient().newCall(requestBuilder.build());
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handleResponse(null, e, call);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handleResponse(response, null, call);
            }
        });

        timeMilestone = System.currentTimeMillis();
    }

    /**
     * Handle the response, working in non main thread
     */
    private void handleResponse(Response response, Exception exception, Call call) {
        httpResponse.url = url;
        httpResponse.callbackType = parseType;
        httpResponse.response = response;
        httpResponse.exception = exception;
        httpResponse.call = call;
        httpResponse.canceled = call.isCanceled();
        httpResponse.requestCostTime = System.currentTimeMillis() - timeMilestone;
        httpResponse.extra = httpParams.extra;

        String originalResponseString = null;

        if (response != null) {
            httpResponse.code = response.code();
            httpResponse.message = response.message();
            httpResponse.success = response.isSuccessful();
            httpResponse.headers = response.headers();

            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                try {
                    originalResponseString = responseBody.string();
                } catch (Exception ignore) {
                }
            }

            if (TextUtils.isEmpty(originalResponseString)) {
                httpResponse.setError(HttpResponse.SERVER_NO_RESPONSE);
            } else {
                httpResponse.responseString = originalResponseString;
            }
        } else {
            httpResponse.setError(HttpResponse.NETWORK_ERROR);
        }

        // custom filter
        if (Volar.getDefault().getConfiguration().getCustomFilter() != null) {
            httpResponse = Volar.getDefault().getConfiguration().getCustomFilter().filter(httpResponse);
        }

        // show original response or not
        if (Volar.getDefault().getConfiguration().isLogResponseBeforeFilter()) {
            Volar.getDefault().log("RESPONSE: " + originalResponseString);
        } else {
            Volar.getDefault().log("RESPONSE: " + httpResponse.responseString);
        }
        Volar.getDefault().log("RESPONSE CODE: " + httpResponse.code
                + "\nREQUEST COST TIME: " + httpResponse.requestCostTime + " ms"
                + "\nURL: " + httpResponse.url);

        // data parse
        if (httpResponse.success) {
            timeMilestone = System.currentTimeMillis();
            if (!parseToData(httpResponse.responseString)) {
                httpResponse.setError(HttpResponse.DATA_PARSE_FAILURE);
            } else {
                Volar.getDefault().log("PARSE DATA COST TIME: " + httpResponse.parseDataCostTime + "ms");
            }
            httpResponse.parseDataCostTime = System.currentTimeMillis() - timeMilestone;
        }

        // post to main thread to callback
        if (!httpResponse.cancelCallback) {
            Message msg = Volar.getDefault().getMainHandler().obtainMessage(Volar.getDefault().generateMessageWhat());
            msg.obj = this;
            Volar.getDefault().getMainHandler().sendMessage(msg);
        }
    }

    /**
     * Parse response string to data
     *
     * @param responseString response string
     * @return data
     */
    private boolean parseToData(String responseString) {
        switch (parseType) {
            case HttpResponse.PARSE_TYPE_STRING:
                try {
                    httpResponse.responseData = (T) responseString;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return httpResponse.responseData != null;
            case HttpResponse.PARSE_TYPE_JSON:
                try {
                    httpResponse.responseData = (T) new JSONObject(responseString);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return httpResponse.responseData != null;
            case HttpResponse.PARSE_TYPE_JSON_ARRAY:
                try {
                    httpResponse.responseData = (T) new JSONArray(responseString);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return httpResponse.responseData != null;
            case HttpResponse.PARSE_TYPE_OBJECT:
                try {
                    httpResponse.responseData = (T) JSON.parseObject(responseString, dataClass);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return httpResponse.responseData != null;
            case HttpResponse.PARSE_TYPE_OBJECT_LIST:
                try {
                    httpResponse.responseData = (T) JSON.parseArray(responseString, dataClass);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return httpResponse.responseData != null;
        }
        return false;
    }

    /**
     * Callback in main thread
     */
    void callback() {
        if (callback != null) {
            if (httpResponse.success && httpResponse.responseData != null) {
                switch (parseType) {
                    case HttpResponse.PARSE_TYPE_STRING:
                        ((StringCallback) callback).onSuccess(httpResponse, (String) httpResponse.responseData);
                        break;
                    case HttpResponse.PARSE_TYPE_JSON:
                        ((JsonCallback) callback).onSuccess(httpResponse, (JSONObject) httpResponse.responseData);
                        break;
                    case HttpResponse.PARSE_TYPE_JSON_ARRAY:
                        ((JsonArrayCallback) callback).onSuccess(httpResponse, (JSONArray) httpResponse.responseData);
                        break;
                    case HttpResponse.PARSE_TYPE_OBJECT:
                        ((ObjectCallback<T>) callback).onSuccess(httpResponse, httpResponse.responseData);
                        break;
                    case HttpResponse.PARSE_TYPE_OBJECT_LIST:
                        ((ObjectListCallback<T>) callback).onSuccess(httpResponse, (T[]) httpResponse.responseData);
                        break;
                }
            } else {
                callback.onFailure(httpResponse, httpResponse.code, httpResponse.message);
            }
        }
    }

    public static final class HttpRequestBuilder<V> {
        private String url;
        private int method;
        private HttpParams httpParams;
        private BaseCallback callback;
        private Class dataClass;
        private int parseType;
        private Object tag;

        <V> HttpRequestBuilder(String url, int method) {
            if (TextUtils.isEmpty(url)) {
                throw new NullPointerException("Http request URL cannot be null !");
            }

            this.url = url;
            this.method = method;

            httpParams = new HttpParams();
            parseType = HttpResponse.PARSE_TYPE_STRING;
        }

        public <V> HttpRequestBuilder params(HttpParams val) {
            if (val != null) {
                httpParams = val;
            }
            return this;
        }

        public <V> HttpRequestBuilder callback(ObjectCallback<V> val1, Class val2) {
            if (val1 != null && val2 != null) {
                callback = val1;
                parseType = HttpResponse.PARSE_TYPE_OBJECT;
                dataClass = val2;
            }
            return this;
        }

        public <V> HttpRequestBuilder callback(ObjectListCallback<V> val1, Class val2) {
            if (val1 != null && val2 != null) {
                callback = val1;
                parseType = HttpResponse.PARSE_TYPE_OBJECT_LIST;
                dataClass = val2;
            }
            return this;
        }

        public <V> HttpRequestBuilder callback(JsonCallback val1) {
            if (val1 != null) {
                callback = val1;
                parseType = HttpResponse.PARSE_TYPE_JSON;
            }
            return this;
        }

        public <V> HttpRequestBuilder callback(JsonArrayCallback val1) {
            if (val1 != null) {
                callback = val1;
                parseType = HttpResponse.PARSE_TYPE_JSON_ARRAY;
            }
            return this;
        }

        public <V> HttpRequestBuilder callback(StringCallback val1) {
            if (val1 != null) {
                callback = val1;
                parseType = HttpResponse.PARSE_TYPE_STRING;
            }
            return this;
        }

        public <V> HttpRequestBuilder tag(Object val) {
            tag = val;
            return this;
        }

        public <V> void execute() {
            switch (parseType) {
                case HttpResponse.PARSE_TYPE_STRING:
                    new HttpRequest<String>(this).execute();
                    break;
                case HttpResponse.PARSE_TYPE_JSON:
                    new HttpRequest<JSONObject>(this).execute();
                    break;
                case HttpResponse.PARSE_TYPE_JSON_ARRAY:
                    new HttpRequest<JSONArray>(this).execute();
                    break;
                case HttpResponse.PARSE_TYPE_OBJECT:
                    new HttpRequest<V>(this).execute();
                    break;
                case HttpResponse.PARSE_TYPE_OBJECT_LIST:
                    new HttpRequest<List<V>>(this).execute();
                    break;
            }
        }
    }
}