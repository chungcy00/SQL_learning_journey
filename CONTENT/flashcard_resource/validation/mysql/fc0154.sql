-- fc0154：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `snap` (`warehouse_id` INT, `product_id` INT, `quantity` INT, `unit_cost_cents` INT) ENGINE=InnoDB;
INSERT INTO `snap` VALUES (1, 1, 80, 10000), (1, 2, 120, 5000), (1, 3, 100, 7000);
CREATE TABLE `outbound` (`warehouse_id` INT, `product_id` INT, `quantity` INT) ENGINE=InnoDB;
INSERT INTO `outbound` VALUES (1, 1, 30), (1, 3, 10);
SELECT s.product_id FROM snap s LEFT JOIN outbound o ON s.warehouse_id=o.warehouse_id AND s.product_id=o.product_id WHERE s.quantity*s.unit_cost_cents>500000 AND COALESCE(o.quantity,0)<20 ORDER BY s.product_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["product_id"], "rows": [[2], [3]]}
-- 典型错误在另一个相同初态的独立测试库复验；不可与修正语句连续更新同一状态。
