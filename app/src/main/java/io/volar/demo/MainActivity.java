package io.volar.demo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import io.volar.HttpParams;
import io.volar.HttpResponse;
import io.volar.Volar;
import io.volar.callback.StringCallback;
import io.volar.configuration.CustomFilter;
import io.volar.configuration.NetworkConfiguration;

public class MainActivity extends AppCompatActivity {

    private WebView wvMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wvMain = findViewById(R.id.wvMain);
        wvMain.setWebViewClient(new WebViewClient());
        wvMain.setWebChromeClient(new WebChromeClient());
    }

    public void onClick(View v) {
        NetworkConfiguration separateConfiguration = Volar.getDefault().getSeparateConfigurationBuilder()
                .connectTimeout(5 * 1000)
                .readTimeout(20 * 1000)
                .writeTimeout(20 * 1000)
                .logTag("VolarDemo")
                .logEnabled(true)
                .build();

        Volar.getDefault().GET("https://m.baidu.com/")
                .tag(this)
                .useSeparateConfiguration(separateConfiguration)
                .callback(new StringCallback() {
                    @Override
                    public void onSuccess(HttpResponse response, String responseString) {
                        wvMain.loadDataWithBaseURL("", responseString, "text/html; charset=UTF-8", null, "404");
                    }

                    @Override
                    public void onFailure(HttpResponse response, int errorCode, String errorMessage) {

                    }
                })
                .execute();
    }
}