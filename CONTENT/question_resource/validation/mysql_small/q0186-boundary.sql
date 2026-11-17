-- 全新独立MySQL 8.4测试库，原生未运行。
SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;
CREATE TABLE schedule_bounds(start_day INT,step_days INT,max_day INT);
INSERT INTO schedule_bounds VALUES (2,4,11);
WITH RECURSIVE schedule(n,step_days,max_day) AS (SELECT start_day,step_days,max_day FROM schedule_bounds UNION ALL SELECT n+step_days,step_days,max_day FROM schedule WHERE n+step_days<=max_day) SELECT n FROM schedule ORDER BY n;
-- 等价解
WITH RECURSIVE schedule(n,step_days,max_day) AS (SELECT start_day,step_days,max_day FROM schedule_bounds UNION ALL SELECT n+step_days,step_days,max_day FROM schedule WHERE n<=max_day-step_days) SELECT n FROM schedule ORDER BY n;
-- 典型错误（预期越界）
WITH RECURSIVE schedule(n,step_days,max_day) AS (SELECT start_day,step_days,max_day FROM schedule_bounds UNION ALL SELECT n+step_days,step_days,max_day FROM schedule WHERE n<max_day) SELECT n FROM schedule ORDER BY n;
