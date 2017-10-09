package com.github.florent37.adsmanager;

import android.app.Application;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.Nullable;
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
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class AdsManager {

    private static final String TAG = "AdsManager";
    private static boolean showAdsOnDebug = true;
    private final boolean debug;
    private final Application application;
    private final Subject<AdsEvent> eventSubject = PublishSubject.create();
    private final Subject<AdsVideoEvent> eventVideoSubject = PublishSubject.create();
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

    public static void showAdsOnDebug(boolean showAdsOnDebug) {
        AdsManager.showAdsOnDebug = showAdsOnDebug;
    }

    public void setEnableLogs(boolean enableLogs) {
        this.enableLogs = enableLogs;
    }

    public void setAdInvisibilityOnDebug(int adVisibilityOnDebug) {
        this.adInvisibilityOnDebug = adVisibilityOnDebug;
    }

    private AdsManager showInterstitial(final int stringId, final AdClosedListener adCloseListener) {
        if (debug && !showAdsOnDebug) {
            adCloseListener.onAdClosed();
        } else {
            final String string = application.getString(stringId);

            log("showInterstitial push event " + string);

            eventSubject.onNext(new AdsEvent(string, adCloseListener));
        }
        return this;
    }

    private AdsManager showRewardedVideo(final int stringId, final AdVideoClosedListener adVideoClosedListener) {
        if (!showAdsOnDebug && debug) {
            adVideoClosedListener.onAdClosedWithReward();
        } else {
            eventVideoSubject.onNext(new AdsVideoEvent(application.getString(stringId), adVideoClosedListener));
        }
        return this;
    }

    public Single<Boolean> loadInterstitial(final int stringId) {
        return loadInterstitial(application.getString(stringId));
    }

    public Single<Boolean> loadAndShowInterstitial(final int stringId) {
        if (!showAdsOnDebug && debug) {
            return Single.just(true);
        } else {
            return loadInterstitial(application.getString(stringId))
                    .flatMap(new Function<Boolean, SingleSource<? extends Boolean>>() {
                        @Override
                        public SingleSource<? extends Boolean> apply(@NonNull Boolean ok) throws Exception {
                            AdsManager.this.log("loadAndShowInterstitial " + ok);
                            if (ok) return AdsManager.this.showInterstitial(stringId);
                            else return Single.just(ok);
                        }
                    });
        }
    }

    private void log(String text) {
        if (enableLogs) {
            Log.d(TAG, text);
        }
    }

    private Single<Boolean> loadInterstitial(final String id) {

        log("loadInterstitial " + id);

        return Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull final SingleEmitter<Boolean> e) throws Exception {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        final InterstitialAd interstitialAd = new InterstitialAd(application);
                        final AdRequest.Builder adRequestBuilder = new AdRequest.Builder();
                        adRequestBuilder.addTestDevice(AdRequest.DEVICE_ID_EMULATOR);
                        if (debug) {
                            adRequestBuilder.addTestDevice(DeviceIdFounder.getDeviceId(application));
                        }
                        interstitialAd.setAdUnitId(id);

                        interstitialAd.setAdListener(new AdListener() {

                            @Nullable
                            private AdClosedListener adClosedListener;

                            @Override
                            public void onAdLoaded() {
                                super.onAdLoaded();
                                log("onAdLoaded");

                                log("wait event " + id);

                                eventSubject
                                        //on attend un event "show(id)"
                                        .filter(new Predicate<AdsEvent>() {
                                            @Override
                                            public boolean test(@NonNull AdsEvent adsEvent) throws Exception {
                                                return adsEvent.getAdId().equals(id);
                                            }
                                        })
                                        .doOnNext(new Consumer<AdsEvent>() {
                                            @Override
                                            public void accept(@NonNull AdsEvent adsEvent) throws Exception {
                                                log("eventSubject");
                                                adClosedListener = adsEvent.getAdCloseListener();
                                            }
                                        })
                                        .subscribeOn(Schedulers.newThread())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(new Consumer<AdsEvent>() {
                                            @Override
                                            public void accept(@NonNull AdsEvent adsEvent) throws Exception {
                                                interstitialAd.show();
                                            }
                                        });
                                e.onSuccess(true);
                            }

                            @Override
                            public void onAdFailedToLoad(int i) {
                                super.onAdFailedToLoad(i);
                                log("onAdFailedToLoad " + i);
                                e.onSuccess(false);
                                if (adClosedListener != null) {
                                    adClosedListener.onAdClosed();
                                    loadInterstitial(id).subscribe();
                                }
                            }

                            @Override
                            public void onAdClosed() {
                                super.onAdClosed();
                                log("onAdClosed");
                                if (adClosedListener != null) {
                                    adClosedListener.onAdClosed();
                                }
                                loadInterstitial(id).subscribe();
                            }
                        });
                        interstitialAd.loadAd(adRequestBuilder.build());
                    }
                });
            }
        });
    }

    public Single<Boolean> showInterstitial(final int stringId) {
        return Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull final SingleEmitter<Boolean> e) throws Exception {
                new Handler(Looper.getMainLooper()).postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                AdsManager.this.showInterstitial(stringId, new AdClosedListener() {
                                    @Override
                                    public void onAdClosed() {
                                        AdsManager.this.log("showInterstitial finished");
                                        e.onSuccess(true);
                                    }
                                });
                            }
                        }, 100
                );
            }
        });
    }

    public Single<Boolean> showRewardedVideo(final int stringId) {
        return Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull final SingleEmitter<Boolean> e) throws Exception {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        AdsManager.this.showRewardedVideo(stringId, new AdVideoClosedListener() {
                            @Override
                            public void onAdClosedWithReward() {
                                e.onSuccess(true);
                            }

                            @Override
                            public void onAdClosedWithoutReward() {
                                e.onSuccess(false);
                            }
                        });
                    }
                });
            }
        });
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

    private Single<Boolean> loadRewardedVideo(final int stringId) {
        return loadRewardedVideo(application.getString(stringId));
    }

    private Single<Boolean> loadRewardedVideo(final String id) {
        return Single.create(new SingleOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull final SingleEmitter<Boolean> e) throws Exception {
                final RewardedVideoAd mAd = MobileAds.getRewardedVideoAdInstance(application);
                mAd.setRewardedVideoAdListener(new RewardedVideoAdListener() {

                    boolean rewarded = false;

                    @Nullable
                    private AdVideoClosedListener adVideoClosedListener;

                    @Override
                    public void onRewardedVideoAdLoaded() {
                        eventVideoSubject
                                .filter(new Predicate<AdsVideoEvent>() {
                                    @Override
                                    public boolean test(@NonNull AdsVideoEvent adsVideoEvent) throws Exception {
                                        return adsVideoEvent.getAdId().equals(id);
                                    }
                                })
                                .doOnNext(new Consumer<AdsVideoEvent>() {
                                    @Override
                                    public void accept(@NonNull AdsVideoEvent adsVideoEvent) throws Exception {
                                        adVideoClosedListener = adsVideoEvent.getAdVideoClosedListener();
                                    }
                                })
                                .subscribeOn(Schedulers.newThread())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Consumer<AdsVideoEvent>() {
                                    @Override
                                    public void accept(@NonNull AdsVideoEvent adsVideoEvent) throws Exception {
                                        mAd.show();
                                    }
                                });
                        e.onSuccess(true);
                    }

                    @Override
                    public void onRewardedVideoAdOpened() {

                    }

                    @Override
                    public void onRewardedVideoStarted() {

                    }

                    @Override
                    public void onRewardedVideoAdClosed() {
                        if (adVideoClosedListener != null) {
                            if (rewarded) {
                                adVideoClosedListener.onAdClosedWithReward();
                            } else {
                                adVideoClosedListener.onAdClosedWithoutReward();
                            }

                            loadRewardedVideo(id);
                        }
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
                        if (adVideoClosedListener != null) {
                            adVideoClosedListener.onAdClosedWithoutReward();
                        }
                        loadRewardedVideo(id);
                        e.onSuccess(false);
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

    public interface AdClosedListener {
        void onAdClosed();
    }

    public interface AdVideoClosedListener {
        void onAdClosedWithReward();

        void onAdClosedWithoutReward();
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

    private class AdsEvent {
        private final String adId;
        private final AdClosedListener adCloseListener;

        public AdsEvent(String adId, AdClosedListener adCloseListener) {
            this.adId = adId;
            this.adCloseListener = adCloseListener;
        }

        public String getAdId() {
            return adId;
        }

        public AdClosedListener getAdCloseListener() {
            return adCloseListener;
        }
    }

    private class AdsVideoEvent {
        private final String adId;
        private final AdVideoClosedListener adVideoClosedListener;

        public AdsVideoEvent(String adId, AdVideoClosedListener adVideoClosedListener) {
            this.adId = adId;
            this.adVideoClosedListener = adVideoClosedListener;
        }

        public String getAdId() {
            return adId;
        }

        public AdVideoClosedListener getAdVideoClosedListener() {
            return adVideoClosedListener;
        }
    }

    private class AdError extends Throwable {

    }

}
