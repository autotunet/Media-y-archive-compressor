package com.example.core.ads

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdMobManager {

    private const val TAG = "AdMobManager"

    // User-provided AdMob Ad Unit IDs
    val BANNER_AD_UNIT_IDS = listOf(
        "ca-app-pub-1495262574338316/2803163788", // 1
        "ca-app-pub-1495262574338316/2903409205"  // 2
    )

    val INTERSTITIAL_AD_UNIT_IDS = listOf(
        "ca-app-pub-1495262574338316/2452733430", // 3
        "ca-app-pub-1495262574338316/8970040322", // 4
        "ca-app-pub-1495262574338316/1182179137", // 5
        "ca-app-pub-1495262574338316/4962596335", // 6
        "ca-app-pub-1495262574338316/2714752655", // 7
        "ca-app-pub-1495262574338316/3503687777"  // 8
    )

    // Standard Google test IDs for safe fallback / emulator verification
    const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    private var currentInterstitialIndex = 0
    private var currentBannerIndex = 0

    private var interstitialAd: InterstitialAd? = null
    private var isAdLoading = false
    private var isInitialized = false

    fun isRunningOnEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
            || Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || Build.PRODUCT.contains("sdk_google")
            || Build.PRODUCT.contains("google_sdk")
            || Build.PRODUCT.contains("sdk")
            || Build.PRODUCT.contains("emulator")
    }

    fun initialize(context: Context) {
        if (isInitialized) return
        if (isRunningOnEmulator()) {
            Log.d(TAG, "Entorno de emulador detectado: AdMob inicializado en modo simulado para evitar errores de Chromium WebView.")
            isInitialized = true
            return
        }

        try {
            val configuration = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(configuration)

            MobileAds.initialize(context) { status ->
                Log.d(TAG, "AdMob inicializado en dispositivo físico: $status")
                isInitialized = true
                loadInterstitial(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al inicializar AdMob: ${e.message}")
        }
    }

    fun getNextBannerAdUnitId(): String {
        val id = BANNER_AD_UNIT_IDS[currentBannerIndex % BANNER_AD_UNIT_IDS.size]
        currentBannerIndex++
        return id
    }

    fun getEffectiveBannerAdUnitId(): String {
        return if (isRunningOnEmulator()) {
            TEST_BANNER_ID
        } else {
            getNextBannerAdUnitId()
        }
    }

    fun loadInterstitial(context: Context) {
        if (isRunningOnEmulator() || isAdLoading || interstitialAd != null) return

        isAdLoading = true
        val adUnitId = INTERSTITIAL_AD_UNIT_IDS[currentInterstitialIndex % INTERSTITIAL_AD_UNIT_IDS.size]
        currentInterstitialIndex++

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isAdLoading = false
                    Log.d(TAG, "Anuncio intersticial cargado con ID: $adUnitId")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isAdLoading = false
                    Log.w(TAG, "Fallo al cargar anuncio intersticial ($adUnitId): ${error.message}.")
                    if (adUnitId != TEST_INTERSTITIAL_ID) {
                        tryLoadTestInterstitial(context)
                    }
                }
            }
        )
    }

    private fun tryLoadTestInterstitial(context: Context) {
        if (isRunningOnEmulator()) return
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            TEST_INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Anuncio de prueba cargado correctamente.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.d(TAG, "No hay anuncios disponibles en este momento: ${error.message}")
                }
            }
        )
    }

    /**
     * Shows interstitial ad when a key task finishes (e.g. video/audio compressed,
     * or archive created/extracted). Adheres to Google Play's interstitial policy.
     */
    fun showInterstitial(activity: Activity, onCompleted: (() -> Unit)? = null) {
        if (isRunningOnEmulator()) {
            // In emulator environment, proceed immediately without triggering Chromium WebView
            onCompleted?.invoke()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onCompleted?.invoke()
                }

                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onCompleted?.invoke()
                }
            }
            ad.show(activity)
        } else {
            // Ad was not ready, continue operation seamlessly
            loadInterstitial(activity)
            onCompleted?.invoke()
        }
    }
}
