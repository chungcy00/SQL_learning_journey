# enterprise 实战任务

## BP31 畅销缺货与滞销积压双向筛查（困难）

销售＋库存联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 2025年底仓库商品；近30日毛销量≥30且库存低于15日需求为畅销风险；近30日销量≤3且库存金额≥300000分为积压；仅有效商品。

**输出列：** warehouse_id, product_id, quantity, sold, value_cents, issue

**思路：** 汇总仓库实际出库；连接期末；应用两套互斥规则。把门店库存与仓库库存混为一谈。

```sql
WITH s AS (SELECT warehouse_id,product_id,SUM(-quantity_delta) sold FROM stock_movements WHERE movement_type='sale' AND movement_date BETWEEN '2025-12-02' AND '2025-12-31' GROUP BY warehouse_id,product_id), a AS (SELECT i.warehouse_id,i.product_id,i.quantity,i.quantity*i.unit_cost_cents value_cents,COALESCE(s.sold,0) sold FROM inventory_snapshots i JOIN products p ON p.product_id=i.product_id LEFT JOIN s ON s.warehouse_id=i.warehouse_id AND s.product_id=i.product_id WHERE i.snapshot_date='2025-12-31' AND p.active=1) SELECT warehouse_id,product_id,quantity,sold,value_cents,CASE WHEN sold>=30 AND quantity<sold*.5 THEN 'fast_shortage' ELSE 'slow_excess' END issue FROM a WHERE (sold>=30 AND quantity<sold*.5) OR (sold<=3 AND value_cents>=300000) ORDER BY issue,value_cents DESC,warehouse_id,product_id;
```

真实执行结果：698 行；完整结果 `results/BP31.json`。

## BP32 商品销售贡献与库存资金占用错配（困难）

销售＋库存联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 2025年按商品汇总所有渠道净销售，年底各仓库存总价值；计算两个全局份额和差值（库存占比-销售占比），保留全部商品，按差值倒序。

**输出列：** product_id, net_cents, stock_cents, sales_pct, stock_pct, gap_points

**思路：** 分别汇总销售和库存；补全商品；窗口求总量；比较份额。只用有销售的商品作为库存分母会漏掉滞销。

```sql
WITH a AS (SELECT product_id,SUM(net_cents) net FROM v_sales_lines WHERE order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY product_id), b AS (SELECT product_id,SUM(quantity*unit_cost_cents) stock FROM inventory_snapshots WHERE snapshot_date='2025-12-31' GROUP BY product_id), z AS (SELECT p.product_id,COALESCE(a.net,0) net,COALESCE(b.stock,0) stock FROM products p LEFT JOIN a ON a.product_id=p.product_id LEFT JOIN b ON b.product_id=p.product_id), q AS (SELECT *,100.0*net/NULLIF(SUM(net) OVER(),0) sales_share,100.0*stock/NULLIF(SUM(stock) OVER(),0) stock_share FROM z) SELECT product_id,net net_cents,stock stock_cents,ROUND(sales_share,4) sales_pct,ROUND(stock_share,4) stock_pct,ROUND(stock_share-sales_share,4) gap_points FROM q ORDER BY gap_points DESC,product_id;
```

真实执行结果：500 行；完整结果 `results/BP32.json`。

## BP33 跨仓库存调拨候选（挑战）

销售＋库存联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 2025年底，每仓每商品库存超过近30日销量两倍且至少多10件为供给；库存低于同期间销量为需求。按商品匹配异仓供需，输出候选调拨量=min(超额,缺口)，仅候选不能同时执行多个配对。

**输出列：** product_id, from_warehouse, to_warehouse, proposed_units

**思路：** 计算供需；自连接不同仓；取可行上限；注明非全局优化。候选配对不等于完成调拨优化，同一库存可能被重复分配。

```sql
WITH s AS (SELECT warehouse_id,product_id,SUM(-quantity_delta) sold FROM stock_movements WHERE movement_type='sale' AND movement_date BETWEEN '2025-12-02' AND '2025-12-31' GROUP BY warehouse_id,product_id), a AS (SELECT i.warehouse_id,i.product_id,i.quantity,COALESCE(s.sold,0) sold FROM inventory_snapshots i JOIN products p ON p.product_id=i.product_id LEFT JOIN s ON s.warehouse_id=i.warehouse_id AND s.product_id=i.product_id WHERE i.snapshot_date='2025-12-31' AND p.active=1), b AS (SELECT *,quantity-2*sold surplus,sold-quantity shortage FROM a) SELECT x.product_id,x.warehouse_id from_warehouse,y.warehouse_id to_warehouse,CASE WHEN x.surplus<y.shortage THEN x.surplus ELSE y.shortage END proposed_units FROM b x JOIN b y ON y.product_id=x.product_id AND y.warehouse_id<>x.warehouse_id WHERE x.surplus>=10 AND y.shortage>0 ORDER BY x.product_id,x.warehouse_id,y.warehouse_id;
```

