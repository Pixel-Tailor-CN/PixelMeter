package vip.mystery0.pixel.meter.data.source.impl

import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.TrafficStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import vip.mystery0.pixel.meter.data.source.ISpeedDataSource
import vip.mystery0.pixel.meter.data.source.NetworkTrafficData
import java.util.concurrent.ConcurrentHashMap

class SpeedDataSource(
    private val connectivityManager: ConnectivityManager
) : ISpeedDataSource {
    private val validInterfaces = ConcurrentHashMap<Network, String>()
    private val networkCapabilities = ConcurrentHashMap<Network, NetworkCapabilities>()
    private val linkProperties = ConcurrentHashMap<Network, LinkProperties>()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            capabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, capabilities)
            networkCapabilities[network] = capabilities
            updateNetwork(network)
        }

        override fun onLinkPropertiesChanged(network: Network, properties: LinkProperties) {
            super.onLinkPropertiesChanged(network, properties)
            linkProperties[network] = properties
            updateNetwork(network)
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            networkCapabilities.remove(network)
            linkProperties.remove(network)
            validInterfaces.remove(network)
        }
    }

    init {
        // Listen for supported physical transports and filter them again in updateNetwork.
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }

    private fun updateNetwork(network: Network) {
        val caps = networkCapabilities[network]
        val props = linkProperties[network]
        if (caps == null || props == null) {
            validInterfaces.remove(network)
            return
        }

        // Ignore VPN transports to avoid counting the virtual interface in addition to the
        // underlying physical link.
        val isVpn = caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        if (isVpn) {
            validInterfaces.remove(network)
            return
        }

        // The request already filters transports, but capabilities may change after registration.
        val isPhysical = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

        if (isPhysical) {
            val ifaceName = props.interfaceName
            if (!ifaceName.isNullOrEmpty()) {
                validInterfaces[network] = ifaceName
            }
        } else {
            validInterfaces.remove(network)
        }
    }

    override suspend fun getTrafficData(): NetworkTrafficData = withContext(Dispatchers.Default) {
        var totalRx = 0L
        var totalTx = 0L

        // Read cached interface names directly without querying ConnectivityManager in the
        // sampling loop.
        val trafficDataList = validInterfaces.values.map { ifaceName ->
            async {
                var currentRx = 0L
                var currentTx = 0L

                val rx = withContext(Dispatchers.IO) { TrafficStats.getRxBytes(ifaceName) }
                val tx = withContext(Dispatchers.IO) { TrafficStats.getTxBytes(ifaceName) }

                if (rx != TrafficStats.UNSUPPORTED.toLong()) {
                    currentRx += rx
                }
                if (tx != TrafficStats.UNSUPPORTED.toLong()) {
                    currentTx += tx
                }
                NetworkTrafficData(currentRx, currentTx)
            }
        }.awaitAll()

        trafficDataList.forEach {
            totalRx += it.rxBytes
            totalTx += it.txBytes
        }

        return@withContext NetworkTrafficData(totalRx, totalTx)
    }
}
