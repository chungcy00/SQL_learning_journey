import sqlite3, pathlib as P, json,csv,hashlib,shutil,re,collections,time,datetime as dt,zipfile,sys
R=P.Path('/mnt/data/enterprise_sql_upgrade'); D=R/'databases'; db=D/'enterprise'/'enterprise.sqlite'; c=sqlite3.connect(db); c.execute('PRAGMA foreign_keys=ON'); c.create_function('CONCAT',-1,lambda *x: None if None in x else ''.join(map(str,x)),deterministic=True)
def dump(p,x):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(x,ensure_ascii=False,indent=2),encoding='utf-8')
def write(p,x):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(x,encoding='utf-8')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
# intentional isolated business anomalies: duplicates reference number, overpayment and settlement mismatch
v=c.execute('SELECT supplier_id,MIN(supplier_invoice_id),MAX(supplier_invoice_id) FROM v_invoice_balance GROUP BY supplier_id HAVING COUNT(*)>1 ORDER BY supplier_id LIMIT 8').fetchall()
for s,a,b in v:c.execute('UPDATE supplier_invoices SET invoice_no=(SELECT invoice_no FROM supplier_invoices WHERE supplier_invoice_id=?) WHERE supplier_invoice_id=?',(a,b))
for a,p in c.execute("SELECT supplier_invoice_id,paid_cents FROM v_invoice_balance WHERE balance_cents=0 ORDER BY supplier_invoice_id LIMIT 6").fetchall():
 i=c.execute('SELECT MAX(supplier_payment_id)+1 FROM supplier_payments').fetchone()[0];c.execute('INSERT INTO supplier_payments VALUES (?,?,?,?)',(i,a,'2025-12-31',250))
c.commit()
exec((P.Path(__file__).parent/'task_definitions.py').read_text())
# fix one business issue: cutoff 90 days >= 90 means last purchase at cutoff-90 (inclusive); wording says no recent 90 days.
# actual SQLite and native query variants; no implicit dialect claim
schema={n:s for n,s in c.execute("SELECT name,sql FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY rowid")};views={n:s for n,s in c.execute("SELECT name,sql FROM sqlite_master WHERE type='view' ORDER BY rowid")};idx={n:s for n,s in c.execute("SELECT tbl_name,sql FROM sqlite_master WHERE type='index' AND sql IS NOT NULL")}
indexes=collections.defaultdict(list)
for n,s in c.execute("SELECT tbl_name,sql FROM sqlite_master WHERE type='index' AND sql IS NOT NULL ORDER BY name"):indexes[n].append(s)
allnames=set(schema)|set(views)
def deps(sql):return {x for x in re.findall(r'\b(?:FROM|JOIN)\s+([a-z_][a-z_0-9]*)',sql,re.I) if x in allnames}
def closure(names):
 names=set(names)
 while True:
  add=set()
  for n in names:
   if n in views:add|=deps(views[n])
   else:add|={x[2] for x in c.execute(f'PRAGMA foreign_key_list({n})')}
  if add<=names:return names
  names|=add
# use actual independent database dependencies, include logical group's additional tables
modules={'retail':{'stores','categories','products','customers','product_costs','orders','order_items','returns','payments','sales_targets','promotions','raw_order_import'},'inventory':{'warehouses','products','product_costs','inventory_opening','stock_movements','inventory_snapshots','purchase_orders','purchase_order_items','goods_receipts'},'ecommerce':{'customers','customer_sessions','traffic_sources','marketing_campaigns','orders','order_items','returns','payments','products','raw_order_import'},'finance':{'accounts','journal_entries','journal_lines','bank_transactions','budgets','supplier_invoices','supplier_invoice_items','supplier_payments'},'hr':{'departments','positions','employees','employment_history','salary_history','attendance'},'procurement':{'suppliers','products','supplier_contracts','purchase_orders','purchase_order_items','goods_receipts','supplier_invoices','supplier_invoice_items'}}
for k in modules:modules[k]=closure(modules[k]|set().union(*(deps(t['sql']) for t in T if t['projectId']==k)))
modules['enterprise']=allnames
# execute and repair compatibility query ambiguities if necessary
for t in T:
 t['mysqlSql']=re.sub(r' AS INTEGER\)', ' AS SIGNED)',t['sql']); t['sqliteSql']=t['sql'];t['compatibility']='转换执行' if 'CONCAT(' in t['sql'] or t['mysqlSql']!=t['sql'] else '直接兼容（原生待验证）'
 if 'CONCAT(' in t['sql']:
  # SQLite newer versions have CONCAT, but app older may need UDF; explicit function registration used for test
  pass
 try:
  st=time.perf_counter();cur=c.execute(t['sqliteSql']); rr=cur.fetchall();t['columns']=[x[0] for x in cur.description];t['rows']=rr;t['elapsedMs']=round((time.perf_counter()-st)*1000,1)
 except Exception as e:print('FAIL',t['title'],e,flush=True);raise
 print(t['projectId'],t['title'],len(rr),t['elapsedMs'],flush=True)
