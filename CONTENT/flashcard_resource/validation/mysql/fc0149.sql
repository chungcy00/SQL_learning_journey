-- fc0149：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `shop_totals` (`store_id` INT, `gross_cents` INT, `discount_cents` INT, `refund_cents` INT, `cost_after_returns_cents` INT, `target_cents` INT) ENGINE=InnoDB;
INSERT INTO `shop_totals` VALUES (1, 120000, 10000, 20000, 54000, 100000), (2, 0, 0, 0, 0, 0);
SELECT store_id,gross_cents-discount_cents-refund_cents AS net_cents,gross_cents-discount_cents-refund_cents-cost_after_returns_cents AS profit_cents,100.0*(gross_cents-discount_cents-refund_cents)/NULLIF(target_cents,0) AS target_pct FROM shop_totals ORDER BY store_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["store_id", "net_cents", "profit_cents", "target_pct"], "rows": [[1, 90000, 36000, 90.0], [2, 0, 0, null]]}
