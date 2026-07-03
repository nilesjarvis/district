package com.district.feature.onboarding

import com.district.domain.DistrictError
import com.district.domain.MusicLibrary
import com.district.domain.ServerInfo

enum class OnboardingStep {
    Welcome,
    Server,
    SignIn,
    Connected,
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val serverAddress: String = "",
    val port: String = "8096",
    val protocol: String = "http",
    val username: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val rememberDevice: Boolean = true,
    val isLoading: Boolean = false,
    val serverInfo: ServerInfo? = null,
    val libraries: List<MusicLibrary> = emptyList(),
    val error: DistrictError? = null,
) {
    val serverUrl: String
        get() {
            val value = serverAddress.trim().trimEnd('/')
            if (value.startsWith("http://") || value.startsWith("https://")) {
                return value
            }
            val hostHasPort = value.substringAfterLast('/').contains(':')
            val suffix = if (port.isBlank() || hostHasPort) "" else ":${port.trim()}"
            return "$protocol://$value$suffix"
        }
}
