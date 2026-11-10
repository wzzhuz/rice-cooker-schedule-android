# Spec Delta Snapshot · schedule-list

> **归档快照（摘要）**。本变更已于 2026-11-11 完成并归档，delta 已合并进
> `openspec/specs/schedule-list/spec.md`。完整 Requirement 文本、Scenario 与数值验收用例
> 以合并后的规格为准；本文件仅供追溯「这次新增了哪些能力」。

## ADDED Requirements

| Requirement | 一句话约束 | 合并后位置 |
|---|---|---|
| `Real-time countdown rows` | 每行实时显示预约时长，每分钟更新，不显示秒 | `specs/schedule-list/spec.md` |
| `Countdown means "done after"` | 预约 = 几小时后煮好，MUST NOT 减烹饪时长 | 同上 |
| `Step-aligned rounding down` | 按步进向下取整，实际完成不晚于预期完成 | 同上 |
| `Next occurrence and roll-over` | 取下一次出现，标「今天/明天」，不出现 0 分钟 | 同上 |
| `24-hour ceiling guard` | `maxSet` 兜底，`T ≤ 24 小时`，不显示上限警示 | 同上 |
| `Actual completion time` | 预期时刻下方小字显示 `N + T` | 同上 |
| `Target list management` | 默认三行、可加可左滑删、重复忽略、空状态 | 同上 |
| `Step configuration` | 默认 10 分；预设 + 自定义 10–240 整数分钟 | 同上 |
| `Automatic persistence` | 改动即存、无保存按钮、旧版迁移 | 同上 |
| `Visual hierarchy` | 左侧 27px 橙色为主角，右侧 20px 不等大 | 同上 |
| `Night theme` | 20:00–07:00 自动深色，无主题按钮 | 同上 |
| `Zero-control surface` | 主界面只有「+ 添加时间」一个可见按钮 | 同上 |

## MODIFIED Requirements

无（项目首次定稿）。

## REMOVED Requirements

无。相对早期配置式方案移除的能力属于**设计取舍**，记录于 `design.md` 与 `project.md` 的 Out of Scope，不属于本次 delta 的规格删除。

## Representative Scenarios（验收摘要）

- 周一 21:37 / step 10 → `9:50 / 10:20 / 11:50`，实际 `07:27 / 07:57 / 09:27`，均标明天。
- 周二 08:05 → `23:20 / 23:50 / 1:20`，前两行明天、09:30 今天。
- 周三 07:30 → 07:30 行顺延为 `24 小时`，实际 07:30。
- step 25 + 顺延 → `23 小时 45 分`，不超 24 小时。
- 非法步进 `5 / 300 / 12.5` → 行内提示，当前值不变。
