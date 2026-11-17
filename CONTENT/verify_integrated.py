#!/usr/bin/env python3
# 复验整合版，不依赖原始制作目录和网络。题库复验会创建临时数据库副本。
from pathlib import Path
import json,sqlite3,hashlib,collections,itertools,subprocess,sys
R=Path(__file__).resolve().parent;C=R/'courses_resource';Q=R/'question_resource';F=R/'flashcard_resource';E=R/'enterprise_reference'
def load(p):return json.loads(p.read_text(encoding='utf-8'))
def sha(p):
 h=hashlib.sha256()
 with p.open('rb') as f:
  for b in iter(lambda:f.read(1024*1024),b''):h.update(b)
 return h.hexdigest()
def main():
 for row in load(R/'manifest.json')['files']:
  p=R/row['path'];assert p.is_file() and sha(p)==row['sha256'],row['path']
 ks=[k for p in sorted((C/'courses').glob('ch*.json')) for k in load(p)['knowledgePoints']];qs=[q for p in sorted((Q/'questions').glob('ch*.json')) for q in load(p)['questions']];fs=[f for p in sorted((F/'flashcards').glob('ch*.json')) for f in load(p)['cards']]
 ki={k['id']:k for k in ks};qi={q['id']:q for q in qs};fi={f['id']:f for f in fs};assert len(ks)==len(ki)==152 and len(qs)==len(qi)==300 and len(fs)==len(fi)==200
 assert [len(load(Q/'questions'/f'ch{i:02}.json')['questions']) for i in range(1,21)]==[8,12,22,8,10,15,18,25,12,20,10,15,8,12,25,10,10,10,30,20]
 for k in ks:
  assert set(k['relatedQuestionIds'])=={q['id'] for q in qs if k['id'] in q['knowledgePointIds']};assert set(k['relatedFlashcardIds'])=={f['id'] for f in fs if k['id'] in f['knowledgePointIds']};assert set(k['primaryQuestionIds'])=={q['id'] for q in qs if k['id'] in q['primaryKnowledgePointIds']};assert set(k['primaryFlashcardIds'])=={f['id'] for f in fs if k['id'] in f['primaryKnowledgePointIds']};assert (C/k['mysqlVerificationScript']).is_file()
 for q in qs:
  assert set(q['relatedFlashcardIds'])=={f['id'] for f in fs if q['id'] in f['relatedQuestionIds']}
  if q.get('expectedOutputPath'):assert (Q/q['expectedOutputPath'].split('#')[0]).is_file()
 db={x['id']:Q/x['path'] for x in load(Q/'databases/manifest.json')['datasets']};conns={};passed=0;theory=0;biz=0
 for k in ks:
  sql=k['sqliteSql'];res=k['result']
  if not sql:theory+=1;continue
  if k.get('taskId'):
   tid=k['taskId'];t=load(E/'business_tasks'/f'{tid}.json');q=next(q for q in qs if q.get('taskId')==tid);assert k['datasetId']==q['datasetId']==t['datasetId'];assert sql==t['sqliteSql']==q['referenceSolutions'][0]['sqliteSql'];assert k['mysqlSql']==t['mysqlSql']==q['referenceSolutions'][0]['mysqlSql'];assert q['requirements'].startswith(t['requirements']);assert q['difficulty']==k['difficulty']==t['difficulty'];d=k['datasetId']
   if d not in conns:conns[d]=sqlite3.connect(db[d].as_uri()+'?mode=ro',uri=True)
   cur=conns[d].execute(sql);rows=[list(x) for x in cur];cols=[x[0] for x in cur.description];full=load(C/res['fullResultPath']);assert rows==q['expectedOutput']['rows']==full['rows'] and cols==q['expectedOutput']['columns']==full['columns'];assert rows[:len(res['rows'])]==res['rows'] and len(rows)==res['totalRows'];biz+=1
  else:
   c=sqlite3.connect(':memory:');c.execute('PRAGMA foreign_keys=ON')
   for fid in k['fixtureIds']:
    f=load(C/'fixtures'/f'{fid}.json');c.execute('CREATE TABLE "'+f['table']+'"('+','.join('"'+a['name']+'" '+a['sqliteType'] for a in f['columns'])+')');c.executemany('INSERT INTO "'+f['table']+'" VALUES('+','.join('?' for _ in f['columns'])+')',f['rows'])
   c.commit()
   if k.get('setupSql'):c.executescript(k['setupSql'])
   cur=c.execute(sql);assert [list(x) for x in cur]==res['rows'] and [x[0] for x in cur.description]==res['columns'],k['id'];c.close()
  passed+=1
 for c in conns.values():c.close()
 assert (passed,theory,biz)==(151,1,50)
 em=sqlite3.connect(db['enterprise-v2'].as_uri()+'?mode=ro',uri=True);shared=0
 for id,p in db.items():
  c=sqlite3.connect(p.as_uri()+'?mode=ro',uri=True);assert c.execute('PRAGMA integrity_check').fetchall()==[('ok',)] and not c.execute('PRAGMA foreign_key_check').fetchall()
  if id!='enterprise-v2':
   for t, in c.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"):
    n=len(c.execute('PRAGMA table_info("'+t+'")').fetchall());s='SELECT * FROM "'+t+'" ORDER BY '+','.join(map(str,range(1,n+1)));assert all(a==b for a,b in itertools.zip_longest(c.execute(s),em.execute(s)));shared+=1
  c.close()
 for x in load(E/'validation/checks.json')['businessChecks']:assert em.execute(x['sql']).fetchone()[0]==0,x['name']
 em.close();print('课程',passed,'理论',theory,'业务',biz,'共享表核对',shared,flush=True)
 # 原验证程序会刷新 recheck.json；重新运行后如日志内容改变，应重建发布清单，而非把旧清单当成新版本。
 for p in [Q/'validation/verify_bank.py',F/'validation/verify_flashcards.py']:subprocess.run([sys.executable,str(p)],check=True)
 print('整合复验通过；原生 MySQL 未执行。',flush=True)
if __name__=='__main__':main()
