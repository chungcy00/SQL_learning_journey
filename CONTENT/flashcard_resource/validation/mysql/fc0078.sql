-- fc0078：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `clients` (`id` INT, `name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;
INSERT INTO `clients` VALUES (1, '林'), (2, '陈'), (3, '周'), (4, '王');
CREATE TABLE `sales` (`id` INT, `client_id` INT, `store_id` INT, `amount` INT, `day` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `status` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;
INSERT INTO `sales` VALUES (1, 1, 1, 40, '2025-01-02', 'completed'), (2, 1, 1, 60, '2025-01-15', 'completed'), (3, 2, 2, 30, '2025-02-01', 'completed'), (4, NULL, 2, 0, '2025-02-02', 'completed'), (5, 3, 1, 100, '2025-03-01', 'cancelled'), (6, 2, 1, 60, '2025-03-05', 'completed');
SELECT c.id FROM clients c WHERE NOT EXISTS (SELECT 1 FROM sales s WHERE s.client_id=c.id) ORDER BY c.id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["id"], "rows": [[4]]}
-- 典型错误在另一个相同初态的独立测试库复验；不可与修正语句连续更新同一状态。
