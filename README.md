# AdsManager

<a href="https://goo.gl/WXW8Dc">
  <img alt="Android app on Google Play" src="https://developer.android.com/images/brand/en_app_rgb_wo_45.png" />
</a>

# Download

<a href='https://ko-fi.com/A160LCC' target='_blank'><img height='36' style='border:0px;height:36px;' src='https://az743702.vo.msecnd.net/cdn/kofi1.png?v=0' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>

[ ![Download](https://api.bintray.com/packages/florent37/maven/adsmanager/images/download.svg) ](https://bintray.com/florent37/maven/adsmanager/_latestVersion)
```java
dependencies {
    compile 'com.github.florent37:adsmanager:1.0.2'
}
```

# Application

```java
final AdsManager adsManager = new AdsManager(this, R.string.admob_app, BuildConfig.DEBUG);
adsManager.showAdsOnDebug(true);
adsManager.setAdInvisibilityOnDebug(View.INVISIBLE);
```

# Banner

On activity
```java
adsManager.insertAdView(this, adContainer, R.string.admob_footer, AdSize.BANNER);
```

# Interstitial

```java
adsManager.loadAndShowInterstitial(R.string.admob_interstitial_install)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        //closed
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        //error
                    }
                });
```


