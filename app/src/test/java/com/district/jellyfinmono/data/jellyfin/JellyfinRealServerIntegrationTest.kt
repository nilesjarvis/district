package com.district.jellyfinmono.data.jellyfin

import com.district.jellyfinmono.core.network.FixedDeviceIdProvider
import com.district.jellyfinmono.core.network.TestDispatcherProvider
import com.district.jellyfinmono.domain.DistrictResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class JellyfinRealServerIntegrationTest {
    @Test
    fun realServerSmokeWhenEnvironmentIsConfigured() = runTest {
        val url = System.getenv("JELLYFIN_TEST_URL").orEmpty()
        val username = System.getenv("JELLYFIN_TEST_USERNAME").orEmpty()
        val password = System.getenv("JELLYFIN_TEST_PASSWORD").orEmpty()
        assumeTrue(
            "Set JELLYFIN_TEST_URL, JELLYFIN_TEST_USERNAME, and JELLYFIN_TEST_PASSWORD to run real Jellyfin checks.",
            url.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
        )
        val repository = DefaultJellyfinRepository(
            api = OkHttpJellyfinApi(
                client = OkHttpClient.Builder()
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build(),
                dispatchers = TestDispatcherProvider(Dispatchers.IO),
            ),
            deviceIdProvider = FixedDeviceIdProvider("district-real-server-test"),
        )

        val info = repository.checkServer(url)
        assertTrue(info is DistrictResult.Success)

        val session = repository.authenticate(url, username, password)
        assertTrue(session is DistrictResult.Success)
        session as DistrictResult.Success

        val libraries = repository.libraries(session.value)
        assertTrue(libraries is DistrictResult.Success)
        libraries as DistrictResult.Success
        assertFalse("Expected at least one Jellyfin library", libraries.value.isEmpty())

        val albums = repository.albums(session.value, libraries.value.firstOrNull { it.collectionType == "music" }?.id)
        assertTrue(albums is DistrictResult.Success)

        val search = repository.search(session.value, "a")
        assertTrue(search is DistrictResult.Success)
    }
}
