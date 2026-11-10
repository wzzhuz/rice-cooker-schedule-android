# 电饭煲预约助手 · 项目归档

**版本**：v0.5（定稿）
**归档日期**：2026-11-11
**形态**：纯静态单页原型 + 需求与界面设计 + OpenSpec 规格
**仓库**：[wzzhuz/rice-cooker-schedule](https://github.com/wzzhuz/rice-cooker-schedule)（公开）
**自动编译**：GitHub Actions `.github/workflows/build.yml`，push / PR 自动触发

---

## 一句话目标

打开 App，扫一眼列表，找到你要的那个时间，照着左边的数字按电饭煲。全程不需要任何设置。

---

## 归档文件清单

| 路径 | 内容 |
|---|---|
| `docs/需求与界面设计_v0.5.md` | 需求澄清、计算模型、界面线框、交互、取舍与验收用例（最终版） |
| `prototype/index.html` | 可直接用浏览器打开的高保真原型，无需构建、无需依赖、离线可用 |
| `previews/主界面.png` | 周一 21:37 夜间深色，三行记录（含「实际完成」小字） |
| `previews/步进设置.png` | 周二 08:05 日间浅色，展开步进面板（预设 + 自定义输入框） |
| `openspec/project.md` | 项目上下文、领域约束、技术栈、范围外事项 |
| `openspec/AGENTS.md` | 后续维护时的约定与验证方式 |
| `openspec/specs/schedule-list/spec.md` | 当前生效的行为规格（Requirement + Scenario，可验收） |
| `openspec/changes/archive/2026-11-11-rice-cooker-schedule/` | 本次变更的 proposal / design / tasks / spec delta，已归档 |
| `.github/workflows/build.yml` | GitHub Actions：自动编译、验收、生成预览、发布 Pages |
| `.github/scripts/build.py` | 构建脚本：文件校验、JS 语法检查、无头浏览器验收、打包 |

> `dist/` 与 `电饭煲预约助手_项目归档.zip` 是自动编译的**产物**，不入库。

---

## 自动编译（GitHub Actions）

项目是纯静态单页，没有传统意义上的「编译」。这里的「编译」定义为一条可复现的流水线：**校验 → 验收 → 生成预览 → 组装站点 → 打包 → 产出 artifact**。

推送到 `main` 或提 PR 时自动触发，也可在 Actions 页手动运行：

1. **文件校验**：12 项归档文件齐全、原型内联 JS 通过 `node --check`、规格里 Requirement / Scenario 数量达标。
2. **浏览器验收**（Playwright + Chromium，26 项）：注入 `?sim=` 模拟时间，逐条核对 `spec.md` 的场景——睡前正常三行、部分行已过自动顺延、目标时刻已到跳 24 小时、自定义 25 分不整除 1440 的 `maxSet` 兜底、非法步进拦截、添加/删除/自动保存、夜间深色与日间浅色、无运行时错误。
3. **生成预览**：把两张界面预览图重新渲染到 `dist/previews/`（与仓库中的归档图一致）。
4. **组装站点**：`dist/` 放入原型入口 `index.html`、设计文档、OpenSpec 全文与 `BUILD_INFO.json`（版本、commit、验收通过数）。
5. **打包**：生成 `电饭煲预约助手_项目归档.zip` 与 `rice-cooker-build` artifact（保留 30 天，含 `dist/` 站点与 `BUILD_INFO.json`）。

工作流只申请 `contents: read`，不依赖 Pages 管理权限，因此不受仓库 Pages 配置影响，任何情况下都能完整跑通。若日后需要在线预览，可在仓库 Settings → Pages 手动启用「GitHub Actions」构建，再按 `.github/workflows/build.yml` 末尾注释补三个部署步骤即可。

本地复现：

```bash
pip install playwright && playwright install --with-deps chromium
python .github/scripts/build.py          # 26/26 通过后会生成 dist/ 与归档 zip
```

---

## 已由客服确认的设备事实

| 项 | 值 |
|---|---|
| 预约语义 | **几小时后煮好**，机器根据烹饪时间自行倒推开始时刻 |
| 最小步进 | 10 分钟（默认；预设 10/15/30/60，另可自定义 10–240 整数分钟） |
| 最大预约 | 24 小时 |
| 输入方式 | 仅倒计时 |

**取整方向**：向下取整，宁可提前煮好进入保温，绝不晚于预期完成时刻。默认 10 分步进时最多提前 9 分钟，已确认可接受。

---

## 核心计算

```
N       = 当前时刻向下取整到分钟
D       = 该行时刻下一次出现（今天剩余 ≥ step 取今天，否则顺延到明天）
maxSet  = floor(1440 / step) × step
T       = min( floor((D − N) / step) × step, maxSet )
实际完成 = N + T
```

- `T` 不减烹饪时长——机器自己会扣。
- `maxSet` 保证自定义步进不整除 1440 时也不会算出超过 24 小时、机器按不进去的值；右侧「实际 HH:MM 完成」如实显示。
- 每行都取下一次出现，因此 `T ≤ 24 小时` 恒成立，不需要上限警示或「回来设置」提醒。

---

## 如何预览

直接双击 `prototype/index.html`（或用浏览器打开），无需服务器、无需安装依赖。

边界态用 URL 参数模拟时间，界面上没有任何演示控件：

```
prototype/index.html?sim=2026-11-09T21:37     # 睡前正常场景
prototype/index.html?sim=2026-11-10T08:05     # 部分行已过、自动顺延
prototype/index.html?sim=2026-11-11T07:30     # 目标时刻已到、跳到 24 小时
```

数据存浏览器 `localStorage`（键 `rice_cooker_v5`），刷新保持；旧版键会自动迁移，不丢已添加的时刻与步进设置。

---

## 设计取舍（简表）

| 保留 | 砍掉 | 理由 |
|---|---|---|
| 一张实时列表 | 工作日/周末分档、临时改、跳过今天 | 多个时刻并列，看另一行 / 加一行 / 删一行即可 |
| 步进设置（一个文字链） | 取整方向、设备档案、上限警示 | 步进会改变所有行取值且因机型而异；其余固化或问题不再发生 |
| 字样「实际完成」小字 | 烹煮档与「来不及」拦截 | C 不进公式；边缘场景由用户自己判断 |
| 自动深色、左滑删除、自动保存 | 保存按钮、确认弹窗、撤销、设置后锁定 | 零控件目标；重加只要 2 秒 |

两个已知代价：**不校验 `T < C`**（剩 5 分钟却要煮 30 分钟的饭），以及**按完若停留页面数字仍会变**。均只在边缘场景出现。

---

## 唯一遗留待确认

自定义步进范围暂定 **10–240 整数分钟**。若以后遇到更大步进的机器，放宽上限只需改一个常量。其余前几版悬着的问题（工作日分档、焖饭余量、取整方向、到点提醒）均已确认或不再需要决策。

---

> 本归档不含任何密钥、账号或用户隐私数据。原型为纯前端静态页面，不联网、不上传。
