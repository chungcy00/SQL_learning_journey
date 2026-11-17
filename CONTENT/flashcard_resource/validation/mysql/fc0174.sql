-- fc0174：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `price_history` (`po_item_id` INT, `supplier_id` INT, `product_id` INT, `order_date` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `unit_price_cents` INT) ENGINE=InnoDB;
INSERT INTO `price_history` VALUES (1, 1, 1, '2025-01-01', 100), (2, 2, 1, '2025-01-02', 130), (3, 1, 1, '2025-01-03', 110);
SELECT po_item_id,LAG(unit_price_cents) OVER(PARTITION BY supplier_id,product_id ORDER BY order_date,po_item_id) AS prev FROM price_history ORDER BY po_item_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["po_item_id", "prev"], "rows": [[1, null], [2, null], [3, 100]]}
-- 典型错误在另一个相同初态的独立测试库复验；不可与修正语句连续更新同一状态。
