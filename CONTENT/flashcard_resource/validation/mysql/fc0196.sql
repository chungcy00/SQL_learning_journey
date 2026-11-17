-- fc0196：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `refund_metric` (`source_id` INT, `discounted_before_refund_cents` INT, `refund_cents` INT, `after_gross_cents` INT, `returned_cost_cents` INT) ENGINE=InnoDB;
INSERT INTO `refund_metric` VALUES (1, 10000, 1500, 3000, 800), (2, 0, 0, 0, 0);
SELECT source_id,100.0*refund_cents/NULLIF(discounted_before_refund_cents,0) AS refund_pct,after_gross_cents,after_gross_cents+refund_cents-returned_cost_cents AS before_refund_profit FROM refund_metric ORDER BY source_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["source_id", "refund_pct", "after_gross_cents", "before_refund_profit"], "rows": [[1, 15.0, 3000, 3700], [2, null, 0, 0]]}
