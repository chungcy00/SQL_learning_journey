# ecommerce 实战任务

## BP11 新老客户净收入及客单价（中等）

营销部门比较首购客户与已有购买历史的客户。

**要求：** 仅线上有效订单。客户首次有效订单以日期、订单编号决胜；2025年按订单是否首购分组，输出客户数、订单数、净收入和平均订单净额（分）。

**输出列：** segment, customers, orders, net_cents, avg_order_cents

**思路：** 计算完整历史订单序号；筛选目标年；按首购标记汇总。先筛2025会把已有购买客户误标为新客；客户可在两个组出现。

```sql
WITH a AS (SELECT *,ROW_NUMBER() OVER(PARTITION BY customer_id ORDER BY order_date,order_id) rn FROM v_order_net WHERE channel='online') SELECT CASE WHEN rn=1 THEN 'new' ELSE 'returning' END segment,COUNT(DISTINCT customer_id) customers,COUNT(*) orders,SUM(net_cents) net_cents,ROUND(1.0*SUM(net_cents)/COUNT(*),2) avg_order_cents FROM a WHERE order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY CASE WHEN rn=1 THEN 'new' ELSE 'returning' END ORDER BY segment;
```

真实执行结果：2 行；完整结果 `results/BP11.json`。

## BP12 最近消费、频率、金额客户分层（困难）

客户运营团队需要可执行的分层名单。

**要求：** 2025年有线上有效购买的客户；截止12-31距最近购买天数；年度订单数和净消费金额；分别按最近日期近到远、频率高到低、金额高到低分五组（并列客户编号）；三个分数5为最好；总分≥13为高价值、≤6为低活跃。

**输出列：** customer_id, recency_days, freq, net_cents, r_score, f_score, m_score, segment

**思路：** 先按客户聚合；三种独立窗口分箱；按总分分类；计算距截止日天数。每行订单做NTILE会将同一客户拆入多组。

```sql
WITH a AS (SELECT customer_id,MAX(order_date) last_day,COUNT(*) freq,SUM(net_cents) money FROM v_order_net WHERE channel='online' AND order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY customer_id), b AS (SELECT *,6-NTILE(5) OVER(ORDER BY last_day DESC,customer_id) r_score,6-NTILE(5) OVER(ORDER BY freq DESC,customer_id) f_score,6-NTILE(5) OVER(ORDER BY money DESC,customer_id) m_score FROM a) SELECT b.customer_id,1095-d.day_index recency_days,freq,money net_cents,r_score,f_score,m_score,CASE WHEN r_score+f_score+m_score>=13 THEN 'high_value' WHEN r_score+f_score+m_score<=6 THEN 'low_activity' ELSE 'regular' END segment FROM b JOIN calendar_days d ON d.day=b.last_day ORDER BY b.customer_id;
```

真实执行结果：2889 行；完整结果 `results/BP12.json`。

## BP13 首购月份留存矩阵（困难）

业务经理希望观察首购后的月度复购留存。

**要求：** 线上有效订单；首次购买月为同期群，观察第0至第6月；按首购月与月偏移输出同期群规模、活跃客户与留存率；没有活跃但已观察的月份为0，未来月份不输出。

**输出列：** cohort, month_offset, cohort_size, active_customers, retention_pct

**思路：** 确定首购月份；构造可观测月份；去重客户；计算留存。直接COUNT订单会把同一客户的多次复购重复计数。

```sql
WITH a AS (SELECT customer_id,MIN(SUBSTR(order_date,1,7)) cohort FROM v_order_net WHERE channel='online' GROUP BY customer_id), b AS (SELECT cohort,COUNT(*) n FROM a GROUP BY cohort), m AS (SELECT DISTINCT month_key FROM calendar_days), x AS (SELECT b.cohort,m.month_key,b.n,(CAST(SUBSTR(m.month_key,1,4) AS SIGNED)-CAST(SUBSTR(b.cohort,1,4) AS SIGNED))*12+CAST(SUBSTR(m.month_key,6,2) AS SIGNED)-CAST(SUBSTR(b.cohort,6,2) AS SIGNED) k FROM b CROSS JOIN m WHERE m.month_key>=b.cohort), y AS (SELECT a.cohort,SUBSTR(o.order_date,1,7) m,COUNT(DISTINCT o.customer_id) active FROM a JOIN v_order_net o ON o.customer_id=a.customer_id AND o.channel='online' GROUP BY a.cohort,SUBSTR(o.order_date,1,7)) SELECT x.cohort,x.k month_offset,x.n cohort_size,COALESCE(y.active,0) active_customers,ROUND(100.0*COALESCE(y.active,0)/x.n,2) retention_pct FROM x LEFT JOIN y ON y.cohort=x.cohort AND y.m=x.month_key WHERE x.k BETWEEN 0 AND 6 ORDER BY x.cohort,x.k;
```

