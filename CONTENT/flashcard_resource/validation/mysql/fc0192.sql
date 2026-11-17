-- fc0192：MySQL 8.4 原生待复验。
-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE `invoice_risk` (`invoice_id` INT, `billed_cents` INT, `accepted_cents` INT, `paid_cents` INT) ENGINE=InnoDB;
INSERT INTO `invoice_risk` VALUES (1, 10000, 8000, 9000), (2, 10000, 8000, 0), (3, 5000, 5000, 5000);
SELECT invoice_id,paid_cents-accepted_cents AS excess FROM invoice_risk WHERE ABS(billed_cents-accepted_cents)>100 AND paid_cents>0 ORDER BY invoice_id;
-- 期望列及值（按卡片结果比较，数字格式可等价）：{"columns": ["invoice_id", "excess"], "rows": [[1, 1000]]}
-- 典型错误在另一个相同初态的独立测试库复验；不可与修正语句连续更新同一状态。