真实执行结果：116 行；完整结果 `results/BP33.json`。

## BP34 销售毛利与库存投资回报（挑战）

销售＋库存联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 2025年全部渠道，按类别计算净销售毛利与2024年底/2025年底平均库存标准价值，毛利/平均库存为库存投资回报倍数；保留全部类别。

**输出列：** category_id, gross_cents, avg_stock_cents, gmroi

**思路：** 汇总类别毛利；汇总两端库存；计算回报并标明估算口径。用销售收入替代毛利会变成另一个指标。

```sql
WITH a AS (SELECT p.category_id,SUM(s.net_cents-s.cost_cents) gross FROM v_sales_lines s JOIN products p ON p.product_id=s.product_id WHERE s.order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY p.category_id), b AS (SELECT p.category_id,SUM(i.quantity*i.unit_cost_cents)/2.0 avg_stock FROM inventory_snapshots i JOIN products p ON p.product_id=i.product_id WHERE i.snapshot_date IN ('2024-12-31','2025-12-31') GROUP BY p.category_id) SELECT c.category_id,COALESCE(a.gross,0) gross_cents,b.avg_stock avg_stock_cents,ROUND(COALESCE(a.gross,0)/NULLIF(b.avg_stock,0),4) gmroi FROM categories c LEFT JOIN a ON a.category_id=c.category_id LEFT JOIN b ON b.category_id=c.category_id ORDER BY gmroi DESC,c.category_id;
```

真实执行结果：15 行；完整结果 `results/BP34.json`。

## BP35 逾期在途与缺货叠加（困难）

库存＋采购联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 截止2025年底，将已到期未足量收货采购行按仓库商品汇总；结合当前库存和近30日销量，筛选库存低于30日需求且逾期未收货量>0的商品。

**输出列：** warehouse_id, product_id, quantity, demand, overdue_units, shortage

**思路：** 汇总逾期缺口；汇总销售；匹配期末并排序。将拒收库存算作可售库存会低估缺口。

```sql
WITH p AS (SELECT warehouse_id,product_id,SUM(quantity_ordered-accepted) overdue FROM v_po_lines WHERE promised_date<='2025-12-31' AND accepted<quantity_ordered GROUP BY warehouse_id,product_id), s AS (SELECT warehouse_id,product_id,SUM(-quantity_delta) sold FROM stock_movements WHERE movement_type='sale' AND movement_date BETWEEN '2025-12-02' AND '2025-12-31' GROUP BY warehouse_id,product_id) SELECT i.warehouse_id,i.product_id,i.quantity,s.sold demand,p.overdue overdue_units,s.sold-i.quantity shortage FROM inventory_snapshots i JOIN p ON p.warehouse_id=i.warehouse_id AND p.product_id=i.product_id JOIN s ON s.warehouse_id=i.warehouse_id AND s.product_id=i.product_id WHERE i.snapshot_date='2025-12-31' AND i.quantity<s.sold ORDER BY shortage DESC,i.warehouse_id,i.product_id;
```

真实执行结果：32 行；完整结果 `results/BP35.json`。

## BP36 收货入库完整性核对（困难）

库存＋采购联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 比较每条合格收货记录与库存流水入库数量，包含拒收全部的记录；输出采购行、收货编号、应入库及已入库差额；仅差额非0。预计干净主库无差异，不能伪造异常。

**输出列：** receipt_id, po_item_id, expected_units, posted_units, difference

**思路：** 先聚合库存流水；与质检收货匹配；筛选差额；空结果也是成功。内连接会漏掉应入库但未记账的记录。

```sql
WITH a AS (SELECT receipt_id,SUM(quantity_delta) qty FROM stock_movements WHERE movement_type='receipt' GROUP BY receipt_id) SELECT g.receipt_id,g.po_item_id,g.quantity_received-g.quantity_rejected expected_units,COALESCE(a.qty,0) posted_units,COALESCE(a.qty,0)-(g.quantity_received-g.quantity_rejected) difference FROM goods_receipts g LEFT JOIN a ON a.receipt_id=g.receipt_id WHERE COALESCE(a.qty,0)<>g.quantity_received-g.quantity_rejected ORDER BY g.receipt_id;
```