# 6+29+15 desired; cross challenge exactly 9
print(collections.Counter(t['difficulty'] for t in T),flush=True)
# table metadata: explicit field meanings plus SQL types, FK, indexes
meaning={'day':'日历日期','day_index':'从2023-01-01开始的日序号（0起）','month_key':'统计月份 YYYY-MM','quantity':'数量（件）','quantity_delta':'库存变化数量（入库正、出库负）','quantity_received':'实际收货件数（含拒收）','quantity_rejected':'拒收件数','quantity_ordered':'订购件数','quantity_billed':'开票件数','active':'有效标志 1/0','grade':'职级','status':'业务状态（见表约束及口径）','channel':'store 线下 / online 线上','reference_no':'对账参考编号','invoice_no':'供应商发票业务编号（可重复异常）','region':'区域（客户可缺失）','movement_type':'receipt 收货 / return 退货 / sale 销售','account_type':'expense 费用 / cash 银行存款','work_minutes':'正常出勤分钟','overtime_minutes':'加班分钟','raw_date':'未经清洗日期','raw_amount':'未经清洗金额文本','raw_customer':'未经清洗客户文本','source_ref':'原始导入参考号'}
mdesc={'stores':'门店主档','categories':'商品分类','products':'商品主档','customers':'客户主档（虚构）','suppliers':'供应商主档','warehouses':'配送仓主档','departments':'部门主档','calendar_days':'连续日期维度','product_costs':'标准成本历史','promotions':'促销期间','traffic_sources':'线上访问渠道','marketing_campaigns':'渠道月广告支出','customer_sessions':'访问会话','orders':'销售订单（线上线下统一）','order_items':'销售明细','returns':'退货/退款','payments':'订单支付','sales_targets':'月度门店净销售目标','supplier_contracts':'年度采购合同价格','purchase_orders':'采购订单','purchase_order_items':'采购明细','goods_receipts':'收货与质检数量','supplier_invoices':'供应商发票头','supplier_invoice_items':'供应商发票明细','supplier_payments':'供应商付款','inventory_opening':'2023年初库存','stock_movements':'可售库存入出流水','inventory_snapshots':'月末库存及标准成本','accounts':'财务科目','journal_entries':'费用凭证头','journal_lines':'借贷分录','bank_transactions':'银行流水','budgets':'部门科目预算','positions':'职位职级','employees':'虚构员工','employment_history':'历史岗位和门店','salary_history':'员工月薪','attendance':'出勤及加班','raw_order_import':'原始导入脏数据隔离区'}
# field SQL mapping; MySQL foreign key table constraints explicitly emitted
# split commas at top-level while respecting quotes and parentheses
def split(s):
 out=[];buf='';depth=0;q=False
 for ch in s:
  if ch=="'":q=not q
  if not q:
   if ch=='(':depth+=1
   if ch==')':depth-=1
   if ch==',' and depth==0:out.append(buf.strip());buf='';continue
  buf+=ch
 if buf.strip():out.append(buf.strip())
 return out
def mysqltype(col,typ):
 if typ=='INTEGER':return 'BIGINT' if col.endswith('_cents') or col.endswith('_id') else 'INT'
 if col!='raw_date' and (col in ('day','start_date','end_date') or col.endswith('_date')):return 'DATE'
 if col=='month_key':return 'CHAR(7)'
 return 'VARCHAR(160)'
def mysql_ddl(n):
 f=[]; refs=[]
 for part in split(schema[n][schema[n].index('(')+1:-1]):
  m=re.match(r'(\w+) (INTEGER|TEXT)\b(.*)',part)
  if not m:f.append(part);continue
  col,typ,tail=m.groups();match=re.search(r'\s*REFERENCES\s+(\w+)\((\w+)\)',tail)
  if match:
   refs.append(f'FOREIGN KEY ({col}) REFERENCES {match[1]}({match[2]})');tail=tail[:match.start()]+tail[match.end():]
  f.append(f'{col} {mysqltype(col,typ)}{tail}')
 return f'CREATE TABLE {n} (\n  '+',\n  '.join(f+refs)+'\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin;'
# date range and metadata
meta={}; structured=R/'structured_data';structured.mkdir(exist_ok=True)
for n in schema:
 cols=[];fks={x[3]:{'table':x[2],'column':x[4]} for x in c.execute(f'PRAGMA foreign_key_list({n})')}
 for _,col,typ,nn,default,pk in c.execute(f'PRAGMA table_info({n})'):
  label=meaning.get(col,('主键/标识' if col.endswith('_id') else '金额（马币分）' if col.endswith('_cents') else '日期（YYYY-MM-DD）' if col.endswith('_date') else '名称' if col.endswith('_name') else '业务字段'))
  cols.append(dict(name=col,description=label,sqliteType=typ,mysqlType=mysqltype(col,typ),nullable=not(nn or pk),primaryKey=bool(pk),foreignKey=fks.get(col),default=default))
 count=c.execute(f'SELECT COUNT(*) FROM {n}').fetchone()[0]
 meta[n]={'description':mdesc[n],'rowCount':count,'columns':cols,'sqliteDDL':schema[n],'mysqlDDL':mysql_ddl(n),'indexes':indexes[n]}
 with (structured/f'{n}.csv').open('w',encoding='utf-8',newline='') as out:
  w=csv.writer(out);w.writerow([f['name'] for f in cols]);w.writerows(([r'\N' if x is None else x for x in row] for row in c.execute(f'SELECT * FROM {n}')))
# MySQL portable script literal escape using apostrophes doubled, no backslash in records
# SQL scripts are complete, but shared seeds single source to avoid duplicated export. per-module DDL + insert script combined per module via copied full canonical seed tables list in runner? user wants complete scripts, export each.
def lit(v):return 'NULL' if v is None else str(v) if isinstance(v,(int,float)) else "'"+str(v).replace("'","''")+"'"
# canonical insert per table, per-module combined scripts generated with repeated tables, compressed ZIP dedup not global. acceptable
seed=R/'mysql_seed';seed.mkdir(exist_ok=True)
for n in schema:
 with (seed/f'{n}.sql').open('w',encoding='utf-8') as f:
  cur=c.execute(f'SELECT * FROM {n}');names=','.join(x[0] for x in cur.description)
  while batch:=cur.fetchmany(500):f.write(f'INSERT INTO {n} ({names}) VALUES\n'+',\n'.join('('+','.join(lit(v) for v in row)+')' for row in batch)+';\n')
