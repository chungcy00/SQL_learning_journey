-- 同岗位薪资分布与异常
-- 原生MySQL 8.4未执行。先从第二部分资源导入对应隔离数据库。
-- 公开基准参考解；独立执行等价解，不得变动原库。
WITH a AS (SELECT employee_id,department_id,position_id,base_cents,AVG(base_cents) OVER(PARTITION BY department_id,position_id) avg_base,COUNT(*) OVER(PARTITION BY department_id,position_id) n,RANK() OVER(PARTITION BY department_id,position_id ORDER BY base_cents DESC) salary_rank FROM v_staff_month WHERE month_key='2025-12') SELECT employee_id,department_id,position_id,base_cents,ROUND(avg_base,2) avg_base_cents,salary_rank,ROUND(100.0*(base_cents-avg_base)/NULLIF(avg_base,0),2) deviation_pct FROM a WHERE n>=3 AND ABS(base_cents-avg_base)>avg_base*.15 ORDER BY department_id,position_id,salary_rank,employee_id;

-- 等价解（SQLite已验证，MySQL待复验）
WITH a AS (SELECT employee_id,department_id,position_id,base_cents,AVG(base_cents) OVER(PARTITION BY department_id,position_id) avg_base,COUNT(1) OVER(PARTITION BY department_id,position_id) n,RANK() OVER(PARTITION BY department_id,position_id ORDER BY base_cents DESC) salary_rank FROM v_staff_month WHERE month_key='2025-12') SELECT employee_id,department_id,position_id,base_cents,ROUND(avg_base,2) avg_base_cents,salary_rank,ROUND(100.0*(base_cents-avg_base)/NULLIF(avg_base,0),2) deviation_pct FROM a WHERE n>=3 AND ABS(base_cents-avg_base)>avg_base*.15 ORDER BY department_id,position_id,salary_rank,employee_id;
