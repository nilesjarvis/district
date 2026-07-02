package com.district.jellyfinmono.core.design

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellMetricsTest {
    @Test
    fun shellKeepsRequiredTwoHalfStructure() {
        assertEquals(46f, ShellMetrics.HeaderHeight.value)
        assertEquals(52f, ShellMetrics.ContextBarHeight.value)
        assertEquals(56f, ShellMetrics.NowPlayingHeight.value)
        assertEquals(2f, ShellMetrics.DividerHeight.value)
        assertTrue(ShellMetrics.ControlZoneHeight.value >= 232f)
        assertTrue(ShellMetrics.MinTouchTarget.value >= 44f)
    }
}
