-- 仅在全新、一次性测试数据库中运行；不得选择生产或个人数据库。

SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;

SET SESSION time_zone = '+00:00';

SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';

CREATE TABLE `accounts` (`account_id` INT, `account_name` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `account_type` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;

INSERT INTO `accounts` VALUES (1,'租金','expense'),
(2,'销售','income'),
(3,'运费','expense');

CREATE TABLE `transactions` (`transaction_id` INT, `account_id` INT, `transaction_date` DATE, `amount` DECIMAL(12,2), `direction` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin, `reference_no` VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin) ENGINE=InnoDB;

INSERT INTO `transactions` VALUES (1,1,'2026-01-04',100,'expense','1'),
(2,2,'2026-01-10',1000,'income','2'),
(3,2,'2026-02-05',1200,'income','3'),
(4,3,'2026-01-20',75,'expense','4'),
(5,1,'2026-01-25',300,'expense','5');

SELECT a.account_name,SUM(t.amount) AS total_expense FROM transactions t JOIN accounts a ON t.account_id=a.account_id WHERE t.direction='expense' AND t.transaction_date>='2026-01-01' AND t.transaction_date<'2026-02-01' GROUP BY a.account_id,a.account_name ORDER BY total_expense DESC;
