-- fc0186：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `receipt_ledger` (`receipt_id` INT, `accepted` INT) ENGINE=InnoDB;
INSERT INTO `receipt_ledger` VALUES (1, 5), (2, 4), (3, 0), (4, 3);
CREATE TABLE `movement_ledger` (`receipt_id` INT, `qty` INT) ENGINE=InnoDB;
INSERT INTO `movement_ledger` VALUES (1, 5), (2, 2);
SELECT r.receipt_id,r.accepted-COALESCE(m.qty,0) AS gap FROM receipt_ledger r LEFT JOIN movement_ledger m ON r.receipt_id=m.receipt_id WHERE r.accepted-COALESCE(m.qty,0)<>0 ORDER BY r.receipt_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["receipt_id", "gap"], "rows": [[2, 2], [4, 3]]}
-- 典型错误在另一个相同初态的独立测试库复验；不可与修正语句连续更新同一状态。
