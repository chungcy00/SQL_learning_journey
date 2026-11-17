-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `budgets` (`budget_id` INT, `account_id` INT, `budget_month` DATE, `budget_amount` DECIMAL(12,2)) ENGINE=InnoDB;

INSERT INTO `budgets` VALUES (1,1,'2026-01-01',350),
(2,3,'2026-01-01',100);

CREATE TABLE `transactions` (`transaction_id` INT, `account_id` INT, `transaction_date` DATE, `amount` DECIMAL(12,2), `direction` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `reference_no` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;

INSERT INTO `transactions` VALUES (1,1,'2026-01-04',100,'expense','1'),
(2,2,'2026-01-10',1000,'income','2'),
(3,2,'2026-02-05',1200,'income','3'),
(4,3,'2026-01-20',75,'expense','4'),
(5,1,'2026-01-25',300,'expense','5');

WITH a AS (SELECT account_id,SUM(amount) AS actual FROM transactions WHERE direction='expense' AND transaction_date>='2026-01-01' AND transaction_date<'2026-02-01' GROUP BY account_id) SELECT b.account_id,b.budget_amount,COALESCE(a.actual,0) AS actual,COALESCE(a.actual,0)-b.budget_amount AS variance FROM budgets b LEFT JOIN a ON b.account_id=a.account_id WHERE b.budget_month='2026-01-01' ORDER BY b.account_id;