真实执行结果：0 行；完整结果 `results/BP36.json`。

## BP37 供应商延迟与仓库资金压力（挑战）

库存＋采购联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 2025年到期采购行，按供应商计算逾期未接收金额；按商品仓库去重后汇总关联期末库存价值，仅纳入该供应商有逾期未足量收货的商品仓库；库存价值无法视为单一供应商独占。

**输出列：** supplier_id, affected_pairs, overdue_cents, exposed_stock_cents

**思路：** 按供应商商品仓聚合；连接快照；汇总暴露值。跨供应商库存暴露值不可全局相加，可能重叠。

```sql
WITH a AS (SELECT supplier_id,warehouse_id,product_id,SUM((quantity_ordered-accepted)*unit_price_cents) overdue_value FROM v_po_lines WHERE promised_date BETWEEN '2025-01-01' AND '2025-12-31' AND quantity_ordered>accepted GROUP BY supplier_id,warehouse_id,product_id) SELECT a.supplier_id,COUNT(*) affected_pairs,SUM(a.overdue_value) overdue_cents,SUM(i.quantity*i.unit_cost_cents) exposed_stock_cents FROM a JOIN inventory_snapshots i ON i.warehouse_id=a.warehouse_id AND i.product_id=a.product_id AND i.snapshot_date='2025-12-31' GROUP BY a.supplier_id ORDER BY overdue_cents DESC,a.supplier_id;
```

真实执行结果：60 行；完整结果 `results/BP37.json`。

## BP38 门店贡献利润与费用（困难）

销售＋财务联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 2025年线下每门店，净销售-销售成本-归属于门店的费用分录作为贡献利润（未扣人工与总部费用，非净利润）；保留所有门店，输出贡献率。

**输出列：** store_id, net_cents, cost_cents, expense_cents, contribution_cents, contribution_pct

**思路：** 独立收入成本与费用；合并门店；计算利润。费用不可按销售明细直接连接；贡献利润不是会计净利润。

```sql
WITH a AS (SELECT store_id,SUM(net_cents) net,SUM(cost_cents) cost FROM v_sales_lines WHERE channel='store' AND order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY store_id), b AS (SELECT store_id,SUM(expense_cents) expense FROM v_expenses WHERE entry_date BETWEEN '2025-01-01' AND '2025-12-31' AND store_id IS NOT NULL GROUP BY store_id) SELECT s.store_id,COALESCE(a.net,0) net_cents,COALESCE(a.cost,0) cost_cents,COALESCE(b.expense,0) expense_cents,COALESCE(a.net-a.cost,0)-COALESCE(b.expense,0) contribution_cents,ROUND(100.0*(COALESCE(a.net-a.cost,0)-COALESCE(b.expense,0))/NULLIF(a.net,0),2) contribution_pct FROM stores s LEFT JOIN a ON a.store_id=s.store_id LEFT JOIN b ON b.store_id=s.store_id ORDER BY contribution_cents DESC,s.store_id;
```

真实执行结果：12 行；完整结果 `results/BP38.json`。

## BP39 订单结算与收入核对（困难）

销售＋财务联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 2025年全部已完成订单，支付成功金额减实际退款，比较净销售金额；输出存在差额的订单。退款本库即退款已支付，支付日期≤年底；失败支付不计。

**输出列：** order_id, net_cents, cash_net_cents, difference_cents

**思路：** 聚合订单付款；聚合退款；对照订单净额；允许空结果。使用所有付款状态或连接拆分付款导致重复。

```sql
WITH a AS (SELECT order_id,SUM(amount_cents) paid FROM payments WHERE status='settled' AND payment_date<='2025-12-31' GROUP BY order_id), b AS (SELECT i.order_id,SUM(r.refund_cents) refunded FROM returns r JOIN order_items i ON i.order_item_id=r.order_item_id WHERE r.return_date<='2025-12-31' GROUP BY i.order_id) SELECT o.order_id,o.net_cents,COALESCE(a.paid,0)-COALESCE(b.refunded,0) cash_net_cents,COALESCE(a.paid,0)-COALESCE(b.refunded,0)-o.net_cents difference_cents FROM v_order_net o LEFT JOIN a ON a.order_id=o.order_id LEFT JOIN b ON b.order_id=o.order_id WHERE o.order_date BETWEEN '2025-01-01' AND '2025-12-31' AND COALESCE(a.paid,0)-COALESCE(b.refunded,0)<>o.net_cents ORDER BY o.order_id;
```

