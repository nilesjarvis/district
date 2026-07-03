package com.district.core.media

import kotlin.math.abs
import kotlin.math.floor

fun horizontalFraction(x: Float, width: Float): Float =
    if (width <= 0f) 0f else (x / width).coerceIn(0f, 1f)

fun detentIndex(fraction: Float, detents: Int): Int =
    floor(fraction.coerceIn(0f, 1f) * detents.coerceAtLeast(1)).toInt().coerceIn(0, detents.coerceAtLeast(1))

fun crossedDetent(previous: Float, current: Float, detents: Int): Boolean =
    detentIndex(previous, detents) != detentIndex(current, detents)

class DetentHapticGate(
    private val detents: Int,
    initialFraction: Float = 0f,
) {
    private var previousFraction = initialFraction.coerceIn(0f, 1f)

    fun shouldFire(nextFraction: Float): Boolean {
        val next = nextFraction.coerceIn(0f, 1f)
        val result = crossedDetent(previousFraction, next, detents)
        previousFraction = next
        return result
    }

    fun reset(fraction: Float) {
        previousFraction = fraction.coerceIn(0f, 1f)
    }
}

fun nearlySameFraction(a: Float, b: Float, epsilon: Float = 0.001f): Boolean =
    abs(a - b) <= epsilon
