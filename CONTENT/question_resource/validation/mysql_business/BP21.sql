-- 月末人员规模与变动
-- 原生MySQL 8.4未执行。先从第二部分资源导入对应隔离数据库。
-- 公开基准参考解；独立执行等价解，不得变动原库。
WITH m AS (SELECT month_key,MAX(day) last_day FROM calendar_days WHERE month_key>='2024-12' GROUP BY month_key), a AS (SELECT d.department_id,m.month_key,COUNT(h.employee_id) headcount FROM departments d CROSS JOIN m LEFT JOIN employment_history h ON h.department_id=d.department_id AND h.start_date<=m.last_day AND (h.end_date IS NULL OR h.end_date>m.last_day) GROUP BY d.department_id,m.month_key), b AS (SELECT *,LAG(headcount) OVER(PARTITION BY department_id ORDER BY month_key) prev FROM a) SELECT department_id,month_key,headcount,headcount-prev net_change FROM b WHERE month_key>='2025-01' ORDER BY department_id,month_key;

-- 等价解（SQLite已验证，MySQL待复验）
WITH m AS (SELECT month_key,MAX(day) last_day FROM calendar_days WHERE NOT (month_key<'2024-12') GROUP BY month_key), a AS (SELECT d.department_id,m.month_key,COUNT(h.employee_id) headcount FROM departments d CROSS JOIN m LEFT JOIN employment_history h ON h.department_id=d.department_id AND h.start_date<=m.last_day AND (h.end_date IS NULL OR h.end_date>m.last_day) GROUP BY d.department_id,m.month_key), b AS (SELECT *,LAG(headcount) OVER(PARTITION BY department_id ORDER BY month_key) prev FROM a) SELECT department_id,month_key,headcount,headcount-prev net_change FROM b WHERE month_key>='2025-01' ORDER BY department_id,month_key;
