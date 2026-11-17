# 题库生成及判题内容规范

## 题量（同一题库的不同维度）
CURRICULUM各章题量合计300；第19章30个实战任务包含在内。题型：选择30、代码填空35、独立编写120、代码纠错30、结果预测25、业务情景60。难度：入门60、简单90、中等90、困难45、挑战15。第19章30题与第20章20题均归入业务情景60；前18章另10道业务情景题。保留现有分配，不重新生成。

## 数据结构
每题稳定 id、chapterId、knowledgePointIds、type、difficulty、title、context、requirements、datasetId或专用fixtureId、schemaVersion、initialCode、options（选择）、acceptedBlanks（填空，说明等价形式）、expectedOutput、referenceSolutions、hints（三层，非编程可缩减）、explanation、commonErrors、testFixtureIds、orderPolicy、columnPolicy、numericTolerance、requiredAstFeatures（可选）、supportStatus、interviewTag、projectId/taskId（可选）、contentVersion。明确输入表、输出列/别名/顺序、排序/并列、NULL与精度、计量单位、时间范围、是否含边界。不要求题目未给的条件。

## 六题型
- 选择：4个合理选项，单选一个正确项，答案及每个干扰项原因；不使用歧义或多个合理答案。
- 填空：代码保留上下文，允许键盘输入；可运行则对完整代码进行语义/结果验证，非执行型才按明确可接受答案比较。
- 独立编写：从需求写完整SQL；除非专项约束，等价写法均可通过。
- 纠错：给出真正错误代码，修改全文；解析错误所在及业务后果。
- 预测：原始数据+代码+明确要求，提交前不开放执行或答案；可选择结果（表格）或填写规定值，按题设校验。
- 情景：用真实业务描述设定指标、业务粒度、表关系、时间/币种，避免缺失定义；查询结果比对。

## 三层提示及反馈
第一级提示思路不送答案；第二级提示语法/关系；第三级给局部结构。答案解析说明为什么、执行过程、易错区及可选替代解。提交后展示通过/未通过、实际结果、预期结果/可安全公开的差异、影响字段，中文提示+原始错误。使用提示、查看答案、独立答对分开记。运行不正式计分。自动判题细节见 TECHNICAL。

## 正式资源与验收
读取 `CONTENT/question_resource/questions/ch01.json` 至 `ch20.json`，以实际 JSON 字段为准。q0001–q0300，BP01–BP50 对应 q0251–q0300，不重复新增。每题 testFixtures[].path 相对题库资源根，expectedOutputPath 的 #expected 是字段片段。776份夹具包括选择题答案键；270道可执行题实际运行，30道选择题按答案键。结果预测仅比较手工输入的结果，禁止提交前执行；填空补全后真实执行。企业题按数据变体在隔离副本运行。具体列、排序、容差、状态及专项约束按题目字段，不得擅改。无序结果用保留重复行的绝对容差一对一匹配，不使用舍入桶；复用验证器七项回归语义。14道专项语法或计划题需解析结构或可靠执行计划核验，不能用关键词冒充。原生MySQL与手机兼容层均未验收。见 `CONTENT/AUDIT_REPORT.md`、`CONTENT/question_resource/validation/verify_bank.py`。发布前完整复验参考解、等价解、典型错误及业务口径；不得重生成正式内容。
