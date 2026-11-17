# retail 实战任务

## BP01 门店净销售、利润与目标达成（中等）

区域经理需要2025年门店经营表。

**要求：** 每门店一行；有效线下订单，按原订单日期归属截至年底退货；毛利=净销售-退货冲减后成本；毛利率和目标达成率使用百分比，分母为0返回NULL；金额单位分。保留无销售门店。

**输出列：** store_id, net_cents, gross_cents, margin_pct, target_cents, achievement_pct

**思路：** 先按门店聚合销售与成本；独立累计目标；左连接门店；计算比率。直接连接月目标与订单明细会重复累计目标。

```sql
WITH a AS (SELECT store_id,SUM(net_cents) net,SUM(cost_cents) cost FROM v_sales_lines WHERE channel='store' AND order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY store_id), b AS (SELECT store_id,SUM(target_cents) target FROM sales_targets WHERE month_key BETWEEN '2025-01' AND '2025-12' GROUP BY store_id) SELECT s.store_id,COALESCE(a.net,0) net_cents,COALESCE(a.net-a.cost,0) gross_cents,ROUND(100.0*(a.net-a.cost)/NULLIF(a.net,0),2) margin_pct,b.target target_cents,ROUND(100.0*COALESCE(a.net,0)/NULLIF(b.target,0),2) achievement_pct FROM stores s LEFT JOIN a ON a.store_id=s.store_id LEFT JOIN b ON b.store_id=s.store_id ORDER BY s.store_id;
```

真实执行结果：12 行；完整结果 `results/BP01.json`。

## BP02 门店月度环比同比与持续下滑（困难）

经营主管希望识别连续三个月环比负增长的门店。

**要求：** 构造2023—2025完整门店月份轴；2025年逐月返回净销售、环比、同比及连续三月负增长标志；上期零或缺失时比率NULL。

**输出列：** store_id, month_key, net_cents, mom_pct, yoy_pct, three_declines

**思路：** 构造月份轴；汇总销售并补零；计算两个历史窗口；滚动检查连续下滑；最后筛2025。先筛2025再计算窗口会丢失去年和上月。

```sql
WITH m AS (SELECT DISTINCT month_key FROM calendar_days), a AS (SELECT store_id,SUBSTR(order_date,1,7) m,SUM(net_cents) net FROM v_sales_lines WHERE channel='store' GROUP BY store_id,SUBSTR(order_date,1,7)), b AS (SELECT s.store_id,m.month_key,COALESCE(a.net,0) net FROM stores s CROSS JOIN m LEFT JOIN a ON a.store_id=s.store_id AND a.m=m.month_key), p AS (SELECT *,LAG(net) OVER(PARTITION BY store_id ORDER BY month_key) prev,LAG(net,12) OVER(PARTITION BY store_id ORDER BY month_key) prior_year FROM b), q AS (SELECT *,CASE WHEN prev>0 AND net<prev THEN 1 ELSE 0 END falling FROM p), z AS (SELECT *,SUM(falling) OVER(PARTITION BY store_id ORDER BY month_key ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) fall3 FROM q) SELECT store_id,month_key,net net_cents,ROUND(100.0*(net-prev)/NULLIF(prev,0),2) mom_pct,ROUND(100.0*(net-prior_year)/NULLIF(prior_year,0),2) yoy_pct,CASE WHEN fall3=3 THEN 1 ELSE 0 END three_declines FROM z WHERE month_key>='2025-01' ORDER BY store_id,month_key;
```

真实执行结果：144 行；完整结果 `results/BP02.json`。

## BP03 高销售低毛利商品诊断（困难）

商品经理不希望销售额掩盖利润问题。

**要求：** 2025年线下商品净销售额位于前四分之一且毛利率低于30%；返回销售、毛利率、折扣额、退款额；按净销售降序、商品编号升序。四分位使用NTILE(4)，并列按编号。

**输出列：** product_id, net_cents, margin_pct, discount_cents, refund_cents

**思路：** 按商品独立汇总；按全体净额分箱；应用毛利阈值；输出折扣退款。先按毛利筛选再计算销售四分位改变总体。

