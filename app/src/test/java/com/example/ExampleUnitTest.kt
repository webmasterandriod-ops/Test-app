package com.example

import com.example.data.EncodingSpeed
import com.example.data.ExportResolution
import com.example.data.SkipFrameConfig
import com.example.engine.TimeSlice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max

class ExampleUnitTest {
    @Test
    fun testUnifiedConfigDefaults() {
        val config = SkipFrameConfig()
        assertEquals(-28f, config.relativeSilenceThresholdDb, 0.01f)
        assertEquals(0.5f, config.quietPartsSensitivity, 0.01f)
        assertEquals(350L, config.minSilenceDurationMs)
        assertEquals(120L, config.edgeCushionMs)
        assertEquals(8, config.frameSkipN)
        assertEquals(3, config.audioCrossfadeMs)
        assertTrue(config.denoiseEnabled)
        assertEquals(EncodingSpeed.BALANCED, config.encodingSpeed)
        assertEquals(ExportResolution.ORIGINAL, config.exportResolution)
        assertEquals(45, config.clipLengthL)
        assertEquals(1, config.clipCountK)
        assertEquals(0.5f, config.chaosC, 0.01f)
        assertTrue(config.rawMode)
        assertTrue(config.freshOnly)
    }

    @Test
    fun testCutSliceMath() {
        val slice = TimeSlice(1.5, 4.5)
        assertEquals(3.0, slice.duration, 0.001)

        val zeroSlice = TimeSlice(5.0, 5.0)
        assertEquals(0.0, zeroSlice.duration, 0.001)
    }
}
