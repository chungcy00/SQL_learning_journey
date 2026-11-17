# hr 实战任务

## BP21 月末人员规模与变动（中等）

人资经理需要2025年部门月末人数和上月变化。

**要求：** 任职历史使用左闭右开区间；2024-12至2025-12构建部门月末轴，月末人数及净变化；最终输出2025年。调动计入所属部门变化，不等于离职。

**输出列：** department_id, month_key, headcount, net_change

**思路：** 构造月末轴；匹配生效任职；计算前月人数；保留2025。用当前部门回填历史月份会错计调动。

```sql
WITH m AS (SELECT month_key,MAX(day) last_day FROM calendar_days WHERE month_key>='2024-12' GROUP BY month_key), a AS (SELECT d.department_id,m.month_key,COUNT(h.employee_id) headcount FROM departments d CROSS JOIN m LEFT JOIN employment_history h ON h.department_id=d.department_id AND h.start_date<=m.last_day AND (h.end_date IS NULL OR h.end_date>m.last_day) GROUP BY d.department_id,m.month_key), b AS (SELECT *,LAG(headcount) OVER(PARTITION BY department_id ORDER BY month_key) prev FROM a) SELECT department_id,month_key,headcount,headcount-prev net_change FROM b WHERE month_key>='2025-01' ORDER BY department_id,month_key;
```

真实执行结果：84 行；完整结果 `results/BP21.json`。

## BP22 月度离职率与趋势（困难）

人资经理比较各部门真实离职率。

**要求：** 2025年每部门每月真实离职人数（终止日期落月内，取终止前岗位）/期初期末平均在职数；期初以当月首日之前一天在职为准；滚动三月率用离职总数/三个月平均人数之和，非简单平均。

**输出列：** department_id, month_key, opening, closing, exits, exit_pct, rolling_pct

**思路：** 从历史岗位重建人数；匹配真正终止；计算月率及加权滚动率。把部门调动当离职会夸大离职率。

```sql
WITH m AS (SELECT month_key,MIN(day) first_day,MAX(day) last_day,MIN(day_index) first_idx FROM calendar_days WHERE month_key>='2025-01' GROUP BY month_key), a AS (SELECT d.department_id,m.month_key,SUM(CASE WHEN h.start_date< m.first_day AND (h.end_date IS NULL OR h.end_date>=m.first_day) THEN 1 ELSE 0 END) opening,SUM(CASE WHEN h.start_date<=m.last_day AND (h.end_date IS NULL OR h.end_date>m.last_day) THEN 1 ELSE 0 END) closing,SUM(CASE WHEN e.termination_date BETWEEN m.first_day AND m.last_day AND h.end_date=e.termination_date THEN 1 ELSE 0 END) exits FROM departments d CROSS JOIN m LEFT JOIN employment_history h ON h.department_id=d.department_id LEFT JOIN employees e ON e.employee_id=h.employee_id GROUP BY d.department_id,m.month_key), b AS (SELECT *, (opening+closing)/2.0 avg_h FROM a), z AS (SELECT *,SUM(exits) OVER(PARTITION BY department_id ORDER BY month_key ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) e3,SUM(avg_h) OVER(PARTITION BY department_id ORDER BY month_key ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) h3,COUNT(*) OVER(PARTITION BY department_id ORDER BY month_key ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) n3 FROM b) SELECT department_id,month_key,opening,closing,exits,ROUND(100.0*exits/NULLIF(avg_h,0),2) exit_pct,CASE WHEN n3=3 THEN ROUND(100.0*e3/NULLIF(h3,0),2) ELSE NULL END rolling_pct FROM z ORDER BY department_id,month_key;
```

真实执行结果：84 行；完整结果 `results/BP22.json`。

## BP23 同岗位薪资分布与异常（困难）

薪酬分析师需要同部门同岗位的横向基准。

**要求：** 2025-12实发基础薪资；同部门同职位至少3人；计算组均值、排名以及偏离均值超过15%的员工；异常仅表示复核候选。

**输出列：** employee_id, department_id, position_id, base_cents, avg_base_cents, salary_rank, deviation_pct

**思路：** 确定薪资期间；按可比组窗口计算；筛选足够样本与偏差。比较全部员工薪资忽略岗位、职级和部门。