counts={}; modulecheck=[]
for g,names in modules.items():
 dest=D/g;dest.mkdir(exist_ok=True);tables=[n for n in schema if n in names];vs=[n for n in views if n in names];dbp=dest/f'{g}.sqlite'
 if g!='enterprise':
  if dbp.exists():dbp.unlink()
  q=sqlite3.connect(dbp);c.backup(q);q.execute('PRAGMA foreign_keys=OFF')
  for n in reversed(list(views)):
   if n not in vs:q.execute(f'DROP VIEW {n}')
  for n in reversed(list(schema)):
   if n not in tables:q.execute(f'DROP TABLE {n}')
  q.commit();q.execute('VACUUM');q.close()
 q=sqlite3.connect(dbp);q.create_function('CONCAT',-1,lambda *x:None if None in x else ''.join(map(str,x)),deterministic=True)
 assert q.execute('PRAGMA integrity_check').fetchone()[0]=='ok';assert not q.execute('PRAGMA foreign_key_check').fetchall()
 for t in T:
  if t['projectId']==g:
   assert q.execute(t['sqliteSql']).fetchall()==t['rows'],t['title']
 q.close();counts[g]={'tables':len(tables),'views':len(vs),'rows':sum(meta[n]['rowCount'] for n in tables),'bytes':dbp.stat().st_size,'sha256':sha(dbp)}
 dump(dest/'schema.json',{'datasetId':g+'-v2','schemaVersion':2,'tables':{n:meta[n] for n in tables},'views':{n:views[n] for n in vs},'cutoff':'2025-12-31','currency':'MYR','moneyUnit':'cent'})
 txt=f'# {g} 数据字典\n\n金额字段 `_cents` 为整数马币分，展示层除以100。日期范围2023—2025，部分承诺/到期日期延至2026。\n'
 for n in tables:
  txt+=f'\n## {n} · {mdesc[n]}（{meta[n]["rowCount"]} 行）\n\n| 字段 | MySQL 类型 | 可空 | 键 | 说明 |\n|---|---|---|---|---|\n'
  for col in meta[n]['columns']:
   fk=col['foreignKey'];txt+=f'| {col["name"]} | {col["mysqlType"]} | {"是" if col["nullable"] else "否"} | {"PK " if col["primaryKey"] else ""}{(fk["table"]+"."+fk["column"]) if fk else ""} | {col["description"]} |\n'
  txt+='\n约束及索引：\n```sql\n'+meta[n]['mysqlDDL']+'\n'+';\n'.join(indexes[n])+'\n```\n'
 for n in vs:txt+=f'\n## {n}（派生视图）\n```sql\n{views[n]};\n```\n'
 write(dest/'DATA_DICTIONARY.md',txt)
 # standalone complete import, fresh empty schema only; unique db names and no destructive drops
 with (dest/'mysql_import.sql').open('w',encoding='utf-8') as out:
  out.write(f'-- 请先选择新的空数据库；脚本不删除既有数据。\nSET NAMES utf8mb4;\nSET SESSION sql_mode=\'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION\';\n')
  for n in tables:out.write(mysql_ddl(n)+'\n')
  out.write('START TRANSACTION;\n')
  for n in tables:
   with (seed/f'{n}.sql').open() as aa:shutil.copyfileobj(aa,out)
  out.write('COMMIT;\n')
  for n in tables:
   for ix in indexes[n]:out.write(ix+';\n')
  for n in vs:out.write(views[n]+';\n')
 write(dest/'README.md',f'# {g} 业务数据库\n\n独立文件 `{g}.sqlite` 直接打开，不能修改基准。`mysql_import.sql` 在空 MySQL 8.4 库执行（原生尚未实测）。结构化 CSV 共用 `structured_data/`，以本目录 schema.json 的表清单为准。\n\n{counts[g]["tables"]} 张表，{counts[g]["views"]} 个视图，{counts[g]["rows"]} 行。支持本项目的5项实战或综合20项。共享表完整复制，可能含其他渠道依赖数据，不能默认所有记录属于本业务。\n')
 modulecheck.append({'dataset':g,'integrity':'ok','foreignKeyViolations':0,'taskResultsMatchEnterprise':sum(t['projectId']==g for t in T)})
 print('exported',g,counts[g],flush=True)
