package vip.mystery0.pixel.meter.format

import java.util.Locale

/**
 * Formats speed text consistently across the main screen, notifications, Live Update, and Overlay.
 */
object SpeedFormatter {
    /**
     * Selects decimal precision by value: 0 places at >= 100, 1 at >= 10, and 2 otherwise.
     */
    private fun formatFixedValue(value: Double): String {
        val pattern = when {
            value >= 100 -> "%.0f"
            value >= 10 -> "%.1f"
            else -> "%.2f"
        }
        return pattern.format(Locale.getDefault(), value)
    }

    fun formatSpeedTextForLiveUpdate(
        bytes: Long,
        speedUnit: Int = 0,
        minSpeedUnit: Int = 0
    ): String {
        if (minSpeedUnit > 0 && speedUnit == 0) {
            val threshold = when (minSpeedUnit) {
                1 -> 1024L
                2 -> 1048576L
                3 -> 1073741824L
                else -> 0L
            }
            if (bytes < threshold) {
                return "0" + when (minSpeedUnit) {
                    1 -> "K/s"
                    2 -> "M/s"
                    3 -> "G/s"
                    else -> "B/s"
                }
            }
        }

        when (speedUnit) {
            1 -> return "${formatFixedValue(bytes.toDouble())}B/s"
            2 -> return "${formatFixedValue(bytes / 1024.0)}K/s"
            3 -> return "${formatFixedValue(bytes / 1048576.0)}M/s"
            4 -> return "${formatFixedValue(bytes / 1073741824.0)}G/s"
        }
        if (bytes < 1024) return "${bytes}B/s"
        val kb = bytes / 1024.0
        if (kb < 1000) return "${"%.0f".format(Locale.getDefault(), kb)}K/s"
        val mb = kb / 1024.0
        if (mb < 1000) {
            return if (mb < 100) "${"%.1f".format(Locale.getDefault(), mb)}M/s"
            else "${"%.0f".format(Locale.getDefault(), mb)}M/s"
        }
        val gb = mb / 1024.0
        return "${"%.1f".format(Locale.getDefault(), gb)}G/s"
    }

    fun formatSpeedText(
        bytes: Long,
        speedUnit: Int = 0,
        minSpeedUnit: Int = 0
    ): Pair<String, String> {
        if (minSpeedUnit > 0 && speedUnit == 0) {
            val threshold = when (minSpeedUnit) {
                1 -> 1024L
                2 -> 1048576L
                3 -> 1073741824L
                else -> 0L
            }
            if (bytes < threshold) {
                val unit = when (minSpeedUnit) {
                    1 -> "KB/s"
                    2 -> "MB/s"
                    3 -> "GB/s"
                    else -> "B/s"
                }
                return "0" to unit
            }
        }

        when (speedUnit) {
            1 -> return formatFixedValue(bytes.toDouble()) to "B/s"
            2 -> return formatFixedValue(bytes / 1024.0) to "KB/s"
            3 -> return formatFixedValue(bytes / 1048576.0) to "MB/s"
            4 -> return formatFixedValue(bytes / 1073741824.0) to "GB/s"
        }
        if (bytes < 1024) return bytes.toString() to "B/s"
        val kb = bytes / 1024.0
        if (kb < 1000) return "%.0f".format(Locale.getDefault(), kb) to "KB/s"
        val mb = kb / 1024.0
        if (mb < 1000) {
            return if (mb < 10) "%.1f".format(Locale.getDefault(), mb) to "MB/s"
            else "%.0f".format(Locale.getDefault(), mb) to "MB/s"
        }
        val gb = mb / 1024.0
        return "%.1f".format(Locale.getDefault(), gb) to "GB/s"
    }

    fun formatSpeedLine(
        bytes: Long,
        speedUnit: Int = 0,
        minSpeedUnit: Int = 0
    ): String {
        val (value, unit) = formatSpeedText(bytes, speedUnit, minSpeedUnit)
        return "$value$unit"
    }
}