```sql
WITH a AS (SELECT employee_id,department_id,position_id,base_cents,AVG(base_cents) OVER(PARTITION BY department_id,position_id) avg_base,COUNT(*) OVER(PARTITION BY department_id,position_id) n,RANK() OVER(PARTITION BY department_id,position_id ORDER BY base_cents DESC) salary_rank FROM v_staff_month WHERE month_key='2025-12') SELECT employee_id,department_id,position_id,base_cents,ROUND(avg_base,2) avg_base_cents,salary_rank,ROUND(100.0*(base_cents-avg_base)/NULLIF(avg_base,0),2) deviation_pct FROM a WHERE n>=3 AND ABS(base_cents-avg_base)>avg_base*.15 ORDER BY department_id,position_id,salary_rank,employee_id;
```

真实执行结果：7 行；完整结果 `results/BP23.json`。

## BP24 部门人力成本与加班（困难）

财务与人资联合分析部门第四季度人工成本。

**要求：** 按员工月薪归属岗位；基础薪资+奖金+估算加班费（每月176小时、1.5倍基础时薪，按分钟；逐员工月四舍五入到分）；统计部门×月成本、工时与加班，成本每出勤小时。

**输出列：** department_id, month_key, base_cents, bonus_cents, overtime_minutes, labor_cents, cost_per_hour_cents

**思路：** 从员工月成本视图核对计算规则；汇总部門及月份；按总工时计算成本。连接每日考勤后直接求月薪会重复累计；估算工资不代表实际法定工资。

```sql
SELECT department_id,month_key,SUM(base_cents) base_cents,SUM(bonus_cents) bonus_cents,SUM(overtime_minutes) overtime_minutes,SUM(labor_cents) labor_cents,ROUND(60.0*SUM(labor_cents)/NULLIF(SUM(worked_minutes+overtime_minutes),0),2) cost_per_hour_cents FROM v_staff_month WHERE month_key BETWEEN '2025-10' AND '2025-12' GROUP BY department_id,month_key ORDER BY department_id,month_key;
```

真实执行结果：21 行；完整结果 `results/BP24.json`。

## BP25 入职同期群90及180日留任（挑战）

人资部门希望判断不同入职部门的新人留任。

**要求：** 以首次任职部门为入职部门；按入职季度与部门分组；只纳入截至2025年底已有90/180日观察期的员工；终止日等于观察日算不在职；返回两个分母和留任率。

**输出列：** department_id, cohort, n90, retention90_pct, n180, retention180_pct

**思路：** 确定首次岗位；计算观察资格；在各自分母上计算留任。把未达到观察期限的员工当作未留任会使新同期群率偏低。

```sql
WITH a AS (SELECT e.employee_id,h.department_id,e.hire_date,e.termination_date,d.day_index hire_idx,t.day_index term_idx,CONCAT(SUBSTR(e.hire_date,1,4),'-Q',CASE WHEN SUBSTR(e.hire_date,6,2)<='03' THEN '1' WHEN SUBSTR(e.hire_date,6,2)<='06' THEN '2' WHEN SUBSTR(e.hire_date,6,2)<='09' THEN '3' ELSE '4' END) cohort FROM employees e JOIN employment_history h ON h.employee_id=e.employee_id AND h.start_date=e.hire_date JOIN calendar_days d ON d.day=e.hire_date LEFT JOIN calendar_days t ON t.day=e.termination_date), b AS (SELECT department_id,cohort,SUM(CASE WHEN hire_idx+90<=1095 THEN 1 ELSE 0 END) n90,SUM(CASE WHEN hire_idx+90<=1095 AND (term_idx IS NULL OR term_idx>hire_idx+90) THEN 1 ELSE 0 END) r90,SUM(CASE WHEN hire_idx+180<=1095 THEN 1 ELSE 0 END) n180,SUM(CASE WHEN hire_idx+180<=1095 AND (term_idx IS NULL OR term_idx>hire_idx+180) THEN 1 ELSE 0 END) r180 FROM a GROUP BY department_id,cohort) SELECT department_id,cohort,n90,ROUND(100.0*r90/NULLIF(n90,0),2) retention90_pct,n180,ROUND(100.0*r180/NULLIF(n180,0),2) retention180_pct FROM b ORDER BY department_id,cohort;
```

真实执行结果：62 行；完整结果 `results/BP25.json`。

