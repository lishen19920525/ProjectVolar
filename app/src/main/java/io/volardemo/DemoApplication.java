package io.volardemo;

import android.app.Application;

import io.volar.Volar;
import io.volar.configuration.VolarConfiguration;

/**
 * Project: ProjectVolar
 * Author: LiShen
 * Time: 2018/11/21 11:37
 */
public class DemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        VolarConfiguration configuration = new VolarConfiguration.Builder()
                .logTag("VolarDemo")
                .connectTimeout(5000)
                .build();
        Volar.init(configuration);
    }
}
