# 正式资源读取合同
项目根为本包解压后的目录；`CONTENT` 内保持原来的四个独立根。运行时构建映射必须保持相对路径或等价显式映射，不能按文件名全局查找。

| 对象 | 入口（相对项目根） | 路径解析基准 |
|---|---|---|
| 总学习清单 | CONTENT/manifest.json | CONTENT/ |
| 课程 | CONTENT/courses_resource/manifest.json | CONTENT/courses_resource/ |
| 章节 | CONTENT/courses_resource/courses/ch01.json … ch20.json | 同上；knowledgePoints数组 |
| 题库 | CONTENT/question_resource/manifest.json | CONTENT/question_resource/ |
| 章节题 | CONTENT/question_resource/questions/ch01.json … ch20.json | 同上；questions数组 |
| 判题夹具 | CONTENT/question_resource/fixtures/ | testFixtures[].path相对题库根 |
| 数据库 | CONTENT/question_resource/databases/manifest.json | datasets[].path相对题库根 |
| 闪卡 | CONTENT/flashcard_resource/manifest.json | CONTENT/flashcard_resource/ |
| 章节卡 | CONTENT/flashcard_resource/flashcards/ch01.json … ch20.json | 同上；cards数组 |
| 复习配置 | CONTENT/flashcard_resource/review_policy.json | 闪卡根 |
| 企业任务证据 | CONTENT/enterprise_reference/business_tasks/ | 企业资源根 |
| 视觉素材 | ASSETS/INDEX.md | ASSETS/ |
| 页面 | UI/INDEX.md | UI/ |

课程企业案例 result.fullResultPath 可跨至同级 enterprise_reference；需保留其实际结果文件。题库 expectedOutputPath 的 #expected 是JSON片段，不是文件名。课程 fixtureIds 指向课程夹具；闪卡 fixturePath 指向自身夹具。企业 datasetId 通过数据库清单解析；小型题通过自己的 testFixtures 建库，绝不混入企业库。内置内容只读，用户记录单独存储。保留原 contentVersion/schemaVersion，不统一改写；发布包版本12.0.0，内容发行4.0.0。

开发期运行 `python verify_delivery.py`（仅完整性与结构）；在独立副本运行 `python CONTENT/verify_integrated.py`，它会更新复验记录。运行时另建应用资源清单与缓存策略，仅复制实际运行所需资源、依赖及素材；不可将所有审计资料或历史图部署到应用。
