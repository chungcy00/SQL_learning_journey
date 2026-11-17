import sqlite3,json,pathlib,collections,hashlib,shutil,re,sys,math
R=pathlib.Path('/mnt/data/sql_flashcards'); CR=pathlib.Path('/mnt/data/enterprise_sql_upgrade/CONTENT/courses'); QR=pathlib.Path('/mnt/data/sql_question_bank'); DEV=pathlib.Path('/mnt/data/sql_mobile_development/docs'); ENT=pathlib.Path('/mnt/data/enterprise_sql_upgrade')
C={f.stem:json.loads(f.read_text()) for f in sorted(CR.glob('ch*.json'))}
K={k['id']:k for c in C.values() for k in c['knowledgePoints']}; Q={q['id']:q for f in sorted((QR/'questions').glob('ch*.json')) for q in json.loads(f.read_text())['questions']}; links=collections.defaultdict(list); primary_links=collections.defaultdict(list)
for q in Q.values():
 for k in q['knowledgePointIds']: links[k].append(q['id'])
 for k in q.get('primaryKnowledgePointIds',[]): primary_links[k].append(q['id'])
for f in (QR/'course_links').glob('ch*.json'):
 for k in json.loads(f.read_text())['knowledgePointUpdates']:
  for q in k['relatedQuestionIds']:
   if q not in links[k['id']]:links[k['id']].append(q)
TYPES={'C':'概念记忆','S':'语法结构','D':'用法辨析','F':'代码填空','P':'结果预测','E':'错误纠正'}
V='1.0.0'; CARDS=[]; FIX={}; REPORT=[]
# 表的数据定义，与企业库隔离的小型闪卡教学夹具。
def table(name, cols, rows, constraints=''):
 return {'name':name,'columns':[{'name':x.split()[0], 'type':x.split(' ',1)[1]} for x in cols.split(',')],'rows':rows,'constraints':constraints}
TAB={}
def reg(name,cols,rows,constraints=''):TAB[name]=table(name,cols,rows,constraints)
reg('items','id INTEGER,name TEXT,category TEXT,price INTEGER,stock INTEGER',[[1,'Tea','A',12,0],[2,'Rice','A',20,8],[3,'Soap','B',12,3],[4,'Milk','B',None,0],[5,'Bag','C',30,None],[6,'Tea_2','A',20,6]])
reg('clients','id INTEGER,name TEXT',[[1,'林'],[2,'陈'],[3,'周'],[4,'王']])
reg('sales','id INTEGER,client_id INTEGER,store_id INTEGER,amount INTEGER,day TEXT,status TEXT',[[1,1,1,40,'2025-01-02','completed'],[2,1,1,60,'2025-01-15','completed'],[3,2,2,30,'2025-02-01','completed'],[4,None,2,0,'2025-02-02','completed'],[5,3,1,100,'2025-03-01','cancelled'],[6,2,1,60,'2025-03-05','completed']])
reg('stores','id INTEGER,name TEXT',[[1,'甲店'],[2,'乙店'],[3,'丙店']])
reg('lines','id INTEGER,order_id INTEGER,quantity INTEGER,amount INTEGER',[[1,1,2,30],[2,1,1,10],[3,2,3,60],[4,3,1,30]])
reg('receipts','id INTEGER,order_id INTEGER,paid INTEGER',[[1,1,15],[2,1,25],[3,2,60]])
reg('monthly','month TEXT,amount INTEGER',[['2025-01',100],['2025-02',0],['2025-03',150],['2025-04',150]])
reg('months','month TEXT',[['2025-01'],['2025-02'],['2025-03'],['2025-04']])
reg('raw_names','id INTEGER,label TEXT,ref TEXT,updated TEXT',[[1,'  tea ','A','2025-01-01'],[2,'TEA','A','2025-02-01'],[3,'Rice','B','2025-01-01'],[4,'','C','2025-01-01'],[5,None,'D','2025-01-01']])
reg('scores','id INTEGER,team TEXT,score INTEGER',[[1,'A',90],[2,'A',90],[3,'A',70],[4,'A',60],[5,'B',80],[6,'B',60]])
reg('events','id INTEGER,day TEXT,value INTEGER',[[1,'2025-01-01',10],[2,'2025-01-01',20],[3,'2025-01-02',30],[4,'2025-01-03',40]])
reg('tree','id INTEGER,parent_id INTEGER,label TEXT',[[1,None,'总部'],[2,1,'东区'],[3,1,'西区'],[4,2,'分店']])
reg('list_a','value INTEGER',[[1],[1],[2],[None]])
reg('list_b','value INTEGER',[[1],[3],[None]])
reg('stock_log','id INTEGER,qty INTEGER',[[1,8],[2,0],[3,4]])
reg('date_demo','id INTEGER,day TEXT,ts TEXT',[[1,'2025-01-31','2025-01-31 23:59:59'],[2,'2025-02-01','2025-02-01 00:00:00'],[3,'2025-02-28','2025-02-28 23:59:59'],[4,'2025-03-01','2025-03-01 00:00:00']])
reg('lookup','id INTEGER,value TEXT',[[1,'甲'],[2,'乙'],[3,'甲']])
reg('amounts','id INTEGER,x INTEGER,y INTEGER',[[1,7,2],[2,-7,2],[3,0,0],[4,8,4]])
reg('dim','id INTEGER,label TEXT',[[1,'红'],[2,'蓝']])
reg('dept','id INTEGER,label TEXT',[[1,'销售'],[2,'物流']])
reg('employee','id INTEGER,dept_id INTEGER,pay INTEGER',[[1,1,100],[2,1,150],[3,2,80]])
reg('empty_values','id INTEGER,value INTEGER',[])

def J(p,obj):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def spec(k):
 a=k.split('+');return [x if x.startswith('kp') else 'kp'+x for x in a]
def related_questions(ids):
 out=[]
 for k in ids:
  ls=primary_links[k] or links[k]
  for q in ls[:2]:
   if q not in out:out.append(q)
 return out

