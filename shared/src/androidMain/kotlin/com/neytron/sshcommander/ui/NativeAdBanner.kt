package com.neytron.sshcommander.ui

import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.neytron.sshcommander.AdsSdk
import com.neytron.sshcommander.shared.R
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.nativeads.MediaView
import com.yandex.mobile.ads.nativeads.NativeAd
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener
import com.yandex.mobile.ads.nativeads.NativeAdLoader
import com.yandex.mobile.ads.nativeads.NativeAdView
import com.yandex.mobile.ads.nativeads.NativeAdViewBinder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

private const val TAG = "NativeAdBanner"

@Composable
fun NativeAdBanner(
    blockId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nativeAd by remember(blockId) { mutableStateOf<NativeAd?>(null) }

    val adLoader = remember(blockId) { NativeAdLoader(context) }

    LaunchedEffect(blockId) {
        // Ждем инициализации SDK
        AdsSdk.isInitialized.first { it }
        
        // Даем SDK 500мс "продышаться" после инициализации
        delay(500)

        val cleanBlockId = blockId.trim()
        Log.d(TAG, "Requesting ad for ID: '$cleanBlockId'")

        val adRequest = AdRequest.Builder(cleanBlockId).build()

        adLoader.loadAd(
            adRequest,
            object : NativeAdLoadListener {
                override fun onAdLoaded(ad: NativeAd) {
                    Log.d(TAG, "Ad loaded successfully!")
                    nativeAd = ad
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    Log.e(TAG, "Ad failed to load: code=${error.code}, desc=${error.description}, unitId=${error.adUnitId}")
                    nativeAd = null
                }
            }
        )
    }

    val ad = nativeAd ?: return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                LayoutInflater.from(ctx).inflate(R.layout.native_ad_view, null, false) as NativeAdView
            },
            update = { adView ->
                val binder = NativeAdViewBinder.Builder(adView)
                    .setTitleView(adView.findViewById(R.id.title))
                    .setBodyView(adView.findViewById(R.id.body))
                    .setDomainView(adView.findViewById(R.id.domain))
                    .setCallToActionView(adView.findViewById(R.id.call_to_action))
                    .setIconView(adView.findViewById(R.id.icon))
                    .setFeedbackView(adView.findViewById(R.id.feedback))
                    .setMediaView(adView.findViewById(R.id.media))
                    .setSponsoredView(adView.findViewById(R.id.sponsored))
                    .setWarningView(adView.findViewById(R.id.warning))
                    .build()

                try {
                    ad.bindNativeAd(binder)
                } catch (e: Exception) {
                    Log.e(TAG, "Binding failed", e)
                }
            }
        )
    }
}
