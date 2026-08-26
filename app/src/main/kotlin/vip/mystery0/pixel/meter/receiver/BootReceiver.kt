package vip.mystery0.pixel.meter.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import vip.mystery0.pixel.meter.data.repository.NetworkRepository
import vip.mystery0.pixel.meter.service.NetworkMonitorService

class BootReceiver : BroadcastReceiver(), KoinComponent {
    companion object {
        private const val TAG = "BootReceiver"
    }

    private val repository: NetworkRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        // Run synchronously in onReceive so the process is not killed after an asynchronous return.
        // StateFlow value access is synchronous, so no thread switch is required.
        val isAutoStart = repository.isAutoStartServiceEnabled.value
        if (isAutoStart) {
            Log.i(TAG, "boot completed, starting service")
            try {
                val serviceIntent = Intent(context, NetworkMonitorService::class.java)
                context.startForegroundService(serviceIntent)
            } catch (e: Exception) {
                Log.e(TAG, "failed to start foreground service on boot", e)
            }
        } else {
            Log.i(TAG, "boot completed, but auto-start is disabled")
        }
    }
}