真实执行结果：0 行；完整结果 `results/BP39.json`。

## BP40 门店月度收入与费用偏离（挑战）

销售＋财务联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 2025年每店每月，净销售、费用、费用率及过去3个月费用率（总费用/总净销售）；至少三个月才输出滚动率，缺失月份销售费用为0。

**输出列：** store_id, month_key, net_cents, expense_cents, expense_pct, rolling_pct

**思路：** 建立连续月份网格；连接两个独立汇总；窗口计算滚动率。直接平均各月费用率不能得到整体费用率。

```sql
WITH m AS (SELECT DISTINCT month_key FROM calendar_days WHERE month_key>='2025-01'), a AS (SELECT store_id,SUBSTR(order_date,1,7) m,SUM(net_cents) net FROM v_sales_lines WHERE channel='store' AND order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY store_id,SUBSTR(order_date,1,7)), b AS (SELECT store_id,SUBSTR(entry_date,1,7) m,SUM(expense_cents) expense FROM v_expenses WHERE entry_date BETWEEN '2025-01-01' AND '2025-12-31' AND store_id IS NOT NULL GROUP BY store_id,SUBSTR(entry_date,1,7)), z AS (SELECT s.store_id,m.month_key,COALESCE(a.net,0) net,COALESCE(b.expense,0) expense FROM stores s CROSS JOIN m LEFT JOIN a ON a.store_id=s.store_id AND a.m=m.month_key LEFT JOIN b ON b.store_id=s.store_id AND b.m=m.month_key), p AS (SELECT *,SUM(net) OVER(PARTITION BY store_id ORDER BY month_key ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) n3,SUM(expense) OVER(PARTITION BY store_id ORDER BY month_key ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) e3,COUNT(*) OVER(PARTITION BY store_id ORDER BY month_key ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) k FROM z) SELECT store_id,month_key,net net_cents,expense expense_cents,ROUND(100.0*expense/NULLIF(net,0),2) expense_pct,CASE WHEN k=3 THEN ROUND(100.0*e3/NULLIF(n3,0),2) ELSE NULL END rolling_pct FROM p ORDER BY store_id,month_key;
```

真实执行结果：144 行；完整结果 `results/BP40.json`。

## BP41 采购承诺、收货、开票及付款敞口（困难）

采购＋财务联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 截至2025年底每供应商，采购订单金额、实际合格收货计价、发票金额、实付及未付余额；两套数据分别按供应商汇总，保留无采购供应商。

**输出列：** supplier_id, ordered_cents, received_cents, billed_cents, paid_cents, balance_cents

**思路：** 聚合采购行和发票余额；匹配供应商；形成资金敞口。采购行和发票行直接连接会重复计入付款。

```sql
WITH a AS (SELECT supplier_id,SUM(ordered_cents) ordered,SUM(accepted*unit_price_cents) received FROM v_po_lines GROUP BY supplier_id), b AS (SELECT supplier_id,SUM(billed_cents) billed,SUM(paid_cents) paid,SUM(balance_cents) balance FROM v_invoice_balance GROUP BY supplier_id) SELECT s.supplier_id,COALESCE(a.ordered,0) ordered_cents,COALESCE(a.received,0) received_cents,COALESCE(b.billed,0) billed_cents,COALESCE(b.paid,0) paid_cents,COALESCE(b.balance,0) balance_cents FROM suppliers s LEFT JOIN a ON a.supplier_id=s.supplier_id LEFT JOIN b ON b.supplier_id=s.supplier_id ORDER BY balance_cents DESC,s.supplier_id;
```

真实执行结果：60 行；完整结果 `results/BP41.json`。

## BP42 三方不一致且已经付款的发票（困难）

采购＋财务联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 每采购单（本模拟正常一单一发票），实际接受数量按采购价计价，比较发票金额；找出金额差超过100分且已经付款的发票，并返回超付于合格收货金额的差值。

**输出列：** supplier_invoice_id, po_id, billed_cents, paid_cents, received_cents, bill_difference_cents, paid_difference_cents

**思路：** 聚合合格收货金额；关联付款余额；筛选差异。已付款不代表货物、价格、发票均一致。

```sql
WITH a AS (SELECT po_id,SUM(accepted*unit_price_cents) received FROM v_po_lines GROUP BY po_id) SELECT b.supplier_invoice_id,b.po_id,b.billed_cents,b.paid_cents,a.received received_cents,b.billed_cents-a.received bill_difference_cents,b.paid_cents-a.received paid_difference_cents FROM v_invoice_balance b JOIN a ON a.po_id=b.po_id WHERE b.paid_cents>0 AND ABS(b.billed_cents-a.received)>100 ORDER BY ABS(b.billed_cents-a.received) DESC,b.supplier_invoice_id;
```

