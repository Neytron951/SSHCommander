package com.neytron.sshcommander

import android.content.Context
import android.util.Log
import com.yandex.mobile.ads.common.YandexAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.*
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
        YandexAds.enableLogging(true)
        YandexAds.initialize(context) {
            Log.d("AdsSdk", "Yandex Ads SDK initialization CALLBACK reached")
            _isInitialized.value = true
        }
        
        // Резервный механизм: если через 7 секунд коллбэка нет, разрешаем загрузку
        // (бывает при проблемах с сетью при первом запуске)
        GlobalScope.launch(Dispatchers.Main) {
            delay(7000)
            if (!_isInitialized.value) {
                Log.w("AdsSdk", "Initialization timeout reached! Forcing _isInitialized = true")
                _isInitialized.value = true
            }
        }
    }
}
