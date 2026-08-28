package vip.mystery0.pixel.meter.format

import java.util.Locale
import kotlin.math.roundToLong

/** Formats speed text consistently across every display surface. */
object SpeedFormatter {
    private const val RATE_BITS = 1

    private fun formatFixedValue(value: Double): String {
        val pattern = when {
            value >= 100 -> "%.0f"
            value >= 10 -> "%.1f"
            else -> "%.2f"
        }
        return pattern.format(Locale.ROOT, value)
    }

    private fun normalizedRateUnit(rateUnit: Int): Int = if (rateUnit == RATE_BITS) RATE_BITS else 0
    private fun normalizedBytes(bytes: Long): Long = bytes.coerceAtLeast(0)
    private fun rateValue(bytes: Long, rateUnit: Int): Double =
        normalizedBytes(bytes).toDouble() * if (normalizedRateUnit(rateUnit) == RATE_BITS) 8.0 else 1.0

    private fun base(rateUnit: Int): Double = if (normalizedRateUnit(rateUnit) == RATE_BITS) 1000.0 else 1024.0

    private fun divisor(unit: Int, rateUnit: Int): Double {
        val base = base(rateUnit)
        return when (unit) {
            2 -> base
            3 -> base * base
            4 -> base * base * base
            else -> 1.0
        }
    }

    private fun fullUnit(unit: Int, rateUnit: Int): String {
        val bits = normalizedRateUnit(rateUnit) == RATE_BITS
        return when (unit) {
            2 -> if (bits) "kb/s" else "KB/s"
            3 -> if (bits) "Mb/s" else "MB/s"
            4 -> if (bits) "Gb/s" else "GB/s"
            else -> if (bits) "b/s" else "B/s"
        }
    }

    private fun compactUnit(unit: Int, rateUnit: Int): String {
        val bits = normalizedRateUnit(rateUnit) == RATE_BITS
        return when (unit) {
            2 -> if (bits) "k/s" else "K/s"
            3 -> "M/s"
            4 -> "G/s"
            else -> if (bits) "b/s" else "B/s"
        }
    }

    private fun minimumUnitIndex(minSpeedUnit: Int): Int = when (minSpeedUnit) {
        1 -> 2
        2 -> 3
        3 -> 4
        else -> 1
    }

    fun formatSpeedTextForLiveUpdate(bytes: Long, rateUnit: Int = 0): String {
        val value = rateValue(bytes, rateUnit)
        val base = base(rateUnit)
        if (value < base) return "${value.toLong()}${compactUnit(1, rateUnit)}"
        val kilo = value / base
        if (kilo < base) return "${"%.0f".format(Locale.ROOT, kilo)}${compactUnit(2, rateUnit)}"
        val mega = kilo / base
        if (mega < base) {
            return if (mega < 100) "${"%.1f".format(Locale.ROOT, mega)}${compactUnit(3, rateUnit)}"
            else "${"%.0f".format(Locale.ROOT, mega)}${compactUnit(3, rateUnit)}"
        }
        return "${"%.1f".format(Locale.ROOT, mega / base)}${compactUnit(4, rateUnit)}"
    }

    fun formatSpeedText(
        bytes: Long,
        speedUnit: Int = 0,
        minSpeedUnit: Int = 0,
        rateUnit: Int = 0
    ): Pair<String, String> {
        val value = rateValue(bytes, rateUnit)
        if (minSpeedUnit > 0 && speedUnit == 0) {
            val unitIndex = minimumUnitIndex(minSpeedUnit)
            if (value < divisor(unitIndex, rateUnit)) return "0" to fullUnit(unitIndex, rateUnit)
        }
        if (speedUnit in 1..4) {
            return formatFixedValue(value / divisor(speedUnit, rateUnit)) to fullUnit(speedUnit, rateUnit)
        }
        val base = base(rateUnit)
        if (value < base) return value.toLong().toString() to fullUnit(1, rateUnit)
        val kilo = value / base
        if (kilo < base) return "%.0f".format(Locale.ROOT, kilo) to fullUnit(2, rateUnit)
        val mega = kilo / base
        if (mega < base) {
            return if (mega < 10) "%.1f".format(Locale.ROOT, mega) to fullUnit(3, rateUnit)
            else "%.0f".format(Locale.ROOT, mega) to fullUnit(3, rateUnit)
        }
        return "%.1f".format(Locale.ROOT, mega / base) to fullUnit(4, rateUnit)
    }

    fun formatSpeedLine(bytes: Long, speedUnit: Int = 0, minSpeedUnit: Int = 0, rateUnit: Int = 0): String {
        val (value, unit) = formatSpeedText(bytes, speedUnit, minSpeedUnit, rateUnit)
        return "$value$unit"
    }

    fun formatThresholdInputValue(bytes: Long, rateUnit: Int): String {
        val value = normalizedBytes(bytes).toDouble() / if (normalizedRateUnit(rateUnit) == RATE_BITS) 125.0 else 1024.0
        return "%.3f".format(Locale.ROOT, value).trimEnd('0').trimEnd('.')
    }

    fun parseThresholdInputValue(text: String, rateUnit: Int): Long? {
        val normalizedText = text.trim()
        if (!normalizedText.matches(Regex("^(?:\\d+\\.?\\d*|\\.\\d+)$"))) return null
        val value = normalizedText.toDoubleOrNull() ?: return null
        if (!value.isFinite() || value < 0) return null
        val bytes = value * if (normalizedRateUnit(rateUnit) == RATE_BITS) 125.0 else 1024.0
        if (!bytes.isFinite() || bytes > Long.MAX_VALUE.toDouble()) return null
        return bytes.roundToLong()
    }
}