# Downgrader

Downgrader 是一款基于 [Jetpack Compose](https://developer.android.com/compose) 和 [Miuix](https://github.com/compose-miuix-ui/miuix) 的 OriginOS 设备降级包查询客户端。

## 功能特性

- **设备信息读取** — 展示设备名称、型号、当前系统版本、Android 版本及脱敏序列号
- **降级凭据检测** — 检查设备是否提供降级凭据，判断是否具备降级包查询条件
- **降级包查询** — 使用设备降级凭据请求 vivo 服务（`sysupgrade.vivo.com.cn`），查询可用的降级包
- **降级包信息展示** — 展示目标系统版本、Android 版本、ROM 版本、下载文件名、文件大小、发布时间及 SHA-256 校验值
- **dota_sig 管理** — 支持复制和保存服务返回的 `dota_sig`
- **降级包下载** — 支持应用内后台下载、进度显示、取消与失败后重试；按服务端提供的文件大小和 SHA-256 校验后保存到内部存储根目录

降级包查询取决于设备是否提供降级凭据。未检测到相关凭据时，应用将提示无法进行查询。

## 许可证

Downgrader 采用 GNU General Public License v3.0 发布。详情请参阅 [LICENSE](LICENSE)。

## 致谢

本项目使用或参考了以下开源项目：

- [Miuix](https://github.com/compose-miuix-ui/miuix)
- [Payload Extract GUI](https://github.com/YuKongA/payload_extract_gui)