# discard intermediate SQL seed (complete scripts retained)
shutil.rmtree(seed)
# integrity business rules (independent of tasks)
checks={
 '库存月末与期初流水一致':"WITH m AS (SELECT warehouse_id,product_id,SUBSTR(movement_date,1,7) m,SUM(quantity_delta) delta FROM stock_movements GROUP BY warehouse_id,product_id,SUBSTR(movement_date,1,7)), a AS (SELECT i.warehouse_id,i.product_id,i.snapshot_date,i.quantity,o.quantity+SUM(COALESCE(m.delta,0)) OVER(PARTITION BY i.warehouse_id,i.product_id ORDER BY i.snapshot_date ROWS UNBOUNDED PRECEDING) expected FROM inventory_snapshots i JOIN inventory_opening o ON o.warehouse_id=i.warehouse_id AND o.product_id=i.product_id LEFT JOIN m ON m.warehouse_id=i.warehouse_id AND m.product_id=i.product_id AND m.m=SUBSTR(i.snapshot_date,1,7)) SELECT COUNT(*) FROM a WHERE quantity<>expected",
 '收货关联采购一致':"SELECT COUNT(*) FROM goods_receipts g JOIN purchase_order_items i ON i.po_item_id=g.po_item_id JOIN purchase_orders p ON p.po_id=i.po_id WHERE p.status='cancelled' OR g.receipt_date<p.order_date",
 '销售库存出库数量一致':"WITH a AS (SELECT order_item_id,SUM(-quantity_delta) sold FROM stock_movements WHERE movement_type='sale' GROUP BY order_item_id) SELECT COUNT(*) FROM order_items i JOIN orders o ON o.order_id=i.order_id LEFT JOIN a ON a.order_item_id=i.order_item_id WHERE (o.status='completed' AND COALESCE(a.sold,0)<>i.quantity) OR (o.status<>'completed' AND a.sold IS NOT NULL)",
 '退货数量不超原销售':"SELECT COUNT(*) FROM (SELECT i.order_item_id,i.quantity,SUM(r.quantity) q FROM order_items i JOIN returns r ON r.order_item_id=i.order_item_id GROUP BY i.order_item_id,i.quantity HAVING SUM(r.quantity)>i.quantity) a",
 '凭证借贷平衡':"SELECT COUNT(*) FROM (SELECT entry_id FROM journal_lines GROUP BY entry_id HAVING SUM(debit_cents)<>SUM(credit_cents)) a",
 '员工任职区间不重叠':"SELECT COUNT(*) FROM employment_history a JOIN employment_history b ON a.employee_id=b.employee_id AND a.employment_id<b.employment_id WHERE a.start_date<COALESCE(b.end_date,'9999-12-31') AND b.start_date<COALESCE(a.end_date,'9999-12-31')",
 '线上会话客户日期一致':"SELECT COUNT(*) FROM orders o JOIN customer_sessions s ON s.session_id=o.session_id WHERE o.customer_id<>s.customer_id OR o.order_date<>s.session_date",
 '合同商品供应商有效期一致':"SELECT COUNT(*) FROM purchase_order_items i JOIN purchase_orders p ON p.po_id=i.po_id JOIN supplier_contracts s ON s.contract_id=i.contract_id WHERE p.supplier_id<>s.supplier_id OR i.product_id<>s.product_id OR p.order_date NOT BETWEEN s.start_date AND s.end_date",
 '发票明细与采购订单一致':"SELECT COUNT(*) FROM supplier_invoice_items i JOIN supplier_invoices s ON s.supplier_invoice_id=i.supplier_invoice_id JOIN purchase_order_items p ON p.po_item_id=i.po_item_id WHERE p.po_id<>s.po_id",
 '库存成本历史匹配':"SELECT COUNT(*) FROM inventory_snapshots i JOIN product_costs p ON p.product_id=i.product_id AND p.effective_date=CONCAT(SUBSTR(i.snapshot_date,1,4),'-01-01') WHERE i.unit_cost_cents<>p.unit_cost_cents"}
check_results=[]
for name,sql in checks.items():
 count=c.execute(sql).fetchone()[0];assert count==0,(name,count);check_results.append({'name':name,'sql':sql,'violations':count})
# boundary mutations: run in savepoint and verify known deltas (rollback). actual tests, not comprehensive full 50
boundary=[]
def test(name,mutate,taskindex,predicate):
 c.execute('SAVEPOINT boundary');before=c.execute(T[taskindex]['sqliteSql']).fetchall()
 try:
  for q,args in mutate:c.execute(q,args)
  after=c.execute(T[taskindex]['sqliteSql']).fetchall();assert predicate(before,after),name;boundary.append({'name':name,'taskId':f'BP{taskindex+1:02d}','status':'通过','mutation':[{'sql':q,'params':args} for q,args in mutate]})
 finally:c.execute('ROLLBACK TO boundary');c.execute('RELEASE boundary')