真实执行结果：231 行；完整结果 `results/BP13.json`。

## BP14 营销渠道转化与获客成本（困难）

营销负责人希望评估不同渠道效率。

**要求：** 2025年，按访问渠道归因；转化率=有有效订单的不同会话数/会话数；新客数只计历史首次线上有效订单；获客成本=渠道广告支出/新客数；净收入广告回报=净销售/广告支出；零分母NULL。

**输出列：** source_id, sessions, converted, conversion_pct, new_customers, spend_cents, cac_cents, roas

**思路：** 按渠道汇总会话；历史首次购买标记；按渠道汇总转换与收入；独立汇总广告支出。广告支出不能直接连接订单明细求和；转化分母是会话不是客户。

```sql
WITH s AS (SELECT source_id,COUNT(*) sessions FROM customer_sessions WHERE session_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY source_id), o AS (SELECT *,ROW_NUMBER() OVER(PARTITION BY customer_id ORDER BY order_date,order_id) rn FROM v_order_net WHERE channel='online'), a AS (SELECT t.source_id,COUNT(DISTINCT o.session_id) converted,SUM(CASE WHEN rn=1 THEN 1 ELSE 0 END) new_customers,SUM(net_cents) net FROM o JOIN customer_sessions t ON t.session_id=o.session_id WHERE o.order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY t.source_id), p AS (SELECT source_id,SUM(spend_cents) spend FROM marketing_campaigns WHERE month_key BETWEEN '2025-01' AND '2025-12' GROUP BY source_id) SELECT t.source_id,s.sessions,COALESCE(a.converted,0) converted,ROUND(100.0*COALESCE(a.converted,0)/NULLIF(s.sessions,0),2) conversion_pct,COALESCE(a.new_customers,0) new_customers,p.spend spend_cents,ROUND(1.0*p.spend/NULLIF(a.new_customers,0),2) cac_cents,ROUND(1.0*a.net/NULLIF(p.spend,0),4) roas FROM traffic_sources t LEFT JOIN s ON s.source_id=t.source_id LEFT JOIN a ON a.source_id=t.source_id LEFT JOIN p ON p.source_id=t.source_id ORDER BY t.source_id;
```

真实执行结果：5 行；完整结果 `results/BP14.json`。

## BP15 高价值流失风险客户（挑战）

运营希望针对长期未购买但历史价值高的客户。

**要求：** 截至2025年底所有线上有效购买客户；历史订单数≥3、历史净金额排名前20%（NTILE(5)，按编号决胜）、最近90日无消费，输出历史毛利、最近购买距今天数和风险优先级（毛利降序）。

**输出列：** customer_id, freq, net_cents, gross_cents, recency_days

**思路：** 汇总历史价值；在全体客户上分位；计算距截止日天数；筛风险名单。仅凭最后消费日期无法识别高价值，且流失风险不是已流失事实。

```sql
WITH a AS (SELECT customer_id,COUNT(*) freq,MAX(order_date) last_day,SUM(net_cents) net,SUM(net_cents-cost_cents) gross FROM v_order_net WHERE channel='online' GROUP BY customer_id), b AS (SELECT *,NTILE(5) OVER(ORDER BY net DESC,customer_id) bucket FROM a) SELECT b.customer_id,freq,net net_cents,gross gross_cents,1095-d.day_index recency_days FROM b JOIN calendar_days d ON d.day=b.last_day WHERE bucket=1 AND freq>=3 AND 1095-d.day_index>=90 ORDER BY gross DESC,b.customer_id;
```

真实执行结果：547 行；完整结果 `results/BP15.json`。

