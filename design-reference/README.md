# 设计归档（自包含镜像）

> **本目录是 [wzzhuz/rice-cooker-schedule](https://github.com/wzzhuz/rice-cooker-schedule) 的完整内容镜像。**
> 该仓库后续可能删除；删除后，本目录仍是 v0.5 定稿的唯一完整存档，不依赖任何外部链接即可查阅全部设计文档、OpenSpec 规格与网页原型。

**归档日期**：2026-11-11
**来源**：`wzzhuz/rice-cooker-schedule` 的 `main` 分支（按文件 SHA 逐个取回，内容逐字节一致）
**性质**：只读资料，安卓工程不引用本目录，Gradle 构建完全不依赖它

> `归档总说明.md` 是原仓库 README 的逐字节副本，其中「仓库名、自动编译、Pages」等描述属于当时状态，仅作历史记录；**当前**的构建与发布方式以本仓库根目录 `README.md` 为准。

---

## 目录清单

| 路径 | 内容 |
|---|---|
| `归档总说明.md` | 原仓库 `README.md` 副本：文件清单、设备事实、核心公式、预览方式、取舍简表 |
| `docs/需求与界面设计_v0.5.md` | 需求澄清、计算模型、界面线框、交互、取舍与验收用例（最终版） |
| `prototype/index.html` | 网页版高保真原型，浏览器直接打开；`?sim=2026-11-09T21:37` 可注入模拟时间 |
| `previews/主界面.png` | 周一 21:37 夜间深色，三行记录（含「实际完成」小字） |
| `previews/步进设置.png` | 周二 08:05 日间浅色，展开步进面板（预设 + 自定义输入框） |
| `openspec/project.md` | 项目上下文、已确认设备事实、约束、技术栈、关键决策、范围外事项 |
| `openspec/AGENTS.md` | 后续维护约定：变更流程、硬约束、Spec 写法、验证方式 |
| `openspec/specs/schedule-list/spec.md` | **当前生效规格**：12 个 Requirement + 20 个 Scenario，可直接当验收用例 |
| `openspec/changes/archive/2026-11-11-rice-cooker-schedule/` | 本次变更的 proposal / design / tasks / spec delta，已归档 |
| `ci-reference/build.yml`、`build.py` | 原仓库的网页原型自动编译脚本（校验 / Playwright 26 项验收 / 打包），仅作存档参考 |

**两点说明**：

1. 原仓库的 `.github/` 在此改名为 `ci-reference/`，避免与**本仓库**根目录的 `.github/workflows/android.yml`（安卓 CI）混淆。脚本里的相对路径按原仓库结构写成，在此目录下**不能直接运行**，只用于追溯当时的流水线定义。安卓侧的构建与验收请看根目录 `.github/workflows/android.yml` 与 `app/src/test/`。
2. 原仓库的 `.gitignore`（仅 3 行忽略规则）未复制——放在子目录里会意外影响 Git 忽略行为，且无归档价值。其余 14 个文件全部保留。

---

## 内容关系

| 资料 | 安卓实现里的对应位置 |
|---|---|
| `openspec/specs/schedule-list/spec.md` | `app/src/main/java/.../ScheduleCalculator.kt`（计算规则）+ `ScheduleCalculatorTest.kt`（18 个 JVM 用例） |
| `docs/需求与界面设计_v0.5.md` 的界面线框 | `app/src/main/java/.../ui/ScheduleScreen.kt` |
| 网页原型 `prototype/index.html` | 视觉与交互对照；原生化差异见根目录 README「与网页原型的差异」 |

规格与实现保持一致：预约 = 几小时后煮好（不减烹饪时长）、按步进向下取整、`maxSet` 24 小时兜底、绝不迟到。若日后改动，先按 `openspec/AGENTS.md` 的流程更新规格，再改代码与测试。

---

## 快速查阅

```bash
# 直接打开网页原型（无需服务器、无需依赖）
open prototype/index.html          # macOS；Windows 双击即可

# 查看当前生效规格
cat openspec/specs/schedule-list/spec.md

# 看设计推导与验收用例
cat docs/需求与界面设计_v0.5.md
```
