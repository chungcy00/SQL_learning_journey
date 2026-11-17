-- fc0148：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `stock_last` (`product_id` INT, `quantity` INT, `unit_cost_cents` INT) ENGINE=InnoDB;
INSERT INTO `stock_last` VALUES (1, 12, 1000), (2, 6, 2000), (3, 5, 500);
CREATE TABLE `sale_qty` (`product_id` INT, `qty` INT) ENGINE=InnoDB;
INSERT INTO `sale_qty` VALUES (1, 7), (3, 0);
SELECT s.product_id FROM stock_last s LEFT JOIN sale_qty q ON s.product_id=q.product_id WHERE s.quantity>0 AND COALESCE(q.qty,0)=0 ORDER BY s.product_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["product_id"], "rows": [[2], [3]]}
-- 典型错误在另一个相同初态的独立测试库复验；不可与修正语句连续更新同一状态。
