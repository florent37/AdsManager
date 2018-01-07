package com.github.florent37.adsmanager.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.github.florent37.adsmanager.AdsManager;
import com.google.android.gms.ads.AdSize;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    AdsManager adsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ViewGroup adContainer = findViewById(R.id.adContainer);

        adsManager = ((MainApplication) getApplicationContext()).getAdsManager();

        adsManager.insertAdView(this, adContainer, R.string.admob_footer, AdSize.BANNER);

        adsManager.loadAndShowInterstitial(R.string.admob_interstitial_install)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {

                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {

                    }
                });
    }
}
