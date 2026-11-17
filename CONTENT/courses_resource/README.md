# 第十一阶段第一部分：课程知识库

以 MySQL 8.4 为教学标准。20章独立 JSON 与对应 Markdown 内容同源生成。课程示例使用小型独立夹具，不是六套正式业务数据库；课堂金额主要为整数以减少精度歧义。第19章采用正式数据字典核心字段的微型演示，部分表名加前缀避免与前面教学夹具冲突，课文注明映射。六项目各五项指标已全部提供课堂示例，共30个知识点；正式计分任务已在题库中完成；升级后的第19、20章含50个企业知识点，使用统一企业数据库。

## 读取方法
读取 manifest.json，按 chapters 顺序加载 courses/chNN.json；knowledgePoints 是每章知识点数组。原始表、代码、步骤、结果、参数均结构化；fixtureIds 指向 fixtures 同名 JSON。每节创建独立数据库，将字段类型及原始行装入，再执行 setupSql（如有）及 sqliteSql。不可污染用户数据库。mysqlSetupSql 为存在差异的原生初始化脚本。

## 验证范围
SQLite 实际执行结果已保存；仅理论示例不执行。直接兼容仅指本例，转换执行为明确示例改写，不代表兼容层已经开发。MySQL 8.4 原生服务不可用，必须在开发环境执行原生查询并进行双引擎结果对照；严格分组、排序规则、类型、精度和事务差异不得忽略。教材展示的错误代码不是可执行示例。题库和闪卡关联已实际写回每个知识点；直接考查与关联复习分别记录。

参考：MySQL 8.4 官方手册 https://dev.mysql.com/doc/refman/8.4/en/ ，函数 https://dev.mysql.com/doc/refman/8.4/en/functions.html ，字符串 https://dev.mysql.com/doc/refman/8.4/en/string-functions.html ，窗口 https://dev.mysql.com/doc/refman/8.4/en/window-functions-usage.html ，隐式提交 https://dev.mysql.com/doc/refman/8.4/en/implicit-commit.html 。

## 原生复验
validation/mysql 下每个知识点有一个独立 SQL 脚本。在真实 MySQL 8.4 的全新一次性测试库中逐个运行，勿使用生产库；脚本尚未在原生 MySQL 执行。文本采用明确二进制排序规则，金额采用 DECIMAL；日期字段使用 DATE。运行后须根据输出列、行及数值语义对照 JSON。

## 审核版本接入
所有相对路径以本资源目录为基准；企业脚本和完整结果位于 ../enterprise_reference，七套SQLite数据库位于 ../question_resource/databases。旧版验证报告保留为历史证据，最新结果见上一级 audit_results.json。

整合审核版本 4.0.0：以根目录 AUDIT_REPORT.md 与 audit_results.json 为最新结果；早期阶段报告及制作脚本仅为历史证据，不应据其重新生成或覆盖修订内容。
