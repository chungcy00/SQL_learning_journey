-- fc0118：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `monthly` (`month` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `amount` INT) ENGINE=InnoDB;
INSERT INTO `monthly` VALUES ('2025-01', 100), ('2025-02', 0), ('2025-03', 150), ('2025-04', 150);
SELECT month,AVG(amount) OVER(ORDER BY month ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS moving FROM monthly ORDER BY month;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["month", "moving"], "rows": [["2025-01", 100.0], ["2025-02", 50.0], ["2025-03", 75.0], ["2025-04", 150.0]]}
