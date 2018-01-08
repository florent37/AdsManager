package com.github.florent37.adsmanager;

import android.app.Application;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.provider.Settings;
import android.support.annotation.StringRes;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import florent37.github.com.rxlifecycle.RxLifecycle;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public class AdsManager {

    private static final String TAG = "AdsManager";
    private static boolean showAdsOnDebug = true;
    private final boolean debug;
    private final Application application;
    protected List<AdView> adViewList = new ArrayList<>();
    private boolean enableLogs = true;
    private int adInvisibilityOnDebug = View.INVISIBLE;

    public AdsManager(Application application, String admobApp, boolean debug) {
        this.application = application;
        this.debug = debug;
        MobileAds.initialize(application, admobApp);
    }

    public AdsManager(Application application, @StringRes int admobApp, boolean debug) {
        this(application, application.getString(admobApp), debug);
    }

    public AdsManager showAdsOnDebug(boolean showAdsOnDebug) {
        showAdsOnDebug = showAdsOnDebug;
        return this;
    }

    public void setEnableLogs(boolean enableLogs) {
        this.enableLogs = enableLogs;
    }

    public void setAdInvisibilityOnDebug(int adVisibilityOnDebug) {
        this.adInvisibilityOnDebug = adVisibilityOnDebug;
    }

    public Single<Boolean> loadAndShowInterstitial(final int id) {
        if (!showAdsOnDebug && debug) {
            return Single.just(true);
        } else {
            return Single.create(new SingleOnSubscribe<Boolean>() {
                @Override
                public void subscribe(final SingleEmitter<Boolean> e) throws Exception {
                    final InterstitialAd interstitialAd = new InterstitialAd(application);
                    final AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
                    adRequestBuilder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
                    if (debug) {
                        adRequestBuilder.addTestDevice(DeviceIdFounder.getDeviceId(application));
                    }
                    interstitialAd.setAdUnitId(application.getString(id));

                    interstitialAd.setAdListener(new AdListener() {

                        @Override
                        public void onAdLoaded() {
                            super.onAdLoaded();
                            interstitialAd.show();
                        }

                        @Override
                        public void onAdFailedToLoad(int i) {
                            super.onAdFailedToLoad(i);
                            log("onAdFailedToLoad " + i);
                            e.onError(new AdError());
                        }

                        @Override
                        public void onAdClosed() {
                            super.onAdClosed();
                            log("onAdClosed");
                            e.onSuccess(true);
                        }
                    });
                    interstitialAd.loadAd(adRequestBuilder.build());
                }
            })
                    .subscribeOn(AndroidSchedulers.mainThread());
        }
    }

    private void log(String text) {
        if (enableLogs) {
            Log.d(TAG, text);
        }
    }

    public void executeAdView(final LifecycleOwner lifecycleOwner, final AdView adView) {
        final CompositeDisposable compositeDisposable = new CompositeDisposable();

        RxLifecycle.with(lifecycleOwner)
                .onResume()
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable d) throws Exception {
                        compositeDisposable.add(d);
                    }
                })
                .subscribe(new Consumer<Lifecycle.Event>() {
                    @Override
                    public void accept(@NonNull Lifecycle.Event resume) throws Exception {
                        adView.resume();
                    }
                });

        RxLifecycle.with(lifecycleOwner)
                .onPause()
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable d) throws Exception {
                        compositeDisposable.add(d);
                    }
                })
                .subscribe(new Consumer<Lifecycle.Event>() {
                    @Override
                    public void accept(@NonNull Lifecycle.Event resume) throws Exception {
                        adView.pause();
                    }
                });

        RxLifecycle.with(lifecycleOwner)
                .onDestroy()
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable d) throws Exception {
                        compositeDisposable.add(d);
                    }
                })
                .subscribe(new Consumer<Lifecycle.Event>() {
                    @Override
                    public void accept(@NonNull Lifecycle.Event resume) throws Exception {
                        adView.destroy();
                        compositeDisposable.clear();
                    }
                });

        if (debug && !showAdsOnDebug) {
            adView.setVisibility(adInvisibilityOnDebug);
        } else {
            final AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
            if (debug) {
                adRequestBuilder.addTestDevice(DeviceIdFounder.getDeviceId(application));
            }
            adView.loadAd(adRequestBuilder.build());
            adViewList.add(adView);
        }
    }

    private Single<Boolean> loadAndshowRewardedVideo(final String id) {
        return Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull final SingleEmitter<Boolean> e) throws Exception {
                final RewardedVideoAd mAd = MobileAds.getRewardedVideoAdInstance(application);
                mAd.setRewardedVideoAdListener(new RewardedVideoAdListener() {

                    boolean rewarded = false;

                    @Override
                    public void onRewardedVideoAdLoaded() {
                        mAd.show();
                    }

                    @Override
                    public void onRewardedVideoAdOpened() {

                    }

                    @Override
                    public void onRewardedVideoStarted() {

                    }

                    @Override
                    public void onRewardedVideoAdClosed() {
                        e.onSuccess(rewarded);
                    }

                    @Override
                    public void onRewarded(RewardItem rewardItem) {
                        rewarded = true;
                    }

                    @Override
                    public void onRewardedVideoAdLeftApplication() {

                    }

                    @Override
                    public void onRewardedVideoAdFailedToLoad(int i) {
                        e.onError(new AdError());
                    }
                });

                final AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
                adRequestBuilder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
                if (debug) {
                    adRequestBuilder.addTestDevice(DeviceIdFounder.getDeviceId(application));
                }

                mAd.loadAd(id, adRequestBuilder.build());
            }
        });
    }

    public void insertAdView(LifecycleOwner lifecycleOwner, ViewGroup adContainer, String adUnitId, AdSize adSize) {
        final AdView adView = new AdView(adContainer.getContext());
        adView.setAdSize(adSize);
        adView.setAdUnitId(adUnitId);
        adContainer.addView(adView);
        executeAdView(lifecycleOwner, adView);
    }

    public void insertAdView(LifecycleOwner lifecycleOwner, ViewGroup adContainer, @StringRes int adUnitId, AdSize adSize) {
        insertAdView(lifecycleOwner, adContainer, application.getString(adUnitId), adSize);
    }

    public static class DeviceIdFounder {
        public static String getDeviceId(Context context) {
            final String android_id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            return md5(android_id).toUpperCase();
        }

        public static final String md5(final String s) {
            try {
                // Create MD5 Hash
                MessageDigest digest = MessageDigest
                        .getInstance("MD5");
                digest.update(s.getBytes());
                byte messageDigest[] = digest.digest();

                // Create Hex String
                StringBuffer hexString = new StringBuffer();
                for (int i = 0; i < messageDigest.length; i++) {
                    String h = Integer.toHexString(0xFF & messageDigest[i]);
                    while (h.length() < 2)
                        h = "0" + h;
                    hexString.append(h);
                }
                return hexString.toString();

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return "";
        }
    }

    private class AdError extends Throwable {

    }

}
