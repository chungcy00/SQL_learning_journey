# procurement 实战任务

## BP26 采购价格与合同、历史价格比较（中等）

采购人员检查实际采购价是否偏离合同。

**要求：** 2025年非取消采购行；比较合同单价、成交价及同供应商同商品上一采购行的实际单价；上一行按订单日期、行编号；输出合同差价及较上一单变动率。

**输出列：** po_item_id, supplier_id, product_id, unit_price_cents, contract_cents, contract_variance_cents, previous_cents, change_pct

**思路：** 连接有效合同；计算历史上一行；筛年度并比较。简单比较未经加权的供应商总价可能混入不同商品。

```sql
WITH a AS (SELECT i.po_item_id,p.supplier_id,i.product_id,p.order_date,i.unit_price_cents,c.unit_price_cents contract_cents,LAG(i.unit_price_cents) OVER(PARTITION BY p.supplier_id,i.product_id ORDER BY p.order_date,i.po_item_id) previous_cents FROM purchase_order_items i JOIN purchase_orders p ON p.po_id=i.po_id JOIN supplier_contracts c ON c.contract_id=i.contract_id WHERE p.status<>'cancelled') SELECT po_item_id,supplier_id,product_id,unit_price_cents,contract_cents,unit_price_cents-contract_cents contract_variance_cents,previous_cents,ROUND(100.0*(unit_price_cents-previous_cents)/NULLIF(previous_cents,0),2) change_pct FROM a WHERE order_date BETWEEN '2025-01-01' AND '2025-12-31' ORDER BY po_item_id;
```

真实执行结果：3129 行；完整结果 `results/BP26.json`。

## BP27 供应商准时、完整交付与质量（困难）

采购经理比较供应商2025年到期采购行表现。

**要求：** 按2025年承诺到期日且非取消采购行；准时足量=截止承诺日接受数量≥订购量；完整=截至年底接受数量≥订购量；质量合格率=接受件数/实际收货件数；输出供应商及分母。

**输出列：** supplier_id, due_lines, ontime_pct, complete_pct, quality_pct

**思路：** 按采购行预聚合收货；筛已到期；分别计算两种件数履约和质量指标。有任意一批准时到货并不等于整行准时足量。

```sql
SELECT supplier_id,COUNT(*) due_lines,ROUND(100.0*SUM(CASE WHEN on_time>=quantity_ordered THEN 1 ELSE 0 END)/COUNT(*),2) ontime_pct,ROUND(100.0*SUM(CASE WHEN accepted>=quantity_ordered THEN 1 ELSE 0 END)/COUNT(*),2) complete_pct,ROUND(100.0*SUM(accepted)/NULLIF(SUM(received),0),2) quality_pct FROM v_po_lines WHERE promised_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY supplier_id ORDER BY supplier_id;
```

真实执行结果：60 行；完整结果 `results/BP27.json`。

## BP28 采购、收货、发票三方核对（困难）

采购控制人员识别数量、价格与金额差异。

**要求：** 2025年非取消采购行（截止年底）；输出订购、合格收货、开票数量，开票金额与合格收货按采购单价的金额差；仅数量不等或金额差不为0；未开票也需要呈现。

**输出列：** po_item_id, po_id, supplier_id, product_id, quantity_ordered, accepted, billed, billed_cents, expected_cents, variance_cents

**思路：** 核对采购行预聚合定义；计算数量及金额差；保留未开票未收货。收货明细与发票明细未经聚合直接连接形成多对多膨胀。

```sql
SELECT po_item_id,po_id,supplier_id,product_id,quantity_ordered,accepted,billed,billed_cents,accepted*unit_price_cents expected_cents,billed_cents-accepted*unit_price_cents variance_cents FROM v_po_lines WHERE order_date BETWEEN '2025-01-01' AND '2025-12-31' AND (quantity_ordered<>accepted OR accepted<>billed OR billed_cents<>accepted*unit_price_cents) ORDER BY po_item_id;
```

真实执行结果：876 行；完整结果 `results/BP28.json`。

## BP29 同商品跨供应商采购价差（困难）

管理层希望看到同品不同供应商的采购单价差异。

**要求：** 2025年非取消采购行，同商品同供应商采购金额/订购量为加权单价；只比较有至少两家供应商商品；输出供应商加权价、最低价及相对最低价的溢价率。

**输出列：** product_id, supplier_id, weighted_price_cents, low_price_cents, premium_pct

**思路：** 按每商品供应商求加权价；窗口求最低与样本数；比较价差。对单位价格直接AVG没有反映采购数量权重。

```sql
WITH a AS (SELECT product_id,supplier_id,SUM(ordered_cents)*1.0/SUM(quantity_ordered) avg_price FROM v_po_lines WHERE order_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY product_id,supplier_id), b AS (SELECT *,MIN(avg_price) OVER(PARTITION BY product_id) low,COUNT(*) OVER(PARTITION BY product_id) n FROM a) SELECT product_id,supplier_id,ROUND(avg_price,2) weighted_price_cents,ROUND(low,2) low_price_cents,ROUND(100.0*(avg_price-low)/NULLIF(low,0),2) premium_pct FROM b WHERE n>=2 ORDER BY product_id,premium_pct,supplier_id;
```

真实执行结果：900 行；完整结果 `results/BP29.json`。

## BP30 供应商多指标综合评价（挑战）

采购部门需要可解释的供应商评分。

**要求：** 2025年到期采购行；至少10行；准时足量率40%、完整率25%、质量合格率20%、合同价格达标率15%（实际采购价≤合同价行数/到期行数）；质量分母为0时评分NULL；输出各指标和总分，按总分降序。

**输出列：** supplier_id, due_lines, ontime_pct, complete_pct, quality_pct, contract_pct, score

**思路：** 计算四项百分比；保留充足样本；加权评分；排序。不说明权重、样本门槛和缺失指标处理会使评分不可解释。

```sql
WITH a AS (SELECT v.supplier_id,COUNT(*) n,100.0*SUM(CASE WHEN v.on_time>=v.quantity_ordered THEN 1 ELSE 0 END)/COUNT(*) ot,100.0*SUM(CASE WHEN v.accepted>=v.quantity_ordered THEN 1 ELSE 0 END)/COUNT(*) fill,100.0*SUM(accepted)/NULLIF(SUM(received),0) quality,100.0*SUM(CASE WHEN v.unit_price_cents<=c.unit_price_cents THEN 1 ELSE 0 END)/COUNT(*) price_ok FROM v_po_lines v JOIN purchase_order_items i ON i.po_item_id=v.po_item_id JOIN supplier_contracts c ON c.contract_id=i.contract_id WHERE v.promised_date BETWEEN '2025-01-01' AND '2025-12-31' GROUP BY v.supplier_id) SELECT supplier_id,n due_lines,ROUND(ot,2) ontime_pct,ROUND(fill,2) complete_pct,ROUND(quality,2) quality_pct,ROUND(price_ok,2) contract_pct,ROUND(.4*ot+.25*fill+.2*quality+.15*price_ok,2) score FROM a WHERE n>=10 ORDER BY score DESC,supplier_id;
```

真实执行结果：60 行；完整结果 `results/BP30.json`。