真实执行结果：598 行；完整结果 `results/BP42.json`。

## BP43 供应商资金风险分层（挑战）

采购＋财务联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 2025年底，供应商逾期应付占未付总额比例、2025年到期订单行准时足量率；风险规则：逾期应付≥500000分且逾期占比>40%，或准时率<60%且≥10到期行；按应付降序。

**输出列：** supplier_id, outstanding_cents, overdue_cents, overdue_pct, due_lines, ontime_pct

**思路：** 独立计算账龄和履约；合并供应商；组合风险条件。不能以未到期应付代替逾期应付，也不能用零代替无历史履约。

```sql
WITH a AS (SELECT supplier_id,SUM(CASE WHEN balance_cents>0 THEN balance_cents ELSE 0 END) outstanding,SUM(CASE WHEN balance_cents>0 AND due_date<'2025-12-31' THEN balance_cents ELSE 0 END) overdue FROM v_invoice_balance GROUP BY supplier_id), b AS (SELECT supplier_id,COUNT(*) n,SUM(CASE WHEN on_time>=quantity_ordered THEN 1 ELSE 0 END) good FROM v_po_lines WHERE promised_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY supplier_id), z AS (SELECT s.supplier_id,COALESCE(a.outstanding,0) outstanding,COALESCE(a.overdue,0) overdue,100.0*a.overdue/NULLIF(a.outstanding,0) overdue_pct,COALESCE(b.n,0) due_lines,100.0*b.good/NULLIF(b.n,0) ontime_pct FROM suppliers s LEFT JOIN a ON a.supplier_id=s.supplier_id LEFT JOIN b ON b.supplier_id=s.supplier_id) SELECT supplier_id,outstanding outstanding_cents,overdue overdue_cents,ROUND(overdue_pct,2) overdue_pct,due_lines,ROUND(ontime_pct,2) ontime_pct FROM z WHERE (overdue>=500000 AND overdue_pct>40) OR (due_lines>=10 AND ontime_pct<60) ORDER BY outstanding DESC,supplier_id;
```

真实执行结果：60 行；完整结果 `results/BP43.json`。

## BP44 渠道广告后的贡献利润（困难）

电商＋财务联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 2025年线上订单归因会话渠道，净销售、毛利减广告支出为广告后贡献；未分摊管理、人力或配送费用；输出贡献率；按广告后贡献金额降序、渠道编号升序排列。

**输出列：** source_id, net_cents, gross_cents, spend_cents, contribution_cents, contribution_pct

**思路：** 独立归因毛利与支出；连接渠道；计算贡献。广告后毛利不等于公司净利润。

```sql
WITH a AS (SELECT s.source_id,SUM(o.net_cents) net,SUM(o.net_cents-o.cost_cents) gross FROM v_order_net o JOIN customer_sessions s ON s.session_id=o.session_id WHERE o.channel='online' AND o.order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY s.source_id), b AS (SELECT source_id,SUM(spend_cents) spend FROM marketing_campaigns WHERE month_key BETWEEN '2025-01' AND '2025-12' GROUP BY source_id) SELECT t.source_id,COALESCE(a.net,0) net_cents,COALESCE(a.gross,0) gross_cents,COALESCE(b.spend,0) spend_cents,COALESCE(a.gross,0)-COALESCE(b.spend,0) contribution_cents,ROUND(100.0*(COALESCE(a.gross,0)-COALESCE(b.spend,0))/NULLIF(a.net,0),2) contribution_pct FROM traffic_sources t LEFT JOIN a ON a.source_id=t.source_id LEFT JOIN b ON b.source_id=t.source_id ORDER BY contribution_cents DESC,t.source_id;
```

真实执行结果：5 行；完整结果 `results/BP44.json`。

## BP45 首购客户90天毛利与获客成本（挑战）

电商＋财务联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 按历史首次线上订单归因渠道及首购月份；只纳入截至年底有90日完整观察期客户；首购日起第0—89日的净毛利；对照该渠道该月广告支出/该月所有新客数（包括未成熟客户），求每人成熟期毛利和获客成本差额。

**输出列：** source_id, cohort, mature, new_customers, gross90_per_customer_cents, cac_cents, difference_cents

**思路：** 确定首次订单与渠道；建立完整观察窗口；计算客户毛利；独立获客成本。将未满90天客户也进入90天价值分母会产生右删失偏差。

