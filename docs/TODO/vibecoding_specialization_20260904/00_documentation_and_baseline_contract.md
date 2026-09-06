# 00 文档与基线契约

## 旧实现情况

仓库原先没有 Vibe Coding Workflow 要求的四份有效核心文档。早期生成的 `goal.md` 和 `plan.md` 结构不符合 Skill，并混入未经用户确认的假设；`rules.md` 和 `structure.md` 缺失。

## 意图修正

建立满足 Skill 和仓库自身文档规范的唯一目标、结构、规则和总计划，并明确运行时 TODO 与仓库计划文档不是同一系统。

## 预期新实现

- 四份核心文档章节完整
- 当前事实与目标设计明确分开
- 用户确认项与未知项明确分开
- L3 总计划包含里程碑、任务、验收、风险和回滚
- 本 TODO 文件夹包含全部最小交付步骤

## 作用域

仅限：

- `docs/goal.md`
- `docs/plan.md`
- `docs/rules.md`
- `docs/structure.md`
- `docs/TODO/vibecoding_specialization_20260904/`

不修改业务源码、Gradle、Manifest、数据库或构建环境。

## 验证

```bash
python3 <VIBE_SKILL>/scripts/ensure_core_docs.py --project-root . --check-only
python3 ci/script/check_markdown_links.py

git status --short
git diff --name-only -- app/src build.gradle.kts settings.gradle.kts gradle
```

通过条件：四份核心文档均非 MISSING/INVALID；业务源码差异为空；相对链接有效。

## 验证记录

2026-09-04 自动校验结果：

- 四份核心文档全部为 `EXISTS`，无 `MISSING`、无 `INVALID`
- `rules.md` 出现“关键占位内容尚未替换”警告，经读取校验器源码确认属于校验器缺陷：`rules.md` 的必需标题和唯一占位提示均为 `## 默认规则（创建即生效）`，任何合法文档都会触发该警告
- 核心文档、索引和 13 个步骤文档共 18 个文件，工作区相对链接检查结果为 `broken_links=0`
- 00-12 共 13 个步骤文件均存在且被索引引用
- 13 个步骤文件均未标记为已完成，符合尚未实施的事实
- `app/src`、根 Gradle、`terminal`、`quickjs`、`llm`、`avator` 和 `showerclient` 均无已跟踪源码差异
- 核心文档与本任务目录中没有其他项目名称引用

当前仅等待用户审阅并确认 L3 总计划；未确认前不添加完成标记。

2026-09-05 计划修订与复核记录：

- 根据用户新约束，T01 已从“本地安装 Android 工具链”修订为“GitHub Actions 云测试、固定签名构建和 GitHub Release 基线”
- 版本契约已统一为根 `VERSION`、App `versionName`、确定性 `versionCode`、Git Tag、GitHub Release 和 APK 文件名的一一映射；bootstrap 版本为 `0.0.0`
- 已把 PR、`main` Ruleset、版本递增、失败重跑、固定签名、Release 资产和覆盖升级纳入 T01
- 用户已确认目标 `applicationId` 为 `io.github.black0bag.minibile`，源码 `namespace` 暂时保留；签名资产目录为工作区根级 `.release-signing/`
- 工作区根级 `WORKSPACE.md` 已建立，索引 `.operit/`、`.backup/`、`.release-signing/` 和项目仓库；T01 的明确实施授权仍是剩余门禁
- `ensure_core_docs.py --check-only`：四份核心文档均为 `EXISTS`，无 `MISSING`/`INVALID`；`rules.md` 的既有占位警告仍为校验器误报
- `git diff --check` 通过；13 个步骤文件存在且均未添加完成标记
- 本地 `HEAD` 与 GitHub `main` 仍为 `f323d6c50fa661837fad06d4618462861779b562`；本轮未修改业务源码、Gradle、Workflow、远端 Tag、Release、Secrets 或 Ruleset

## 风险

- 文档可能把候选方案误写成已实现事实
- 文档可能把用户未确认的产品取舍写成已决定
- 核心文档校验器对 `rules.md` 存在不可消除的误报警告，判断通过时必须同时确认无 `MISSING` 和 `INVALID`

## 回滚

这些文件当前均为新增未跟踪文档。如用户拒绝该计划，保留代码库分析资料并根据用户反馈重新编写，不触碰业务源码。
