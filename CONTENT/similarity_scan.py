from pathlib import Path
import json,re,collections
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
r=Path(__file__).resolve().parent; D=lambda p:json.loads(p.read_text());
ks=[k for p in sorted((r/'courses_resource/courses').glob('ch*.json')) for k in D(p)['knowledgePoints']];qs=[q for p in sorted((r/'question_resource/questions').glob('ch*.json')) for q in D(p)['questions']];fs=[f for p in sorted((r/'flashcard_resource/flashcards').glob('ch*.json')) for f in D(p)['cards']];
records=[(k['id'],'课程',k['title']+' '+k['concept']) for k in ks]+[(q['id'],'题目',q['title']+' '+' '.join(x for x in q['requirements'].split('\n') if not x.startswith(('不运行下方SQL','补全代码中的','下面提供的初始代码','输出列名称','输出字段名称')))) for q in qs]+[(f['id'],'闪卡',f['front']['question']) for f in fs]
texts=[re.sub(r'\s+','',x[2]).lower() for x in records]; M=TfidfVectorizer(analyzer='char',ngram_range=(2,4),min_df=1).fit_transform(texts); sim=cosine_similarity(M); pairs=[]
for i in range(len(records)):
 for j in range(i):
  if sim[i,j]>=.65:pairs.append({'id1':records[j][0],'id2':records[i][0],'type1':records[j][1],'type2':records[i][1],'similarity':round(float(sim[i,j]),4),'text1':records[j][2],'text2':records[i][2]})
pairs.sort(key=lambda x:-x['similarity']); print('total',len(pairs),collections.Counter((p['type1'],p['type2']) for p in pairs));
for p in pairs:
 if p['type1']==p['type2']:print(p['id1'],p['id2'],p['similarity'],p['text1'][:140],p['text2'][:140])
(r/'similarity.json').write_text(json.dumps({'method':'字符2至4元组 TF-IDF 余弦相似度；阈值0.65，仅候选筛查，不等价于语义判定','items':len(records),'pairsAboveThreshold':len(pairs),'pairs':pairs},ensure_ascii=False,indent=2))
