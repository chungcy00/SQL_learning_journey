-- fc0189：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `order_net_mini` (`order_id` INT, `net_cents` INT, `refund_cents` INT) ENGINE=InnoDB;
INSERT INTO `order_net_mini` VALUES (1, 9000, 1000), (2, 5000, 0);
CREATE TABLE `settle_mini` (`order_id` INT, `status` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `amount_cents` INT) ENGINE=InnoDB;
INSERT INTO `settle_mini` VALUES (1, 'settled', 10000), (1, 'failed', 10000), (2, 'settled', 4000);
WITH p AS (SELECT order_id,SUM(amount_cents) AS paid FROM settle_mini WHERE status='settled' GROUP BY order_id) SELECT o.order_id,COALESCE(p.paid,0)-o.refund_cents-o.net_cents AS gap FROM order_net_mini o LEFT JOIN p ON o.order_id=p.order_id WHERE COALESCE(p.paid,0)-o.refund_cents-o.net_cents<>0 ORDER BY o.order_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["order_id", "gap"], "rows": [[2, -1000]]}
-- 典型错误在另一个相同初态的独立测试库复验；不可与修正语句连续更新同一状态。
