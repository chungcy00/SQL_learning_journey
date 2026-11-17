-- fc0155：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `inv_cost` (`category_id` INT, `opening_cents` INT, `closing_cents` INT, `sales_cost_cents` INT) ENGINE=InnoDB;
INSERT INTO `inv_cost` VALUES (1, 100000, 200000, 450000), (2, 0, 0, 1000);
SELECT category_id,ROUND(1.0*sales_cost_cents/NULLIF((opening_cents+closing_cents)/2.0,0),2) AS turnover,ROUND(365.0/NULLIF(1.0*sales_cost_cents/NULLIF((opening_cents+closing_cents)/2.0,0),0),2) AS days FROM inv_cost ORDER BY category_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["category_id", "turnover", "days"], "rows": [[1, 3.0, 121.67], [2, null, null]]}
