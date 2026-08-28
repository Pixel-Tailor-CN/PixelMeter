## 新增

- 新增首次启动设置向导，可分步选择通知网速、Android 16+ Live Update 和悬浮窗显示方式，并仅申请所选功能需要的权限。
- 新增俄语本地化，并补充葡萄牙语、巴西葡萄牙语以及现有中英文界面的引导页文案。

## 修复

- 修复 Android 12 和 Android 12L 上错误检查 Android 13 通知运行时权限，导致监听服务可能无法启动的问题。
- 修复前台服务首次启动通知未使用当前通知显示、颜色、单位和低流量配置的问题。

## 优化

- 统一主页、通知、Live Update 和悬浮窗的网速格式化逻辑，使数值精度、单位和最低显示单位保持一致。
- 通知渠道说明及界面辅助文本改为使用本地化资源，完善多语言体验。

---

## Added

- Added global byte/bit rate units with decimal SI bit formatting.
- Added Upload and download, Upload, Download and Total Overlay modes.
- Added Total, Upload and Download content modes for the status-bar icon and Live Update.

- Added a first-launch setup wizard for display modes and required permissions.
- Added Russian and completed onboarding translations.

## Fixed

- Fixed monitoring service startup on Android 12 and 12L.
- Fixed initial notification settings not applying correctly.

## Improved

- Unified speed formatting across the app.
- Improved localized notification and accessibility text.
