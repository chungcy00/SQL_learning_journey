-- fc0199：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `report_mini` (`month_key` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `profit_cents` INT, `expense_cents` INT, `labor_cents` INT, `spend_cents` INT, `ordered_cents` INT) ENGINE=InnoDB;
INSERT INTO `report_mini` VALUES ('2025-01', 50000, 10000, 15000, 6000, 30000), ('2025-02', 20000, 8000, 15000, 4000, 10000);
SELECT month_key,profit_cents-expense_cents-labor_cents AS operating_contribution,spend_cents,ordered_cents FROM report_mini ORDER BY month_key;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["month_key", "operating_contribution", "spend_cents", "ordered_cents"], "rows": [["2025-01", 25000, 6000, 30000], ["2025-02", -3000, 4000, 10000]]}
