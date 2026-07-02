package com.district.jellyfinmono.core.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ControlMathTest {
    @Test
    fun horizontalFractionMapsAndClamps() {
        assertEquals(0f, horizontalFraction(-20f, 100f), 0.001f)
        assertEquals(0.5f, horizontalFraction(50f, 100f), 0.001f)
        assertEquals(1f, horizontalFraction(140f, 100f), 0.001f)
        assertEquals(0f, horizontalFraction(50f, 0f), 0.001f)
    }

    @Test
    fun detentCrossingIsTickBased() {
        assertFalse(crossedDetent(0.101f, 0.109f, 100))
        assertTrue(crossedDetent(0.10f, 0.12f, 100))
        assertEquals(100, detentIndex(1.2f, 100))
    }

    @Test
    fun hapticGateOnlyFiresWhenDetentChanges() {
        val gate = DetentHapticGate(detents = 10, initialFraction = 0.10f)

        assertFalse(gate.shouldFire(0.109f))
        assertTrue(gate.shouldFire(0.21f))
        assertFalse(gate.shouldFire(0.219f))

        gate.reset(0.50f)
        assertFalse(gate.shouldFire(0.509f))
    }
}
