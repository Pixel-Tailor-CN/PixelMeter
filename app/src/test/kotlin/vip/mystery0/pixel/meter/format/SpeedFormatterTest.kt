package vip.mystery0.pixel.meter.format

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedFormatterTest {
    @Test
    fun fullSpeedHidesOnlyTheSuffixWhenRequested() {
        assertEquals("12.3KB/s", SpeedFormatter.formatSpeedLine(12_595, speedUnit = 2))
        assertEquals(
            "12.3KB",
            SpeedFormatter.formatSpeedLine(12_595, speedUnit = 2, hidePerSecondSuffix = true)
        )
        assertEquals(
            "0kb",
            SpeedFormatter.formatSpeedLine(1, minSpeedUnit = 1, rateUnit = 1, hidePerSecondSuffix = true)
        )
        assertEquals("8" to "kb/s", SpeedFormatter.formatSpeedText(1_000, rateUnit = 1))
        assertEquals(
            "8" to "kb",
            SpeedFormatter.formatSpeedText(1_000, rateUnit = 1, hidePerSecondSuffix = true)
        )
    }

    @Test
    fun compactSpeedKeepsItsExistingScaleAndCase() {
        assertEquals("123K/s", SpeedFormatter.formatSpeedTextForLiveUpdate(123 * 1_024L))
        assertEquals(
            "123K",
            SpeedFormatter.formatSpeedTextForLiveUpdate(123 * 1_024L, hidePerSecondSuffix = true)
        )
        assertEquals("8k/s", SpeedFormatter.formatSpeedTextForLiveUpdate(1_000, rateUnit = 1))
        assertEquals(
            "8k",
            SpeedFormatter.formatSpeedTextForLiveUpdate(1_000, rateUnit = 1, hidePerSecondSuffix = true)
        )
        assertEquals("0B", SpeedFormatter.formatSpeedTextForLiveUpdate(0, hidePerSecondSuffix = true))
    }
}