```sql
WITH f AS (SELECT o.*,ROW_NUMBER() OVER(PARTITION BY customer_id ORDER BY order_date,order_id) rn FROM v_order_net o WHERE channel='online'), a AS (SELECT f.customer_id,f.order_date,d.day_index idx,s.source_id,SUBSTR(f.order_date,1,7) m FROM f JOIN customer_sessions s ON s.session_id=f.session_id JOIN calendar_days d ON d.day=f.order_date WHERE f.rn=1), n AS (SELECT source_id,m,COUNT(*) customers FROM a GROUP BY source_id,m), g AS (SELECT a.source_id,a.m,a.customer_id,SUM(o.net_cents-o.cost_cents) gross FROM a JOIN f o ON o.customer_id=a.customer_id JOIN calendar_days d ON d.day=o.order_date AND d.day_index BETWEEN a.idx AND a.idx+89 WHERE a.idx+89<=1095 GROUP BY a.source_id,a.m,a.customer_id), z AS (SELECT source_id,m,COUNT(*) mature,AVG(gross) avg_gross FROM g GROUP BY source_id,m) SELECT z.source_id,z.m cohort,z.mature,n.customers new_customers,ROUND(z.avg_gross,2) gross90_per_customer_cents,ROUND(1.0*p.spend_cents/n.customers,2) cac_cents,ROUND(z.avg_gross-1.0*p.spend_cents/n.customers,2) difference_cents FROM z JOIN n ON n.source_id=z.source_id AND n.m=z.m JOIN marketing_campaigns p ON p.source_id=z.source_id AND p.month_key=z.m ORDER BY z.source_id,z.m;
```

真实执行结果：169 行；完整结果 `results/BP45.json`。

## BP46 退款影响与渠道利润风险（困难）

电商＋财务联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 2025年线上渠道，退款率=退款/折后退款前销售额，退货后毛利，以及退款前毛利估算（原销售成本加回退货原成本）；按退款率降序，分母为0返回NULL。

**输出列：** source_id, before_refund_cents, refund_cents, refund_pct, before_gross_cents, after_gross_cents

**思路：** 会话内分别汇总原销售与净销售；连接会话渠道；比较前后毛利。只减退款不冲回退货成本会误算毛利。

```sql
WITH a AS (SELECT o.session_id,SUM(i.quantity*i.unit_price_cents-i.discount_cents) gross_revenue,SUM(i.quantity*i.unit_cost_cents) original_cost FROM orders o JOIN order_items i ON i.order_id=o.order_id WHERE o.channel='online' AND o.status='completed' AND o.order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY o.session_id), b AS (SELECT session_id,SUM(net_cents) net,SUM(cost_cents) cost,SUM(refund_cents) refund FROM v_sales_lines WHERE channel='online' AND order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY session_id) SELECT s.source_id,SUM(a.gross_revenue) before_refund_cents,SUM(b.refund) refund_cents,ROUND(100.0*SUM(b.refund)/NULLIF(SUM(a.gross_revenue),0),2) refund_pct,SUM(a.gross_revenue-a.original_cost) before_gross_cents,SUM(b.net-b.cost) after_gross_cents FROM customer_sessions s JOIN a ON a.session_id=s.session_id JOIN b ON b.session_id=s.session_id GROUP BY s.source_id ORDER BY refund_pct DESC,s.source_id;
```

真实执行结果：5 行；完整结果 `results/BP46.json`。

## BP47 门店人效及人工占比（困难）

人力＋销售联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 2025年线下门店，按员工月归属门店的人力成本、出勤+加班工时；销售每小时及人工占净销售比例；保留所有门店。

**输出列：** store_id, net_cents, labor_cents, worked_hours, sales_per_hour_cents, labor_pct

**思路：** 聚合历史岗位月成本；汇总销售；合并计算人效。不得使用当前归属替代历史岗位或重复累加月薪。

```sql
WITH a AS (SELECT store_id,SUM(net_cents) net FROM v_sales_lines WHERE channel='store' AND order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY store_id), b AS (SELECT store_id,SUM(labor_cents) labor,SUM(worked_minutes+overtime_minutes) minutes_worked FROM v_staff_month WHERE month_key BETWEEN '2025-01' AND '2025-12' AND store_id IS NOT NULL GROUP BY store_id) SELECT s.store_id,COALESCE(a.net,0) net_cents,COALESCE(b.labor,0) labor_cents,ROUND(1.0*COALESCE(b.minutes_worked,0)/60,2) worked_hours,ROUND(60.0*COALESCE(a.net,0)/NULLIF(b.minutes_worked,0),2) sales_per_hour_cents,ROUND(100.0*COALESCE(b.labor,0)/NULLIF(a.net,0),2) labor_pct FROM stores s LEFT JOIN a ON a.store_id=s.store_id LEFT JOIN b ON b.store_id=s.store_id ORDER BY s.store_id;
```

