# 课程大纲与内容生成标准

以 MySQL 8.4 为教学标准，中文讲解，代码、原始表字段用英文。不要求强制解锁。四阶段20章，题量为同一总题库的章节分配（60+90+80+70=300）。

| 章 | 阶段 | 主题 | 具体知识点与学习目标 | 题量 |
|---|---|---|---|---:|
| 01 | 入门 | 数据库基础 | 库表、行列、数据类型、主键/外键、关系、一对多、NULL 基础；能读数据字典 | 8 |
| 02 | 入门 | 基础查询 | SELECT、FROM、字段选择、DISTINCT、AS、表达式与别名；避免 SELECT * 误用 | 12 |
| 03 | 入门 | 条件筛选 | WHERE、比较符、AND/OR/NOT 优先级、IN、BETWEEN、LIKE、通配符/转义；边界值 | 22 |
| 04 | 入门 | 排序与分页 | ORDER BY、多列排序、ASC/DESC、LIMIT/OFFSET、并列及稳定分页 | 8 |
| 05 | 入门 | 空值处理 | IS NULL、IS NOT NULL、三值逻辑、IFNULL、COALESCE、NULL 与零/空串 | 10 |
| 06 | 核心 | 聚合函数 | SUM、COUNT(*)、COUNT(col)、COUNT(DISTINCT)、AVG、MIN、MAX、NULL 影响 | 15 |
| 07 | 核心 | 分组统计 | GROUP BY、多字段聚合、HAVING、WHERE 区别、严格分组、逻辑顺序 | 18 |
| 08 | 核心 | 多表连接 | INNER/LEFT/RIGHT/CROSS JOIN、自连接、ON、别名、重复行、外连接过滤位置 | 25 |
| 09 | 核心 | 条件表达式 | CASE WHEN、IF、条件聚合、分类及区间边界、ELSE、NULLIF | 12 |
| 10 | 核心 | 子查询 | 标量、多行、相关子查询、IN、EXISTS、NOT EXISTS、NOT IN 与NULL | 20 |
| 11 | 进阶 | 字符串处理 | CONCAT、SUBSTRING、TRIM、REPLACE、CHAR_LENGTH/LENGTH、大小写；清洗与格式 | 10 |
| 12 | 进阶 | 数值及日期 | ROUND、ABS、日期格式、日期差、月份/年份、区间、除零及月边界 | 15 |
| 13 | 进阶 | 集合运算 | UNION/UNION ALL、INTERSECT、EXCEPT、列数/类型、去重、排序范围 | 8 |
| 14 | 进阶 | 公用表表达式 | WITH、多CTE、递归CTE、终止条件、层级查询、复杂语句拆分 | 12 |
| 15 | 进阶 | 窗口函数 | OVER、PARTITION BY、ORDER BY、ROW_NUMBER/RANK/DENSE_RANK、LAG/LEAD、SUM/AVG窗口、ROWS/RANGE、累计与移动平均 | 25 |
| 16 | 进阶 | 数据清洗 | 重复记录、缺失值、异常值规则、CAST、统一分类、清洗顺序 | 10 |
| 17 | 实战 | 数据表管理 | CREATE/ALTER/DROP、INSERT/UPDATE/DELETE、约束、事务基础；兼容子集实操 | 10 |
| 18 | 实战 | 查询优化 | 索引、选择性、复合索引、执行计划、避免全表扫描的常见思路；原生执行计划理论 | 10 |
| 19 | 实战 | 业务数据分析 | 六项目各5任务，指标口径见 DATABASE | 30 |
| 20 | 实战 | 综合挑战 | 多知识点、时间序列、排名、连接、分析指标、面试综合题 | 20 |

## 单知识点内容结构
稳定ID、章ID、标题、前置知识、学习目标、概念（先直观再技术）、原生语法模板、参数/子句解释、执行逻辑、最小原始表（2–8行）、可验证查询代码、真实结果、业务使用场景、易混淆语法、常见错误及正确修正、边界注意、方言支持状态、相关课/卡/题ID。难点配数据流/步骤图（例如连接匹配、分组归类、窗口内排序）；不要用大量文字代替例子。并非每节都必须有全部栏目，但语法、例子、解释、兼容状态必须明确。对理论内容注明不能本地实操。

## 第07章示例：分组统计
原始 orders：category 为护肤品、化妆品、护肤品、化妆品、日用品；sales 为800、400、600、300、1500。各组总额为1400、700、1500。逻辑顺序 FROM→WHERE→GROUP BY→聚合→HAVING→SELECT→ORDER BY→LIMIT（作为教学逻辑模型，不是物理执行计划）。

```sql
SELECT category, SUM(sales) AS total_sales
FROM orders
GROUP BY category
HAVING SUM(sales) > 1000
ORDER BY total_sales DESC;
```
结果：日用品1500、护肤品1400。WHERE 针对分组前的行，HAVING 针对分组后的条件，不能用 WHERE SUM(sales)。不要误称 HAVING 在无 GROUP BY 时必然报错。对严格分组模式举出未聚合非分组列的不确定性及函数依赖例外。原例在小型课程专用夹具中运行，正式零售数据表以 DATABASE 为准。

## 质量门槛
知识点不得堆砌近义重复内容；每个示例独立可运行（理论示例明确说明），预期结果由数据库验证而非人工臆测。章节与标签覆盖矩阵必须可查询，题库和闪卡与知识点交叉关联。先完成第02、07、08、15章小样及实测后再生成全部内容。不得用“待补充”占位课程或把设计图内容直接当作题库。
