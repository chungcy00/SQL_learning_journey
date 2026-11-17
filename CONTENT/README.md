# SQL 离线学习资源 · 整合审核版 4.0.0

本包是第十一阶段实际学习内容的统一审核交付，替代此前四个学习内容压缩包；第十阶段UI、视觉素材与产品开发资料仍保留，未包含在本包。这不是已开发完成的软件。

## 正式读取入口
- `courses_resource/manifest.json` → `courses/ch01.json` 至 `ch20.json`（152知识点）；
- `question_resource/manifest.json` → `questions/ch01.json` 至 `ch20.json`（300题），题目夹具从本目录解析；
- `flashcard_resource/manifest.json` → `flashcards/ch01.json` 至 `ch20.json`（200卡），夹具、复习策略从本目录解析；
- `question_resource/databases/manifest.json` → 七套唯一的真实 SQLite 库，另含各自 MySQL 导入脚本和数据字典；
- `enterprise_reference/` → 企业原始CSV、业务任务、原生与兼容查询、完整输出、四份新版规范。企业任务内部相对路径以 enterprise_reference 为基准。

保持四个独立目录，不合并同名 fixtures，不再次套用旧关联补丁，不更换任何稳定编号。各资源内的路径相对于对应资源根；课程的企业脚本、完整结果及数据集路径按修订后的相对路径解析。题库 `expectedOutputPath` 的 #expected 是JSON字段片段，不属于文件名。

## 给 Codex
先读第十阶段 AGENTS.md、PRD.md、DESIGN.md 等，再读本包 enterprise_reference/docs 中修订规范和本包审核报告。对于内容冲突，以本包实际课程、题目、闪卡和夹具为准。不要重新生成课程、题目、闪卡或数据库；不要将BP01–BP50重复新增。只开发手机端离线软件，保留四档FSRS、个人闪卡和数据集，不加入自定义题目、云端账号或实时AI。使用真实执行、隔离副本、隐藏测试和精度规则。原生与手机端验证是发布门槛，不可伪造通过。

## 复验
先备份或在新解压目录运行 `python verify_integrated.py`（Python 3.10+、较新SQLite，实际3.46.1）；先验证全部文件哈希，再复跑课程、数据库、题库和闪卡。原复验程序会重写 recheck.json，若日志改变请重建发布清单。`similarity_scan.py` 可选，需要 scikit-learn，复跑会刷新候选列表。原制作脚本保留作为历史证据，不适合覆盖现有资源。最新验证和限制以 AUDIT_REPORT.md 为准。