真实执行结果：12 行；完整结果 `results/BP47.json`。

## BP48 门店人员变化与销售变化诊断（挑战）

人力＋销售联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 2025年门店×月，月末在岗人数使用历史区间，月度净销售使用实际交易；计算环比人力及销售变化率，上月为0或缺失返回NULL；标记人数增加而销售下降。

**输出列：** store_id, month_key, headcount, net_cents, headcount_change_pct, sales_change_pct, review_flag

**思路：** 重建历史人数；独立月份销售；窗口比较；生成复核标志。人力变动与销售变化相关并不证明因果。

```sql
WITH m AS (SELECT month_key,MAX(day) last_day FROM calendar_days WHERE month_key>='2024-12' GROUP BY month_key), h AS (SELECT s.store_id,m.month_key,COUNT(e.employee_id) headcount FROM stores s CROSS JOIN m LEFT JOIN employment_history e ON e.store_id=s.store_id AND e.start_date<=m.last_day AND (e.end_date IS NULL OR e.end_date>m.last_day) GROUP BY s.store_id,m.month_key), a AS (SELECT store_id,SUBSTR(order_date,1,7) m,SUM(net_cents) net FROM v_sales_lines WHERE channel='store' AND order_date>='2024-12-01' GROUP BY store_id,SUBSTR(order_date,1,7)), z AS (SELECT h.*,COALESCE(a.net,0) net FROM h LEFT JOIN a ON a.store_id=h.store_id AND a.m=h.month_key), p AS (SELECT *,LAG(headcount) OVER(PARTITION BY store_id ORDER BY month_key) prev_h,LAG(net) OVER(PARTITION BY store_id ORDER BY month_key) prev_net FROM z) SELECT store_id,month_key,headcount,net net_cents,ROUND(100.0*(headcount-prev_h)/NULLIF(prev_h,0),2) headcount_change_pct,ROUND(100.0*(net-prev_net)/NULLIF(prev_net,0),2) sales_change_pct,CASE WHEN headcount>prev_h AND net<prev_net THEN 1 ELSE 0 END review_flag FROM p WHERE month_key>='2025-01' ORDER BY store_id,month_key;
```

真实执行结果：144 行；完整结果 `results/BP48.json`。

## BP49 月度经营汇总表（挑战）

企业综合经营联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 2025年每月全渠道净销售及毛利、费用分录、人工估算成本、广告支出、采购承诺金额；广告支出独立展示（不并入费用避免潜在双计）；经营贡献=毛利-费用-人工，不是净利润。

**输出列：** month_key, net_cents, gross_cents, expense_cents, labor_cents, advertising_cents, committed_purchase_cents, operating_contribution_cents

**思路：** 各模块独立汇总月份；连接月份轴；展示经营贡献及独立指标。采购承诺不是费用，不能直接从当月利润扣除。

```sql
WITH m AS (SELECT DISTINCT month_key FROM calendar_days WHERE month_key>='2025-01'), s AS (SELECT SUBSTR(order_date,1,7) m,SUM(net_cents) net,SUM(net_cents-cost_cents) gross FROM v_sales_lines WHERE order_date>='2025-01-01' GROUP BY SUBSTR(order_date,1,7)), f AS (SELECT SUBSTR(entry_date,1,7) m,SUM(expense_cents) expense FROM v_expenses WHERE entry_date>='2025-01-01' GROUP BY SUBSTR(entry_date,1,7)), h AS (SELECT month_key,SUM(labor_cents) labor FROM v_staff_month WHERE month_key>='2025-01' GROUP BY month_key), p AS (SELECT SUBSTR(order_date,1,7) m,SUM(ordered_cents) procurement FROM v_po_lines WHERE order_date>='2025-01-01' GROUP BY SUBSTR(order_date,1,7)), a AS (SELECT month_key,SUM(spend_cents) spend FROM marketing_campaigns WHERE month_key>='2025-01' GROUP BY month_key) SELECT m.month_key,COALESCE(s.net,0) net_cents,COALESCE(s.gross,0) gross_cents,COALESCE(f.expense,0) expense_cents,COALESCE(h.labor,0) labor_cents,COALESCE(a.spend,0) advertising_cents,COALESCE(p.procurement,0) committed_purchase_cents,COALESCE(s.gross,0)-COALESCE(f.expense,0)-COALESCE(h.labor,0) operating_contribution_cents FROM m LEFT JOIN s ON s.m=m.month_key LEFT JOIN f ON f.m=m.month_key LEFT JOIN h ON h.month_key=m.month_key LEFT JOIN p ON p.m=m.month_key LEFT JOIN a ON a.month_key=m.month_key ORDER BY m.month_key;
```

