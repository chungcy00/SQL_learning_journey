-- fc0164：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `budget_mini` (`department_id` INT, `account_id` INT, `budget_cents` INT) ENGINE=InnoDB;
INSERT INTO `budget_mini` VALUES (1, 1, 10000), (1, 2, 5000), (2, 1, 0);
CREATE TABLE `expense_mini` (`department_id` INT, `account_id` INT, `expense_cents` INT) ENGINE=InnoDB;
INSERT INTO `expense_mini` VALUES (1, 1, 8000), (1, 1, 3500), (2, 1, 1000);
WITH a AS (SELECT department_id,account_id,SUM(expense_cents) AS actual FROM expense_mini GROUP BY department_id,account_id) SELECT b.department_id,b.account_id,COALESCE(a.actual,0)-b.budget_cents AS variance FROM budget_mini b LEFT JOIN a ON b.department_id=a.department_id AND b.account_id=a.account_id ORDER BY b.department_id,b.account_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["department_id", "account_id", "variance"], "rows": [[1, 1, 1500], [1, 2, -5000], [2, 1, 1000]]}
-- 典型错误在另一个相同初态的独立测试库复验；不可与修正语句连续更新同一状态。
