package vip.mystery0.pixel.meter.service.tile

import android.content.Context
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import vip.mystery0.pixel.meter.R
import vip.mystery0.pixel.meter.data.repository.NetworkRepository
import vip.mystery0.pixel.meter.util.AppLocaleManager

class NotificationTileService : TileService() {
    private val networkRepository: NetworkRepository by inject()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var job: Job? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleManager.wrapBaseContext(newBase))
    }

    override fun onStartListening() {
        super.onStartListening()
        job = scope.launch {
            networkRepository.isNotificationEnabled.collect { isEnabled ->
                val tile = qsTile ?: return@collect
                tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                tile.label = getString(R.string.tile_notification_label)
                tile.subtitle =
                    if (isEnabled) getString(R.string.tile_desc_active) else getString(R.string.tile_desc_inactive)
                tile.updateTile()
            }
        }
    }

    override fun onStopListening() {
        super.onStopListening()
        job?.cancel()
    }

    override fun onClick() {
        super.onClick()
        val currentState = qsTile?.state == Tile.STATE_ACTIVE
        networkRepository.setNotificationEnabled(!currentState)
    }
}
