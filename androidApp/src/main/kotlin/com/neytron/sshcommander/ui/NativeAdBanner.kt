package com.neytron.sshcommander.ui

import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.neytron.sshcommander.AdsSdk
import com.neytron.sshcommander.R
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestError
import com.yandex.mobile.ads.nativeads.MediaView
import com.yandex.mobile.ads.nativeads.NativeAd
import com.yandex.mobile.ads.nativeads.NativeAdLoadListener
import com.yandex.mobile.ads.nativeads.NativeAdLoader
import com.yandex.mobile.ads.nativeads.NativeAdView
import com.yandex.mobile.ads.nativeads.NativeAdViewBinder
import kotlinx.coroutines.flow.first

private const val TAG = "NativeAdBanner"

@Composable
fun NativeAdBanner(
    blockId: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var nativeAd by remember(blockId) {
        mutableStateOf<NativeAd?>(null)
    }

    val adLoader = remember(blockId) {
        NativeAdLoader(context)
    }

    LaunchedEffect(blockId) {
        // Wait until the Yandex Mobile Ads SDK has fully initialized,
        // otherwise the ad block config may not be loaded yet.
        AdsSdk.isInitialized.first { it }

        Log.d(TAG, "Loading native ad. blockId=$blockId")

        val adRequest = AdRequest.Builder(blockId).build()

        adLoader.loadAd(
            adRequest,
            object : NativeAdLoadListener {
                override fun onAdLoaded(ad: NativeAd) {
                    Log.d(TAG, "Native ad loaded successfully")
                    nativeAd = ad
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    Log.e(
                        TAG,
                        "Native ad failed to load: " +
                            "code=${error.code}, description=${error.description}, adUnitId=${error.adUnitId}"
                    )
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
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                LayoutInflater
                    .from(ctx)
                    .inflate(
                        R.layout.native_ad_view,
                        null,
                        false
                    ) as NativeAdView
            },
            update = { adView ->
                val title = adView.findViewById<TextView>(R.id.title)
                val body = adView.findViewById<TextView>(R.id.body)
                val domain = adView.findViewById<TextView>(R.id.domain)
                val callToAction = adView.findViewById<Button>(R.id.call_to_action)
                val icon = adView.findViewById<ImageView>(R.id.icon)
                val feedback = adView.findViewById<ImageView>(R.id.feedback)
                val media = adView.findViewById<MediaView>(R.id.media)
                val sponsored = adView.findViewById<TextView>(R.id.sponsored)
                val warning = adView.findViewById<TextView>(R.id.warning)

                val binder = NativeAdViewBinder.Builder(adView)
                    .setTitleView(title)
                    .setBodyView(body)
                    .setDomainView(domain)
                    .setCallToActionView(callToAction)
                    .setIconView(icon)
                    .setFeedbackView(feedback)
                    .setMediaView(media)
                    .setSponsoredView(sponsored)
                    .setWarningView(warning)
                    .build()

                try {
                    val result = ad.bindNativeAd(binder)
                    Log.d(TAG, "Native ad binding result: $result")
                    Log.d(TAG, "Native ad bound successfully")
                } catch (exception: Exception) {
                    Log.e(TAG, "Native ad binding failed", exception)
                }
            }
        )
    }
}
