package com.district.jellyfinmono.domain

sealed interface DistrictError {
    data class Network(val message: String) : DistrictError
    data class InvalidServerUrl(val message: String) : DistrictError
    data class Http(val code: Int, val message: String) : DistrictError
    data class Parse(val message: String) : DistrictError
    data object AuthRejected : DistrictError
    data object ExpiredToken : DistrictError
    data object Empty : DistrictError
    data class Playback(val message: String) : DistrictError
    data class Storage(val message: String) : DistrictError
}

sealed interface DistrictResult<out T> {
    data class Success<T>(val value: T) : DistrictResult<T>
    data class Failure(val error: DistrictError) : DistrictResult<Nothing>
}

inline fun <T, R> DistrictResult<T>.map(transform: (T) -> R): DistrictResult<R> =
    when (this) {
        is DistrictResult.Success -> DistrictResult.Success(transform(value))
        is DistrictResult.Failure -> this
    }
