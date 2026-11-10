# 电饭煲预约助手 · Android 应用

**版本**：v0.5.0（对应设计定稿 v0.5）
**仓库**：`wzzhuz/rice-cooker-schedule-android`（公开）
**形态**：Kotlin + Jetpack Compose 原生安卓应用，手机本地运行，**完全离线**

> 打开 App，扫一眼列表，找到你要的那个时间，照着左边的数字按电饭煲。全程不需要任何设置。

---

## 与另一个仓库的关系

| 仓库 | 内容 |
|---|---|
| [wzzhuz/rice-cooker-schedule](https://github.com/wzzhuz/rice-cooker-schedule) | **设计归档**：需求与界面设计文档、OpenSpec 规格、网页版高保真原型、界面预览图 |
| **本仓库** | **安卓实现**：可编译、可安装的原生 App；网页原型与预览图放在 `design-reference/` 作视觉与行为参考 |

网页原型是设计验证工具，不是交付形态。安卓工程以 `app/src/main` 下的 Kotlin 代码为准，`design-reference/` 只读。

---

## 功能

| 功能 | 实现 |
|---|---|
| 实时列表 | 每行左侧显示「现在该设的预约时长」，每分钟更新，整分钟内数值稳定，不显示秒 |
| 预期完成时间 | 右侧 `07:30 / 08:00 / 09:30` 等目标时刻，自动标注「今天 / 明天」，按时刻升序 |
| 实际完成时间 | 右侧下方小字 `实际 07:57 完成`，把向下取整造成的提前量直接写出来 |
| 添加时间 | 点「+ 添加时间」弹出原生 24 小时 TimePicker，选完即插入并排序 |
| 删除 | 对应行左滑，红色「删除」按钮滑入，点一下即删；行内容不位移 |
| 步进设置 | 底部文字链「步进 10 分钟 ›」展开：预设 `10/15/30/60` + 自定义输入框（10–240 整数分钟），非法值行内提示 |
| 自动保存 | 所有改动即时写入 `SharedPreferences`，无保存按钮、无确认弹窗、无撤销 |
| 自动深色 | 20:00–07:00 自动切换，不跟随系统，不提供主题按钮 |
| 空状态 | 删到空显示「还没有时间，点下方添加」；重复时刻自动忽略 |

**主界面可见控件只有一个「+ 添加时间」按钮**，步进设置是说明行里的一个文字链，属于换机器时的一次性配置。

---

## 计算模型（与网页原型完全一致）

客服已确认：**预约 = 几小时后煮好**，机器根据烹饪时间自行倒推开始煮的时刻，因此 `T` **不减烹饪时长**。默认步进 10 分钟，最大预约 24 小时，仅倒计时。

```
N       = 当前时刻向下取整到分钟
D       = 该行时刻下一次出现（今天剩余 ≥ step 取今天，否则顺延到明天）
maxSet  = floor(1440 / step) × step
T       = min( floor((D − N) / step) × step, maxSet )
实际完成 = N + T
```

- **向下取整**：`N + T ≤ D`，饭只会提前煮好进保温，绝不晚于右边那个出门时刻。默认 10 分步进最多提前 9 分钟，已确认可接受。
- **`maxSet` 兜底**：自定义步进不整除 1440（如 25 分）时，顺延到明天后直接取整会算出「24 小时 10 分」这种机器按不进去的值；收敛到 1425 分后仍可按、仍不迟到，只是提前更多，由「实际完成」小字如实显示。
- 每行都取下一次出现，加上 `maxSet`，`T ≤ 24 小时` 恒成立，不需要上限警示或「回来设置」提醒。

示例（周一 21:37，步进 10 分）：

| 目标 | 现在设为 | 实际完成 |
|---|---|---|
| 07:30 明天 | 9 小时 50 分 | 07:27 |
| 08:00 明天 | **10 小时 20 分** | 07:57 |
| 09:30 明天 | 11 小时 50 分 | 09:27 |

---

## 构建与安装

需要 **JDK 17** 与 Android SDK（compileSdk 35）。仓库自带 Gradle Wrapper，无需本机预装 Gradle。

```bash
# 单元测试（纯 JVM，不需要模拟器）
./gradlew :app:testDebugUnitTest

# 构建可安装 APK
./gradlew :app:assembleDebug

# 产物
app/build/outputs/apk/debug/app-debug.apk

# 安装到手机（需开启 USB 调试）
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

最低支持 **Android 8.0（API 26）**，目标 API 35。debug APK 已带调试签名，可直接安装；发正式版时再在 `app/build.gradle.kts` 配置 release signingConfig。

**权限**：`AndroidManifest.xml` 里没有 `INTERNET`，不申请任何运行时权限。数据只存本机 `SharedPreferences`（键 `rice_cooker_android_v1`），时间取设备系统时钟，不联网、不校时、不上传。

---

## 工程结构

```
app/src/main/java/com/wzzhuz/ricecooker/schedule/
├── ScheduleCalculator.kt   # 纯 Kotlin 计算核心，无 Android 依赖，可 JVM 单测
├── ScheduleRepository.kt   # SharedPreferences 自动持久化
├── ScheduleViewModel.kt    # 每秒时钟流 + 设置状态
├── MainActivity.kt         # ComponentActivity 入口
└── ui/
    ├── Theme.kt            # 深浅色调色板（20:00–07:00 自动切换）
    └── ScheduleScreen.kt   # 全部界面：列表、左滑删除、TimePicker、步进面板

app/src/test/.../ScheduleCalculatorTest.kt   # 18 个 JVM 单元测试
design-reference/                            # 网页原型 + 设计文档 + 预览图（只读参考）
.github/workflows/android.yml                # CI：单测 + 构建 APK + 上传 artifact
```

---

## 自动编译（GitHub Actions）

push / PR / 手动触发，依次执行：

1. 准备 JDK 17 与 Gradle 缓存；
2. `:app:testDebugUnitTest` —— 覆盖睡前设置、跨天顺延、24 小时边界、自定义 25 分兜底、任意时刻×任意步进「绝不迟到」不变量、日期与深浅色文案等 18 个用例；
3. `:app:assembleDebug` —— 产出可安装 APK；
4. 上传三个 artifact：`rice-cooker-debug-apk`（可安装 APK）、`unit-test-report`（测试报告）、`gradle-build-log`（构建日志）。

工作流只申请 `contents: read`，不依赖 Pages 或其他仓库设置。

---

## 与网页原型的差异（原生化调整）

| 项 | 网页原型 | 安卓实现 | 原因 |
|---|---|---|---|
| 添加时间 | 底部展开面板 | 原生 `TimePicker` 对话框 | 安卓平台习惯，且步进面板互斥更自然 |
| 左滑删除 | DOM touch 事件 | `pointerInput` + `animateDpAsState` | 同样做到「按钮滑入、行内容不位移」 |
| 虚线按钮 | CSS `dashed` | `Modifier.dashedBorder`（Canvas 自绘） | Compose 原生 border 不支持虚线 |
| 数据 | `localStorage` 键 `rice_cooker_v5` | `SharedPreferences` 键 `rice_cooker_android_v1` | 平台原生存储；不做跨端迁移 |
| 模拟时间 | URL 参数 `?sim=` | 单元测试直接注入 `LocalDateTime` | 原生没有 URL，测试在 domain 层做更可靠 |

界面层级、字号、颜色、文案、计算规则、交互取舍全部保持一致：左侧 27sp 橙色预约值是行内唯一主角，右侧 20sp 预期时刻不与左侧等大，下方 10.5sp 灰字为实际完成时间。

---

## 设计取舍（沿用 v0.5）

| 保留 | 砍掉 | 理由 |
|---|---|---|
| 一张实时列表 | 工作日/周末分档、临时改、跳过今天 | 多个时刻并列，看另一行 / 加一行 / 删一行即可 |
| 步进设置（一个文字链） | 取整方向、设备档案、上限警示 | 步进会改变所有行取值且因机型而异；其余固化或问题不再发生 |
| 「实际完成」小字 | 烹煮档与「来不及」拦截 | 烹饪时长不进公式；边缘场景由用户自己判断 |
| 自动深色、左滑删除、自动保存 | 保存按钮、确认弹窗、撤销、设置后锁定 | 零控件目标；重加只要 2 秒 |

两个已知代价：**不校验 `T < C`**（剩 5 分钟却要煮 30 分钟的饭），以及**按完若停留页面数字仍会变**。都只在边缘场景出现，日常睡前设置不受影响。

完整推导、线框、验收用例与 OpenSpec 规格见 [设计归档仓库](https://github.com/wzzhuz/rice-cooker-schedule)，本仓库的 `design-reference/` 也留了一份快照。