```sql
WITH a AS (SELECT product_id,SUM(net_cents) net,SUM(cost_cents) cost,SUM(discount_cents) discount,SUM(refund_cents) refund FROM v_sales_lines WHERE channel='store' AND order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY product_id), b AS (SELECT *,NTILE(4) OVER(ORDER BY net DESC,product_id) bucket FROM a) SELECT product_id,net net_cents,ROUND(100.0*(net-cost)/NULLIF(net,0),2) margin_pct,discount discount_cents,refund refund_cents FROM b WHERE bucket=1 AND 100.0*(net-cost)/NULLIF(net,0)<30 ORDER BY net DESC,product_id;
```

真实执行结果：3 行；完整结果 `results/BP03.json`。

## BP04 促销与非促销的日均表现（困难）

促销期天数不同，不能直接用总额比较。

**要求：** 2025年线下订单，按日期是否落在任一促销区间分组；计算日均净销售、日均净件数、毛利率；日数来自完整日历，包含零销售日；不将相关性解释为因果。

**输出列：** promo, days, daily_net_cents, daily_units, margin_pct

**思路：** 日历标记促销日；独立日级汇总；补全日历；计算日均及加权毛利率。按订单数除总额得到客单价而不是日均。

```sql
WITH d AS (SELECT day,CASE WHEN EXISTS(SELECT 1 FROM promotions p WHERE day BETWEEN p.start_date AND p.end_date) THEN 1 ELSE 0 END promo FROM calendar_days WHERE day BETWEEN '2025-01-01' AND '2025-12-31'), a AS (SELECT order_date,SUM(net_cents) net,SUM(net_qty) qty,SUM(cost_cents) cost FROM v_sales_lines WHERE channel='store' AND order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY order_date) SELECT d.promo,COUNT(*) days,ROUND(1.0*SUM(COALESCE(a.net,0))/COUNT(*),2) daily_net_cents,ROUND(1.0*SUM(COALESCE(a.qty,0))/COUNT(*),2) daily_units,ROUND(100.0*SUM(a.net-a.cost)/NULLIF(SUM(a.net),0),2) margin_pct FROM d LEFT JOIN a ON a.order_date=d.day GROUP BY d.promo ORDER BY d.promo;
```

真实执行结果：2 行；完整结果 `results/BP04.json`。

## BP05 门店综合经营异常看板（挑战）

管理层需要优先检查经营异常的门店。

**要求：** 2025年第四季度，门店净销售、毛利率、目标达成率、退款率（退款/退款前折后销售额）、净销售排名；异常项：毛利率<30%、达成率<90%、退款率>8%；按异常数降序。

**输出列：** store_id, net_cents, margin_pct, achievement_pct, refund_pct, flags, sales_rank

**思路：** 按门店汇总经营指标；独立关联季度目标；用未舍入比率标记；窗口排名。阈值用四舍五入后的比率可能误判边界。

```sql
WITH a AS (SELECT store_id,SUM(net_cents) net,SUM(cost_cents) cost,SUM(refund_cents) refund FROM v_sales_lines WHERE channel='store' AND order_date BETWEEN '2025-10-01' AND '2025-12-31' GROUP BY store_id), b AS (SELECT store_id,SUM(target_cents) target FROM sales_targets WHERE month_key BETWEEN '2025-10' AND '2025-12' GROUP BY store_id), p AS (SELECT s.store_id,COALESCE(a.net,0) net,100.0*(a.net-a.cost)/NULLIF(a.net,0) margin,100.0*COALESCE(a.net,0)/NULLIF(b.target,0) attainment,100.0*a.refund/NULLIF(a.net+a.refund,0) refund_rate FROM stores s LEFT JOIN a ON a.store_id=s.store_id LEFT JOIN b ON b.store_id=s.store_id), z AS (SELECT *,CASE WHEN margin<30 THEN 1 ELSE 0 END+CASE WHEN attainment<90 THEN 1 ELSE 0 END+CASE WHEN refund_rate>8 THEN 1 ELSE 0 END flags,RANK() OVER(ORDER BY net DESC) sales_rank FROM p) SELECT store_id,net net_cents,ROUND(margin,2) margin_pct,ROUND(attainment,2) achievement_pct,ROUND(refund_rate,2) refund_pct,flags,sales_rank FROM z ORDER BY flags DESC,store_id;
```

真实执行结果：12 行；完整结果 `results/BP05.json`。

