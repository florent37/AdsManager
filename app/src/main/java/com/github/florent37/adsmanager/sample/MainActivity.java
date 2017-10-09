package com.github.florent37.adsmanager.sample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.github.florent37.adsmanager.AdsManager;
import com.google.android.gms.ads.AdSize;

public class MainActivity extends AppCompatActivity {

    AdsManager adsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ViewGroup adContainer = findViewById(R.id.adContainer);

        adsManager = ((MainApplication) getApplicationContext()).getAdsManager();

        adsManager.insertAdView(this, adContainer, R.string.admob_footer, AdSize.BANNER);
    }
}
