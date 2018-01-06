package com.github.florent37.adsmanager.sample;

import android.app.Application;
import android.view.View;

import com.github.florent37.adsmanager.AdsManager;

public class MainApplication extends Application {

    private AdsManager adsManager;

    @Override
    public void onCreate() {
        super.onCreate();
        adsManager = new AdsManager(this, R.string.admob_app, BuildConfig.DEBUG);
        adsManager.showAdsOnDebug(true);
        adsManager.setAdInvisibilityOnDebug(View.INVISIBLE);
    }

    public AdsManager getAdsManager() {
        return adsManager;
    }
}
