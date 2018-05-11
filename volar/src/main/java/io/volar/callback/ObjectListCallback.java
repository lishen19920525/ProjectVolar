package io.volar.callback;

import java.util.List;

import io.volar.HttpResponse;

/**
 * Created by LiShen on 2017/11/27.
 * Object list callback
 */

public interface ObjectListCallback<T> extends BaseCallback {
    void onSuccess(HttpResponse response, List<T> tList);
}