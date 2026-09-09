package com.haber73.candivo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.UserMessagingPlatform;

import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final String TAG = "Candivo";
    private static final int RC_GOOGLE = 7301;
    private WebView webView;
    private GoogleSignInClient googleClient;
    private ConsentInformation consentInformation;
    private boolean adsInitialized = false;
    private RewardedAd rewardedAd;
    private InterstitialAd interstitialAd;
    private String pendingRewardType = "";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        setupGoogleSignIn();
        setupWebView();
        requestConsentAndInitializeAds();
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.server_client_id)).requestEmail().build();
        googleClient = GoogleSignIn.getClient(this, options);
    }

    private void setupWebView() {
        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            s.setAllowFileAccessFromFileURLs(true);
            s.setAllowUniversalAccessFromFileURLs(true);
        }
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return openExternalIfNeeded(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return openExternalIfNeeded(Uri.parse(url));
            }
        });
        webView.addJavascriptInterface(new AndroidBridge(), "CandivoAndroid");
        webView.loadUrl("file:///android_asset/index.html");
    }

    private boolean openExternalIfNeeded(Uri uri) {
        if (uri == null) return false;
        String scheme = uri.getScheme();
        if ("file".equalsIgnoreCase(scheme) || "about".equalsIgnoreCase(scheme)) return false;
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme) || "mailto".equalsIgnoreCase(scheme)) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) {}
            return true;
        }
        return false;
    }

    private void requestConsentAndInitializeAds() {
        consentInformation = UserMessagingPlatform.getConsentInformation(this);
        ConsentRequestParameters params = new ConsentRequestParameters.Builder().build();
        consentInformation.requestConsentInfoUpdate(this, params,
                () -> {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(this, formError -> {
                        if (formError != null) js("window.CandivoAds&&window.CandivoAds.onConsentError(" + q(formError.getMessage()) + ")");
                        if (consentInformation.canRequestAds()) initializeAdsOnce();
                    });
                    if (consentInformation.canRequestAds()) initializeAdsOnce();
                },
                requestError -> {
                    js("window.CandivoAds&&window.CandivoAds.onConsentError(" + q(requestError.getMessage()) + ")");
                    if (consentInformation.canRequestAds()) initializeAdsOnce();
                });
    }

    private synchronized void initializeAdsOnce() {
        if (adsInitialized) return;
        adsInitialized = true;
        MobileAds.initialize(this, status -> runOnUiThread(() -> {
            loadRewarded();
            loadInterstitial();
        }));
    }

    private void loadRewarded() {
        if (!adsInitialized || rewardedAd != null) return;
        RewardedAd.load(this, BuildConfig.ADMOB_REWARDED_ID, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override public void onAdLoaded(RewardedAd ad) { rewardedAd = ad; }
            @Override public void onAdFailedToLoad(LoadAdError error) { rewardedAd = null; Log.w(TAG, "Rewarded load: " + error); }
        });
    }

    private void showRewardedInternal(String rewardType) {
        if (!adsInitialized) { jsRewardError("Reklam izni hazırlanıyor. Biraz sonra tekrar dene."); return; }
        if (rewardedAd == null) { loadRewarded(); jsRewardError("Ödüllü reklam henüz hazır değil. Biraz sonra tekrar dene."); return; }
        pendingRewardType = rewardType == null ? "" : rewardType;
        RewardedAd ad = rewardedAd; rewardedAd = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() { pendingRewardType = ""; js("window.CandivoAds&&window.CandivoAds.onRewardClosed()"); loadRewarded(); }
            @Override public void onAdFailedToShowFullScreenContent(AdError error) { pendingRewardType = ""; jsRewardError("Reklam gösterilemedi."); loadRewarded(); }
        });
        ad.show(this, reward -> {
            String kind = pendingRewardType;
            js("window.CandivoAds&&window.CandivoAds.onRewardEarned(" + q(kind) + ")");
        });
    }

    private void loadInterstitial() {
        if (!adsInitialized || interstitialAd != null) return;
        InterstitialAd.load(this, BuildConfig.ADMOB_INTERSTITIAL_ID, new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
            @Override public void onAdLoaded(InterstitialAd ad) { interstitialAd = ad; }
            @Override public void onAdFailedToLoad(LoadAdError error) { interstitialAd = null; Log.w(TAG, "Interstitial load: " + error); }
        });
    }

    private void showInterstitialInternal() {
        if (!adsInitialized) return;
        if (interstitialAd == null) { loadInterstitial(); return; }
        InterstitialAd ad = interstitialAd; interstitialAd = null;
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override public void onAdDismissedFullScreenContent() { loadInterstitial(); }
            @Override public void onAdFailedToShowFullScreenContent(AdError error) { loadInterstitial(); }
        });
        ad.show(this);
    }

    private void showPrivacyOptionsInternal() {
        if (consentInformation == null) { requestConsentAndInitializeAds(); return; }
        UserMessagingPlatform.showPrivacyOptionsForm(this, formError -> {
            if (formError != null) js("window.CandivoAds&&window.CandivoAds.onConsentError(" + q(formError.getMessage()) + ")");
            if (consentInformation.canRequestAds()) initializeAdsOnce();
        });
    }

    private void startGoogleSignIn() { startActivityForResult(googleClient.getSignInIntent(), RC_GOOGLE); }
    private void googleSignOut() { googleClient.signOut(); }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != RC_GOOGLE) return;
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            String token = account.getIdToken();
            if (token == null || token.isEmpty()) throw new IllegalStateException("Google kimlik belirteci alınamadı.");
            js("window.CandivoNativeAuth&&window.CandivoNativeAuth.onGoogleIdToken(" + q(token) + ")");
        } catch (Exception error) {
            js("window.CandivoNativeAuth&&window.CandivoNativeAuth.onGoogleError(" + q(error.getMessage()) + ")");
        }
    }

    private void jsRewardError(String message) { js("window.CandivoAds&&window.CandivoAds.onRewardError(" + q(message) + ")"); }
    private void js(String script) { runOnUiThread(() -> { if (webView != null) webView.evaluateJavascript(script, null); }); }
    private static String q(String value) { return JSONObject.quote(value == null ? "" : value); }

    public final class AndroidBridge {
        @JavascriptInterface public void googleSignIn() { runOnUiThread(MainActivity.this::startGoogleSignIn); }
        @JavascriptInterface public void googleSignOut() { runOnUiThread(MainActivity.this::googleSignOut); }
        @JavascriptInterface public void showRewarded(String rewardType) { runOnUiThread(() -> showRewardedInternal(rewardType)); }
        @JavascriptInterface public void showInterstitial(String reason) { runOnUiThread(MainActivity.this::showInterstitialInternal); }
        @JavascriptInterface public void openPrivacyOptions() { runOnUiThread(MainActivity.this::showPrivacyOptionsInternal); }
        @JavascriptInterface public boolean isAdMobTestMode() { return BuildConfig.ADMOB_TEST_MODE; }
    }

    @Override protected void onDestroy() {
        if (webView != null) { webView.removeJavascriptInterface("CandivoAndroid"); webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
