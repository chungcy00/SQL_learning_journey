-- 在独立空 MySQL 8.4 测试库中执行，不得用于正式业务库。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
SET SESSION sql_mode='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION';
-- 参考解；等价解须从同一初始快照另行运行。
WITH RECURSIVE a(n,factorial) AS (SELECT 1,1 UNION ALL SELECT n+1,factorial*(n+1) FROM a WHERE n<5) SELECT n,factorial FROM a ORDER BY n;