# task indexes 0-based. Receipt anomaly check cross index35 (6th cross). find by title.
def ti(title):return next(i for i,t in enumerate(T) if t['title']==title)
rid=c.execute("SELECT receipt_id FROM stock_movements WHERE movement_type='receipt' ORDER BY receipt_id LIMIT 1").fetchone()[0]
test('删除一笔收货入库后，核对查询必须报告该收货',[("DELETE FROM stock_movements WHERE receipt_id=?",(rid,))],ti('收货入库完整性核对'),lambda b,a: not b and any(x[0]==rid and x[-1]<0 for x in a))
po=c.execute("SELECT supplier_invoice_id FROM v_invoice_balance WHERE balance_cents=0 LIMIT 1").fetchone()[0];newid=c.execute('SELECT MAX(supplier_payment_id)+1 FROM supplier_payments').fetchone()[0]
test('增加一分钱付款后足额发票变超额',[("INSERT INTO supplier_payments VALUES(?,?,?,?)",(newid,po,'2025-12-31',1))],ti('供应商发票与付款核对'),lambda b,a: next(x for x in b if x[0]==po)[-1]=='paid' and next(x for x in a if x[0]==po)[-1]=='overpaid')
o=c.execute("SELECT order_id FROM v_order_net WHERE order_date>='2025-01-01' LIMIT 1").fetchone()[0];pay=c.execute("SELECT payment_id FROM payments WHERE order_id=? AND status='settled' LIMIT 1",(o,)).fetchone()[0]
test('减少一分钱结算后产生订单核对差额',[("UPDATE payments SET amount_cents=amount_cents-1 WHERE payment_id=?",(pay,))],ti('订单结算与收入核对'),lambda b,a: not b and any(x[0]==o and x[-1]==-1 for x in a))
# task full results, examples and lessons; keep original 102 stable and add advanced 50
C=R/'CONTENT';shutil.copytree('/mnt/data/sql_course_content',C,dirs_exist_ok=True)
advanced={19:[],20:[]};tasks=[];detail=R/'business_tasks';detail.mkdir(exist_ok=True)
for i,t in enumerate(T,1):
 ch=19 if i<=30 else 20; kp=f'kp{ch}-{30+i:02d}' if ch==19 else f'kp20-{i-30+4:02d}'; taskid=f'BP{i:02d}';project=t['projectId'];dataset=project+'-v2';sql=t['sqliteSql'];res={'columns':t['columns'],'rows':t['rows'],'orderGuaranteed':True};resultpath=f'business_tasks/results/{taskid}.json';dump(R/resultpath,res)
 write(detail/'mysql_queries'/f'{taskid}.sql',t['mysqlSql']+';\n');write(detail/'sqlite_queries'/f'{taskid}.sql',sql+';\n')
 used=sorted(closure(deps(sql)));source=[]
 # actual small source preview only, not runnable fixture; store reference path and dataset ID instead of conflicting old fixture schema
 for n in sorted(deps(sql)):
  cur=c.execute(f'SELECT * FROM {n} LIMIT 3');source.append({'name':n,'columns':[{'name':x[0],'type':next((col['sqliteType'] for col in meta.get(n,{}).get('columns',[]) if col['name']==x[0]),'派生')} for x in cur.description],'rows':cur.fetchall(),'previewOnly':True})
 concept=t['requirements'];steps=t['executionSteps'];k={'id':kp,'chapterId':f'ch{ch}','title':t['title'],'learningObjective':'在统一企业数据库中独立完成：'+t['title'],'prerequisites':['ch18'] if ch==19 else ['ch19'],'concept':concept,'syntax':t['mysqlSql'],'parameters':{'统计截止日':'2025-12-31','金额':'整数马币分；比率按题设','输出粒度':t['grain']},'fixtureIds':[],'datasetId':dataset,'sourceTables':source,'sourceTablesArePreview':True,'mysqlSql':t['mysqlSql'],'sqliteSql':sql,'executionSteps':steps,'useCase':t['context'],'comparison':'先检查输出粒度和连接键，再决定是否需要独立预聚合；不能仅比较SQL文本。','commonError':{'example':t['commonError'],'correction':'按本节业务口径及执行步骤拆分查询，并与完整输出核对。'},'notes':'完整输出见 '+resultpath+'；本节展示前12行。主库参考结果仅为公开基准，后续判题仍需独立隐藏夹具。','compatibility':{'status':t['compatibility'],'detail':'SQLite本机实际执行；MySQL 8.4 原生未运行。CONCAT 查询兼容层需支持NULL传播；整数/小数和排序需复验。'},'relatedQuestionIds':[],'relatedFlashcardIds':[],'relatedKnowledgePointIds':['kp07-01','kp08-01','kp14-01','kp15-01'],'mysqlVerification':'未运行原生 MySQL 8.4','result':{'columns':t['columns'],'rows':t['rows'][:12],'status':'SQLite实际执行（预览）','orderGuaranteed':True,'totalRows':len(t['rows']),'fullResultPath':resultpath},'projectId':project,'taskId':taskid,'difficulty':t['difficulty'],'crossGroup':t.get('crossGroup'),'mysqlVerificationScript':f'business_tasks/mysql_queries/{taskid}.sql','visualization':{'kind':'stepFlow','steps':[{'id':str(j+1),'label':s} for j,s in enumerate(steps)],'sourceFixtureIds':[],'resultColumns':t['columns']}}
 advanced[ch].append(k)
 tasks.append({**{key:val for key,val in t.items() if key not in ('rows','sql','elapsedMs')},'id':taskid,'chapterId':f'ch{ch}','knowledgePointIds':[kp],'datasetId':dataset,'difficulty':t['difficulty'],'type':'业务情景','schemaVersion':2,'contentVersion':'2.0.0','expectedOutputPath':resultpath,'expectedRowCount':len(t['rows']),'expectedColumns':t['columns'],'sourceTables':sorted(deps(sql)),'dependencyTables':used,'orderPolicy':'ordered','numericTolerance':{'integerCents':0,'decimalAbsolute':0.000001},'referenceSolutions':[{'mysqlSql':t['mysqlSql'],'sqliteSql':sql}],'hints':[steps[0], '；'.join(steps[:2]),'重点核对输出粒度：'+t['grain']],'explanation':'；'.join(steps)+'。'+t['commonError'],'testFixtureIds':[],'validationStatus':'公开基准已执行；正式题库隐藏夹具及MySQL待验证','elapsedMs':t['elapsedMs']})
 dump(detail/f'{taskid}.json',tasks[-1]);
# business_tasks also readable index
for g in modules:
 group=[x for x in tasks if x['projectId']==g];text=f'# {g} 实战任务\n\n'
 for t in group:
  text+=f'## {t["id"]} {t["title"]}（{t["difficulty"]}）\n\n{t["context"]}\n\n**要求：** {t["requirements"]}\n\n**输出列：** '+', '.join(t['expectedColumns'])+'\n\n**思路：** '+t['explanation']+'\n\n```sql\n'+t['mysqlSql']+';\n```\n\n真实执行结果：'+str(t['expectedRowCount'])+' 行；完整结果 `results/'+t['id']+'.json`。\n\n'
 write(detail/f'{g}.md',text)
