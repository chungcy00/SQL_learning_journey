-- fc0166：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `aging_mini` (`invoice_id` INT, `days_overdue` INT, `balance_cents` INT) ENGINE=InnoDB;
INSERT INTO `aging_mini` VALUES (1, -1, 1000), (2, 0, 1000), (3, 30, 1000), (4, 31, 1000), (5, 91, 1000);
SELECT invoice_id,CASE WHEN days_overdue<=0 THEN '未逾期' WHEN days_overdue BETWEEN 1 AND 30 THEN '1—30' WHEN days_overdue<=60 THEN '31—60' WHEN days_overdue<=90 THEN '61—90' ELSE '90以上' END AS band FROM aging_mini WHERE balance_cents>0 ORDER BY invoice_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["invoice_id", "band"], "rows": [[1, "未逾期"], [2, "未逾期"], [3, "1—30"], [4, "31—60"], [5, "90以上"]]}
