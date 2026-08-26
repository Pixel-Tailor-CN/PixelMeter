# 网络数据源

## 1. 目标

Pixel Meter 需要在 VPN 开启时只统计物理链路流量，避免物理接口和 VPN 虚拟网络被同时计入。

## 2. 网络识别

`SpeedDataSource` 使用包含以下 Transport 的 `NetworkRequest` 注册 `ConnectivityManager.NetworkCallback`：

- `TRANSPORT_WIFI`
- `TRANSPORT_CELLULAR`
- `TRANSPORT_ETHERNET`

`onCapabilitiesChanged` and `onLinkPropertiesChanged` cache the `NetworkCapabilities` and `LinkProperties` supplied by the callbacks directly. The callbacks do not synchronously call `getNetworkCapabilities()` or `getLinkProperties()`, avoiding stale or null state returned by callback-time lookups.

更新缓存接口时再次校验：

1. Capabilities 与 LinkProperties 必须存在。
2. 若包含 `TRANSPORT_VPN`，立即从缓存移除。
3. 必须至少包含一种受支持的物理 Transport。
4. `LinkProperties.interfaceName` 必须非空。
5. 使用 `ConcurrentHashMap<Network, String>` 保存 Network 与接口名。

`onLost` 会删除已失效的 Network 以及对应的 callback 参数缓存。

## 3. 为什么不使用接口名黑名单

`tun0`、wg、ppp 等名称不是稳定的 Android API，不同 VPN、设备或系统版本可能使用不同名称。Pixel Meter 依据系统提供的 `NetworkCapabilities` 排除 `TRANSPORT_VPN`，而不是维护固定名称列表。

## 4. TrafficStats 读取

采样时直接遍历缓存的接口名称，并行读取：

```kotlin
TrafficStats.getRxBytes(interfaceName)
TrafficStats.getTxBytes(interfaceName)
```

返回 `TrafficStats.UNSUPPORTED` 时按无数据处理。所有有效接口的 Rx/Tx 分别求和，生成 `NetworkTrafficData`。

采样循环不会重复遍历所有 Network 或查询 Capabilities；ConnectivityManager 查询只发生在 Callback 更新阶段。

## 5. 速度计算

`NetworkRepository` 保存上次总字节数与时间戳：

```text
downloadSpeed = max((currentRx - previousRx) × 1000 / elapsedMs, 0)
uploadSpeed   = max((currentTx - previousTx) × 1000 / elapsedMs, 0)
```

Sampling durations and loop timing use `SystemClock.elapsedRealtime()`, a monotonic clock that is not affected by wall-clock corrections.

首次采样只建立基线。接口重置、计数回退或网络切换产生的负数会被限制为 0。

## 6. 并发与线程

- 接口缓存和 callback 参数缓存使用 `ConcurrentHashMap`。
- 接口读取在 Coroutine 中并行执行。
- TrafficStats 调用切换到 `Dispatchers.IO`。
- 汇总和速度计算在后台 Dispatcher 完成。

## 7. 兼容性与验证

主要目标为 Google Pixel，兼容实现标准 NetworkCapabilities 和 TrafficStats 行为的 Android 12+ 设备。

数据源变更必须真机验证：

- Wi-Fi、Cellular、Ethernet 切换。
- VPN 开启和关闭。
- 多网络并存。
- 网络断开和重新连接。
- 不应出现 VPN 导致的近似双倍速度。

## 8. Known limitation

A network switch or physical-interface recreation can make two adjacent samples refer to counters from different interface lifetimes. This can produce a transient one-sample speed spike. Pixel Meter intentionally does not add network-generation tracking or a dedicated baseline-reset protocol for this case, keeping the real-time data path simple; the transient spike is treated as a known limitation.
