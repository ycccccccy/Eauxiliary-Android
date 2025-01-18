# EAuxiliary

## 项目介绍

EAuxiliary 是一款Android应用，旨在帮助各位高效地获取和查看E听说答案。它能够自动扫描E听说资源列表，并将答案内容提取出来。应用支持按时间分组显示文件夹，并提供简洁直观的界面，方便各位快速查找所需内容。
此外，EAuxiliary 还具备版本更新检查功能，你们需要自行配置，也可以跟随我的仓库更新

## 主要功能

* **自动扫描答案:**  自动扫描E听说已下载试题的答案
* **版本更新检查:**  自动检查最新版本，并提示用户更新
* **答案上传/更新:** 将答案上传到数据库，用于公共访问

## 使用方法

1. **授权访问:** 首次启动应用时，需要授予应用访问特定目录的权限。 应用会引导你完成授权流程
2. **选择目录:** 允许访问所有文件后，应用会自动定位目标路径，无需再手动指定
3. **查看答案:** 应用会自动扫描所选目录下的文件夹，并显示答案内容。 点击卡片即可查看详细答案
4. **设置:**  在设置页面，你可以配置单答案模式、检查更新等选项

## 配置

应用的配置信息存储在 `SHARED_PREFS_NAME`  (值为 "app_prefs") 指定的 SharedPreferences 文件中
其中包含以下关键配置项：

* `KEY_DIRECTORY_URI`:  存储所选目录的 URI
* `KEY_STUDENT_NAME`:  存储名字

## 项目内容

* About: 负责显示“关于”页面，其中包含应用的版本信息、开发者信息、联系方式等

* AnswerActivity: 这个类负责显示具体问题的答案。 它接收来自 MainActivity 的答案数据，并将其显示在屏幕上

* DirectoryFragment: 用于显示目录结构或文件列表，让用户选择E听说所在的目录

* EULA: 这个类负责显示最终用户许可协议 (End-User License Agreement)。 用户需要同意 EULA 才能使用应用

* MainActivity.kt: 这是应用的主活动类，负责应用的启动和主要功能的协调。 它处理用户交互、权限请求、版本更新检查等

* OnboardingActivity: 这个类负责应用的首次启动引导流程，例如介绍应用的功能、请求必要的权限等

* PermissionFragment: 用于请求和处理应用所需的权限。 这可以提供更友好的用户体验

* SecureStorageUtils: 这个工具类负责安全地存储和读取敏感数据，例如用户凭据、API 密钥等。
它负责管理黑名单、白名单、激活状态和答案缓存

* SettingsActivity: 这个类负责应用的设置页面，允许用户配置应用的各项参数，例如单答案模式、更新检查频率等

* Shouce: 一个显示使用手册或帮助文档的 Activity

## 构建

本项目使用 Android Studio 构建。 你需要安装 Android Studio 并配置好 Android 开发环境

## 依赖

本项目使用了以下主要依赖库：

* **OkHttp:** 用于网络请求 (版本更新检查)
* **Jsoup:** 用于解析 HTML (版本更新检查)
* **Kotlin Serialization:** 用于 JSON 数据处理
* **Supabase Client Library (Android):**  用于与 Supabase 数据库交互

## 贡献

欢迎各位自行改进

## 许可证

[GNU Affero General Public License v3.0]

## 联系方式

微信:x2463274
邮箱:yc@zh206yc.onmicrosoft.com

## 致谢

以后添加，现在没有

## 免责声明

本应用仅供学习和研究使用，请勿用于非法用途。