for ch in (19,20):
 p=C/'courses'/f'ch{ch}.json';d=json.loads(p.read_text());d['contentVersion']='2.0.0';d['summary']+='；升级：原小型教学保留为先修，新增统一企业完整实战。';d['knowledgePoints']+=advanced[ch];d['knowledgePointCount']=len(d['knowledgePoints']);d['advancedTaskIds']=[x['taskId'] for x in advanced[ch]];dump(p,d)
 p=C/'reading'/f'ch{ch}.md';s=p.read_text();s+='\n\n# 第二部分：统一企业业务实战（新增正式案例）\n\n前述小型示例仍用于理解语法；以下使用实际大型业务库。编号保持原有知识点不变。\n'
 for k in advanced[ch]:
  s+=f'\n## {k["id"]} {k["title"]} · {k["difficulty"]}\n\n**学习目标：** {k["learningObjective"]}\n\n**业务背景：** {k["useCase"]}\n\n**业务口径：** {k["concept"]}\n\n**参数：** '+ '；'.join(f'{a}：{b}' for a,b in k['parameters'].items())+'\n\n**原始数据预览（每表至多3行，完整数据见对应数据库）：**\n'
  for source in k['sourceTables']:
   s+=f'\n`{source["name"]}`\n\n| '+' | '.join(x['name'] for x in source['columns'])+' |\n| '+' | '.join('---' for x in source['columns'])+' |\n'
   for row in source['rows']:s+='| '+' | '.join('NULL' if v is None else str(v).replace('|','\\|') for v in row)+' |\n'
  s+='\n**MySQL 参考查询：**\n```sql\n'+k['mysqlSql']+';\n```\n\n**执行过程：** '+' → '.join(k['executionSteps'])+'\n\n**SQLite 实际输出（前12行）：**\n\n| '+' | '.join(k['result']['columns'])+' |\n| '+' | '.join('---' for x in k['result']['columns'])+' |\n'
  for row in k['result']['rows']:s+='| '+' | '.join('NULL' if v is None else str(v) for v in row)+' |\n'
  s+=f'\n总计 {k["result"]["totalRows"]} 行。\n\n**易错：** {k["commonError"]["example"]}\n\n**兼容性：** {k["compatibility"]["detail"]}\n'
 write(p,s)
# update manifest and coverage without misrepresenting historical validation
p=C/'manifest.json';m=json.loads(p.read_text());m['contentVersion']='2.0.0';m['advancedTasks']=50;m['knowledgePointCount']=152
for a in m['chapters']:
 a['knowledgePointCount']=len(json.loads((C/a['path']).read_text())['knowledgePoints'])
m['enterpriseResourceRoot']='..';m['datasets']={g:{'path':f'../databases/{g}/{g}.sqlite','datasetId':g+'-v2','schemaVersion':2} for g in modules};dump(p,m)
# save task index and report
summary={'difficulty':dict(collections.Counter(t['difficulty'] for t in tasks)),'chapterCounts':dict(collections.Counter(t['chapterId'] for t in tasks)),'projectCounts':dict(collections.Counter(t['projectId'] for t in tasks)),'crossCounts':dict(collections.Counter(t['crossGroup'] for t in tasks if t.get('crossGroup'))),'tasks':[{'id':t['id'],'title':t['title'],'projectId':t['projectId'],'knowledgePointIds':t['knowledgePointIds'],'difficulty':t['difficulty'],'expectedRowCount':t['expectedRowCount'],'path':f'business_tasks/{t["id"]}.json'} for t in tasks]};dump(detail/'manifest.json',summary)
validation=R/'validation';dump(validation/'checks.json',{'sqliteVersion':sqlite3.sqlite_version,'datasets':modulecheck,'businessChecks':check_results,'boundaryTests':boundary,'executions':[{'id':t['id'],'rows':t['expectedRowCount'],'ms':t['elapsedMs'],'mysql':'未执行','sqlite':'通过'} for t in tasks]})
# validate copies by exact table checksums/row comparison (using EXCEPT both directions; each table PK unique)
for g in modules:
 if g=='enterprise':continue
 q=sqlite3.connect(D/g/f'{g}.sqlite');q.execute('ATTACH DATABASE ? AS master',(str(db),))
 for n in [n for n in schema if n in modules[g]]:
  assert q.execute(f'SELECT COUNT(*) FROM (SELECT * FROM main.{n} EXCEPT SELECT * FROM master.{n}) a').fetchone()[0]==0
  assert q.execute(f'SELECT COUNT(*) FROM (SELECT * FROM master.{n} EXCEPT SELECT * FROM main.{n}) a').fetchone()[0]==0
 q.close()
