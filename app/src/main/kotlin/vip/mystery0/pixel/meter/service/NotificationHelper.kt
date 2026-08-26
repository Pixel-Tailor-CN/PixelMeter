package vip.mystery0.pixel.meter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import vip.mystery0.pixel.meter.MainActivity
import vip.mystery0.pixel.meter.R
import vip.mystery0.pixel.meter.data.source.NetSpeedData
import vip.mystery0.pixel.meter.format.SpeedFormatter
import kotlin.math.roundToInt

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "net_monitor_silent"
        const val NOTIFICATION_ID = 1001

        fun createNotificationChannel(context: Context) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val group = NotificationChannelGroup(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name)
            )
            notificationManager.createNotificationChannelGroup(group)

            // Use IMPORTANCE_LOW for silent notification
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_description)
                setShowBadge(false)
                setGroup(CHANNEL_ID)
                setSound(null, null)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    data class NotificationBuildResult(
        val notification: Notification,
        val fingerprint: String
    )

    private data class NotificationRenderState(
        val mode: String,
        val contentText: String,
        val statusText: String? = null,
        val valueText: String? = null,
        val unitText: String? = null,
        val useCustomColor: Boolean = false,
        val color: Int = 0
    ) {
        /**
         * Creates a stable fingerprint from visible output to avoid reposting equivalent notifications.
         */
        fun toFingerprint(): String = listOf(
            mode,
            contentText,
            statusText.orEmpty(),
            valueText.orEmpty(),
            unitText.orEmpty(),
            useCustomColor.toString(),
            if (useCustomColor) color.toString() else ""
        ).joinToString(separator = "|")
    }

    // Icon generation
    // On Pixel, small icon is typically 24dp. We render at higher res (e.g. 48px or 96px) for clarity
    private val size =
        (context.resources.displayMetrics.density * 24).roundToInt().coerceAtLeast(48)
    private val bitmap = createBitmap(size, size)
    private val canvas = Canvas(bitmap)

    // Paints
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textSize = size * 0.65f // Value text
    }

    private val unitPaint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        textSize = size * 0.35f // Unit text
    }

    fun buildNotification(
        speed: NetSpeedData,
        isLiveUpdate: Boolean,
        isNotificationEnabled: Boolean,
        textUp: String,
        textDown: String,
        upFirst: Boolean,
        displayMode: Int,
        textSize: Float = 0.65f,
        unitSize: Float = 0.35f,
        threshold: Long = 0L,
        lowTrafficMode: Int = 0, // 0: Static, 1: Dynamic
        useCustomColor: Boolean = false,
        color: Int = 0,
        speedUnit: Int = 0,
        minSpeedUnit: Int = 0,
        postedAtMillis: Long
    ): NotificationBuildResult {
        val renderState = createRenderState(
            speed = speed,
            isLiveUpdate = isLiveUpdate,
            isNotificationEnabled = isNotificationEnabled,
            textUp = textUp,
            textDown = textDown,
            upFirst = upFirst,
            displayMode = displayMode,
            threshold = threshold,
            lowTrafficMode = lowTrafficMode,
            useCustomColor = useCustomColor,
            color = color,
            speedUnit = speedUnit,
            minSpeedUnit = minSpeedUnit
        )
        return NotificationBuildResult(
            notification = buildNotificationFromState(
                renderState = renderState,
                textSize = textSize,
                unitSize = unitSize,
                postedAtMillis = postedAtMillis
            ),
            fingerprint = renderState.toFingerprint()
        )
    }

    private fun createRenderState(
        speed: NetSpeedData,
        isLiveUpdate: Boolean,
        isNotificationEnabled: Boolean,
        textUp: String,
        textDown: String,
        upFirst: Boolean,
        displayMode: Int,
        threshold: Long,
        lowTrafficMode: Int,
        useCustomColor: Boolean,
        color: Int,
        speedUnit: Int,
        minSpeedUnit: Int
    ): NotificationRenderState {
        var shouldLiveUpdate = isLiveUpdate

        if (!isNotificationEnabled) {
            return NotificationRenderState(
                mode = "disabled-static",
                contentText = context.getString(R.string.notification_content_text),
                useCustomColor = useCustomColor,
                color = color
            )
        }

        if (speed.totalSpeed < threshold) {
            if (lowTrafficMode == 0) {
                return NotificationRenderState(
                    mode = "threshold-static",
                    contentText = context.getString(R.string.notification_content_text_monitoring),
                    useCustomColor = useCustomColor,
                    color = color
                )
            }
            shouldLiveUpdate = false
        }

        val upText = "$textUp${
            SpeedFormatter.formatSpeedLine(
                speed.uploadSpeed,
                speedUnit,
                minSpeedUnit
            )
        }"
        val downText = "$textDown${
            SpeedFormatter.formatSpeedLine(
                speed.downloadSpeed,
                speedUnit,
                minSpeedUnit
            )
        }"
        val contentText = when (displayMode) {
            1 -> upText
            2 -> downText
            else -> if (upFirst) "$upText  $downText" else "$downText  $upText"
        }

        if (shouldLiveUpdate) {
            return NotificationRenderState(
                mode = "live-update",
                contentText = contentText,
                statusText = SpeedFormatter.formatSpeedTextForLiveUpdate(
                    speed.totalSpeed,
                    speedUnit,
                    minSpeedUnit
                ),
                useCustomColor = useCustomColor,
                color = color
            )
        }

        val (valueStr, unitStr) = SpeedFormatter.formatSpeedText(
            speed.totalSpeed,
            speedUnit,
            minSpeedUnit
        )
        return NotificationRenderState(
            mode = "bitmap",
            contentText = contentText,
            valueText = valueStr,
            unitText = unitStr,
            useCustomColor = useCustomColor,
            color = color
        )
    }

    private fun buildNotificationFromState(
        renderState: NotificationRenderState,
        textSize: Float,
        unitSize: Float,
        postedAtMillis: Long
    ): Notification {
        val intent = Intent().apply {
            setClassName(context, MainActivity::class.java.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setWhen(postedAtMillis)
            .setShowWhen(false)
            .setContentTitle(context.getString(R.string.notification_content_title))
            .setContentText(renderState.contentText)

        if (renderState.useCustomColor) {
            builder.setColor(renderState.color)
        }

        when (renderState.mode) {
            "live-update" -> {
                builder
                    .setSmallIcon(R.drawable.ic_speed)
                    .setShortCriticalText(requireNotNull(renderState.statusText))
                    .setRequestPromotedOngoing(true)
            }

            "bitmap" -> {
                val valueText = requireNotNull(renderState.valueText)
                val unitText = requireNotNull(renderState.unitText)

                bitmap.eraseColor(Color.TRANSPARENT)
                val cx = size / 2f
                val cyValue = size * 0.5f
                val cyUnit = size * 0.95f

                textPaint.textSize = size * textSize
                unitPaint.textSize = size * unitSize

                canvas.drawText(valueText, cx, cyValue, textPaint)
                canvas.drawText(unitText, cx, cyUnit, unitPaint)

                builder.setSmallIcon(IconCompat.createWithBitmap(bitmap))
            }

            else -> builder.setSmallIcon(R.drawable.ic_speed)
        }

        return builder.build()
    }
}
