package com.neytron.sshcommander

import android.content.Context
import com.yandex.mobile.ads.common.YandexAds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tracks the state of the Yandex Mobile Ads SDK initialization so that
 * ad loading can be deferred until the SDK is fully ready.
 */
object AdsSdk {

    private val _isInitialized = MutableStateFlow(false)

    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    fun initialize(context: Context) {
        if (_isInitialized.value) {
            android.util.Log.d("AdsSdk", "Already initialized")
            return
        }

        android.util.Log.d("AdsSdk", "Initializing Yandex Ads SDK...")
        YandexAds.initialize(context) {
            android.util.Log.d("AdsSdk", "Yandex Ads SDK initialization complete")
            _isInitialized.value = true
        }
    }
}