def base(t,k,prompt,answer,explanation,compat=None,related='',difficulty=None):
 ids=spec(k); rel=spec(related) if related else []; allids=list(dict.fromkeys(ids+rel)); assert all(x in K for x in allids),allids
 ch=ids[0][2:4];cid=f'fc{len(CARDS)+1:04d}'; n=int(ch)
 level=difficulty or ('入门' if n<=2 else '简单' if n<=5 else '中等' if n<=14 else '困难' if n<=18 else '困难')
 qq=related_questions(allids); assert qq,(k,prompt)
 d={'id':cid,'chapterId':'ch'+ch,'knowledgePointIds':allids,'primaryKnowledgePointIds':ids,'relatedKnowledgePointIds':rel,'relatedQuestionIds':qq,'questionLinkPolicy':'来自正式题库知识点关系；关联不是题干重复或专属考题。','type':TYPES[t],'difficulty':level,'front':{'question':prompt},'back':{'answer':answer,'explanation':explanation},'tags':list(dict.fromkeys([K[ids[0]]['title'],TYPES[t]])),'supportStatus':'仅理论','compatibility':compat or 'MySQL 8.4 教学口径；若涉及本地执行，须按兼容层验证。','contentVersion':V,'schemaVersion':1,'dialect':'MySQL 8.4','validation':{'kind':'conceptual','status':'人工编写答案；未执行SQL','mysqlNative':'未执行'}}
 if n>=19:
  group=next((int(i[-2:]) for i in ids if int(i[2:4])==19 and int(i[-2:])>=31),None)
  if group: bp=group-30
  elif n==20 and int(ids[0][-2:])>=5:bp=int(ids[0][-2:])+26
  else:bp=None
  if bp:
   d['businessTaskId']=f'BP{bp:02d}';d['databaseRef']=['retail','inventory','ecommerce','finance','hr','procurement'][(bp-1)//5] if bp<=30 else 'enterprise';d['businessQuestionId']=f'q{250+bp:04d}'
   if d['businessQuestionId'] not in qq: qq.insert(0,d['businessQuestionId'])
 CARDS.append(d);return d

def text(t,k,p,a,e,compat=None,related='',difficulty=None):return base(t,k,p,a,e,compat,related,difficulty)
def Cc(k,p,a,e,**kw):return text('C',k,p,a,e,**kw)
def Dd(k,p,a,e,**kw):return text('D',k,p,a,e,**kw)

def connect(tables):
 c=sqlite3.connect(':memory:');c.execute('PRAGMA foreign_keys=ON');
 # MySQL兼容示例函数，仅覆盖显式标记卡片所需的简单语义。
 c.create_function('CONCAT',-1,lambda *a: None if None in a else ''.join(map(str,a)))
 c.create_function('CHAR_LENGTH',1,lambda x:None if x is None else len(str(x)))
 c.create_function('MYSQL_LENGTH',1,lambda x:None if x is None else len(str(x).encode('utf-8')))
 c.create_function('MYSQL_IF',3,lambda x,y,z:y if x not in (0,None,False) else z)
 c.create_function('MYSQL_DIV',2,lambda a,b:None if a is None or b in (None,0) else math.trunc(a/b))
 from datetime import date,datetime
 c.create_function('DATEDIFF',2,lambda a,b:None if a is None or b is None else (date.fromisoformat(str(a)[:10])-date.fromisoformat(str(b)[:10])).days)
 for t in tables:
  names=[x['name'] for x in t['columns']]; defs=', '.join('"'+x['name']+'" '+x['type'] for x in t['columns'])
  c.execute(f'CREATE TABLE "{t["name"]}" ({defs}{", "+t["constraints"] if t.get("constraints") else ""})')
  c.executemany(f'INSERT INTO "{t["name"]}" VALUES ({",".join("?" for _ in names)})',t['rows'])
 c.commit();return c

def run(tables,sql,check=None):
 c=connect(tables)
 try:
  if check:
   c.executescript(sql);cur=c.execute(check)
  else:cur=c.execute(sql)
  return {'kind':'result','columns':[x[0] for x in cur.description] if cur.description else [],'rows':[list(r) for r in cur.fetchall()] if cur.description else []}
 except sqlite3.Error as e:return {'kind':'error','message':str(e)}
 finally:c.close()

def code(t,k,p,ans,sql,tab,expected,e,*,front_sql=None,sqlite_sql=None,wrong_sql=None,wrong_sqlite=None,check=None,compat=None,related='',difficulty=None,blanks=None):
 d=base(t,k,p,ans,e,compat,related,difficulty);cid=d['id'];tabs=[TAB[x] if isinstance(x,str) else x for x in tab]
 if t=='F':
  assert front_sql and front_sql.count('□')==1, (cid,front_sql)
  assert blanks and front_sql.replace('□',blanks[0]).strip().rstrip(';')==sql.strip().rstrip(';'),(cid,front_sql,blanks,sql)
  d['front']['code']=front_sql;d['back']['acceptedBlanks']=blanks
 elif t=='P':d['front']['code']=sql
 elif t=='E':assert wrong_sql;d['front']['code']=wrong_sql
 elif front_sql:d['front']['code']=front_sql
 d['front']['sourceTables']=tabs;d['front']['dataNote']='独立微型教学数据，不是完整企业数据库查询结果。' if int(d['chapterId'][2:])>=19 else '本卡独立教学数据；只以列出的记录为准。'
 if check:d['front']['verificationQuery']=check
 d['back']['code']=sql;d['back']['sqliteCode']=sqlite_sql or sql
 if check:d['back']['verificationQuery']=check
 actual=run(tabs,sqlite_sql or sql,check);assert actual['kind']=='result',(cid,p,actual,sql)
 assert actual['rows']==expected,(cid,p,actual['rows'],expected)
 d['back']['result']=actual
 if t=='P':d['back']['answer']='返回以下结果。'+('无返回行。' if not expected else '')
 d['supportStatus']='转换执行' if sqlite_sql and sqlite_sql!=sql else '直接兼容'
 d['compatibility']=compat or ('本示例使用SQLite对应写法或注册兼容函数；MySQL 8.4原生尚未执行。' if d['supportStatus']=='转换执行' else '本示例在SQLite实际通过；MySQL 8.4原生尚未执行，类型、排序、严格模式仍需复验。')
 wrong=None
 if t=='E':
  wrong=run(tabs,wrong_sqlite or wrong_sql,check);assert wrong['kind']=='error' or wrong['rows']!=actual['rows'],(cid,p,wrong,actual);d['back']['wrongResult']=wrong
  d['back']['correctionReason']=e
 fixture={'id':cid+'-demo','tables':tabs,'dialect':'SQLite','sqliteSql':sqlite_sql or sql,'mysqlSql':sql,'verificationQuery':check,'expectedRows':expected,'expectedColumns':actual['columns'],'wrongSqlite':wrong_sqlite or wrong_sql,'wrongMysql':wrong_sql,'wrongResult':wrong,'independentAuthorExpectation':True}
 fp=R/'fixtures'/f'{cid}.json'; J(fp,fixture);d['fixtureIds']=[fixture['id']];d['fixturePath']=str(fp.relative_to(R));d['validation']={'kind':'query_or_state','status':'SQLite执行结果与预先编写的结果断言一致','sqliteVersion':sqlite3.sqlite_version,'fixtureSha256':sha(fp),'mysqlNative':'未执行','wrongRejected':True if t=='E' else None,'orderPolicy':'按展示的查询顺序；无ORDER BY且多行时仅用于说明当前结果，不承诺顺序。'}
 REPORT.append({'id':cid,'type':TYPES[t],'fixturePath':d['fixturePath'],'sha256':sha(fp),'referencePassed':True,'wrongRejected':True if t=='E' else None,'rows':len(expected)})
 return d

def S(k,p,a,sql,tab,x,e,**kw):return code('S',k,p,a,sql,tab,x,e,**kw)
def F(k,p,template,blank,tab,x,e,**kw):return code('F',k,p,blank,template.replace('□',blank),tab,x,e,front_sql=template,blanks=[blank],**kw)
def P(k,p,sql,tab,x,e,**kw):return code('P',k,p,'',sql,tab,x,e,**kw)
def E(k,p,wrong,sql,tab,x,e,**kw):return code('E',k,p,'改用背面的修正语句。',sql,tab,x,e,wrong_sql=wrong,**kw)
# ---------- 第01章 ----------
Cc('01-01','读报表之前，为什么必须先确认一行数据代表什么？','因为统计粒度决定可做的计算。','订单行数、订单数与客户数可能完全不同；连接后也可能改变粒度。先写出“一行=什么”，再选分组和计数键。')
Cc('01-02','金额精确到两位小数时，为什么常用 DECIMAL，而不是 FLOAT？','DECIMAL 是精确定点型；FLOAT 是近似浮点型。','财务汇总不应依赖二进制浮点的偶然舍入。企业库另用整数分；SQLite 的 DECIMAL 声明并不保证与 MySQL 相同的精确十进制运算。',compat='MySQL 8.4 使用定点 DECIMAL；SQLite 采用动态类型与数值亲和性，金额建议整数分。')
S('01-03','写出创建子表时声明外键的基本结构。示例：客户1存在，建订单表并为订单10关联客户1，最后核对关联字段。','FOREIGN KEY (子表字段) REFERENCES 父表(主键字段)','CREATE TABLE parent_card (id INT NOT NULL PRIMARY KEY); CREATE TABLE child_card (id INT NOT NULL PRIMARY KEY, client_id INT, FOREIGN KEY (client_id) REFERENCES parent_card(id)); INSERT INTO parent_card VALUES (1); INSERT INTO child_card VALUES (10,1);',[],[[10,1]],'外键约束限定非空引用必须匹配父记录，不能保证子表的客户编号唯一。',check='SELECT id,client_id FROM child_card ORDER BY id',compat='本例已启用 SQLite 外键；MySQL 需要支持外键的存储引擎，原生未执行。')
Dd('01-03','订单表里的 order_id 与 customer_id 都是整数，为什么前者适合作主键而后者通常不适合？','order_id 标识订单；customer_id 通常是引用客户的外键。','一位客户可有多笔订单；将外键加唯一约束会改变一对多业务关系。')
Dd('01-04','有一行订单但客户编号为 NULL，与根本没有这笔订单，统计时有什么区别？','前者是已存在且客户未知的记录；后者是不存在的记录。','前者会计入 COUNT(*)，但不会计入 COUNT(customer_id)。无法凭一个 NULL 断定记录不存在。')
F('01-01','从商品表取编号及价格，填入数据来源关键字。','SELECT id,price □ items ORDER BY id','FROM',['items'],[[1,12],[2,20],[3,12],[4,None],[5,30],[6,20]],'FROM 决定行来自哪张表，SELECT 决定展示哪些字段。')
P('01-01+01-04','同一数据表中，记录总数与已知价格数分别是多少？','SELECT COUNT(*) AS rows_n, COUNT(price) AS priced_n FROM items',['items'],[[6,5]],'六行商品中有一行价格未填写。计数对象不同会改变统计口径。')
# ---------- 第02章 ----------
Cc('02-01','查询只需要两列时，为何尽量不要在正式分析中写 SELECT *？','明确列名可稳定输出契约，并避免读取无关或敏感列。','表新增列时 SELECT * 的结果会变化；连接时也可能出现同名列。')
S('02-01+02-03','写出投影字段及派生金额的别名结构。对商品展示编号及价格的两倍。','SELECT 字段, 表达式 AS 别名 FROM 表','SELECT id,price*2 AS double_price FROM items ORDER BY id',['items'],[[1,24],[2,40],[3,24],[4,None],[5,60],[6,40]],'别名改变结果列的名称，不修改原表，也不将未知数变成零。')
Dd('02-02','SELECT DISTINCT category 与 SELECT DISTINCT category,price 的去重粒度相同吗？','不同。前者按类别；后者按类别与价格的整行组合。','同一类别下不同价格仍是不同组合；不能把多列 DISTINCT 理解为只去重第一列。')
F('02-03','将结果中的商品名称显示为 item_name，填入标准别名关键字。','SELECT name □ item_name FROM items ORDER BY id','AS',['items'],[['Tea'],['Rice'],['Soap'],['Milk'],['Bag'],['Tea_2']],'AS 仅为本次结果列命名，名称不是新建的物理字段。')
P('02-02','预测不同类别的列表，保留查询的排序。','SELECT DISTINCT category FROM items ORDER BY category',['items'],[['A'],['B'],['C']],'重复类别被消除；与 COUNT(DISTINCT category) 返回单个数字不同。')
E('02-03','要求显示商品每件加价3后的价格，不改动表。现有代码将 price 改名为 new_price，已经完成计算了吗？','SELECT id,price AS new_price FROM items ORDER BY id','SELECT id,price+3 AS new_price FROM items ORDER BY id',['items'],[[1,15],[2,23],[3,15],[4,None],[5,33],[6,23]],'仅起别名不会改变原值；应先计算表达式再命名结果。')
# ---------- 第03章 ----------
LIKE_ITEMS=table('items','id INTEGER,name TEXT,category TEXT,price INTEGER,stock INTEGER',TAB['items']['rows']+[[7,'TeaX2','C',15,2]])
Cc('03-01','WHERE 遇到 TRUE、FALSE、UNKNOWN 三种条件结果时，哪种会留下记录？','只有 TRUE。','涉及 NULL 的普通比较可能得到 UNKNOWN；不能把未知与满足条件等同。')
S('03-01','写出按库存筛选记录的结构。要求找库存大于5的商品编号。','SELECT 列 FROM 表 WHERE 条件','SELECT id FROM items WHERE stock>5 ORDER BY id',['items'],[[2],[6]],'过滤发生在行粒度，未知库存不会命中大于比较。')
S('03-04','回忆 LIKE 中匹配字面下划线的写法；用 ! 作为转义字符，找名称包含下划线的商品。','字段 LIKE 模式 ESCAPE 转义字符','SELECT id FROM items WHERE name LIKE \'%!_%\' ESCAPE \'!\' ORDER BY id',[LIKE_ITEMS],[[6]],'显式 ESCAPE 避免把下划线误认作单字符通配符；不同排序规则的大小写行为不保证一致。',compat='使用显式转义；本数据已在 SQLite 验证，MySQL LIKE 大小写取决于排序规则。')
Dd('03-03','IN (12,20) 与 BETWEEN 12 AND 20 哪个会包含15？','BETWEEN 会；IN 不会。','IN 匹配离散集合；BETWEEN 包含两个端点和内部值。NULL 不会因为位于表达式中就被纳入。')
F('03-02','只返回类别A且库存大于5的商品，填入连接条件的关键字。','SELECT id FROM items WHERE category=\'A\' □ stock>5 ORDER BY id','AND',['items'],[[2],[6]],'两个条件都为真才留下记录。')
F('03-03','筛选价格从12到20（含两端），填入区间关键字。','SELECT id FROM items WHERE price □ 12 AND 20 ORDER BY id','BETWEEN',['items'],[[1],[2],[3],[6]],'区间两个端点都包含；NULL 比较不成立。')
P('03-02','判断括号缺失是否改变筛选结果，预测下面的返回编号。','SELECT id FROM items WHERE category=\'A\' OR category=\'B\' AND stock=0 ORDER BY id',['items'],[[1],[2],[4],[6]],'AND 优先于 OR；等于 A 或（等于 B 且库存为0）。')
P('03-04','名称中恰好匹配 Tea 后再跟任意一个字符及数字2，返回哪些行？','SELECT id,name FROM items WHERE name LIKE \'Tea_2\' ORDER BY id',[LIKE_ITEMS],[[6,'Tea_2'],[7,'TeaX2']],'模式中的 _ 可匹配任意单个字符，不局限于真正的下划线。')
E('03-02','目标：A或B类别且库存为零。修正括号错误。','SELECT id FROM items WHERE category=\'A\' OR category=\'B\' AND stock=0 ORDER BY id','SELECT id FROM items WHERE (category=\'A\' OR category=\'B\') AND stock=0 ORDER BY id',['items'],[[1],[4]],'先把类别条件合成整体，再与库存条件取交集；原写法会把所有A类保留下来。')
E('03-03','目标：保留价格不在12或20这两个离散值中的商品；未知价格排除。纠正连续区间误用。','SELECT id FROM items WHERE price NOT BETWEEN 12 AND 20 ORDER BY id','SELECT id FROM items WHERE price NOT IN (12,20) ORDER BY id',[LIKE_ITEMS],[[5],[7]],'离散排除应针对明确值；区间排除会误删15等内部值。')
# ---------- 第04章 ----------
Cc('04-01','没有 ORDER BY 时，能否把数据库恰好返回的第一行当作最新订单？','不能。','关系表没有承诺默认行序；索引变化、执行计划或数据写入都可能改变结果。')
S('04-02','回忆跳过前两行再返回两行的分页语法；按商品编号升序。','ORDER BY 排序键 LIMIT 每页数量 OFFSET 跳过数量','SELECT id FROM items ORDER BY id LIMIT 2 OFFSET 2',['items'],[[3],[4]],'OFFSET 从零开始计跳过的行数。分页前应确保排序稳定。')
Dd('04-01','ORDER BY price DESC, id ASC 与所有排序键都降序有何区别？','只有价格降序；价格并列时编号升序。','每个排序表达式有自己的方向；显式指定并列规则可固定页面。')
F('04-01','价格相同的商品按编号升序，价格本身由高至低；填入第一排序方向。','SELECT id,price FROM items WHERE price IS NOT NULL ORDER BY price □,id ASC','DESC',['items'],[[5,30],[2,20],[6,20],[1,12],[3,12]],'多列排序先按第一个键分组，再按后续键处理并列。')
P('04-02','按完整顺序取第2至第4行，预测编号。','SELECT id FROM items ORDER BY id DESC LIMIT 3 OFFSET 1',['items'],[[5],[4],[3]],'先降序得到6、5、4、3、2、1；跳过第1行后取三行。')
E('04-02','每页两条，第3页要显示编号5和6。纠正 OFFSET 一页的偏差。','SELECT id FROM items ORDER BY id LIMIT 2 OFFSET 6','SELECT id FROM items ORDER BY id LIMIT 2 OFFSET 4',['items'],[[5],[6]],'从第1页计页码时，偏移量=(页码−1)×每页行数。')
# ---------- 第05章 ----------
Cc('05-01','为什么对 NULL 使用普通等号通常找不到缺失值？','NULL 代表未知；普通等号比较未知值返回 UNKNOWN。','WHERE 只保留真值；应使用 IS NULL 或 IS NOT NULL。')
S('05-02','回忆优先使用首个非空值的通用函数。若价格缺失，用库存量作演示备用值，再缺失用0。','COALESCE(值1, 值2, …, 默认值)','SELECT id,COALESCE(price,stock,0) AS chosen FROM items ORDER BY id',['items'],[[1,12],[2,20],[3,12],[4,0],[5,30],[6,20]],'COALESCE 返回第一个非 NULL 参数；这里只演示函数，不表示库存量能替代价格。')
Dd('05-02','IFNULL(x,y) 与 COALESCE(x,y,z) 有什么区别？','前者两个参数；后者支持多个参数，从左到右选首个非空值。','二者在兼容示例中都可执行，仍需注意 MySQL 类型推导与 SQLite 不同。')
F('05-01','查找未知库存，不把库存为零的商品算作缺失。','SELECT id FROM items WHERE stock □ ORDER BY id','IS NULL',['items'],[[5]],'零是已知值，NULL 是未知值。')
P('05-02','预测 NULLIF 对相同值和不同值的结果。','SELECT NULLIF(5,5) AS a,NULLIF(5,0) AS b,COALESCE(NULL,0,7) AS c',[],[[None,5,0]],'NULLIF 相等返回 NULL；COALESCE 认为0不是缺失值。')
E('05-01','筛选价格未填写的商品。纠正使用等号比较空值的代码。','SELECT id FROM items WHERE price=NULL ORDER BY id','SELECT id FROM items WHERE price IS NULL ORDER BY id',['items'],[[4]],'普通比较传播未知；IS NULL 才检查缺失状态。')
# ---------- 第06章 ----------
Cc('06-01','COUNT(*)、COUNT(字段)、COUNT(DISTINCT 字段) 分别在数什么？','行数、非空字段的行数、非空不同字段值的个数。','先确定业务分母。例如订单数、已登记客户订单数和独立客户数不是同一指标。')
S('06-02','回忆汇总和平均的基本写法，展示价格总和与平均值。','SUM(数值字段), AVG(数值字段)','SELECT SUM(price) AS total,AVG(price) AS avg_price FROM items',['items'],[[94,18.8]],'NULL 价格不参与求和，也不计入 AVG 分母。')
S('06-03','用一条查询取得已知价格的上下界。回忆相应聚合函数。','MIN(字段), MAX(字段)','SELECT MIN(price) AS lo,MAX(price) AS hi FROM items',['items'],[[12,30]],'聚合最小和最大值，不等于返回拥有极值的整行商品。')
Dd('06-02','AVG(price) 和 SUM(price)/COUNT(*) 什么时候不同？','有 NULL 价格时，AVG 排除 NULL，而后者把全部记录数作为分母。','若业务确实要把缺失当作零，应明确缺失填零规则；不要无意改变分母。',compat='MySQL / 对整数可能返回小数；SQLite 整数除法可能截断，须显式转为实数。')
F('06-01','有效订单的不同已知客户数：填入去重计数的参数。','SELECT COUNT(□) AS n FROM sales WHERE status=\'completed\'','DISTINCT client_id',['sales'],[[2]],'客户1与2均有重复订单；匿名订单的 NULL 客户编号不会计数。')
P('06-02','在空表上执行这三个聚合函数，预测返回行。','SELECT COUNT(*) AS n,SUM(value) AS s,AVG(value) AS a FROM empty_values',['empty_values'],[[0,None,None]],'无 GROUP BY 的聚合在空输入仍返回一行：计数0，和及均值未知。')
P('06-01','保留已知库存中的零，预测两个计数值。','SELECT COUNT(stock) AS filled,COUNT(DISTINCT stock) AS unique_n FROM items',['items'],[[5,4]],'库存值0、8、3、0、6均非空，去重后为0、8、3、6。')
E('06-02','公司只想计算已填价格商品的平均价格。修正错误分母。','SELECT SUM(price)*1.0/COUNT(*) AS mean FROM items','SELECT AVG(price) AS mean FROM items',['items'],[[18.8]],'使用总行数会把缺失价格隐式当成零；正确分母是非空价格的数量。')
# ---------- 第07章 ----------
Cc('07-01','分组前应该先写下哪一条业务约定？','结果每行代表的分组粒度。','例如每门店每月一行，必须同时按门店和月份分组。')
S('07-01','回忆按类别计算商品数量的基本分组结构。','SELECT 分组键, 聚合函数 FROM 表 GROUP BY 分组键','SELECT category,COUNT(*) AS n FROM items GROUP BY category ORDER BY category',['items'],[['A',3],['B',2],['C',1]],'类别决定分组，每组内部计数。')
S('07-02','回忆只保留商品数不少于2的类别的语法。','GROUP BY 分组键 HAVING 聚合条件','SELECT category,COUNT(*) AS n FROM items GROUP BY category HAVING COUNT(*)>=2 ORDER BY category',['items'],[['A',3],['B',2]],'HAVING 对聚合后的组判断，不对原始行判断。')
Dd('07-02','筛有效订单和筛销售额大于100的门店，应分别放在哪里？','前者放 WHERE，后者放 HAVING。','先排除无效交易，再检查汇总值；不能交换两个筛选阶段。')
F('07-03','按门店及订单状态统计金额；补齐第二个分组键。','SELECT store_id,status,SUM(amount) AS s FROM sales GROUP BY store_id,□ ORDER BY store_id,status','status',['sales'],[[1,'cancelled',100],[1,'completed',160],[2,'completed',30]],'组合分组键保证同一门店不同状态不混加。')
P('07-01','预测先筛价格已知再分组后的计数。','SELECT category,COUNT(*) AS n FROM items WHERE price IS NOT NULL GROUP BY category ORDER BY category',['items'],[['A',3],['B',1],['C',1]],'WHERE 先移除未知价格行，再统计各组。')
E('07-03','目标：每个类别返回总价格，不展示任意一件商品名。修正非聚合列。','SELECT category,name,SUM(price) AS s FROM items GROUP BY category ORDER BY category','SELECT category,SUM(price) AS s FROM items GROUP BY category ORDER BY category',['items'],[['A',52],['B',12],['C',30]],'name 未由类别唯一决定；SQLite可能任意选值，MySQL默认严格分组模式会拒绝。',compat='SQLite 宽松分组不等于 MySQL ONLY_FULL_GROUP_BY；修正语句已执行。')
# ---------- 第08章 ----------
Cc('08-01','连接两张表前，为什么要检查连接键是否唯一？','连接输出行数取决于键的匹配基数。','两边都重复的键可能产生多对多组合，导致金额被放大。')
Cc('08-07','为什么为了展示所有门店所有月份，需要先构造完整组合？','事实表缺行时，直接分组不会出现该组合。','门店与月份的笛卡尔积作为骨架，再左连接聚合事实。')
S('08-01','回忆仅保留匹配客户及订单的连接结构。','FROM 左表 JOIN 右表 ON 左键=右键','SELECT s.id,c.name FROM sales s JOIN clients c ON s.client_id=c.id ORDER BY s.id',['sales','clients'],[[1,'林'],[2,'林'],[3,'陈'],[5,'周'],[6,'陈']],'匿名订单未匹配客户，内连接将它排除。')
S('08-02','回忆保留所有客户并匹配订单的左连接结构。','FROM 保留表 LEFT JOIN 匹配表 ON 连接条件','SELECT c.id,s.id AS sale_id FROM clients c LEFT JOIN sales s ON c.id=s.client_id ORDER BY c.id,s.id',['clients','sales'],[[1,1],[1,2],[2,3],[2,6],[3,5],[4,None]],'客户4没有订单仍保留一行，其右侧字段为NULL。')
Dd('08-03','LEFT JOIN 的右表条件放 ON 与放 WHERE，为什么结果可能不同？','ON 限定匹配；WHERE 在连接后过滤。','右表未匹配时生成NULL，WHERE右表普通条件会排除这些左表记录。')
Dd('08-05','订单有两条明细及两次支付，为什么不能直接连接两张明细表后求和？','每条明细与每笔支付交叉组合，金额会重复。','分别聚合到订单粒度，再连接两份汇总。')
F('08-06','保留右侧全部门店，包括没有订单的门店，填入连接方向。','SELECT s.id AS sale_id,t.id AS store_id FROM sales s □ stores t ON s.store_id=t.id ORDER BY t.id,s.id','RIGHT JOIN',['sales','stores'],[[1,1],[2,1],[5,1],[6,1],[3,2],[4,2],[None,3]],'右连接保留右表；也可交换两表使用左连接。',sqlite_sql='SELECT s.id AS sale_id,t.id AS store_id FROM stores t LEFT JOIN sales s ON s.store_id=t.id ORDER BY t.id,s.id',compat='MySQL 原生 RIGHT JOIN；本地以交换左右表的 LEFT JOIN 验证。')
F('08-04+08-07','生成所有门店与两种颜色组合，填入无需连接条件的连接类型。','SELECT s.id,d.id AS color_id FROM stores s □ dim d ORDER BY s.id,d.id','CROSS JOIN',['stores','dim'],[[1,1],[1,2],[2,1],[2,2],[3,1],[3,2]],'3×2得到6个组合，可用于生成完整维度。')
P('08-04','自连接中，只输出同类商品编号的无序配对（每对仅一次）。预测返回。','SELECT a.id,b.id AS other_id FROM items a JOIN items b ON a.category=b.category AND a.id<b.id ORDER BY a.id,b.id',['items'],[[1,2],[1,6],[2,6],[3,4]],'id小于另一编号排除自己和反向重复配对。')
P('08-05','先将订单明细汇总，再与支付汇总匹配。预测订单1的明细额及实付额。','WITH l AS (SELECT order_id,SUM(amount) AS s FROM lines GROUP BY order_id),p AS (SELECT order_id,SUM(paid) AS p FROM receipts GROUP BY order_id) SELECT l.order_id,l.s,p.p FROM l JOIN p ON l.order_id=p.order_id WHERE l.order_id=1',['lines','receipts'],[[1,40,40]],'两张一对多表各自聚合，避免明细与支付重复。')
E('08-03','要求包含未完成任何有效订单的客户，显示有效订单数量。修正把有效状态放在错误位置的查询。','SELECT c.id,COUNT(s.id) AS n FROM clients c LEFT JOIN sales s ON c.id=s.client_id WHERE s.status=\'completed\' GROUP BY c.id ORDER BY c.id','SELECT c.id,COUNT(s.id) AS n FROM clients c LEFT JOIN sales s ON c.id=s.client_id AND s.status=\'completed\' GROUP BY c.id ORDER BY c.id',['clients','sales'],[[1,2],[2,2],[3,0],[4,0]],'右表筛选放ON保留未匹配客户，再用COUNT(s.id)而不是COUNT(*)。')
# ---------- 第09章 ----------
Cc('09-01','CASE WHEN 有多个条件同时成立时，选哪个分支？','从上到下第一个为真的 WHEN 分支。','阈值顺序很重要；没有匹配且省略 ELSE 时返回NULL。')
S('09-01','回忆搜索式 CASE 语法。用商品库存将记录分为零、未知、其他。','CASE WHEN 条件 THEN 值 … ELSE 默认值 END','SELECT id,CASE WHEN stock=0 THEN \'零\' WHEN stock IS NULL THEN \'未知\' ELSE \'其他\' END AS label FROM items ORDER BY id',['items'],[[1,'零'],[2,'其他'],[3,'其他'],[4,'零'],[5,'未知'],[6,'其他']],'条件顺序与缺失分支都应明确。')
S('09-04','回忆 MySQL IF 二选一。将订单状态映射为1或0，统计编号5。','IF(条件, 为真结果, 否则结果)','SELECT IF(status=\'completed\',1,0) AS valid FROM sales WHERE id=5',['sales'],[[0]],'MySQL IF 的未知条件按非真分支处理；离线用CASE等价。',sqlite_sql="SELECT CASE WHEN status='completed' THEN 1 ELSE 0 END AS valid FROM sales WHERE id=5")
Dd('09-02','SUM(CASE WHEN 条件 THEN amount ELSE 0 END) 与 COUNT(CASE WHEN 条件 THEN 1 END) 的指标有何不同？','前者统计满足条件的金额，后者统计满足条件的行数。','不能把金额与次数相互替代；COUNT忽略NULL。')
F('09-02','按全表统计有效订单金额，填入不满足条件时的数值。','SELECT SUM(CASE WHEN status=\'completed\' THEN amount ELSE □ END) AS total FROM sales','0',['sales'],[[190]],'条件聚合不必删除其他行，未命中分支贡献零。')
F('09-03','除数为零时返回NULL，填入保护函数。','SELECT id,1.0*x/□(y,0) AS ratio FROM amounts ORDER BY id','NULLIF',['amounts'],[[1,3.5],[2,-3.5],[3,None],[4,2.0]],'NULLIF 把零分母变成NULL，避免依赖除零错误行为。')
P('09-01','预测区间条件重叠时的标签。','SELECT CASE WHEN 85>=60 THEN \'及格\' WHEN 85>=80 THEN \'良好\' ELSE \'未及格\' END AS label',[],[['及格']],'第一条已为真，后续分支不会再决定输出。')
E('09-01','目标：80分以上为良好，60至79分为及格。修正分支优先顺序。','SELECT id,CASE WHEN score>=60 THEN \'及格\' WHEN score>=80 THEN \'良好\' ELSE \'未及格\' END AS label FROM scores ORDER BY id','SELECT id,CASE WHEN score>=80 THEN \'良好\' WHEN score>=60 THEN \'及格\' ELSE \'未及格\' END AS label FROM scores ORDER BY id',['scores'],[[1,'良好'],[2,'良好'],[3,'及格'],[4,'及格'],[5,'良好'],[6,'及格']],'上限较高的条件必须先判断，否则良好分支被及格分支截获。')
# ---------- 第10章 ----------
Cc('10-01','标量子查询对返回列数和行数有什么要求？','必须一列，至多一行；零行当作NULL。','MySQL 8.4 多行标量子查询报错；SQLite可能取第一行，不能依赖该差异。',compat='MySQL 多行标量子查询错误；本地必须额外检查。')
S('10-02','回忆按子查询结果集合筛选商品的语法：与类别A任一已知价格相同。','字段 IN (SELECT 单列 FROM 表 WHERE 条件)','SELECT id FROM items WHERE price IN (SELECT price FROM items WHERE category=\'A\') ORDER BY id',['items'],[[1],[2],[3],[6]],'IN 判断是否属于子查询值集合，不复制外表记录。')
S('10-03','回忆相关存在性查询的结构：查有任意订单的客户。','WHERE EXISTS (SELECT 1 FROM 内表 WHERE 内键=外键)','SELECT c.id FROM clients c WHERE EXISTS (SELECT 1 FROM sales s WHERE s.client_id=c.id) ORDER BY c.id',['clients','sales'],[[1],[2],[3]],'每位客户判断至少一笔匹配订单是否存在。')
Dd('10-02+10-03','IN 与 EXISTS 用于检查是否存在匹配记录时，关注对象有何不同？','IN 比较一个值与集合；EXISTS 判断子查询是否有行。','EXISTS 的投影常用1，投影内容不决定存在性；注意NULL语义。')
Dd('10-04','为什么 NOT IN 的子查询包含 NULL 时可能没有任何结果？','未匹配值仍需与未知值比较，结果可能是UNKNOWN。','NOT EXISTS 使用相关匹配条件，通常适合表达没有匹配记录。')
F('10-01','找价格高于全表平均已知价格的商品，填入平均值函数。','SELECT id FROM items WHERE price>(SELECT □(price) FROM items) ORDER BY id','AVG',['items'],[[2],[5],[6]],'子查询无分组，返回一个均值18.8；外层逐行比较。')
F('10-04','查没有有效订单的门店，填入否定存在谓词。','SELECT t.id FROM stores t WHERE □ (SELECT 1 FROM sales s WHERE s.store_id=t.id AND s.status=\'completed\') ORDER BY t.id','NOT EXISTS',['stores','sales'],[[3]],'不存在匹配有效订单时保留左表门店；不能用右表行数推断全部门店。')
P('10-03','预测是否存在大于当前商品价格的其他商品。','SELECT a.id FROM items a WHERE a.price IS NOT NULL AND NOT EXISTS (SELECT 1 FROM items b WHERE b.price>a.price) ORDER BY a.id',['items'],[[5]],'对每个已知价格检查有没有更高值。此处30为最大值。')
E('10-04','找没有订单的客户。修正含未知客户编号的 NOT IN 写法。','SELECT id FROM clients WHERE id NOT IN (SELECT client_id FROM sales) ORDER BY id','SELECT c.id FROM clients c WHERE NOT EXISTS (SELECT 1 FROM sales s WHERE s.client_id=c.id) ORDER BY c.id',['clients','sales'],[[4]],'子查询有NULL时 NOT IN 可能传播未知；相关 NOT EXISTS 检查实际匹配。')
# ---------- 第11章 ----------
Cc('11-04','一个汉字在 UTF-8 下的字符数与字节数通常相同吗？','不相同。常见汉字一个字符占3字节。','MySQL CHAR_LENGTH 计字符，LENGTH 计字节；还需以实际编码为准。',compat='SQLite LENGTH(文本)按字符计；字节数可对BLOB求长度。')
S('11-01','回忆多字段字符串拼接结构：把编号与商品名以冒号连接，仅展示编号3。','CONCAT(值1, 分隔符, 值2, …)','SELECT CONCAT(id,\':\',name) AS label FROM items WHERE id=3',['items'],[['3:Soap']],'MySQL CONCAT 任一参数为NULL则返回NULL。',sqlite_sql="SELECT CAST(id AS TEXT)||':'||name AS label FROM items WHERE id=3")
S('11-02','回忆从第2个字符起截3个字符的写法，用商品Rice。','SUBSTRING(字符串, 起始位置, 长度)','SELECT SUBSTRING(name,2,3) AS part FROM items WHERE id=2',['items'],[['ice']],'正起始位置从1计；超出长度时只返回可用字符。',sqlite_sql='SELECT substr(name,2,3) AS part FROM items WHERE id=2')
Dd('11-03','TRIM 和 REPLACE 对文本中的空格分别做什么？','TRIM 清除指定的首尾字符（默认空格）；REPLACE 替换全部匹配子串。','清洗名称时不要默认删除有意义的内部空格。')
F('11-03','将原始名称统一去首尾空格并转小写，填入外层函数。','SELECT id,□(TRIM(label)) AS normalized FROM raw_names ORDER BY id','LOWER',['raw_names'],[[1,'tea'],[2,'tea'],[3,'rice'],[4,''],[5,None]],'先去首尾空格，再统一大小写；空字符串和NULL仍不同。')
P('11-04','用 MySQL utf8mb4 语义预测“茶A”的字符数与字节数。','SELECT CHAR_LENGTH(\'茶A\') AS chars,LENGTH(\'茶A\') AS bytes',[],[[2,4]],'茶占3字节，A占1字节；字符数量是2。',sqlite_sql="SELECT length('茶A') AS chars,length(CAST('茶A' AS BLOB)) AS bytes")
E('11-03','目标：把首尾空白去掉，但保留“红 茶”中间的空格。纠正全局替换。','SELECT REPLACE(\'  红 茶  \',\' \',\'\') AS label','SELECT TRIM(\'  红 茶  \') AS label',[],[['红 茶']],'全局替换会删除业务文本内部有意义的分隔。')
# ---------- 第12章 ----------
Cc('12-05','金额比率在 SQL 中为什么不应依赖整数除法的隐式类型？','不同数据库对整数除法及精度的处理不同。','MySQL / 与 DIV 不同；SQLite 两整数的 / 会向零截断，显式实数转换或精确金额计算更稳妥。')
S('12-01','回忆四舍五入及绝对值函数，把-8.456处理为两位非负小数。','ROUND(数值, 小数位数)，ABS(数值)','SELECT ROUND(ABS(-8.456),2) AS rounded',[],[[8.46]],'先取绝对值，再将结果按两位小数舍入；金额中途不要无必要重复舍入。')
S('12-02','回忆 MySQL 提取日期年份、月份的函数。读取日期表编号2。','YEAR(日期)，MONTH(日期)','SELECT YEAR(day) AS y,MONTH(day) AS m FROM date_demo WHERE id=2',['date_demo'],[[2025,2]],'提取数字年份和月份，不等于包含年份的月份分组标签。',sqlite_sql="SELECT CAST(strftime('%Y',day) AS INTEGER) AS y,CAST(strftime('%m',day) AS INTEGER) AS m FROM date_demo WHERE id=2")
Dd('12-03','DATEDIFF 与 TIMESTAMPDIFF 在 MySQL 的参数顺序有何区别？','DATEDIFF(结束,开始) 按日期差；TIMESTAMPDIFF(单位,开始,结束) 按指定单位。','DATEDIFF 忽略时刻；TIMESTAMPDIFF 计算完整指定单位。',compat='MySQL专用日期函数；SQLite需按任务口径改写。')
F('12-04','筛选2025年1月，采用左闭右开时间区间，填入结束边界。','SELECT id FROM date_demo WHERE ts>=\'2025-01-01\' AND ts<□ ORDER BY id',"'2025-02-01'",['date_demo'],[[1]],'结束边界使用下月第一天，保留最后一天所有时刻。')
F('12-02','按年月标签展示日期，填入 MySQL DATE_FORMAT 的格式字符串。','SELECT id,DATE_FORMAT(day,□) AS ym FROM date_demo ORDER BY id',"'%Y-%m'",['date_demo'],[[1,'2025-01'],[2,'2025-02'],[3,'2025-02'],[4,'2025-03']],'%Y四位年，%m两位月，跨年时标签不同。',sqlite_sql="SELECT id,strftime('%Y-%m',day) AS ym FROM date_demo ORDER BY id")
F('12-03','计算2025-03-01距2025-02-27的日期差，填入 MySQL 函数。','SELECT □(\'2025-03-01\',\'2025-02-27\') AS days','DATEDIFF',[],[[2]],'结束日期减开始日期，不加1。',sqlite_sql="SELECT CAST(julianday('2025-03-01')-julianday('2025-02-27') AS INTEGER) AS days")
P('12-05','预测 MySQL 整数除法与普通除法的数值（忽略小数显示尾零）。','SELECT 7 DIV 2 AS whole,7/2 AS ratio',[],[[3,3.5]],'DIV 截断至整数，/ 保留小数；SQLite需要显式转换。',sqlite_sql='SELECT 7/2 AS whole,7.0/2 AS ratio')
P('12-01','预测绝对值、舍入及取余结果。','SELECT ABS(-9) AS a,ROUND(3.146,2) AS r,MOD(17,5) AS m',[],[[9,3.15,2]],'取余17除5剩2。',sqlite_sql='SELECT ABS(-9) AS a,ROUND(3.146,2) AS r,17%5 AS m')
E('12-04','目标：2025年2月全部时间戳，修正漏掉月末白天记录的查询。','SELECT id FROM date_demo WHERE ts>=\'2025-02-01\' AND ts<=\'2025-02-28\' ORDER BY id','SELECT id FROM date_demo WHERE ts>=\'2025-02-01\' AND ts<\'2025-03-01\' ORDER BY id',['date_demo'],[[2],[3]],'日期字符串截止到2月28日零点附近会漏掉该日时间；下月起点作开区间更安全。')
# ---------- 第13章 ----------
Cc('13-04','集合合并为什么要求左右查询拥有相同列数？','集合运算按列位置对齐，而非按列名对齐。','每列的类型和业务含义应兼容；列名通常来自第一个查询。')
S('13-01','回忆保留两份来源全部重复行的合并结构，并增加来源列以便核查。','SELECT 来源标识,列 FROM 表1 UNION ALL SELECT 来源标识,列 FROM 表2','SELECT \'A\' AS src,value FROM list_a UNION ALL SELECT \'B\' AS src,value FROM list_b ORDER BY src,value',['list_a','list_b'],[['A',None],['A',1],['A',1],['A',2],['B',None],['B',1],['B',3]],'保留重复与来源；两个查询的列位置应一致，排序作用于合并结果。')
Dd('13-01','UNION 与 UNION ALL 对重复行的处理有什么区别？','UNION 去掉整行重复；UNION ALL 保留。','汇总交易流水通常需要保留真实重复金额，而不是按整行消重。')
Dd('13-02','INTERSECT 和 EXCEPT 的方向有何区别？','INTERSECT 取交集，EXCEPT 取左侧有而右侧没有的行。','差集有方向；默认 DISTINCT 集合语义，NULL 在集合去重匹配中按不相异处理。')
F('13-02','返回A和B共有的值，默认消除重复，填入集合运算符。','SELECT value FROM list_a □ SELECT value FROM list_b ORDER BY value','INTERSECT',['list_a','list_b'],[[None],[1]],'交集包含共同NULL和1，各只出现一次。',compat='MySQL 8.4 支持 INTERSECT；SQLite已实测默认去重语义。')
P('13-03','预测从A中排除B中的值，最后按值排序。','SELECT value FROM list_a EXCEPT SELECT value FROM list_b ORDER BY value',['list_a','list_b'],[[2]],'A的1和NULL均在B中；剩2。')
E('13-01','目标：合并两份原始流水且保留重复值。纠正无意去重的写法。','SELECT value FROM list_a UNION SELECT value FROM list_b ORDER BY value','SELECT value FROM list_a UNION ALL SELECT value FROM list_b ORDER BY value',['list_a','list_b'],[[None],[None],[1],[1],[1],[2],[3]],'UNION 会消掉有效的重复流水；只有业务确认重复是错误时才去重。')
# ---------- 第14章 ----------
Cc('14-01','为什么不能认定 CTE 一定提高查询性能？','CTE 是逻辑组织结构，不保证物化或特定执行计划。','优化器可能合并、物化或重复处理，实际成本要通过计划验证。')
S('14-01','回忆命名两个连续 CTE 的写法：先筛有效订单，再统计总额。','WITH 名1 AS (查询), 名2 AS (查询) SELECT … FROM 名2','WITH v AS (SELECT amount FROM sales WHERE status=\'completed\'),t AS (SELECT SUM(amount) AS s FROM v) SELECT s FROM t',['sales'],[[190]],'同一WITH内多个CTE逗号分隔；后者可引用前者。')
S('14-02','回忆递归CTE的两部分及连接方式，从1递增到4。','WITH RECURSIVE 名(列) AS (锚点 UNION ALL 递归项) SELECT …','WITH RECURSIVE n(x) AS (SELECT 1 UNION ALL SELECT x+1 FROM n WHERE x<4) SELECT x FROM n ORDER BY x',[],[[1],[2],[3],[4]],'锚点启动递归；递归条件控制有限终止。')
Dd('14-02','递归CTE与普通CTE在引用自身方面有什么区别？','递归CTE可以在递归项引用自身；普通CTE不能递归引用自身。','递归必须控制终止，图结构还需考虑循环。MySQL 递归深度受系统变量控制。')
F('14-01','从命名查询 v 中读取结果，补齐开启命名查询的关键字。','□ v AS (SELECT id FROM items WHERE stock=0) SELECT id FROM v ORDER BY id','WITH',['items'],[[1],[4]],'CTE 的作用范围是紧随其后的单条语句。')
P('14-02','从总部向下递归并计算层级，预测输出。','WITH RECURSIVE h(id,depth) AS (SELECT id,0 FROM tree WHERE parent_id IS NULL UNION ALL SELECT t.id,h.depth+1 FROM tree t JOIN h ON t.parent_id=h.id) SELECT id,depth FROM h ORDER BY id',['tree'],[[1,0],[2,1],[3,1],[4,2]],'每向下匹配一层深度加1。此样例无循环。')
E('14-02','目标：产生1至3。原递归条件多生成一个值，修正终止条件。','WITH RECURSIVE n(x) AS (SELECT 1 UNION ALL SELECT x+1 FROM n WHERE x<=3) SELECT x FROM n ORDER BY x','WITH RECURSIVE n(x) AS (SELECT 1 UNION ALL SELECT x+1 FROM n WHERE x<3) SELECT x FROM n ORDER BY x',[],[[1],[2],[3]],'条件检查的是当前值，不是下一轮生成值。当前值3时必须停止。')
# ---------- 第15章 ----------
Cc('15-01','窗口函数与 GROUP BY 的根本差别是什么？','窗口计算保留输入行；GROUP BY 聚合后改变行粒度。','例如同时展示每张订单金额与部门总额，可使用窗口聚合而不压缩为部门一行。')
Cc('15-05','移动平均中“过去3行”是否必然等于过去3个月？','不必然。','若月份缺失，一行不等于连续月份。先补齐月份轴，再定义窗口边界及最小样本数。')
S('15-01','回忆按类别计算平均价格并保留每条商品的窗口语法，只展示商品1、2、3前先计算全部类别均值。','聚合函数(字段) OVER (PARTITION BY 分区键)','WITH x AS (SELECT id,AVG(price) OVER(PARTITION BY category) AS avg_p FROM items) SELECT id,ROUND(avg_p,2) AS avg_p FROM x WHERE id<=3 ORDER BY id',['items'],[[1,17.33],[2,17.33],[3,12.0]],'窗口在筛选显示范围之前计算，可避免显示过滤意外改变总体。')
S('15-03','回忆读取同分区前一行值的窗口语法；按连续月份读取前一月金额。','LAG(字段, 偏移量) OVER (ORDER BY 排序键)','SELECT month,amount,LAG(amount,1) OVER(ORDER BY month) AS previous FROM monthly ORDER BY month',['monthly'],[['2025-01',100,None],['2025-02',0,100],['2025-03',150,0],['2025-04',150,150]],'第一行没有前值，默认NULL。')
Dd('15-02','三个分数90、90、70，RANK和DENSE_RANK会怎么处理第三个分数？','RANK为3，DENSE_RANK为2。','RANK 跳过并列占用的名次，DENSE_RANK 不跳号；ROW_NUMBER 强制逐行编号。')
F('15-02','每个队伍严格选择一名最高分得主，按编号打破并列；填入窗口排名函数。','WITH x AS (SELECT id,team,□() OVER(PARTITION BY team ORDER BY score DESC,id) AS rn FROM scores) SELECT id FROM x WHERE rn=1 ORDER BY id','ROW_NUMBER',['scores'],[[1],[5]],'严格一人需要唯一的排序规则；不能仅凭RANK在分数并列时保证一行。')
F('15-04','计算从分区起点到当前行的逐行累计，填入窗口边界。','SELECT id,SUM(value) OVER(ORDER BY day,id ROWS BETWEEN □ AND CURRENT ROW) AS running FROM events ORDER BY id','UNBOUNDED PRECEDING',['events'],[[1,10],[2,30],[3,60],[4,100]],'加入唯一编号打破日期并列，ROWS累计逐行增加。')
P('15-06','日期并列时，预测 RANGE 累计总和。','SELECT id,SUM(value) OVER(ORDER BY day RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS total FROM events ORDER BY id',['events'],[[1,30],[2,30],[3,60],[4,100]],'RANGE 把相同排序值的同行集纳入同一边界，两条1月1日事件累计值相同。')
P('15-05','预测相邻两行移动平均，保留首行不完整窗口。','SELECT month,AVG(amount) OVER(ORDER BY month ROWS BETWEEN 1 PRECEDING AND CURRENT ROW) AS moving FROM monthly ORDER BY month',['monthly'],[['2025-01',100.0],['2025-02',50.0],['2025-03',75.0],['2025-04',150.0]],'首行只有一个样本；若报告必须满两期，应再加窗内计数。')
E('15-01+15-02','目标：各队前三名（含并列），为何不能在同层 WHERE 使用窗口函数？','SELECT id,RANK() OVER(PARTITION BY team ORDER BY score DESC) AS r FROM scores WHERE RANK() OVER(PARTITION BY team ORDER BY score DESC)<=3 ORDER BY id','WITH x AS (SELECT id,RANK() OVER(PARTITION BY team ORDER BY score DESC) AS r FROM scores) SELECT id,r FROM x WHERE r<=3 ORDER BY id',['scores'],[[1,1],[2,1],[3,3],[5,1],[6,2]],'窗口计算在WHERE之后，需用外层查询或CTE过滤名次。')
E('15-03','要求读取各团队内部上一编号的分数，纠正忘记分区的写法。','SELECT id,LAG(score) OVER(ORDER BY id) AS prev FROM scores ORDER BY id','SELECT id,LAG(score) OVER(PARTITION BY team ORDER BY id) AS prev FROM scores ORDER BY id',['scores'],[[1,None],[2,90],[3,90],[4,70],[5,None],[6,80]],'分区缺失让另一团队的最后一行成为新团队的上一行。')
# ---------- 第16章 ----------
Cc('16-01','两条记录拥有相同业务参考号，就一定可以删除其中一条吗？','不一定。','先确认业务键是否应该唯一、记录是否为版本更新、是否存在合法拆分交易；再定义保留规则。')
S('16-01','回忆按参考号检查重复的结构。','GROUP BY 业务键 HAVING COUNT(*)>1','SELECT ref,COUNT(*) AS n FROM raw_names GROUP BY ref HAVING COUNT(*)>1 ORDER BY ref',['raw_names'],[['A',2]],'重复计数是发现候选，不能直接认定应删除。')
Dd('16-02','NULL、空字符串和仅空格的文本应如何区别？','NULL是未知；空字符串是长度为0的已知文本；空格字符串含字符。','若业务规定三者统一缺失，可先TRIM再NULLIF；保留原始字段以便审计。')
F('16-03','先修剪再将空字符串统一为NULL，填入函数。','SELECT id,□(TRIM(label),\'\') AS clean FROM raw_names ORDER BY id','NULLIF',['raw_names'],[[1,'tea'],[2,'TEA'],[3,'Rice'],[4,None],[5,None]],'空字符串经NULLIF变为NULL，非空文本保留。')
F('16-03','把可确定为数字的文本“0042”转换成整数，填入 MySQL 类型名。','SELECT CAST(\'0042\' AS □) AS n','SIGNED',[],[[42]],'CAST改变类型；转换前应检查文本是否合法，编号是否需要保留前导零。',sqlite_sql="SELECT CAST('0042' AS INTEGER) AS n")
P('16-01','每个参考号按更新时间保留最新，编号用来打破并列，预测保留编号。','WITH x AS (SELECT id,ROW_NUMBER() OVER(PARTITION BY ref ORDER BY updated DESC,id DESC) AS rn FROM raw_names) SELECT id FROM x WHERE rn=1 ORDER BY id',['raw_names'],[[2],[3],[4],[5]],'参考号A两行中编号2时间较晚，其余各一行。')
E('16-02','目标：识别负库存或未知库存。修正条件同时要求二者成立的错误。','SELECT id FROM items WHERE stock<0 AND stock IS NULL ORDER BY id','SELECT id FROM items WHERE stock<0 OR stock IS NULL ORDER BY id',['items'],[[5]],'缺失或超出范围是不同异常条件，任一命中即可；负值没有在当前样本出现。')
# ---------- 第17章 ----------
Cc('17-01+17-05','数据库约束为什么不能只靠前端表单校验替代？','约束在数据写入时保护底层完整性。','脚本、导入及多入口写入同样需要唯一性、引用完整性与可空性控制。')
Cc('17-04','事务中的 COMMIT 和 ROLLBACK 分别意味着什么？','COMMIT 确认当前事务；ROLLBACK 撤销未提交的事务变更。','事务并不保证任意DDL可回滚：MySQL很多DDL会隐式提交，SQLite行为不同。')
S('17-01','回忆为非负库存声明主键、非空及检查约束的结构。建立表，再写入编号1库存3，核对数据。','CREATE TABLE 表(编号 INT NOT NULL PRIMARY KEY, 数量 INT NOT NULL CHECK(数量>=0))','CREATE TABLE inventory_card(id INT NOT NULL PRIMARY KEY,qty INT NOT NULL CHECK(qty>=0)); INSERT INTO inventory_card VALUES(1,3);',[],[[1,3]],'主键、非空和取值范围限制分别表达不同规则。',check='SELECT id,qty FROM inventory_card ORDER BY id')
S('17-02','回忆根据主键更新单条库存的语法；将编号2库存加5。','UPDATE 表 SET 字段=表达式 WHERE 条件','UPDATE stock_log SET qty=qty+5 WHERE id=2;',['stock_log'],[[1,8],[2,5],[3,4]],'WHERE限定更新范围，执行后读取状态核对。',check='SELECT id,qty FROM stock_log ORDER BY id')
Dd('17-03','ALTER TABLE 与 DROP TABLE 改变的对象有何区别？','前者修改表结构，后者删除整个表及其数据。','修改结构前备份；MySQL DDL可能隐式提交，不能假定事务足以恢复。')
F('17-02','只删除库存为零的记录，填入删除记录的关键字。','□ FROM stock_log WHERE qty=0;','DELETE',['stock_log'],[[1,8],[3,4]],'DELETE移除满足条件的记录，表结构仍在。',check='SELECT id,qty FROM stock_log ORDER BY id')
F('17-03','给库存表添加默认状态字段，填入标准新增列关键字。','ALTER TABLE stock_log □ status VARCHAR(10) DEFAULT \'active\';','ADD COLUMN',['stock_log'],[[1,'active'],[2,'active'],[3,'active']],'修改结构前确认默认值、历史记录及兼容性。',check='SELECT id,status FROM stock_log ORDER BY id')
P('17-04','事务内先增加库存再撤销，预测最后的核对结果。','START TRANSACTION; UPDATE stock_log SET qty=qty+10 WHERE id=1; ROLLBACK;',['stock_log'],[[1,8]],'未提交的DML撤销，恢复原库存。',sqlite_sql='BEGIN; UPDATE stock_log SET qty=qty+10 WHERE id=1; ROLLBACK;',check='SELECT id,qty FROM stock_log WHERE id=1')
# ---------- 第18章 ----------
Cc('18-01','为什么低选择性字段上的单列索引未必明显加速查询？','若大量行都命中，读取索引再回表可能比顺序扫描更贵。','索引选择依赖数据分布、查询列及优化器成本；不能认为有索引一定更快。')
Cc('18-03','EXPLAIN 显示计划就等于测出了真实运行耗时吗？','不等于。','普通EXPLAIN主要提供优化器估算与路径；MySQL EXPLAIN ANALYZE 会实际运行并报告实际统计。SQLite EXPLAIN QUERY PLAN 不提供相同指标。')
S('18-01','回忆在销售表上创建普通复合索引的语法：先门店，再日期。建立后查询门店1在3月的订单编号。','CREATE INDEX 索引名 ON 表(字段1,字段2)','CREATE INDEX ix_store_day ON sales(store_id,day);',['sales'],[[5],[6]],'索引支持匹配前导列及后续日期范围；这只是结果验证，不证明优化器必然使用该索引。',check="SELECT id FROM sales WHERE store_id=1 AND day>='2025-03-01' AND day<'2025-04-01' ORDER BY id")
Dd('18-02','复合索引 (store_id, day) 与 (day, store_id) 为什么不是同一索引？','前导列不同，适用的条件和排序路径不同。','等值门店+日期范围通常适合前者；只按日期检索未必能高效利用前者。最终以执行计划为准。')
F('18-02','用可搜索范围替代对日期列调用函数，填入次月边界。','SELECT id FROM sales WHERE day>=\'2025-01-01\' AND day<□ ORDER BY id',"'2025-02-01'",['sales'],[[1],[2]],'范围条件通常更容易使用日期索引；本卡验证结果，性能需独立计划测量。')
# ---------- 第19章业务微型夹具 ----------
reg('shop_totals','store_id INTEGER,gross_cents INTEGER,discount_cents INTEGER,refund_cents INTEGER,cost_after_returns_cents INTEGER,target_cents INTEGER',[[1,120000,10000,20000,54000,100000],[2,0,0,0,0,0]])
reg('store_month','month_key TEXT,net_cents INTEGER',[['2025-01',10000],['2025-02',0],['2025-03',20000],['2025-04',15000]])
reg('sku_totals','product_id INTEGER,net_cents INTEGER,profit_cents INTEGER',[[1,9000,1800],[2,9000,3600],[3,8000,2000],[4,7000,3500],[5,6000,1200],[6,5000,2000],[7,4000,1000],[8,3000,1500]])
reg('snap','warehouse_id INTEGER,product_id INTEGER,quantity INTEGER,unit_cost_cents INTEGER',[[1,1,80,10000],[1,2,120,5000],[1,3,100,7000]])
reg('outbound','warehouse_id INTEGER,product_id INTEGER,quantity INTEGER',[[1,1,30],[1,3,10]])
reg('inv_cost','category_id INTEGER,opening_cents INTEGER,closing_cents INTEGER,sales_cost_cents INTEGER',[[1,100000,200000,450000],[2,0,0,1000]])
reg('rfm_score','customer_id INTEGER,last_day TEXT,frequency INTEGER,net_cents INTEGER',[[1,'2025-12-01',4,4000],[2,'2025-12-31',1,1000],[3,'2025-12-31',5,3000],[4,'2025-11-01',6,5000],[5,'2025-10-01',3,2000]])
reg('channel_totals','source_id INTEGER,sessions INTEGER,converted_sessions INTEGER,new_customers INTEGER,spend_cents INTEGER',[[1,100,12,4,20000],[2,10,0,0,1000]])
reg('budget_mini','department_id INTEGER,account_id INTEGER,budget_cents INTEGER',[[1,1,10000],[1,2,5000],[2,1,0]])
reg('expense_mini','department_id INTEGER,account_id INTEGER,expense_cents INTEGER',[[1,1,8000],[1,1,3500],[2,1,1000]])
reg('invoice_mini','invoice_id INTEGER,billed_cents INTEGER',[[1,10000],[2,5000],[3,5000]])
reg('pay_mini','invoice_id INTEGER,payment_date TEXT,amount_cents INTEGER',[[1,'2025-12-10',4000],[1,'2026-01-02',6000],[3,'2025-12-20',5500]])
reg('aging_mini','invoice_id INTEGER,days_overdue INTEGER,balance_cents INTEGER',[[1,-1,1000],[2,0,1000],[3,30,1000],[4,31,1000],[5,91,1000]])
reg('job_mini','employee_id INTEGER,department_id INTEGER,start_date TEXT,end_date TEXT',[[1,1,'2024-01-01','2025-12-31'],[1,2,'2025-12-31',None],[2,1,'2025-01-01',None],[3,2,'2026-01-01',None]])
reg('salary_mini','employee_id INTEGER,department_id INTEGER,position_id INTEGER,base_cents INTEGER',[[1,1,1,100000],[2,1,1,120000],[3,1,1,140000],[4,1,2,300000]])
reg('price_history','po_item_id INTEGER,supplier_id INTEGER,product_id INTEGER,order_date TEXT,unit_price_cents INTEGER',[[1,1,1,'2025-01-01',100],[2,2,1,'2025-01-02',130],[3,1,1,'2025-01-03',110]])
reg('po_mini','po_item_id INTEGER,quantity_ordered INTEGER,unit_price_cents INTEGER',[[1,10,100],[2,4,200]])
reg('delivery_mini','receipt_id INTEGER,po_item_id INTEGER,quantity_received INTEGER,quantity_rejected INTEGER',[[1,1,4,0],[2,1,5,1],[3,2,4,0]])
reg('bill_mini','bill_id INTEGER,po_item_id INTEGER,quantity_billed INTEGER,unit_price_cents INTEGER',[[1,1,3,100],[2,1,5,110],[3,2,4,200]])
reg('store_products','store_id INTEGER,product_id INTEGER,net_cents INTEGER',[[1,1,800],[1,2,800],[1,3,600],[1,4,300],[2,1,100],[2,2,200]])
reg('category_sales','category_id INTEGER,net_cents INTEGER',[[1,300],[2,100],[3,100]])
reg('tenure_mini','employee_id INTEGER,hire_date TEXT,termination_date TEXT',[[1,'2025-12-01',None],[2,'2025-12-01','2025-12-11']])
reg('stock_last','product_id INTEGER,quantity INTEGER,unit_cost_cents INTEGER',[[1,12,1000],[2,6,2000],[3,5,500]])
reg('sale_qty','product_id INTEGER,qty INTEGER',[[1,7],[3,0]])
# 第19章基础指标回忆卡（与小型课程知识点对应）
Cc('19-03','复购率的分母是所有注册客户还是观察期内的购买客户？分子是什么？','按本课程口径，分母是期间至少有一次有效购买的不同客户，分子是期间有效订单不少于2笔的不同客户。','先按客户统计有效订单数。不要以订单明细行数充当购买次数，也别把未购买注册客户无条件纳入分母。',difficulty='中等')
Cc('19-27','为什么采购订单金额不能直接当成本费用或已付现金？','采购订单金额代表采购承诺口径：有效采购数量×约定单价。','订单、实际收货、发票及付款分别属于不同阶段，本企业案例不将采购承诺直接计入期间利润。',related='19-06+19-30',difficulty='中等')
S('19-09','每店取严格前三种商品，销售额并列时按商品编号决胜。回忆先排名再限行的窗口结构。','在每店分区内 ROW_NUMBER()，外层筛 rn<=3','WITH r AS (SELECT store_id,product_id,ROW_NUMBER() OVER(PARTITION BY store_id ORDER BY net_cents DESC,product_id) AS rn FROM store_products) SELECT store_id,product_id FROM r WHERE rn<=3 ORDER BY store_id,rn',['store_products'],[[1,1],[1,2],[1,3],[2,2],[2,1]],'输入已是门店×商品净销售汇总，每店不足三种则如实返回。',difficulty='困难')
Dd('19-19+19-20','费用分类汇总与收入月度增长，需要采用相同的统计维度吗？','不是。费用分类按科目和指定期间汇总；收入增长按连续月份聚合后比较上月。','费用与收入应分别定义有效方向；先补齐月份轴再算增长，缺失与零值口径要区分。',difficulty='中等')
Dd('19-25','部门平均薪酬的分母是部门全部员工还是当期有工资记录的员工？','本课程平均薪酬示例以固定月份有薪资记录的员工数为分母。','期末人数、历史人员和有工资记录的人数并不必然一致；奖金缺失也不可默认为零，除非业务已明确。',difficulty='中等')
F('19-07+19-10','类别净销售贡献率＝类别净销售÷总体净销售。输入已是完整类别汇总，补齐全体窗口，保留全部类别。','SELECT category_id,100.0*net_cents/NULLIF(SUM(net_cents) □,0) AS pct FROM category_sales ORDER BY category_id','OVER()',['category_sales'],[[1,60.0],[2,20.0],[3,20.0]],'分母来自同口径全部类别；如只展示一个类别，应在外层过滤，不能改变总体分母。',difficulty='中等')
P('19-26','截至2025-12-31，任职天数按截止日或更早离职日减入职日，不含首尾额外一天。预测结果。','SELECT employee_id,DATEDIFF(COALESCE(termination_date,\'2025-12-31\'),hire_date) AS days FROM tenure_mini ORDER BY employee_id',['tenure_mini'],[[1,30],[2,10]],'在职使用统计日，离职使用终止日；样本均已入职且终止日不晚于截止日。',sqlite_sql="SELECT employee_id,CAST(julianday(COALESCE(termination_date,'2025-12-31'))-julianday(hire_date) AS INTEGER) AS days FROM tenure_mini ORDER BY employee_id",difficulty='中等')
E('19-13+19-11','期内无销售且期末库存正数是滞销候选。输入销售表已按商品汇总，缺行约定为无销售。修正遗漏缺行商品的查询。','SELECT s.product_id FROM stock_last s LEFT JOIN sale_qty q ON s.product_id=q.product_id WHERE s.quantity>0 AND q.qty=0 ORDER BY s.product_id','SELECT s.product_id FROM stock_last s LEFT JOIN sale_qty q ON s.product_id=q.product_id WHERE s.quantity>0 AND COALESCE(q.qty,0)=0 ORDER BY s.product_id',['stock_last','sale_qty'],[[2],[3]],'无销量可能有零记录或无记录。本例已确认事实记录完整；缺行可填0，未知销量数据不能一律填0。',difficulty='中等')
# BP01--BP30：每个正式企业任务独立回忆卡
P('19-31+19-01','口径：gross为退款前且折扣前销售；cost为退货冲减后的成本。预测每店净销售、毛利、目标达成率（金额分；比例%）。','SELECT store_id,gross_cents-discount_cents-refund_cents AS net_cents,gross_cents-discount_cents-refund_cents-cost_after_returns_cents AS profit_cents,100.0*(gross_cents-discount_cents-refund_cents)/NULLIF(target_cents,0) AS target_pct FROM shop_totals ORDER BY store_id',['shop_totals'],[[1,90000,36000,90.0],[2,0,0,None]],'甲店净销售90000分，毛利36000分；目标为零的乙店达成率未定义。',difficulty='中等')
F('19-32+19-08','月份已补齐，计算3月环比前需取得准确上月金额。补齐读取上一行的窗口函数。','SELECT month_key,amount_prev FROM (SELECT month_key,□(net_cents) OVER(ORDER BY month_key) AS amount_prev FROM store_month) m ORDER BY month_key','LAG',['store_month'],[['2025-01',None],['2025-02',10000],['2025-03',0],['2025-04',20000]],'必须保留零销售月份；3月上月为0而不是跳到1月，增长率应为NULL。',related='20-02')
S('19-33','要按净销售额将商品分四组，最高组定义为前四分之一；并列编号决胜。回忆 MySQL 窗口分桶写法。','NTILE(4) OVER(ORDER BY 净销售额 DESC,商品编号)','SELECT product_id,NTILE(4) OVER(ORDER BY net_cents DESC,product_id) AS quartile FROM sku_totals ORDER BY product_id',['sku_totals'],[[1,1],[2,1],[3,2],[4,2],[5,3],[6,3],[7,4],[8,4]],'先在所有有效商品算分桶，再筛最高组且毛利率低于30%；不能先筛低毛利再排名。',related='19-07')
Dd('19-34','促销期间总销售12万元、非促销期间总销售20万元，能直接判断非促销效果更好吗？','不能。应按完整日历分别求日均净销售、日均净件数和毛利率。','期间长短不同，必须计入零销售日；即使日均差异明显也只能说明关联，非因果证据。')
Cc('19-35','为何综合经营看板的退款率、目标达成率和毛利率应分别标记异常？','三个指标使用不同分母，分别反映售后、计划达成及销售盈利质量。','退款率分母为退款前折后销售；目标达成率分母为目标；毛利率分母为净销售。零分母为NULL，不虚构正常或异常分值。')
E('19-36+19-14','90日销量低于20且库存价值超过500000分。输入为有效商品期末快照及已汇总销量；缺行代表无销售。修正遗漏零销售商品的分析。','SELECT s.product_id FROM snap s LEFT JOIN outbound o ON s.warehouse_id=o.warehouse_id AND s.product_id=o.product_id WHERE s.quantity*s.unit_cost_cents>500000 AND o.quantity<20 ORDER BY s.product_id','SELECT s.product_id FROM snap s LEFT JOIN outbound o ON s.warehouse_id=o.warehouse_id AND s.product_id=o.product_id WHERE s.quantity*s.unit_cost_cents>500000 AND COALESCE(o.quantity,0)<20 ORDER BY s.product_id',['snap','outbound'],[[2],[3]],'无销量商品2仍占用600000分库存；业务确认缺行表示无销售后，用COALESCE保留它。')
P('19-37+19-02','已知年初、年末库存价值和期间净销售成本（分）。预测周转率及周转天数，分母无效时NULL。','SELECT category_id,ROUND(1.0*sales_cost_cents/NULLIF((opening_cents+closing_cents)/2.0,0),2) AS turnover,ROUND(365.0/NULLIF(1.0*sales_cost_cents/NULLIF((opening_cents+closing_cents)/2.0,0),0),2) AS days FROM inv_cost ORDER BY category_id',['inv_cost'],[[1,3.0,121.67],[2,None,None]],'平均库存150000分，周转3次，365÷3≈121.67天。该例成本均为正。')
Cc('19-38+19-12','当日库存为15件，近30日销售60件，历史合格收货加权周期10天，为什么可能缺货？','日均2件×10天需20件，库存15件不足。','应以仓库×商品为粒度；无历史收货的任务约定14天，不能将没有周期记录当作即时到货。')
Dd('19-39','先进先出假设推算库龄，与直接使用某商品最早入库日期有什么区别？','前者扣除被销售消耗的最早批次，后者可能给仍在库的商品错误贴上陈旧标签。','本企业模拟无完整真实批次追踪：用累计流入、流出推算剩余批次数量，再与期末标准成本结合。')
Dd('19-40','预计需求100、当前库存50、未逾期未完成采购30、逾期未收20件。补货估算应自动把全部50件在途视为按时可用吗？','不应。按任务口径，未到期在途30纳入，已逾期20单独反映供应风险。','有效库存与可预期到货分别核算，缺口还应结合商品历史准时足量率。',related='19-12+19-28')
Cc('19-41+19-15','新客与老客订单分析中：同一客户2025年首购1次，之后又买2次。应把这3笔订单都归为新客订单吗？','不应。只有历史第一笔有效线上订单是首购，后两笔属于非首购。','本任务按订单分组，首购由日期和订单编号决定；客单价是组内净收入÷有效订单数。')
S('19-42+19-16','最近消费时间越近分数越高，频次与金额越大分数越高。回忆分五组时让最优组得到5分的写法，以最近日期为例。','6 - NTILE(5) OVER(ORDER BY 最近日期 DESC,客户编号)','SELECT customer_id,6-NTILE(5) OVER(ORDER BY last_day DESC,customer_id) AS r_score FROM rfm_score ORDER BY customer_id',['rfm_score'],[[1,3],[2,5],[3,4],[4,2],[5,1]],'显式编号决胜，三项分数分别排序后合计；同一天不能依赖数据库默认返回顺序。')
Dd('19-43+19-17','某首购同期群的第5月没有回购，与第5月尚未到观察截止日期，有何区别？','已经观察但无回购为0%；尚未进入观察期不输出该月份。','同期群留存分母固定为该首购月客户数；不能用尚未成熟的未来月份人为拉低留存。')
F('19-44','已按访问渠道汇总会话及转化会话。计算转化率时除数应使用总会话数，并防御0。','SELECT source_id,100.0*converted_sessions/NULLIF(□,0) AS conversion_pct FROM channel_totals ORDER BY source_id','sessions',['channel_totals'],[[1,12.0],[2,0.0]],'一个会话有多笔订单仍只算一个转化会话；获客成本另用广告支出除历史新客数。')
Cc('19-45+19-18','为什么高价值流失风险不能只按当前年度订单金额排名？','任务用历史有效订单数、历史净消费分位与最近消费间隔共同定义候选。','历史购买需≥3次、净额位于前20%、近90日无购买，再按历史毛利决定优先级。仅当业务口径规定才更换时间窗。')
E('19-46+19-22','要求保留有预算而未发生费用的项目；费用已经先按部门×科目聚合。修正丢失零发生额项目的连接。','WITH a AS (SELECT department_id,account_id,SUM(expense_cents) AS actual FROM expense_mini GROUP BY department_id,account_id) SELECT b.department_id,b.account_id,a.actual-b.budget_cents AS variance FROM budget_mini b JOIN a ON b.department_id=a.department_id AND b.account_id=a.account_id ORDER BY b.department_id,b.account_id','WITH a AS (SELECT department_id,account_id,SUM(expense_cents) AS actual FROM expense_mini GROUP BY department_id,account_id) SELECT b.department_id,b.account_id,COALESCE(a.actual,0)-b.budget_cents AS variance FROM budget_mini b LEFT JOIN a ON b.department_id=a.department_id AND b.account_id=a.account_id ORDER BY b.department_id,b.account_id',['budget_mini','expense_mini'],[[1,1,1500],[1,2,-5000],[2,1,1000]],'从预算出发保留未发生费用项目；零预算的差额仍有效，但百分比偏差未定义。',related='19-19')
P('19-47+19-04','截至2025-12-31，预测每张发票未付余额（分），允许负值标记超额付款。','WITH p AS (SELECT invoice_id,SUM(amount_cents) AS paid FROM pay_mini WHERE payment_date<=\'2025-12-31\' GROUP BY invoice_id) SELECT i.invoice_id,i.billed_cents-COALESCE(p.paid,0) AS balance FROM invoice_mini i LEFT JOIN p ON i.invoice_id=p.invoice_id ORDER BY i.invoice_id',['invoice_mini','pay_mini'],[[1,6000],[2,5000],[3,-500]],'跨截止日付款不能提前冲销；无付款为0，而超额付款导致负余额。')
F('19-48','只对正余额发票作账龄分析。0天及未来到期为未逾期，1—30日为首档。填入首档区间。','SELECT invoice_id,CASE WHEN days_overdue<=0 THEN \'未逾期\' WHEN days_overdue □ THEN \'1—30\' WHEN days_overdue<=60 THEN \'31—60\' WHEN days_overdue<=90 THEN \'61—90\' ELSE \'90以上\' END AS band FROM aging_mini WHERE balance_cents>0 ORDER BY invoice_id','BETWEEN 1 AND 30',['aging_mini'],[[1,'未逾期'],[2,'未逾期'],[3,'1—30'],[4,'31—60'],[5,'90以上']],'到期当天不算逾期；先处理未到期与边界，不能将贷项当未付余额。',related='19-04')
Dd('19-49','银行流水与费用凭证对账时，只以账簿为主表做左连接为什么不够？','会遗漏仅存在于银行侧的记录。','分别按参考号汇总，使用两侧完整键集合再关联；空参考号要保留独立交易键，不能将未知键视为同一个业务事件。')
Cc('19-50+19-21','重复发票编号、异常大额、超额付款被同一记录触发时，能说明一定舞弊吗？','不能，只表示命中多个复核规则。','同供应商同编号重复可能是导入问题；阈值偏差需结合审批、合同与凭证核实，输出标志及异常数。')
F('19-51+19-05','统计2025-12-31每名员工所处部门；任职区间左闭右开，填入结束日期上的严格比较符。','SELECT employee_id,department_id FROM job_mini WHERE start_date<=\'2025-12-31\' AND (end_date IS NULL OR end_date □ \'2025-12-31\') ORDER BY employee_id','>',['job_mini'],[[1,2],[2,1]],'调动当天已结束的旧部门不能重复算；未入职人员排除。')
Dd('19-52+19-24','三个月离职人数分别1、2、0，月平均在职人数10、20、30。滚动三月率为何不是直接平均单月离职率？','按本任务口径，滚动三月率是(1+2+0)/(10+20+30)=5%。','单月比例的分母不同，简单平均会给不同人数的月份相同权重。期初期末平均人数口径需固定。')
S('19-53+19-23','回忆在部门×岗位分区内计算薪资组均值的窗口结构。样本为同月基础薪资，只输出部门1岗位1。','AVG(base_cents) OVER(PARTITION BY department_id,position_id)','SELECT employee_id,AVG(base_cents) OVER(PARTITION BY department_id,position_id) AS group_mean FROM salary_mini WHERE department_id=1 AND position_id=1 ORDER BY employee_id',['salary_mini'],[[1,120000.0],[2,120000.0],[3,120000.0]],'同岗位可比较组至少3人；异常是偏离组均值的候选，非不公平或违规结论。')
Cc('19-54','任务中的估算加班成本为什么先按员工月舍入到分，再按部门求和？','避免不同员工成本计算与舍入粒度不一致。','基础时薪按月薪/176小时估算，加班1.5倍、按分钟换算，逐员工月舍入后汇总。本案例并非实际法定工资规则。')
Dd('19-55','90日留任率与180日留任率可以直接共用同一入职人数分母吗？','不一定。各自只纳入达到该完整观察窗口的人。','终止日在观察日等于已不在职；分母需分别限制90日和180日成熟人群，按首次任职部门归属。',related='19-26')
E('19-56','要求获取同供应商、同商品的上一笔采购价格。修正把所有供应商混在同一个窗口的写法。','SELECT po_item_id,LAG(unit_price_cents) OVER(PARTITION BY product_id ORDER BY order_date,po_item_id) AS prev FROM price_history ORDER BY po_item_id','SELECT po_item_id,LAG(unit_price_cents) OVER(PARTITION BY supplier_id,product_id ORDER BY order_date,po_item_id) AS prev FROM price_history ORDER BY po_item_id',['price_history'],[[1,None],[2,None],[3,100]],'供应商不同不属于同一历史价格系列；日期和采购行编号共同保证顺序。')
Cc('19-57+19-28+19-29','供应商某采购行订购10件，承诺日之前收8件，其中拒收1件，年底再合格收3件。准时足量与最终完整分别如何判断？','准时足量=否（7件）；最终完整=是（10件合格）。','拒收量不能计入合格收货。准时与完整分别使用承诺日和年底截点；质量合格率另用合格件数/真实收货件数。')
E('19-58+19-06+19-30','每采购行分别比较合格收货量和开票量。修正两张明细直接连接造成的数量放大。','SELECT p.po_item_id,SUM(r.quantity_received-r.quantity_rejected) AS accepted,SUM(b.quantity_billed) AS billed FROM po_mini p LEFT JOIN delivery_mini r ON r.po_item_id=p.po_item_id LEFT JOIN bill_mini b ON b.po_item_id=p.po_item_id GROUP BY p.po_item_id ORDER BY p.po_item_id','WITH r AS (SELECT po_item_id,SUM(quantity_received-quantity_rejected) AS accepted FROM delivery_mini GROUP BY po_item_id),b AS (SELECT po_item_id,SUM(quantity_billed) AS billed FROM bill_mini GROUP BY po_item_id) SELECT p.po_item_id,COALESCE(r.accepted,0) AS accepted,COALESCE(b.billed,0) AS billed FROM po_mini p LEFT JOIN r ON r.po_item_id=p.po_item_id LEFT JOIN b ON b.po_item_id=p.po_item_id ORDER BY p.po_item_id',['po_mini','delivery_mini','bill_mini'],[[1,8,8],[2,4,4]],'收货和开票均为一对多，先按采购行独立汇总。数量一致仍需核对金额：示例行1发票价格有差异。')
Cc('19-59','比较供应商采购单价时，一笔买1件单价100，另一笔买9件单价80，应使用简单平均90吗？','不应；采购数量加权平均为(1×100+9×80)/10=82。','实际采购金额除订购数量才反映总体单价。仅比较同商品至少两家供应商，并以最低加权价为基准。')
Cc('19-60','供应商综合评价为何要设置最低到期采购行数，并保留缺失质量指标？','避免极少样本造成不稳定排名；没有收货时合格率分母为零。','本任务至少10行，权重为准时足量40%、完整25%、质量20%、合同价格达标15%；质量未知时总分NULL，不擅自补0。')
# ---------- 第20章跨业务微型夹具与卡片 ----------
reg('share_mini','product_id INTEGER,net_cents INTEGER,stock_value_cents INTEGER',[[1,6000,1000],[2,3000,6000],[3,1000,3000]])
reg('inventory_pair','product_id INTEGER,supply_excess INTEGER,demand_shortage INTEGER',[[1,30,12],[2,15,40]])
reg('receipt_ledger','receipt_id INTEGER,accepted INTEGER',[[1,5],[2,4],[3,0],[4,3]])
reg('movement_ledger','receipt_id INTEGER,qty INTEGER',[[1,5],[2,2]])
reg('store_contrib','store_id INTEGER,net_cents INTEGER,cost_cents INTEGER,expense_cents INTEGER',[[1,100000,60000,15000],[2,50000,35000,20000]])
reg('order_net_mini','order_id INTEGER,net_cents INTEGER,refund_cents INTEGER',[[1,9000,1000],[2,5000,0]])
reg('settle_mini','order_id INTEGER,status TEXT,amount_cents INTEGER',[[1,'settled',10000],[1,'failed',10000],[2,'settled',4000]])
reg('cost_series','month_key TEXT,net_cents INTEGER,expense_cents INTEGER',[['2025-01',10000,1000],['2025-02',1000,500],['2025-03',10000,2000],['2025-04',10000,3000]])
reg('invoice_risk','invoice_id INTEGER,billed_cents INTEGER,accepted_cents INTEGER,paid_cents INTEGER',[[1,10000,8000,9000],[2,10000,8000,0],[3,5000,5000,5000]])
reg('supplier_risk','supplier_id INTEGER,overdue_cents INTEGER,unpaid_cents INTEGER,on_time_pct INTEGER,due_lines INTEGER',[[1,600000,1000000,80,20],[2,100000,1000000,50,12],[3,100000,1000000,50,8],[4,500000,2000000,80,20]])
reg('refund_metric','source_id INTEGER,discounted_before_refund_cents INTEGER,refund_cents INTEGER,after_gross_cents INTEGER,returned_cost_cents INTEGER',[[1,10000,1500,3000,800],[2,0,0,0,0]])
reg('labor_metric','store_id INTEGER,net_cents INTEGER,labor_cents INTEGER,worked_minutes INTEGER,overtime_minutes INTEGER',[[1,60000,12000,300,60],[2,10000,1000,0,0]])
reg('report_mini','month_key TEXT,profit_cents INTEGER,expense_cents INTEGER,labor_cents INTEGER,spend_cents INTEGER,ordered_cents INTEGER',[['2025-01',50000,10000,15000,6000,30000],['2025-02',20000,8000,15000,4000,10000]])
reg('months_missing','month_key TEXT,net_cents INTEGER',[['2025-01',10000],['2025-03',15000]])
reg('month_axis','month_key TEXT',[['2025-01'],['2025-02'],['2025-03']])
# 第20章两张基础综合卡
Cc('20-03','实际经营排行榜写“每店前三商品”时，为什么要先问是否包含并列？','因为业务可能要求严格三行或并列第三名全部纳入。','严格数量用ROW_NUMBER及唯一决胜键；并列名次规则用RANK或DENSE_RANK，依据需求定义。')
P('20-02','完整月份轴已建立，缺行约定为无交易。预测补齐月份后读取上月净销售的结果（分）。','WITH m AS (SELECT a.month_key,COALESCE(s.net_cents,0) AS net FROM month_axis a LEFT JOIN months_missing s ON a.month_key=s.month_key) SELECT month_key,net,LAG(net) OVER(ORDER BY month_key) AS previous FROM m ORDER BY month_key',['month_axis','months_missing'],[['2025-01',10000,None],['2025-02',0,10000],['2025-03',15000,0]],'3月的上月是2月，不是跳过2月去取1月；百分比增长分母为0时不定义。')
Cc('20-05+20-04','为什么“畅销缺货”和“滞销积压”要设成两个独立候选规则，而不是直接按库存数量排序？','因为两种风险的销量、库存需求和资金占用方向相反。','本任务畅销风险要求30日销量至少30且低于15日需求；积压要求销量不高于3且库存金额至少300000分。结果是复核候选，不是库存管理结论。')
F('20-06+20-01','商品已按全渠道净销售与各仓期末库存价值汇总。求商品库存资金占比的分母。','SELECT product_id,100.0*stock_value_cents/NULLIF(□,0) AS inventory_pct FROM share_mini ORDER BY product_id','SUM(stock_value_cents) OVER()',['share_mini'],[[1,10.0],[2,60.0],[3,30.0]],'销售与库存占比使用各自全局分母；两者相减识别错配，不应互相替换。')
P('20-07','给定供给仓当前超额量、需求仓缺口，预测单个候选配对可调拨量。','SELECT product_id,LEAST(supply_excess,demand_shortage) AS candidate_qty FROM inventory_pair ORDER BY product_id',['inventory_pair'],[[1,12],[2,15]],'候选数量不能超过任一侧；多个候选共享同一库存时仍需全局分配，不能同时全部执行。',sqlite_sql='SELECT product_id,MIN(supply_excess,demand_shortage) AS candidate_qty FROM inventory_pair ORDER BY product_id')
Dd('20-08','库存周转率和库存投资回报倍数为什么可能方向不同？','周转率分子为销售成本，库存投资回报分子为毛利。','两者使用同口径平均库存价值分母。低毛利商品即使高周转，也未必带来较高毛利回报。')
Cc('20-09','期末近30日需求60件、库存20件、有一笔30件已逾期未到的采购。为什么仍属于缺货风险？','库存低于需求且存在逾期未收货量，两项风险均命中。','逾期未收货量不等于可售库存。需求和供应按仓库×商品合并，未到期采购不能算逾期。')
E('20-10','每条收货记录均须核对已入库量（入库表已按收货编号汇总），缺少流水视为0。修正遗漏无流水收货的连接，并只输出差额非零记录。','SELECT r.receipt_id,r.accepted-m.qty AS gap FROM receipt_ledger r JOIN movement_ledger m ON r.receipt_id=m.receipt_id WHERE r.accepted-m.qty<>0 ORDER BY r.receipt_id','SELECT r.receipt_id,r.accepted-COALESCE(m.qty,0) AS gap FROM receipt_ledger r LEFT JOIN movement_ledger m ON r.receipt_id=m.receipt_id WHERE r.accepted-COALESCE(m.qty,0)<>0 ORDER BY r.receipt_id',['receipt_ledger','movement_ledger'],[[2,2],[4,3]],'保留所有收货，拒收全部且无入库的记录差额为零。缺失入库流水必须纳入。')
Dd('20-11','供应商有两条逾期采购行，都属于同一仓库同一商品。为什么不能把该仓库商品库存价值加两遍？','库存属于仓库×商品粒度，而非采购行粒度。','对相关仓库商品键去重后汇总库存价值；同一库存还可能与多供应商关联，不能声称为该供应商独有资产。')
P('20-12','预测两家门店贡献利润（分）。这里仅扣销售成本与归属门店费用，不扣人员和总部费用。','SELECT store_id,net_cents-cost_cents-expense_cents AS contribution FROM store_contrib ORDER BY store_id',['store_contrib'],[[1,25000],[2,-5000]],'贡献利润是限定口径的经营指标，不是完整净利润；负值表示该口径下贡献为负。')
E('20-13','有效支付累计额减实际退款，与净销售比较。修正把失败支付算进去的订单结算查询，只输出差异订单。','WITH p AS (SELECT order_id,SUM(amount_cents) AS paid FROM settle_mini GROUP BY order_id) SELECT o.order_id,COALESCE(p.paid,0)-o.refund_cents-o.net_cents AS gap FROM order_net_mini o LEFT JOIN p ON o.order_id=p.order_id WHERE COALESCE(p.paid,0)-o.refund_cents-o.net_cents<>0 ORDER BY o.order_id','WITH p AS (SELECT order_id,SUM(amount_cents) AS paid FROM settle_mini WHERE status=\'settled\' GROUP BY order_id) SELECT o.order_id,COALESCE(p.paid,0)-o.refund_cents-o.net_cents AS gap FROM order_net_mini o LEFT JOIN p ON o.order_id=p.order_id WHERE COALESCE(p.paid,0)-o.refund_cents-o.net_cents<>0 ORDER BY o.order_id',['order_net_mini','settle_mini'],[[2,-1000]],'失败支付不计实际结算；订单1的有效支付10000减退款1000与净收入9000一致。')
S('20-14','回忆完整月份轴上的滚动三月费用率写法。前两月不足三个月不展示比例，分母为三月净销售总额。','SUM(费用) OVER(ORDER BY 月份 ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) / SUM(销售) OVER(同一窗口)','SELECT month_key,CASE WHEN COUNT(*) OVER(ORDER BY month_key ROWS BETWEEN 2 PRECEDING AND CURRENT ROW)=3 THEN ROUND(100.0*SUM(expense_cents) OVER(ORDER BY month_key ROWS BETWEEN 2 PRECEDING AND CURRENT ROW)/NULLIF(SUM(net_cents) OVER(ORDER BY month_key ROWS BETWEEN 2 PRECEDING AND CURRENT ROW),0),2) END AS pct FROM cost_series ORDER BY month_key',['cost_series'],[['2025-01',None],['2025-02',None],['2025-03',16.67],['2025-04',26.19]],'滚动费用率为总费用/总销售，不能简单平均月费用率；当期无足够月份时返回NULL。')
Cc('20-15','供应商经营敞口表中，订单金额、合格收货计价、发票金额、实付金额、未付余额，为什么不宜用一张大连接直接累计？','各表处于不同业务阶段且一对多关系会乘法膨胀。','独立汇总各阶段至供应商粒度，再以供应商表保留全部供应商。未付余额是发票减实付，采购承诺不等于应付。')
E('20-16','目标：发票金额比合格收货计价的绝对差超过100分且已付款。修正把所有金额差异都纳入的写法。','SELECT invoice_id,paid_cents-accepted_cents AS excess FROM invoice_risk WHERE ABS(billed_cents-accepted_cents)>100 ORDER BY invoice_id','SELECT invoice_id,paid_cents-accepted_cents AS excess FROM invoice_risk WHERE ABS(billed_cents-accepted_cents)>100 AND paid_cents>0 ORDER BY invoice_id',['invoice_risk'],[[1,1000]],'未付款差异仍是其他核对候选，却不属于已付款风险；输出付款与合格收货计价差额。')
F('20-17','资金风险满足任一：逾期应付≥500000分且逾期占比>40%；或准时足量率<60%且到期行≥10。填入两组条件间连接词。','SELECT supplier_id FROM supplier_risk WHERE (overdue_cents>=500000 AND 100.0*overdue_cents/NULLIF(unpaid_cents,0)>40) □ (on_time_pct<60 AND due_lines>=10) ORDER BY supplier_id','OR',['supplier_risk'],[[1],[2]],'两种风险独立成立即可。少于10行的低准时率候选不命中该条规则。')
Dd('20-18','营销净销售广告回报与广告后贡献利润有什么区别？','前者净销售除广告支出；后者为净毛利减广告支出。','高收入回报不必然高利润；广告后贡献未扣所有人力、配送和管理成本，不是净利润。')
Cc('20-19','首购客户90天毛利与获客成本比较时，为什么成熟客户分母与获客成本分母不必相同？','成熟期毛利只观察满90日的客户；渠道月获客成本按该月全部新客分摊。','例如年底晚入组客户还没有完整90日数据，不纳入成熟期毛利平均值，但仍是新获客户。')
P('20-20','预测两个渠道退款率、退款后毛利及退款前毛利估算。退款前毛利=退款后毛利+退款额−退货冲回成本。','SELECT source_id,100.0*refund_cents/NULLIF(discounted_before_refund_cents,0) AS refund_pct,after_gross_cents,after_gross_cents+refund_cents-returned_cost_cents AS before_refund_profit FROM refund_metric ORDER BY source_id',['refund_metric'],[[1,15.0,3000,3700],[2,None,0,0]],'退款率的分母为退款前折后销售额，不是退款后的净销售；退款冲回收入及对应成本。')
F('20-21','门店人效按出勤与加班的总小时数计算净销售额（分）/小时，避免除零。补齐分钟分母。','SELECT store_id,60.0*net_cents/NULLIF(□,0) AS cents_per_hour FROM labor_metric ORDER BY store_id','worked_minutes+overtime_minutes',['labor_metric'],[[1,10000.0],[2,None]],'300分钟出勤+60分钟加班=6小时；无工时不生成有效比率。')
Dd('20-22','某店月末人数增加20%、销售下滑10%，能立即断言增加的员工导致销售下滑吗？','不能。这只是同月经营偏离候选。','月末人数是时点指标，销售是期间流量；还应检查员工调动、营业日、促销与外部需求等证据。')
S('20-23','回忆经营汇总中的贡献计算，给定已独立聚合的月度毛利、费用、人工、广告、采购承诺字段。','毛利 - 费用 - 人工；广告与采购承诺单列展示','SELECT month_key,profit_cents-expense_cents-labor_cents AS operating_contribution,spend_cents,ordered_cents FROM report_mini ORDER BY month_key',['report_mini'],[['2025-01',25000,6000,30000],['2025-02',-3000,4000,10000]],'本任务广告独立展示以避免与费用潜在双计；采购承诺非当期费用，贡献也不是完整净利润。')
Cc('20-24','季度风险评分遇到某季度供应商准时率为NULL，如何处理？','保留该季度，未知指标不计入命中风险数，另外记录缺失指标数。','按本任务毛利率<30%、费用率>50%、人工率>20%、准时率<60%分别判断。缺失不等于安全，结论须同时展示数据完整性。')
# ---------- 资源关联、教学质量检查及正式导出 ----------
# 直接考查知识点的关联优先；保证全部正式题均有至少一张共享知识点的卡。
for qid,q in Q.items():
 if any(qid in c['relatedQuestionIds'] for c in CARDS):continue
 qk=set(q.get('primaryKnowledgePointIds') or q['knowledgePointIds']); aq=set(q['knowledgePointIds']);
 candidates=[c for c in CARDS if qk.intersection(c['primaryKnowledgePointIds'])]
 if not candidates:candidates=[c for c in CARDS if aq.intersection(c['knowledgePointIds'])]
 if not candidates:raise AssertionError(qid)
 def affinity(c):
  type_match={'代码填空':'代码填空','结果预测':'结果预测','代码纠错':'错误纠正','选择':'概念记忆','独立编写':'语法结构','业务情景':'用法辨析'}
  return (c['chapterId']==q['chapterId'],len(qk.intersection(c['primaryKnowledgePointIds'])),c['type']==type_match.get(q['type']),-len(c['relatedQuestionIds']))
 c=max(candidates,key=affinity);c['relatedQuestionIds'].append(qid)
for c in CARDS:
 c['relatedQuestionIds']=sorted(set(c['relatedQuestionIds']))
 c['referenceTopicNames']=[K[k]['title'] for k in c['primaryKnowledgePointIds']]
 c['questionLinkPolicy']='通过共享课程知识点关联正式题库；业务卡额外保留对应BP任务编号。关联不表示题目与卡片完全相同。'
 if c['type']=='代码填空':
  c['back']['blankPolicy']='背面给出标准填法；关键词大小写及无意义空白变化可接受。其他语义等价写法可由学习者自检；闪卡采用自评，不当作300题自动判题。'
  if c['back']['acceptedBlanks']==['RIGHT JOIN']:c['back']['acceptedBlanks'].append('RIGHT OUTER JOIN')
 if c['chapterId'] in ('ch19','ch20') and c.get('businessTaskId'):
  b=json.loads((ENT/'business_tasks'/f"{c['businessTaskId']}.json").read_text()); assert b['id']==c['businessTaskId']
  c['businessResource']={'id':b['id'],'title':b['title'],'projectId':b['projectId'],'datasetId':b['datasetId'],'taskQuestionId':c['businessQuestionId'],'taskDifficulty':b['difficulty'],'sourceTables':b.get('sourceTables',[]),'note':'正式业务数据库关联资源；本卡为口径或微型隔离示例，不直接运行完整实战查询。'}
  # 卡片难度衡量回忆任务，不机械复制正式编程题的难度。
  if b['difficulty']=='挑战' and c['type'] in ('语法结构','错误纠正','用法辨析','概念记忆') and c['difficulty']=='困难':c['difficulty']='挑战'
 if 'sourceTables' not in c['front'] and int(c['chapterId'][2:])>=19:c['front']['dataNote']='口径记忆卡；不要求执行企业库查询。涉及的数值为独立教学假设。'
 c['supportStatusLabel']={'仅理论':'概念或业务口径；无需执行','转换执行':'使用本地等价转换','直接兼容':'本示例直接执行'}[c['supportStatus']]
 assert all(q in Q and set(Q[q]['knowledgePointIds']).intersection(c['knowledgePointIds']) or q==c.get('businessQuestionId') for q in c['relatedQuestionIds']),c['id']
# 保存版本索引和依赖指纹，不复制大型企业数据库。
source_index={'courseVersion':C['ch19']['contentVersion'],'questionVersion':next(iter(Q.values()))['contentVersion'],'sourceFiles':{},'knowledgePoints':{},'questions':{},'businessTasks':{},'databases':{}}
for ch,chapter in C.items():
 source_index['sourceFiles'][f'企业升级包/CONTENT/courses/{ch}.json']=sha(CR/f'{ch}.json')
 source_index['sourceFiles'][f'题库包/questions/{ch}.json']=sha(QR/'questions'/f'{ch}.json')
for k,v in K.items():source_index['knowledgePoints'][k]={'chapterId':v['chapterId'],'title':v['title']}
for q,v in Q.items():source_index['questions'][q]={'chapterId':v['chapterId'],'title':v['title'],'knowledgePointIds':v['knowledgePointIds'],'primaryKnowledgePointIds':v.get('primaryKnowledgePointIds',[])}
source_index['sourceFiles']['开发资料/docs/FLASHCARDS.md']=sha(DEV/'FLASHCARDS.md')
for i in range(1,51):
 bp=f'BP{i:02d}'; bpfile=ENT/'business_tasks'/f'{bp}.json';b=json.loads(bpfile.read_text());source_index['businessTasks'][bp]={'title':b['title'],'questionId':f'q{250+i:04d}','projectId':b['projectId'],'datasetId':b['datasetId'],'knowledgePointIds':b['knowledgePointIds'],'sourceSha256':sha(bpfile)}
old_manifest=json.loads((ENT/'manifest_root.json').read_text())
for name, info in old_manifest['datasets'].items():
 f=ENT/'databases'/name/f'{name}.sqlite'; actual=sha(f); file_entry=next(v for v in old_manifest['files'] if v['path']==f'databases/{name}/{name}.sqlite'); assert actual==file_entry['sha256'],f; summary_match=actual==info['sha256']
 cf=QR/'databases'/name/f'{name}.sqlite';assert cf.is_file() and sha(cf)==actual,cf
 con=sqlite3.connect(f'file:{f}?mode=ro',uri=True); schema_names={r[0] for r in con.execute("SELECT name FROM sqlite_master WHERE type IN ('table','view') AND name NOT LIKE 'sqlite_%'")};con.close()
 source_index['databases'][name]={'file':f'databases/{name}/{name}.sqlite','sha256':actual,'bytes':f.stat().st_size,'tables':info['tables'],'views':info['views'],'rows':info['rows'],'objects':sorted(schema_names),'matchesEnterpriseAndQuestionResources':True,'matchesManifestFileEntry':True,'matchesLegacySummaryHash':summary_match,'legacySummaryHash':info['sha256']}
J(R/'reference'/'source_index.json',source_index)
for p,n in [(DEV/'FLASHCARDS.md','FLASHCARDS.md'),(ENT/'docs'/'CURRICULUM.md','CURRICULUM.md'),(ENT/'docs'/'TECHNICAL.md','TECHNICAL.md'),(ENT/'docs'/'DATABASE.md','DATABASE.md'),(ENT/'docs'/'QUESTION_BANK.md','QUESTION_BANK.md')]:shutil.copy2(p,R/'reference'/n)
# 兼容规范字段，不混入学习进度。
review={'schemaVersion':1,'algorithm':'FSRS','implementationVersion':None,'runtimeStatus':'待Codex集成和测试','ratings':[{'id':'again','label':'忘记','fsrsGrade':1},{'id':'hard','label':'困难','fsrsGrade':2},{'id':'good','label':'记得','fsrsGrade':3},{'id':'easy','label':'熟练','fsrsGrade':4}],'modes':[{'id':'due','label':'每日到期'},{'id':'new','label':'新卡'},{'id':'chapter','label':'章节'},{'id':'weak','label':'薄弱'},{'id':'random','label':'随机'},{'id':'favorite','label':'收藏'},{'id':'custom','label':'自定义'}],'policy':{'requireFlipBeforeRating':True,'scheduleFromAlgorithmOutput':True,'hardcodedIntervals':False,'isolatedUserState':True,'atomicRatingEvent':True,'duplicateRatingGuard':True,'skipWithoutScheduleUpdate':True,'previousWithoutScheduleUpdate':True,'pauseWithoutScheduleUpdate':True,'returnRatedCardReadOnly':True,'reratingRequiresExplicitUndo':True,'resumeSession':True,'dueBeforeNew':True,'freeReviewMayOptOutOfSchedule':True,'userOwnedCustomCardsSeparate':True,'builtInCardsReadOnly':True,'offline':True},'defaults':{'desiredRetention':None,'dailyNewLimit':None,'dailyReviewLimit':None,'parameters':None,'note':'原规范未锁定具体数值及FSRS实现版本；由开发阶段验证、锁定并保存版本化参数，不伪造固定间隔。'},'reviewStateFields':['cardId','contentVersion','algorithmVersion','due','stability','difficulty','lastReview','reps','lapses','state','updatedAt'],'reviewEventFields':['eventId','cardId','rating','reviewedAt','algorithmVersion','previousState','nextState','sessionId'],'sessionFields':['sessionId','mode','cardIds','currentPosition','startedAt','endedAt','countTowardsSchedule'],'source':'reference/FLASHCARDS.md'}
J(R/'review_policy.json',review)
# 生成课程补丁与逆向题库关联。只追加/更新闪卡关联字段，禁止覆盖题目关系。
question_links={q:[] for q in Q}; kp_links={k:[] for k in K};kp_primary={k:[] for k in K}
for c in CARDS:
 for q in c['relatedQuestionIds']:question_links[q].append(c['id'])
 for k in c['knowledgePointIds']:kp_links[k].append(c['id'])
 for k in c['primaryKnowledgePointIds']:kp_primary[k].append(c['id'])
for ch,chapter in C.items():
 updates=[{'id':k['id'],'relatedFlashcardIds':kp_links[k['id']],'primaryFlashcardIds':kp_primary[k['id']]} for k in chapter['knowledgePoints']]
 J(R/'course_links'/f'{ch}.json',{'chapterId':ch,'contentVersion':V,'mergePolicy':'按知识点id只更新relatedFlashcardIds、primaryFlashcardIds，不改课程编号、课文和relatedQuestionIds。','knowledgePointUpdates':updates})
J(R/'course_links'/'question_links.json',{'contentVersion':V,'mergePolicy':'按题号关联闪卡；不覆盖原题库内容或判题资源。','questions':[{'questionId':q,'relatedFlashcardIds':v} for q,v in sorted(question_links.items())]})
# 可读 Markdown 格式，同时保留机器可读的完整数据。
def cell(v):
 if v is None:return '`NULL`'
 if v=='':return '空字符串（`\'\'`）'
 return str(v).replace('|','\\|').replace('\n',' ')
def mdtable(headers,rows):
 out=['| '+' | '.join(cell(x) for x in headers)+' |','| '+' | '.join('---' for _ in headers)+' |']
 out+=['| '+' | '.join(cell(x) for x in row)+' |' for row in rows]
 if not rows:out.append('\n无记录。')
 return '\n'.join(out)
def block(s):return '```sql\n'+s+'\n```'
chapter_counts={}
for ch, chapter in C.items():
 cards=[c for c in CARDS if c['chapterId']==ch];chapter_counts[ch]=len(cards)
 J(R/'flashcards'/f'{ch}.json',{'chapterId':ch,'title':chapter['title'],'count':len(cards),'schemaVersion':1,'contentVersion':V,'cards':cards})
 out=[f'# 第{chapter["number"]:02d}章 · {chapter["title"]}闪卡','',f'本章共{len(cards)}张卡。先遮住背面自行回忆，再翻转检查；四档自评由正式软件保存。所有金额的单位见各卡题干，企业微型示例使用马币分。','']
 for c in cards:
  out.extend([f'## {c["id"]} · {c["type"]} · {c["difficulty"]}','',f'**知识点：** '+ '；'.join(f'{k} {K[k]["title"]}' for k in c['primaryKnowledgePointIds']),f'**关联题号：** '+ '、'.join(c['relatedQuestionIds'])])
  if c.get('relatedKnowledgePointIds'):out += ['**延伸关联：** '+ '、'.join(c['relatedKnowledgePointIds'])]
  if c.get('businessTaskId'):out += [f'**对应业务任务：** {c["businessTaskId"]} {c["businessResource"]["title"]}（{c["databaseRef"]}；正式题号 {c["businessQuestionId"]}）']
  out.extend(['','### 正面','',c['front']['question'],''])
  if c['front'].get('code'):out.extend([block(c['front']['code']),''])
  if c['front'].get('dataNote'):out.extend([c['front']['dataNote'],''])
  for t in c['front'].get('sourceTables',[]):
   out.extend([f'数据表 `{t["name"]}`','',mdtable([col['name'] for col in t['columns']],t['rows']),''])
  if c['front'].get('verificationQuery'):out.extend(['执行后核对：',block(c['front']['verificationQuery']),''])
  out.extend(['### 背面','',f'**答案：** {c["back"]["answer"]}','',c['back']['explanation'],''])
  if c['back'].get('code'):out.extend(['MySQL 8.4 教学代码：',block(c['back']['code']),''])
  if c['back'].get('sqliteCode') and c['back']['sqliteCode']!=c['back']['code']:out.extend(['本地 SQLite 兼容写法：',block(c['back']['sqliteCode']),''])
  if c['back'].get('result'):
   rs=c['back']['result'];out.extend(['实际执行结果：','',mdtable(rs['columns'],rs['rows']),''])
  if c['back'].get('wrongResult'):
   w=c['back']['wrongResult'];out.extend(['原错误写法的本地结果：','',('执行报错：`'+w['message']+'`') if w['kind']=='error' else mdtable(w['columns'],w['rows']),''])
  if c['back'].get('acceptedBlanks'):out += ['填空参考：'+'；'.join('`'+x+'`' for x in c['back']['acceptedBlanks']),c['back']['blankPolicy'],'']
  out += [f'**兼容性：** {c["supportStatusLabel"]}。{c["compatibility"]}',f'**验证：** {c["validation"]["status"]}','', '---','']
 (R/'reading'/f'{ch}.md').write_text('\n'.join(out),encoding='utf-8')
# 原生数据库复验输入：每个脚本仅在全新的空测试库执行，不自动删除任何业务表。
(R/'validation'/'mysql').mkdir(exist_ok=True)
def sql_literal(x):
 if x is None:return 'NULL'
 if isinstance(x,(float,int)):return str(x)
 return "'"+x.replace("'","''")+"'"
for c in CARDS:
 if 'fixturePath' not in c:continue
 f=json.loads((R/c['fixturePath']).read_text());lines=['-- '+c['id']+'：MySQL 8.4 原生待复验。','-- 仅在单独的全新空测试库运行，绝不可在企业正式库执行。','SET NAMES utf8mb4 COLLATE utf8mb4_0900_bin;','SET SESSION sql_mode=\'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION\';']
 for t in f['tables']:
  def mysqlcol(col):return '`'+col['name']+'` '+('VARCHAR(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin' if col['type']=='TEXT' else 'INT' if col['type']=='INTEGER' else col['type'])
  lines.append('CREATE TABLE `'+t['name']+'` ('+', '.join(mysqlcol(x) for x in t['columns'])+(', '+t['constraints'] if t.get('constraints') else '')+') ENGINE=InnoDB;')
  if t['rows']:lines.append('INSERT INTO `'+t['name']+'` VALUES '+', '.join('('+', '.join(sql_literal(x) for x in row)+')' for row in t['rows'])+';')
 lines.append(f['mysqlSql'].rstrip(';')+';')
 if f['verificationQuery']:lines.append(f['verificationQuery'].rstrip(';')+';')
 lines.append('-- 期望列及值（按卡片结果比较，数字格式可等价）：'+json.dumps({'columns':f['expectedColumns'],'rows':f['expectedRows']},ensure_ascii=False))
 if f['wrongMysql']:lines.append('-- 典型错误在另一个相同初态的独立测试库复验；不可与修正语句连续更新同一状态。')
 fn=R/'validation'/'mysql'/f'{c["id"]}.sql';fn.write_text('\n'.join(lines)+'\n',encoding='utf-8'); c['mysqlVerificationScript']=str(fn.relative_to(R))
# 刷新正式章节JSON，包含原生复验脚本引用。
for ch,chapter in C.items():
 cards=[c for c in CARDS if c['chapterId']==ch];J(R/'flashcards'/f'{ch}.json',{'chapterId':ch,'title':chapter['title'],'count':len(cards),'schemaVersion':1,'contentVersion':V,'cards':cards})
# JSON结构契约，引用与数量另由验证器核对。
strg={'type':'string','minLength':1};strarr={'type':'array','items':strg,'uniqueItems':True};schema={'$schema':'https://json-schema.org/draft/2020-12/schema','title':'分章节 SQL 闪卡资源','type':'object','required':['chapterId','title','count','schemaVersion','contentVersion','cards'],'properties':{'chapterId':{'type':'string','pattern':'^ch(0[1-9]|1[0-9]|20)$'},'title':strg,'count':{'type':'integer','minimum':1},'schemaVersion':{'const':1},'contentVersion':strg,'cards':{'type':'array','items':{'type':'object','required':['id','chapterId','knowledgePointIds','primaryKnowledgePointIds','relatedQuestionIds','type','difficulty','front','back','tags','supportStatus','compatibility','contentVersion','validation'],'properties':{'id':{'type':'string','pattern':'^fc[0-9]{4}$'},'chapterId':strg,'knowledgePointIds':dict(strarr,minItems=1),'primaryKnowledgePointIds':dict(strarr,minItems=1),'relatedQuestionIds':dict(strarr,minItems=1),'type':{'enum':list(TYPES.values())},'difficulty':{'enum':['入门','简单','中等','困难','挑战']},'front':{'type':'object','required':['question'],'properties':{'question':strg,'code':strg,'sourceTables':{'type':'array','items':{'type':'object','required':['name','columns','rows']}}}},'back':{'type':'object','required':['answer','explanation'],'properties':{'answer':strg,'explanation':strg,'code':strg,'sqliteCode':strg,'result':{'type':'object','required':['kind','columns','rows']}}},'tags':dict(strarr,minItems=1),'supportStatus':{'enum':['直接兼容','转换执行','仅理论']},'compatibility':strg,'contentVersion':strg,'validation':{'type':'object','required':['kind','status','mysqlNative']}}}}}}
J(R/'flashcard_schema.json',schema)
try:
 import jsonschema
 for f in sorted((R/'flashcards').glob('ch*.json')):jsonschema.validate(json.loads(f.read_text()),schema)
 schema_ok=True
except ImportError:schema_ok=False
# 对源内容进行核对：不是重复题干，不重用题库参考查询。
norm=lambda s:re.sub(r'\s+',' ',s.strip().rstrip(';')).lower()
qs={norm(s['mysqlSql']) for q in Q.values() for s in q.get('referenceSolutions',[]) if s.get('mysqlSql')}
card_sql=[norm(c['back']['code']) for c in CARDS if c['back'].get('code')]
allkp={k for c in CARDS for k in c['primaryKnowledgePointIds']};allq={q for c in CARDS for q in c['relatedQuestionIds']};allbp={c['businessTaskId'] for c in CARDS if c.get('businessTaskId')}
counts=collections.Counter(c['type'] for c in CARDS);expected_types={'概念记忆':40,'语法结构':35,'用法辨析':35,'代码填空':35,'结果预测':30,'错误纠正':25}
assert len(CARDS)==200 and counts==expected_types and allkp==set(K) and allq==set(Q) and len(allbp)==50 and len(REPORT)==125
assert len(set(c['id'] for c in CARDS))==200 and len({norm(c['front']['question']) for c in CARDS})==200 and len(card_sql)==len(set(card_sql))==125 and not (set(card_sql)&qs)
dist={'total':200,'byType':dict(counts),'byDifficulty':dict(collections.Counter(c['difficulty'] for c in CARDS)),'byChapter':chapter_counts,'primaryKnowledgePoints':len(allkp),'linkedQuestions':len(allq),'businessTasks':len(allbp),'supportStatus':dict(collections.Counter(c['supportStatus'] for c in CARDS)),'executable':len(REPORT),'correctExecuted':sum(r['referencePassed'] for r in REPORT),'wrongRejected':sum(r['wrongRejected'] is True for r in REPORT),'exactDuplicatePrompts':0,'exactDuplicateSql':0,'exactReferenceSqlOverlapWithQuestions':0,'sourceDatabaseHashesMatched':len(source_index['databases']),'legacySummaryHashMismatches':[n for n,v in source_index['databases'].items() if not v['matchesLegacySummaryHash']],'jsonSchemaLibraryValidated':schema_ok}
J(R/'validation'/'distribution.json',dist);J(R/'validation'/'execution.json',REPORT)
print(dist)
