# 电饭煲预约助手 · Android 应用

[![Android 构建与测试](https://github.com/wzzhuz/rice-cooker-schedule-android/actions/workflows/android.yml/badge.svg)](https://github.com/wzzhuz/rice-cooker-schedule-android/actions/workflows/android.yml)

**版本**：v0.5.0（对应设计定稿 v0.5）
**仓库**：`wzzhuz/rice-cooker-schedule-android`（公开）
**形态**：Kotlin + Jetpack Compose 原生安卓应用，手机本地运行，**完全离线**

> 打开 App，扫一眼列表，找到你要的那个时间，照着左边的数字按电饭煲。全程不需要任何设置。

---

## 设计归档在哪

**全部设计资料已完整归档在本仓库的 `design-reference/`，自包含、不依赖外部仓库。**

| 位置 | 内容 |
|---|---|
| **`design-reference/`（本仓库）** | **完整镜像**：需求与界面设计文档、OpenSpec 规格（`project.md` / `AGENTS.md` / `specs` / `changes`）、网页版高保真原型、两张界面预览图、原仓库归档说明与 CI 参考 |
| [wzzhuz/rice-cooker-schedule](https://github.com/wzzhuz/rice-cooker-schedule) | 原设计仓库，**后续可能删除**；删除不影响本仓库的任何资料 |

网页原型是设计验证工具，不是交付形态。安卓工程以 `app/src/main` 下的 Kotlin 代码为准，`design-reference/` 只读、Gradle 不引用。规格与实现的对应关系见 `design-reference/README.md`。

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

最低支持 **Android 8.0（API 26）**，目标 API 35。仓库内 `app/debug.keystore` 是一把**固定的 debug 密钥**，debug 与 release 默认都用它签名，保证各批次 APK 签名一致、可直接覆盖安装；要出可对外分发的正式包，配齐 4 个签名 Secret 即可自动切换（见「发版」）。

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

app/debug.keystore                           # 固定 debug 签名密钥（公开仓库内，仅供自用）
app/src/test/.../ScheduleCalculatorTest.kt   # 18 个 JVM 单元测试
design-reference/                            # 设计归档完整镜像：docs / openspec / prototype / previews（只读）
.github/workflows/android.yml                # 日常 CI：push 触发，单测 + debug APK
.github/workflows/release.yml                # 发版 CI：手动触发，release APK + 坚果云 WebDAV
.github/scripts/prepare-signing.sh           # 签名密钥准备（正式 / 固定 debug 自动切换）
```

---

## 自动编译（GitHub Actions）

**日常 CI** `.github/workflows/android.yml`：push / PR / 手动触发，依次执行：

1. 准备 JDK 17 与 Gradle 缓存；
2. `:app:testDebugUnitTest` —— 覆盖睡前设置、跨天顺延、24 小时边界、自定义 25 分兜底、任意时刻×任意步进「绝不迟到」不变量、日期与深浅色文案等 18 个用例；
3. `:app:assembleDebug` —— 产出可安装 APK；
4. 上传三个 artifact：`rice-cooker-debug-apk`（可安装 APK）、`unit-test-report`（测试报告）、`gradle-build-log`（构建日志）。

工作流只申请 `contents: read`，不依赖 Pages 或其他仓库设置。**发版 CI 是另一条独立工作流**，见下一节。

---

## 发版：编译 Release 包并推送坚果云

`.github/workflows/release.yml`，**只手动触发**：仓库 → Actions → 左侧「编译 Release 包并推送坚果云」→ Run workflow。

### 输入项

| 输入 | 默认 | 说明 |
|---|---|---|
| `version_name` | 留空 | 留空读 `app/build.gradle.kts` 的 `defaultVersionName`；填了会同时传给 Gradle，APK 内版本号与文件名一致。发版后请同步改文件 |
| `upload_nutstore` | **true** | 上传到坚果云。反复调试时手动关掉，不必删 Secret |
| `nutstore_folder` | `rice-cooker-releases` | 统一父目录 `/my-apps/` 下的应用子目录名，纯英文/数字 |
| `create_release` | **false** | 是否额外打 tag 并建 GitHub Release。**与坚果云上传完全解耦**——只想要国内可下载的安装包时，不必占用版本号 |
| `minify` | false | 是否开 R8 混淆与资源压缩 |

产物固定命名 `rice-cooker-v<版本>.apk`（纯 ASCII），同时上传到 Actions Artifacts 与坚果云 `/my-apps/rice-cooker-releases/`。

### 目录约定（所有应用统一）

所有应用的发布包统一放在坚果云 `/my-apps/` 下：本项目是 `/my-apps/rice-cooker-releases/`，`lifelog` 是 `/my-apps/lifelog-releases/`。父目录 `my-apps` 是**全局常量**，写死在工作流里；只有应用子目录保留 `nutstore_folder` 可临时覆盖。这样坚果云根目录只留一个入口，不再随项目数增长。

工作流会做**两级 MKCOL**（先 `/my-apps` 再应用子目录，201/405 都算成功）——WebDAV 的 MKCOL 不创建多级目录，少建一级会返回 409。

**旧目录不迁移**：此前发版落在根目录 `/rice-cooker-releases/` 的 APK 保留为历史存档，新发版一律进新路径。坚果云免费版「空间 = 累计上传流量，删了也不释放」，且 WebDAV `MOVE` 未经实测，搬动不省空间、只增风险。

### 需要配置的 Secret

**坚果云（必配，否则上传步骤跳过但构建仍成功）**

| Secret | 值 |
|---|---|
| `JGY_USER` | 坚果云账号（一般是注册邮箱） |
| `JGY_APP_PASSWORD` | **应用密码**，坚果云网页端 → 账户信息 → 安全选项 → 第三方应用管理 → 添加应用密码。**不是登录密码**，自 2023 年起登录密码直连 WebDAV 会返回 401/403 |

**正式签名（可选，4 项必须齐全）**

`RELEASE_KEYSTORE_BASE64`（`base64 -w 0 your.keystore`）、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`（PKCS12 下须与 `KEYSTORE_PASSWORD` 相同）。配齐后 `prepare-signing.sh` 自动切到正式密钥；缺任一项则回退仓库内 `app/debug.keystore`。

> ⚠️ `app/debug.keystore` 提交在**公开仓库**里，密钥是公开的，任何人都能签出同名同签名的包。**仅供自用，请勿对外分发**。若手机上装过早期由 CI 随机 debug 密钥签出的包，换成固定密钥后签名不同，需要先卸载再装（会丢掉本机保存的目标时刻列表，重新加一次即可）。

### 免费版配额

坚果云免费版每月上传 **1GB**、下载 3GB、单文件 500MB，且**总空间 = 累计上传流量，删旧文件也不释放**。所以：

- 只给手动触发的 release 构建加上传，**绝不给每次 push 的 debug 构建加**；
- 做成布尔开关而不是「Secret 配了就传」，调试期间可临时关掉。

### 上传实现要点

四段式 WebDAV：`PROPFIND` 认证测试（只读、不耗流量）→ 两级 `MKCOL`（先 `/my-apps` 再应用子目录，201 新建 / 405 已存在）→ `PUT` 上传（201 新建 / 204 覆盖），最后 `HEAD` 校验文件确实落盘。状态码与排查建议直接写进 Job Summary，`trap dump EXIT` 保证失败时诊断日志不丢。

几个刻意的设计，均来自 `my-app-workflow/recipes/上传APK到坚果云.md` 与 `lifelog/.github/workflows/release.yml` 的实测经验：

| 决策 | 理由 |
|---|---|
| 不用 `curl --fail`、脚本不 `set -e` | `--fail` 会在 HTTP 错误时直接退出，状态码和响应体都拿不到，日志只剩一句无信息量的报错 |
| 两级 `MKCOL` 共用 `mkcol()` | 所有应用统一放在 `/my-apps/` 下，根目录只留一个入口；WebDAV 不创建多级目录，父目录缺失时子目录直接 409 |
| 上传带 `--retry 2 --retry-delay 15 --max-time 900` | GitHub runner 在美国，跨境链路抖动常见；`HTTP 000` 是网络层失败，不是配置错 |
| 文件名纯 ASCII | GitHub Release 资产接口会**静默丢弃**中文且不报错，而 Actions artifact 名支持 Unicode，两边显示会不一致 |
| 签名校验用 `apksigner`，不用 `keytool -jarfile` | `keytool -printcert -jarfile` 只能读 JAR v1 签名，AGP 8.x + minSdk 26 默认只签 v2/v3，会报「not a signed jar file」并在 `bash -e` 下直接中断。首次实跑就踩到了，已改用 Android SDK 自带的 `apksigner verify --print-certs`，并把证书输出写进 Job Summary |
| 双向校验签名来源 | 不信脚本自己打印的「✅ 使用正式密钥」——Secret 配漏时同样会打印。声明用正式密钥就不能是 debug 证书，声明回退就必须确实是那把固定密钥 |
| `versionCode` 由版本名推导 | 此前恒为 1，连发两版系统判不出新旧、无法覆盖升级 |
| `create_release` 与上传解绑 | lifelog 原版把上传绑在「创建 GitHub Release」上，不勾打 tag 就不推坚果云；本项目的诉求是「出包 + 推国内网盘」，不该被版本号牵着走 |

---

## 与网页原型的差异（原生化调整）

| 项 | 网页原型 | 安卓实现 | 原因 |
|---|---|---|---|
| 添加时间 | 底部展开面板 | 原生 `TimePicker` 对话框 | 安卓平台习惯，且步进面板互斥更自然 |
| 左滑删除 | DOM touch 事件 | `pointerInput` + `animateFloatAsState` | 同样做到「按钮滑入、行内容不位移」 |
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

完整推导、线框、验收用例与 OpenSpec 规格全部收在 **`design-reference/`**（`docs/`、`openspec/`、`prototype/`、`previews/`），自包含、不依赖任何外部仓库；清单与查阅方式见 `design-reference/README.md`。原设计仓库即使删除，本仓库资料依然完整。
