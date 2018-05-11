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



import io.volar.configuration.CustomErrorMessages;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.Response;

/**
 * Created by LiShen on 2017/11/27.
 * Http Response
 */

public final class HttpResponse<T> {
    public static final int SUCCESS = 200;
    public static final int NETWORK_ERROR = -100001;
    public static final int DATA_PARSE_FAILURE = -100002;
    public static final int SERVER_NO_RESPONSE = -100003;

    public static final int PARSE_TYPE_STRING = 0;
    public static final int PARSE_TYPE_JSON = 1;
    public static final int PARSE_TYPE_JSON_ARRAY = 2;
    public static final int PARSE_TYPE_OBJECT = 3;
    public static final int PARSE_TYPE_OBJECT_LIST = 4;

    public String url;
    public Call call;
    public Headers headers;
    public Response response;
    public String message;
    public Exception exception;
    public boolean canceled;
    public int code;
    public boolean success;
    public String responseString;
    public T responseData;
    public String extra;
    public long requestCostTime;
    public long parseDataCostTime;
    public int callbackType;
    public boolean cancelCallback = false;

    public void setError(int errorCode) {
        success = false;
        code = errorCode;
        CustomErrorMessages customErrorMessages = Volar.getDefault().getConfiguration()
                .getCustomErrorMessages();
        switch (errorCode) {
            case NETWORK_ERROR:
                if (customErrorMessages != null) {
                    message = customErrorMessages.networkError();
                } else {
                    message = Constant.ErrorMessages.NETWORK_ERROR;
                }
                break;
            case SERVER_NO_RESPONSE:
                if (customErrorMessages != null) {
                    message = customErrorMessages.serverNoResponse();
                } else {
                    message = Constant.ErrorMessages.SERVER_NO_RESPONSE;
                }
                break;
            case DATA_PARSE_FAILURE:
                if (customErrorMessages != null) {
                    message = customErrorMessages.dataParseFailed();
                } else {
                    message = Constant.ErrorMessages.DATA_PARSE_FAILURE;
                }
                break;
            default:
                if (customErrorMessages != null) {
                    message = customErrorMessages.otherError(errorCode);
                } else {
                    message = Constant.ErrorMessages.OTHER_ERROR;
                }
                break;
        }
    }
}