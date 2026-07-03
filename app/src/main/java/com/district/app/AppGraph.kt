package com.district.app

import android.content.Context
import com.district.core.network.AndroidDeviceIdProvider
import com.district.core.network.DefaultDispatcherProvider
import com.district.core.media.Media3PlaybackController
import com.district.core.persistence.AndroidSecureSessionStore
import com.district.core.persistence.SharedPreferencesPlaybackStore
import com.district.data.jellyfin.DefaultJellyfinRepository
import com.district.data.jellyfin.OkHttpJellyfinApi
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppGraph(context: Context) {
    private val appContext = context.applicationContext
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val sessionStore = AndroidSecureSessionStore(appContext)
    val playbackStore = SharedPreferencesPlaybackStore(appContext)
    val playbackController = Media3PlaybackController(appContext)
    val jellyfinRepository = DefaultJellyfinRepository(
        api = OkHttpJellyfinApi(httpClient, DefaultDispatcherProvider),
        deviceIdProvider = AndroidDeviceIdProvider(appContext),
    )

    fun appViewModel(): AppViewModel =
        AppViewModel(
            repository = jellyfinRepository,
            sessionStore = sessionStore,
            playbackStore = playbackStore,
            playbackController = playbackController,
        )
}
