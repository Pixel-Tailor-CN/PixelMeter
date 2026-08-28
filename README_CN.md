# Pixel Meter

<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="200" alt="Pixel Meter Logo"/>
</p>

<p align="center">
  <strong>专为 Pixel 和原生 Android 设计的精准网速监控工具。</strong>
</p>

<p align="center">
  <a href="https://github.com/Pixel-Tailor-CN/PixelMeter/releases/latest"><img src="https://img.shields.io/github/v/release/Mystery00/PixelMeter" alt="GitHub Release"></a>
  <a href="https://play.google.com/store/apps/details?id=vip.mystery0.pixel.meter"><img src="https://img.shields.io/badge/Google_Play-PixelMeter-green?logo=google-play&logoColor=white" alt="Google Play"></a>
  <a href="https://hosted.weblate.org/engage/pixel-meter/"><img src="https://hosted.weblate.org/widget/pixel-meter/android-app-strings/svg-badge.svg" alt="翻译状态"></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/Mystery00/PixelMeter" alt="License"></a>
</p>

[English](README.md)

## 简介

Pixel Meter 是一款面向 Google Pixel 和原生/类原生 Android 设备的轻量级实时网速监控应用。

传统网速工具在 VPN 场景下可能同时统计物理网络接口和 VPN 虚拟网络，导致显示速度接近实际值的两倍。Pixel Meter 使用 `ConnectivityManager.NetworkCallback` 识别 Wi-Fi、蜂窝网络和以太网，通过 `TRANSPORT_VPN` 排除 VPN 网络，再使用 `TrafficStats` 读取物理接口计数器。

整个过程不依赖固定的 `tun0` 黑名单，也不需要 Root 或 Shizuku。

## 软件截图

<p align="center">
  <img src="docs/Screenshot_CN.png" width="400" alt="Pixel Meter 软件截图"/>
</p>
<p align="center">
  <img src="docs/Component.png" width="175" alt="Pixel Meter 组件"/>
</p>

## 核心功能

- **精准物理接口统计**：排除 VPN Transport，直接读取 Wi-Fi、蜂窝网络和以太网接口流量。
- **首次设置向导**：分步选择通知网速、Android 16+ Live Update 和悬浮窗，只申请所选功能需要的权限。
- **全局速率单位**：在整个应用中以 bytes/s 或十进制 SI bits/s 显示速度。
- **通知栏网速**：
  - 实时 Bitmap 状态栏图标，可显示总计、上传或下载内容。
  - 支持总网速、仅上行、仅下行。
  - 支持自定义前缀、显示阈值、图标字号和通知颜色。
- **Android 16+ Live Update**：在系统支持且用户启用时使用 Promoted Ongoing 状态栏展示。
- **桌面悬浮窗**：
  - 基于 Compose 的可拖动悬浮窗，支持上传和下载、上传、下载和总计模式。
  - 支持横排/竖排、对齐、间距、字号、内边距、颜色和圆角。
  - 支持锁定和位置记忆。
  - 支持横屏隐藏、沉浸模式隐藏和持续低流量自动隐藏。
- **Quick Settings 磁贴**：从系统下拉栏快速切换通知网速和悬浮窗。
- **Material You**：支持动态取色、固定主题色和 AMOLED Black。
- **后台控制**：可选开机启动、电池优化设置入口和息屏采样暂停。
- **Cloudflare 测速**：通过 Chrome Custom Tabs 打开 `speed.cloudflare.com`。
- **隐私优先**：不包含统计、广告 SDK，不上传网络流量。

## 系统要求

- Android 12 / API 31 及以上。
- 主要目标设备为 Google Pixel，同时兼容遵循标准 Android 网络接口行为的 AOSP 类设备。
- Android 13+ 运行监听服务需要通知权限。
- 仅悬浮窗需要“显示在其他应用上层”权限。
- Live Update 需要 Android 16 或更高版本。

Android 要求 Foreground Service 持续显示通知。关闭动态通知网速后，监听运行期间仍会保留最简服务通知。

## 技术架构

- Kotlin + Jetpack Compose + Material 3
- 单模块分层 MVVM
- Repository + StateFlow
- Koin 依赖注入
- Preferences DataStore
- `ConnectivityManager` + `TrafficStats`

详细项目文档参见 [`docs/README.md`](docs/README.md)。

## 支持语言

当前包含英语、简体中文、葡萄牙语、巴西葡萄牙语和俄语资源，翻译通过 Weblate 管理。

[在 Weblate 上帮助翻译 Pixel Meter](https://hosted.weblate.org/engage/pixel-meter/)

## 隐私

所有流量计数和设置均在设备本地处理，详见 [PRIVACY_POLICY_CN.md](PRIVACY_POLICY_CN.md)。

## 相关链接

- [Pixel Tailor CN](https://pixel.mystery0.app)
- [GitHub Issues](https://github.com/Pixel-Tailor-CN/PixelMeter/issues/new)
- [Telegram 频道](https://t.me/pixel_tailor_cn)

## 许可证

本项目采用 Apache License 2.0，详情参见 [LICENSE](LICENSE)。
