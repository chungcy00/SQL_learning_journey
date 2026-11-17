-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
CREATE TABLE orders(id INT PRIMARY KEY,store_id INT,order_date DATE); INSERT INTO orders VALUES(2,1,'2026-02-01'),(3,2,'2026-02-01'); CREATE TABLE guard(i INT);
-- 参考解；等价解须从同一初始快照另行运行。
CREATE INDEX idx_store_date ON orders(store_id,order_date);
SHOW CREATE TABLE `guard`;
SELECT * FROM `guard`;
SHOW CREATE TABLE `orders`;
SELECT * FROM `orders`;
-- 约束探针应在独立测试快照逐项运行。
