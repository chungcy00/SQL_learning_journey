#!/usr/bin/env python3
# 独立重跑正式导出文件，不依赖原开发目录、网络或外部Python包。
import sqlite3,json,pathlib,collections,hashlib,re,math
R=pathlib.Path(__file__).resolve().parents[1]
def load(p):return json.loads(p.read_text(encoding='utf-8'))
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
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


source=load(R/'reference/source_index.json')
chapters=sorted((R/'flashcards').glob('ch*.json')); assert len(chapters)==20
cards=[]
for p in chapters:
 chapter=load(p); assert chapter['chapterId']==p.stem and chapter['count']==len(chapter['cards'])
 assert all(c['chapterId']==p.stem for c in chapter['cards']);cards.extend(chapter['cards'])
assert len(cards)==200 and len({c['id'] for c in cards})==200
expected={'概念记忆':40,'语法结构':35,'用法辨析':35,'代码填空':35,'结果预测':30,'错误纠正':25}
assert dict(collections.Counter(c['type'] for c in cards))==expected
normal=lambda s:re.sub(r'\s+',' ',s.strip().rstrip(';')).lower()
assert len({normal(c['front']['question']) for c in cards})==200
assert len({normal(c['back']['code']) for c in cards if 'code' in c['back']})==125
kp={k for c in cards for k in c['primaryKnowledgePointIds']};q={q for c in cards for q in c['relatedQuestionIds']};bp={c['businessTaskId'] for c in cards if c.get('businessTaskId')}
assert kp==set(source['knowledgePoints']) and q==set(source['questions']) and bp==set(source['businessTasks'])
count=wrong=0;wrong_errors=wrong_diff=0;state_tests=0;native=0;records=[]
for c in cards:
 assert c['front']['question'] and c['back']['answer'] and c['back']['explanation'] and c['compatibility'] and c['difficulty']
 assert all(k in source['knowledgePoints'] for k in c['knowledgePointIds'])
 assert all(v in source['questions'] for v in c['relatedQuestionIds'])
 assert all(set(source['questions'][v]['knowledgePointIds']).intersection(c['knowledgePointIds']) or v==c.get('businessQuestionId') for v in c['relatedQuestionIds'])
 if c.get('businessTaskId'):
  b=source['businessTasks'][c['businessTaskId']]; assert c['businessQuestionId']==b['questionId'] and c['businessQuestionId'] in c['relatedQuestionIds'] and c['databaseRef'] in source['databases']
 if c['type'] in ['概念记忆','用法辨析']:
  assert not c.get('fixturePath') and c['supportStatus']=='仅理论';continue
 p=R/c['fixturePath'];assert sha(p)==c['validation']['fixtureSha256'];f=load(p)
 assert f['tables']==c['front']['sourceTables'] and f['mysqlSql']==c['back']['code'] and f['sqliteSql']==c['back']['sqliteCode']
 assert f['expectedRows']==c['back']['result']['rows'] and f['expectedColumns']==c['back']['result']['columns']
 assert (R/c['mysqlVerificationScript']).is_file();native+=1
 got=run(f['tables'],f['sqliteSql'],f['verificationQuery']);assert got['kind']=='result' and got['rows']==f['expectedRows'] and got['columns']==f['expectedColumns'],(c['id'],got)
 count+=1
 if f['verificationQuery']:state_tests+=1
 if c['type']=='代码填空':
  assert c['front']['code'].count('□')==1
  assert normal(c['front']['code'].replace('□',c['back']['acceptedBlanks'][0]))==normal(f['mysqlSql'])
 if c['type']=='结果预测':assert c['front']['code']==f['mysqlSql']
 iswrong=False
 if c['type']=='错误纠正':
  assert c['front']['code']==f['wrongMysql']
  w=run(f['tables'],f['wrongSqlite'],f['verificationQuery']);assert w==c['back']['wrongResult'] and (w['kind']=='error' or w['rows']!=got['rows']),c['id']
  wrong+=1;iswrong=True
  if w['kind']=='error':wrong_errors+=1
  else:wrong_diff+=1
 records.append({'id':c['id'],'correctPassed':True,'wrongRejected':iswrong if c['type']=='错误纠正' else None,'fixtureSha256':sha(p)})
assert count==125 and wrong==25
assert len(list((R/'fixtures').glob('fc*.json')))==125
assert len(list((R/'validation/mysql').glob('fc*.sql')))==125
for p in sorted((R/'course_links').glob('ch*.json')):
 for k in load(p)['knowledgePointUpdates']:
  assert set(k['relatedFlashcardIds'])=={c['id'] for c in cards if k['id'] in c['knowledgePointIds']}
  assert set(k['primaryFlashcardIds'])=={c['id'] for c in cards if k['id'] in c['primaryKnowledgePointIds']}
for q in load(R/'course_links/question_links.json')['questions']:
 assert set(q['relatedFlashcardIds'])=={c['id'] for c in cards if q['questionId'] in c['relatedQuestionIds']}
policy=load(R/'review_policy.json');assert [x['label'] for x in policy['ratings']]==['忘记','困难','记得','熟练'] and len(policy['modes'])==7 and policy['algorithm']=='FSRS' and not policy['policy']['hardcodedIntervals']
res={'status':'通过','sqliteVersion':sqlite3.sqlite_version,'cards':len(cards),'chapters':len(chapters),'correctExecuted':count,'wrongRejected':wrong,'wrongExecutionErrors':wrong_errors,'wrongDifferentResults':wrong_diff,'stateTests':state_tests,'fixtureHashMatches':count,'nativeScriptsPresentNotExecuted':native,'primaryKnowledgePoints':len(kp),'linkedQuestions':len(q) if isinstance(q,set) else len(source['questions']),'businessTasks':len(bp),'records':records}
(R/'validation/recheck.json').write_text(json.dumps(res,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');print({k:v for k,v in res.items() if k!='records'})
