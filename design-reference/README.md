# 设计参考（只读）

这里放的是**网页版高保真原型**与定稿设计文档，用于对照视觉效果与交互细节，**不是安卓工程的一部分**，Gradle 构建不会引用本目录。

| 文件 | 用途 |
|---|---|
| `prototype.html` | v0.5 网页原型，浏览器直接打开；`?sim=2026-11-09T21:37` 可注入模拟时间 |
| `需求与界面设计_v0.5.md` | 需求澄清、计算模型、界面线框、交互、取舍与验收用例 |
| `previews/主界面.png` | 周一 21:37 夜间深色，三行记录（含实际完成小字） |
| `previews/步进设置.png` | 周二 08:05 日间浅色，展开步进面板（预设 + 自定义输入框） |

安卓实现以 `app/src/main/java/com/wzzhuz/ricecooker/schedule/` 下的 Kotlin 代码为准。计算规则、字号层级、颜色、文案与取舍保持一致；原生化调整（TimePicker 对话框、Canvas 虚线边框、SharedPreferences）见仓库根目录 README。

完整的 OpenSpec 规格与变更归档在 [wzzhuz/rice-cooker-schedule](https://github.com/wzzhuz/rice-cooker-schedule)，本目录不重复维护。
