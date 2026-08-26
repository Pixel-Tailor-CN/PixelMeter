# Network Data Source

## 1. Goal

Pixel Meter must count only physical-link traffic while a VPN is active, preventing the same bytes from being counted on both physical and virtual interfaces.

## 2. Network Detection

`SpeedDataSource` registers a `ConnectivityManager.NetworkCallback` for these transports:

- `TRANSPORT_WIFI`
- `TRANSPORT_CELLULAR`
- `TRANSPORT_ETHERNET`

`onCapabilitiesChanged` and `onLinkPropertiesChanged` cache the `NetworkCapabilities` and `LinkProperties` supplied directly by the callbacks. They do not synchronously call `getNetworkCapabilities()` or `getLinkProperties()`, avoiding stale or null callback-time lookup results.

A network is added to the interface cache only when:

1. Both capabilities and link properties are available.
2. The capabilities do not contain `TRANSPORT_VPN`.
3. At least one supported physical transport is present.
4. `LinkProperties.interfaceName` is not null.
5. The `Network` and interface name can be stored in `ConcurrentHashMap<Network, String>`.

`onLost` removes the Network, its interface entry, and its cached callback parameters.

## 3. Why There Is No Interface-Name Blacklist

VPN interface names vary by implementation and device. Names such as `tun0` are not reliable identifiers. Filtering by `TRANSPORT_VPN` follows Android's network model and avoids maintaining incomplete device-specific rules.

## 4. Traffic Counters

For each cached physical interface, the data source reads:

```kotlin
TrafficStats.getRxBytes(interfaceName)
TrafficStats.getTxBytes(interfaceName)
```

`TrafficStats.UNSUPPORTED` is ignored. Values from all valid physical interfaces are summed before being returned to the Repository.

The sampling loop reads cached interface names directly and does not query `ConnectivityManager` for every sample.

## 5. Speed Calculation

`NetworkRepository` calculates speed from adjacent samples:

```text
downloadSpeed = max((currentRx - previousRx) × 1000 / elapsedMs, 0)
uploadSpeed   = max((currentTx - previousTx) × 1000 / elapsedMs, 0)
```

Sampling durations and loop timing use `SystemClock.elapsedRealtime()`, a monotonic clock unaffected by wall-clock corrections.

The first sample establishes a baseline. Negative deltas caused by counter resets, network changes, or interface resets are clamped to zero.

## 6. Concurrency and Threads

- Interface and callback-parameter caches use `ConcurrentHashMap`.
- Per-interface reads run concurrently in coroutines.
- `TrafficStats` calls use `Dispatchers.IO`.
- Aggregation and speed calculation run on a background Dispatcher.

## 7. Validation Scenarios

Validate changes to this path on a Pixel device with:

- Wi-Fi only.
- Cellular only.
- Ethernet, where available.
- A VPN enabled and disabled.
- Multiple simultaneous networks.
- Network disconnection and reconnection.
- No near-doubling of reported speed after enabling a VPN.

## 8. Known Limitation

A network switch or physical-interface recreation can cause two adjacent samples to refer to counters from different interface lifetimes, producing a transient one-sample speed spike. Pixel Meter intentionally avoids network-generation tracking or a dedicated baseline-reset protocol to keep the real-time data path simple. The transient spike is treated as a known limitation.