# update docs with precise actual design
DOC=R/'docs';DOC.mkdir(exist_ok=True)
base=P.Path('/mnt/data/sql_mobile_development/docs')
newdb='''# 统一企业数据模型（版本2.0）\n\n本文件覆盖第一版 DATABASE.md 的六套独立虚构业务模型。原课堂小型夹具不改。实际以对应数据库 schema.json 和 DATA_DICTIONARY.md 为准。\n\n## 企业与口径\n虚构马来西亚多渠道零售企业，历史2023-01-01至2025-12-31，所有金额为马币整数分（`_cents`），展示除100；汇率不涉及。日期为日粒度，不含时区时间戳。12门店共用3配送仓，门店编号按 `(store_id-1)%3+1` 对应仓，线上从仓1发货；因此库存只能按仓库分析，不能伪造门店库存。订单有效状态 completed；退款截至2025年底追溯原订单日期，退货恢复原数量成本；毛利不是净利润。商品价格/合同成本有年度历史；库存价值用期末标准成本估算，FIFO库龄是数量假设，非实际批次会计估价。\n\n采购以订单行连接多批收货和发票明细；质量拒收不进入可售库存；一单一正常发票是本模拟约束，发票业务编号可重复（异常）。采购承诺、费用、付款不同，不能直接当成利润。费用账簿用于费用与银行对账，非完整总账；销售支付和供应商付款独立，不能称完整财务报表。员工历史岗位左闭右开，月薪归属当月最后在职岗位，按每月176小时和1.5倍时薪估算加班，非实际法定薪酬。营销按会话渠道归因，广告与财务分录不做强制关联。\n\n## 数据与关联\n六个独立库从同一综合库复制必要的完整表及关联依赖表，主键和数据完全一致；综合库是内部执行资源，不增加第七个业务项目入口。所有表、字段、类型、索引、约束及视图见数据库目录内的数据字典。CSV 为权威结构化导出，UTF-8，首行为字段；字面 `\\N` 表示 NULL，空字符串不是NULL。导入脚本在空 MySQL 8.4 库执行。\n\n## 各库规模\n| 数据库 | 基础表 | 视图 | 行数（含共享） |\n|---|---:|---:|---:|\n'''+''.join(f'| {g} | {x["tables"]} | {x["views"]} | {x["rows"]} |\n' for g,x in counts.items())+'''\n## 数据质量\n正式表维持主外键、非负库存、借贷平衡及合理日期；原始导入表独立保存重复参考号、日期和金额格式问题、空字符串与NULL。正式业务异常包括部分退款、拆分付款、取消订单、部分交货、拒收、超付款、重复发票业务编号、银行差异、员工调动。生成器采用固定种子20261117。\n\n## 分析任务\n第19章30项，第20章20项，完整业务口径及结果列见 business_tasks。六个派生库各自执行对应5项，综合库执行全部50项用于统一对照。原300题总量不变。SQLite 已真实运行，MySQL 原生待验证。\n'''
write(DOC/'DATABASE.md',newdb)
cur=(base/'CURRICULUM.md').read_text();cur+='\n\n# 版本2.0 修订（覆盖旧第19、20章任务安排）\n\n第19章仍30题，六业务每类5题；第20章仍20题，跨域分布4/3/3/3/3/2/2。原34个实战知识点保留为小型先修（ID不变），新增50个实际企业案例知识点，总152个知识点。下列任务的正式细节见 business_tasks，旧指标练习不再占用第19、20章的题额。\n\n'+''.join(f'- {t["id"]} {t["title"]}（{t["difficulty"]}，{t["knowledgePointIds"][0]}）\n' for t in tasks)
write(DOC/'CURRICULUM.md',cur)
qb=(base/'QUESTION_BANK.md').read_text();qb+='\n\n# 版本2.0 覆盖条款\n\n总数300，题型与难度总边际不变。第19章30+第20章20题均归入业务情景60；其余10道普通业务情景分配第1—18章，剩余240为其他五类。实战50题难度为'+str(summary['difficulty'])+'；其他250题必须相应补足全局60入门、90简单、90中等、45困难、15挑战。若实战挑战数低于15，缺额由第1—18章补齐。任务ID BP01—BP50为内容任务，不作正式 q 编号；第19章在 q0251—q0280，第20章 q0281—q0300（前18章合计250）。生成正式题时复用任务口径、知识点、完整公开结果，避免再次生成不同业务任务。\n\n每题公开基准已有真实SQL、结果及运行记录；目前只有针对三个整体校验场景的边界变异测试，不能替代逐题独立隐藏夹具、错误解与等价解测试。不得将这50项标记为正式已验收判题题库。\n'
write(DOC/'QUESTION_BANK.md',qb)
tech=(base/'TECHNICAL.md').read_text();tech+='\n\n# 版本2.0 统一企业数据资源\n\n使用六套派生SQLite与一个综合SQLite（内部数据源，不新增入口）。仅加载当前练习所需库；缓存资源下载前显示实际字节大小；禁止同时在Worker解压全部七库。优先将综合库置于OPFS（支持时），低存储设备按原子安装、版本与哈希策略处理；离线完整就绪必须实际确认所需资源可用。资源较大，应在Android/iOS实机测试冷启动、峰值内存、索引查询、窗口聚合、超时与回收，桌面SQLite运行时间不能代替手机性能。\n\n金额字段为整数马币分，整数金额精确比对；派生比例按题设小数位，显式使用1.0防止SQLite整数除法。CONCAT需要原生或NULL传播兼容函数；CAST AS SIGNED/INTEGER需按方言映射。所有查询在固定截止日运行；MySQL 8.4 原生及浏览器WASM均待实测。教学预览只展示前12行，判题必须读取全量结果。旧小型课程 fixture 与企业库隔离。\n\n资源路径：CONTENT/ 保存课程，databases/ 保存执行数据库，business_tasks/ 保存50条业务任务与参考查询/结果，structured_data/ 为开发审查CSV，不必一并安装到手机。\n'
write(DOC/'TECHNICAL.md',tech)
for n in ('DATABASE','CURRICULUM','TECHNICAL'):shutil.copyfile(DOC/f'{n}.md',C/'reference'/f'{n}.md')
# historical validation clearly separated
write(C/'validation'/'UPGRADE.md','原 validation/REPORT.md、execution.json、coverage.json 和 mysql/ 是版本1.0的102个小型课程知识点验证记录，保留作历史证据。新增50点使用统一企业数据库，验证报告在外层 validation/REPORT.md 和 checks.json；原生查询在 business_tasks/mysql_queries。不要把原报告合计当成2.0全库覆盖。\n')
# comprehensive report
anomalies={ '重复供应商发票编号组':c.execute('SELECT COUNT(*) FROM (SELECT supplier_id,invoice_no FROM v_invoice_balance GROUP BY supplier_id,invoice_no HAVING COUNT(*)>1) x').fetchone()[0], '超额付款发票':c.execute('SELECT COUNT(*) FROM v_invoice_balance WHERE balance_cents<0').fetchone()[0], '取消销售订单':c.execute("SELECT COUNT(*) FROM orders WHERE status='cancelled'").fetchone()[0], '退货记录':meta['returns']['rowCount'], '部分合格收货采购行':c.execute('SELECT COUNT(*) FROM v_po_lines WHERE accepted>0 AND accepted<quantity_ordered').fetchone()[0], '员工岗位调动':c.execute('SELECT COUNT(*) FROM (SELECT employee_id FROM employment_history GROUP BY employee_id HAVING COUNT(*)>1) x').fetchone()[0]}
report='# 统一企业资源验证报告\n\nSQLite '+sqlite3.sqlite_version+'（本地Python），原生 MySQL 8.4 未执行，浏览器WASM及手机实机未运行。\n\n## 结果\n- 7库完整性和外键检查通过。\n- 6个派生库共享表逐表以双向EXCEPT核对，与综合库相同。\n- 50项查询在综合库执行；30项在相应派生库重跑并逐行对照。\n- 10项独立业务一致性检查为零违规。\n- 3项事务隔离边界变异真实执行并回滚。\n- 尚未完成逐题独立隐藏夹具、多解测试、MySQL 原生比较及人工内容审核；不是完整正式判题验收。\n\n## 业务异常实际计数\n'+''.join(f'- {n}：{v}\n' for n,v in anomalies.items())+'\n## 各任务执行结果\n| 编号 | 任务 | 行数 | 本机毫秒 |\n|---|---|---:|---:|\n'+''.join(f'| {t["id"]} | {t["title"]} | {t["expectedRowCount"]} | {t["elapsedMs"]} |\n' for t in tasks)+'\n零结果可能是正确的核对结论，不能人为制造异常。\n'
write(validation/'REPORT.md',report);dump(validation/'anomalies.json',anomalies)
# reproduction scripts incl data generator and packaging code (generator is standalone, export changes files; note rerun in empty location)
shutil.copy('/tmp/build_enterprise.py',R/'validation'/'generate_enterprise.py');shutil.copy('/tmp/tasks_enterprise.py',R/'validation'/'task_definitions.py');shutil.copy('/tmp/export_enterprise.py',R/'validation'/'export_enterprise.py')
# fix path loading in copied export code for portable review
p=R/'validation'/'export_enterprise.py';s=p.read_text().replace("exec((P.Path(__file__).parent/'task_definitions.py').read_text())","exec((P.Path(__file__).parent/'task_definitions.py').read_text())");write(p,s)
write(R/'README.md',f'''# 第十一阶段第二部分 · 企业业务实战升级\n\n使用统一虚构企业数据，6个独立业务库+1综合执行库。实际生成 {counts['enterprise']['rows']} 条核心记录、39个表、6个视图。全部金额为整数马币分。\n\n## 如何交给开发工具\n1. 保留第十阶段原开发包的页面、素材、功能规范。将本包 docs/ 四份规范覆盖旧版同名文件。\n2. 使用 CONTENT/ 替换第十一阶段第一部分课程内容（前18章原样，原知识点ID保留，第19/20新增50项），不要把旧课程与新版重复合并。\n3. 将 databases/ 的 SQLite 文件与 schema.json、business_tasks/、CONTENT/courses 安装为版本2资源，运行时路径按外层相对路径解析；开发时 structured_data、读物、MySQL脚本仅用于验证，不必安装到手机。\n4. 第19章30题、第20章20题直接复用 business_tasks 的任务需求与参考解；尚需第1—18章250题、全300题正式题型结构和逐题隐藏判题验证、200张闪卡。\n5. 必须执行 MySQL 8.4 原生脚本与浏览器WASM实测；勿将SQLite通过等同MySQL。\n\n## 文件\n- docs：修订课程、数据库、技术、题库规范。\n- CONTENT：20章课程（JSON及Markdown），旧课堂夹具与历史验证记录。\n- databases：7库真实SQLite、独立完整MySQL建表插入脚本、字段字典。\n- structured_data：39份CSV、NULL字面\\N。\n- business_tasks：50条需求、参考SQL、全量结果、阅读版、索引。\n- validation：执行、完整性、业务一致性与3项边界测试报告；原始生成脚本为审查参考，含本次工作目录及前阶段依赖路径，非一键通用安装程序。\n\n## 限制\n库存是仓库级，无门店级库存事实；使用标准成本并按FIFO假设估算库龄。费用凭证不是完整总账，人工成本为教学估算，营销归因不是因果评估。记录为虚构，随机参数不能用于现实经营决策。原生MySQL和手机实机性能未验证。\n''')
# root manifest, no hash self
manifest={'schemaVersion':2,'contentVersion':'2.0.0','cutoff':'2025-12-31','period':['2023-01-01','2025-12-31'],'moneyUnit':'MYR cent','seed':20261117,'datasets':counts,'taskSummary':summary,'knowledgePoints':152,'files':[]}
for p in sorted(R.rglob('*')):
 if p.is_file() and p.name!='manifest_root.json':manifest['files'].append({'path':str(p.relative_to(R)),'bytes':p.stat().st_size,'sha256':sha(p)})
dump(R/'manifest_root.json',manifest)
c.close()
zip_path=P.Path('/mnt/data/SQL_第十一阶段_企业业务数据库升级包.zip')
with zipfile.ZipFile(zip_path,'w',zipfile.ZIP_DEFLATED,compresslevel=5) as z:
 for p in sorted(R.rglob('*')):
  if p.is_file():z.write(p,p.relative_to(R.parent))
with zipfile.ZipFile(zip_path) as z:assert z.testzip() is None;print('ZIP',zip_path,zip_path.stat().st_size,len(z.namelist()),flush=True)
print('summary',summary['difficulty'],counts,flush=True)