真实执行结果：12 行；完整结果 `results/BP49.json`。

## BP50 跨域季度风险评分与经营报告（挑战）

企业综合经营联合分析：管理层要求以同一企业口径核对业务数据。

**要求：** 2025年每季度，计算毛利率、费用率、人工率、供应商准时足量率、期末库存资金；风险项为毛利率<30%、费用率>50%、人工率>20%、履约<60%；保留所有季度，缺失不计风险并显示缺失指标数。使用月份映射，不将采购承诺算费用。

**输出列：** q, net_cents, margin_pct, expense_pct, labor_pct, delivery_pct, stock_cents, risk_count, missing_count

**思路：** 建立季度轴；独立汇总各域；连接季度末库存；计算指标和风险。跨不同粒度连表会夸大指标；风险评分不是财务审计结论。

```sql
WITH qm AS (SELECT DISTINCT month_key,CONCAT(SUBSTR(month_key,1,4),'-Q',CASE WHEN SUBSTR(month_key,6,2)<='03' THEN '1' WHEN SUBSTR(month_key,6,2)<='06' THEN '2' WHEN SUBSTR(month_key,6,2)<='09' THEN '3' ELSE '4' END) q FROM calendar_days WHERE month_key>='2025-01'), qs AS (SELECT q,MAX(month_key) end_m FROM qm GROUP BY q), s AS (SELECT qm.q,SUM(v.net_cents) net,SUM(v.net_cents-v.cost_cents) gross FROM v_sales_lines v JOIN qm ON qm.month_key=SUBSTR(v.order_date,1,7) GROUP BY qm.q), f AS (SELECT qm.q,SUM(v.expense_cents) expense FROM v_expenses v JOIN qm ON qm.month_key=SUBSTR(v.entry_date,1,7) GROUP BY qm.q), h AS (SELECT qm.q,SUM(v.labor_cents) labor FROM v_staff_month v JOIN qm ON qm.month_key=v.month_key GROUP BY qm.q), p AS (SELECT qm.q,COUNT(*) n,SUM(CASE WHEN v.on_time>=v.quantity_ordered THEN 1 ELSE 0 END) good FROM v_po_lines v JOIN qm ON qm.month_key=SUBSTR(v.promised_date,1,7) GROUP BY qm.q), i AS (SELECT SUBSTR(snapshot_date,1,7) m,SUM(quantity*unit_cost_cents) stock FROM inventory_snapshots GROUP BY SUBSTR(snapshot_date,1,7)), z AS (SELECT qs.q,s.net,100.0*s.gross/NULLIF(s.net,0) margin,100.0*COALESCE(f.expense,0)/NULLIF(s.net,0) expense_pct,100.0*COALESCE(h.labor,0)/NULLIF(s.net,0) labor_pct,100.0*p.good/NULLIF(p.n,0) delivery,i.stock FROM qs LEFT JOIN s ON s.q=qs.q LEFT JOIN f ON f.q=qs.q LEFT JOIN h ON h.q=qs.q LEFT JOIN p ON p.q=qs.q LEFT JOIN i ON i.m=qs.end_m) SELECT q,net net_cents,ROUND(margin,2) margin_pct,ROUND(expense_pct,2) expense_pct,ROUND(labor_pct,2) labor_pct,ROUND(delivery,2) delivery_pct,stock stock_cents,CASE WHEN margin<30 THEN 1 ELSE 0 END+CASE WHEN expense_pct>50 THEN 1 ELSE 0 END+CASE WHEN labor_pct>20 THEN 1 ELSE 0 END+CASE WHEN delivery<60 THEN 1 ELSE 0 END risk_count,CASE WHEN margin IS NULL THEN 1 ELSE 0 END+CASE WHEN expense_pct IS NULL THEN 1 ELSE 0 END+CASE WHEN labor_pct IS NULL THEN 1 ELSE 0 END+CASE WHEN delivery IS NULL THEN 1 ELSE 0 END missing_count FROM z ORDER BY q;
```

真实执行结果：4 行；完整结果 `results/BP50.json`。

