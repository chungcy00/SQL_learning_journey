-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE guard(id INT PRIMARY KEY); INSERT INTO guard VALUES(99);
-- 参考解；等价解须从同一初始快照另行运行。
CREATE TABLE practice_stock(product_id INT NOT NULL PRIMARY KEY,qty INT NOT NULL CHECK(qty>=0));
SHOW CREATE TABLE `guard`;
SELECT * FROM `guard`;
SHOW CREATE TABLE `practice_stock`;
SELECT * FROM `practice_stock`;
-- 约束探针应在独立测试快照逐项运行。
