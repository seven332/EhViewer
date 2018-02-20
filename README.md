# EhViewer Evelin

EhViewer Evelin 是一个图片资源浏览器，其中漫画为主要对象。

整个项目以 [Kotlin](https://kotlinlang.org/) 的 [Multiplatform Projects](https://kotlinlang.org/docs/reference/multiplatform.html) 为基础。

![architecture](./art/architecture.png)

Core 为 Multiplatform Projects 中的 common 模块，完全用 Kotlin 来写。提供一套 API 接口供 Plugins 使用，并且以与平台无关的代码完成部分业务逻辑。

Core JS，Core JVM，Core Swift 为 Multiplatform Projects 中的 platform 模块，全部或部分用 Kotlin 来写。利用与平台相关的代码完善 Core 中的业务逻辑。

Test JS，Test JVM，Test Swift 全部或部分用 Kotlin 来写。目的是方便开发者测试 Plugins。

WEB，Android，IOS 为 Multiplatform Projects 中的 regular 模块，所用语言分平台讨论。这即为最终客户端，用于提供用户界面，并将用户界面与业务逻辑连接起来。

API 为 Core 导出的一套供 Plugins 使用的接口。可参照 Android SDK。

Plugins 即插件，全部或部分用 Kotlin 来写。Plugins 需要针对各平台，甚至各客户端分别编译一套。Plugins 可根据不同平台进行功能实现，故可能针对不同平台使用不同的代码。

Dependency Plugins 为用于提供其他 Plugins 运行环境的 Plugins。该种 Plugins 可能不需要由 Core 导出的 API，反而可由自身导出 API 供其他 Plugins 使用。目的是减小应用与 Plugins 的大小。

Source Plugins 为提供图片源的 Plugins。源可为设备本地，也可为通过网络访问的。

Collection Plugins 为提供图片源的源的 Plugins，可理解为插件管理器，而被管理的插件为 Virtual Source Plugins。每个 Collection Plugins 都提供了一个新的插件系统，相当于一个 Collection Plugins 可充当多个 Source Plugins。而 Collection Plugins 所支持的 Plugins 并不要求一定要使用 Kotlin，可以使用脚本语言以避免平台问题。
