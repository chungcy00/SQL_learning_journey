-- 部门人力成本与加班
-- 原生MySQL 8.4未执行。先从第二部分资源导入对应隔离数据库。
-- 公开基准参考解；独立执行等价解，不得变动原库。
SELECT department_id,month_key,SUM(base_cents) base_cents,SUM(bonus_cents) bonus_cents,SUM(overtime_minutes) overtime_minutes,SUM(labor_cents) labor_cents,ROUND(60.0*SUM(labor_cents)/NULLIF(SUM(worked_minutes+overtime_minutes),0),2) cost_per_hour_cents FROM v_staff_month WHERE month_key BETWEEN '2025-10' AND '2025-12' GROUP BY department_id,month_key ORDER BY department_id,month_key;

-- 等价解（SQLite已验证，MySQL待复验）
SELECT department_id,month_key,SUM(base_cents) base_cents,SUM(bonus_cents) bonus_cents,SUM(overtime_minutes) overtime_minutes,SUM(labor_cents) labor_cents,ROUND(60.0*SUM(labor_cents)/NULLIF(SUM(worked_minutes+overtime_minutes),0),2) cost_per_hour_cents FROM v_staff_month WHERE (month_key>='2025-10' AND month_key<='2025-12') GROUP BY department_id,month_key ORDER BY department_id,month_key;
