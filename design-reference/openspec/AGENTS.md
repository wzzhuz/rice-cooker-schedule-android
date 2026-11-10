# AGENTS.md · 维护约定

## 阅读顺序

1. `project.md` —— 领域语义、约束、已确认的设备事实、取舍。
2. `specs/schedule-list/spec.md` —— **当前生效**的行为规格，每个 Requirement 都带可验收 Scenario。
3. `changes/archive/` —— 历史变更的提案、设计、任务与 spec delta，只读。
4. `../docs/需求与界面设计_v0.5.md` —— 完整推导、线框、验收用例。
5. `../prototype/index.html` —— 可运行原型，规格的最终实现。

## 变更流程

1. 在 `changes/<change-id>/` 下写 `proposal.md`（Why / What Changes / Impact）、`design.md`（关键决策）、`tasks.md`（可勾选任务）。
2. 涉及行为变化时，在同一 change 下写 `specs/schedule-list/spec.md` 的 delta（`## ADDED/MODIFIED/REMOVED Requirements`），每个 Requirement 至少一个 `#### Scenario:`。
3. 实现后按 `tasks.md` 逐项勾选，验证通过后把 change 移入 `changes/archive/<YYYY-MM-DD>-<change-id>/`，并把 delta 合并进 `specs/schedule-list/spec.md`。
4. 归档产物同步更新 `../README.md` 的文件清单与版本号。

## 硬约束（改动时不要破坏）

- **预约 = 几小时后煮好**，公式里永远不要减烹饪时长。
- 主界面可见控件只有一个「+ 添加时间」按钮；新增能力优先做成说明行里的文字链，不要新增卡片、标签页或设置页。
- 不做保存按钮、确认弹窗、撤销、主题切换按钮、重置按钮。
- 不做工作日/周末分档、临时改、跳过今天、焖饭余量、取整方向、到点提醒。
- 改动即时写 `localStorage`，刷新保持，并能迁移旧键。
- 向下取整语义不变：实际完成时刻永远不晚于预期完成时刻。

## Spec 写法

- Requirement 用 `### Requirement: <名称>`，正文用 `SHALL` / `SHALL NOT` / `MUST`。
- Scenario 用 `#### Scenario: <名称>`，条件用 `- **WHEN** ...`，结果用 `- **THEN** ...`，多个条件可并列多个 `- **AND**`。
- Scenario 尽量写具体时刻与具体数值，便于直接当验收用例跑。

## 验证方式

- 浏览器直接打开 `../prototype/index.html`，用 `?sim=2026-11-09T21:37` 等参数注入模拟时间。
- 至少覆盖：正常睡前设置、部分行已过自动顺延、目标时刻已到跳 24 小时、自定义步进 25 分（不整除 1440）、非法步进输入、刷新持久化、旧版迁移、添加/删除/空态、夜间深色。
- 规格中的数值以 `specs/schedule-list/spec.md` 的 Scenario 为准；实现与规格不一致时先改规格或先改实现，二者必须对齐。
