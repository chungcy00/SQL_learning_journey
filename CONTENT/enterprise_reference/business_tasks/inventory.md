# inventory 实战任务

## BP06 低销量高库存诊断（中等）

仓储经理希望优先处理积压货品。

**要求：** 统计2025-10-03至12-31共90日仓库销售出库量；期末为12-31；有效商品、库存价值超过500000分且销量低于20件；无销售记0，库存覆盖天数无销售返回NULL。

**输出列：** warehouse_id, product_id, quantity, sold_units, stock_value_cents, cover_days

**思路：** 汇总90天销售出库；关联期末快照与有效商品；补零并应用双阈值；计算覆盖天数。用内连接会漏掉零销售的积压商品。

```sql
WITH s AS (SELECT warehouse_id,product_id,SUM(-quantity_delta) sold FROM stock_movements WHERE movement_type='sale' AND movement_date BETWEEN '2025-10-03' AND '2025-12-31' GROUP BY warehouse_id,product_id) SELECT i.warehouse_id,i.product_id,i.quantity,COALESCE(s.sold,0) sold_units,i.quantity*i.unit_cost_cents stock_value_cents,ROUND(90.0*i.quantity/NULLIF(s.sold,0),2) cover_days FROM inventory_snapshots i JOIN products p ON p.product_id=i.product_id LEFT JOIN s ON s.warehouse_id=i.warehouse_id AND s.product_id=i.product_id WHERE p.active=1 AND i.snapshot_date='2025-12-31' AND i.quantity*i.unit_cost_cents>500000 AND COALESCE(s.sold,0)<20 ORDER BY stock_value_cents DESC,i.warehouse_id,i.product_id;
```

真实执行结果：823 行；完整结果 `results/BP06.json`。

## BP07 类别库存周转率及周转天数（困难）

仓储部门比较不同仓库与品类的资金效率。

**要求：** 2025年：净销售出库成本含退货冲回；年初价值以2024-12-31快照、年末以2025-12-31快照；平均库存=两端均值；周转天数365/周转率，销售成本非正时天数NULL。

**输出列：** warehouse_id, category_id, cogs_cents, avg_stock_cents, turnover, turnover_days

**思路：** 分别汇总期间净成本和两端快照；连接相同粒度；计算比例及周转天数。销售额不是出库成本；逐商品周转率的简单平均也不是类别周转率。

```sql
WITH a AS (SELECT warehouse_id,p.category_id,SUM(CASE WHEN movement_type='sale' THEN -quantity_delta*unit_cost_cents WHEN movement_type='return' THEN -quantity_delta*unit_cost_cents ELSE 0 END) cogs FROM stock_movements m JOIN products p ON p.product_id=m.product_id WHERE movement_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY warehouse_id,p.category_id), b AS (SELECT warehouse_id,p.category_id,SUM(quantity*unit_cost_cents)/2.0 avg_stock FROM inventory_snapshots i JOIN products p ON p.product_id=i.product_id WHERE snapshot_date IN ('2024-12-31','2025-12-31') GROUP BY warehouse_id,p.category_id) SELECT b.warehouse_id,b.category_id,COALESCE(a.cogs,0) cogs_cents,b.avg_stock avg_stock_cents,ROUND(COALESCE(a.cogs,0)/NULLIF(b.avg_stock,0),4) turnover,CASE WHEN a.cogs>0 THEN ROUND(365.0*b.avg_stock/a.cogs,2) ELSE NULL END turnover_days FROM b LEFT JOIN a ON a.warehouse_id=b.warehouse_id AND a.category_id=b.category_id ORDER BY b.warehouse_id,b.category_id;
```

真实执行结果：45 行；完整结果 `results/BP07.json`。

## BP08 销售速度与采购周期缺货风险（困难）

补货主管需要找出采购尚未到货时可能售罄的商品。

**要求：** 截止2025年底，近30天销售出库速度；供应商历史实际收货周期按合格收货量加权（使用完整2025年收货记录），无记录则14天；预计需要库存=日均出库×历史周期；仅有效商品且预计需求超过当前库存。

**输出列：** warehouse_id, product_id, quantity, sold, lead_days, expected_shortage

**思路：** 计算销售速度；按合格件加权实际交期；合并库存；识别预计缺口。把下单承诺日当实际收货日，或以0天替代缺失历史。

```sql
WITH s AS (SELECT warehouse_id,product_id,SUM(-quantity_delta) sold FROM stock_movements WHERE movement_type='sale' AND movement_date BETWEEN '2025-12-02' AND '2025-12-31' GROUP BY warehouse_id,product_id), l AS (SELECT p.warehouse_id,i.product_id,SUM((b.day_index-a.day_index)*(g.quantity_received-g.quantity_rejected))*1.0/NULLIF(SUM(g.quantity_received-g.quantity_rejected),0) lead_days FROM goods_receipts g JOIN purchase_order_items i ON i.po_item_id=g.po_item_id JOIN purchase_orders p ON p.po_id=i.po_id JOIN calendar_days a ON a.day=p.order_date JOIN calendar_days b ON b.day=g.receipt_date WHERE g.receipt_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY p.warehouse_id,i.product_id), z AS (SELECT i.warehouse_id,i.product_id,i.quantity,COALESCE(s.sold,0) sold,COALESCE(l.lead_days,14) lead_days FROM inventory_snapshots i JOIN products p ON p.product_id=i.product_id LEFT JOIN s ON s.warehouse_id=i.warehouse_id AND s.product_id=i.product_id LEFT JOIN l ON l.warehouse_id=i.warehouse_id AND l.product_id=i.product_id WHERE i.snapshot_date='2025-12-31' AND p.active=1) SELECT warehouse_id,product_id,quantity,sold,ROUND(lead_days,2) lead_days,ROUND(sold*lead_days/30.0-quantity,2) expected_shortage FROM z WHERE sold*lead_days/30.0>quantity ORDER BY expected_shortage DESC,warehouse_id,product_id;
```

真实执行结果：41 行；完整结果 `results/BP08.json`。

## BP09 按先进先出假设推算库存库龄（困难）

仓储部门希望识别期末长期库存；本库无实物批次拣货记录，按FIFO数量推算，非实际批次审计。

**要求：** 将期初库存视作2023-01-01批次，收货与退货视作入库批次；累计销售出库消耗最早批次；期末剩余批次中2025-07-04及以前为≥180日；按仓库商品输出陈旧件数、陈旧金额（期末标准成本）和陈旧比例；仅有陈旧件数。

**输出列：** warehouse_id, product_id, aged_units, aged_value_cents, aged_pct

**思路：** 合并期初和入库批次；累计入库数量；扣减累计出库；计算旧批次剩余；关联期末成本。使用最后一次入库日期代表所有库存库龄会低估老批次。

```sql
WITH a AS (SELECT warehouse_id,product_id,'2023-01-01' d,0 seq,quantity q FROM inventory_opening WHERE quantity>0 UNION ALL SELECT warehouse_id,product_id,movement_date,movement_id,quantity_delta FROM stock_movements WHERE quantity_delta>0), b AS (SELECT *,SUM(q) OVER(PARTITION BY warehouse_id,product_id ORDER BY d,seq ROWS UNBOUNDED PRECEDING) cum FROM a), s AS (SELECT warehouse_id,product_id,SUM(-quantity_delta) sold FROM stock_movements WHERE movement_type='sale' GROUP BY warehouse_id,product_id), z AS (SELECT b.*,CASE WHEN b.cum<=COALESCE(s.sold,0) THEN 0 WHEN b.cum-b.q>=COALESCE(s.sold,0) THEN b.q ELSE b.cum-COALESCE(s.sold,0) END remain FROM b LEFT JOIN s ON s.warehouse_id=b.warehouse_id AND s.product_id=b.product_id), g AS (SELECT warehouse_id,product_id,SUM(CASE WHEN d<='2025-07-04' THEN remain ELSE 0 END) aged,SUM(remain) total FROM z GROUP BY warehouse_id,product_id) SELECT g.warehouse_id,g.product_id,g.aged aged_units,g.aged*i.unit_cost_cents aged_value_cents,ROUND(100.0*g.aged/NULLIF(g.total,0),2) aged_pct FROM g JOIN inventory_snapshots i ON i.warehouse_id=g.warehouse_id AND i.product_id=g.product_id AND i.snapshot_date='2025-12-31' WHERE g.aged>0 ORDER BY aged_value_cents DESC,g.warehouse_id,g.product_id;
```

真实执行结果：1392 行；完整结果 `results/BP09.json`。

## BP10 补货优先级与供应商风险（挑战）

库存主管需要同时考虑销售速度、在途量与采购履约。

**要求：** 期末，未来30天需求=近30天实际销售出库量；在途=未取消且承诺日严格晚于2025-12-31、且未完成采购的剩余数量；履约风险用对应商品历史采购准时足量率（承诺日已到且非取消），缺失NULL；对有效商品计算缺口、库存金额、供应商风险，按缺口降序。

**输出列：** warehouse_id, product_id, quantity, stock_value_cents, demand, inbound, shortage, reliability_pct

**思路：** 分别统计销售和采购履约；筛选未来承诺在途；连接期末；排名缺口。将已逾期未到货全部当作确定在途会掩盖风险。

```sql
WITH s AS (SELECT warehouse_id,product_id,SUM(-quantity_delta) sold FROM stock_movements WHERE movement_type='sale' AND movement_date BETWEEN '2025-12-02' AND '2025-12-31' GROUP BY warehouse_id,product_id), p AS (SELECT warehouse_id,product_id,SUM(CASE WHEN promised_date>'2025-12-31' AND quantity_ordered>accepted THEN quantity_ordered-accepted ELSE 0 END) inbound,SUM(CASE WHEN promised_date<='2025-12-31' AND on_time>=quantity_ordered THEN 1 ELSE 0 END) good,SUM(CASE WHEN promised_date<='2025-12-31' THEN 1 ELSE 0 END) due FROM v_po_lines GROUP BY warehouse_id,product_id), z AS (SELECT i.warehouse_id,i.product_id,i.quantity,i.quantity*i.unit_cost_cents stock_value_cents,COALESCE(s.sold,0) demand,COALESCE(p.inbound,0) inbound,100.0*p.good/NULLIF(p.due,0) reliability FROM inventory_snapshots i JOIN products a ON a.product_id=i.product_id LEFT JOIN s ON s.warehouse_id=i.warehouse_id AND s.product_id=i.product_id LEFT JOIN p ON p.warehouse_id=i.warehouse_id AND p.product_id=i.product_id WHERE i.snapshot_date='2025-12-31' AND a.active=1) SELECT warehouse_id,product_id,quantity,stock_value_cents,demand,inbound,demand-quantity-inbound shortage,ROUND(reliability,2) reliability_pct FROM z WHERE demand>quantity+inbound ORDER BY shortage DESC,warehouse_id,product_id;
```

真实执行结果：68 行；完整结果 `results/BP10.json`。

