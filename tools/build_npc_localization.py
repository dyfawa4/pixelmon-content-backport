"""Audit all native locale definitions, build a guaranteed reload and old-save text catalogue."""
import argparse,copy,json,re,zipfile
from pathlib import Path
from npc_zh_cn import leaves
from npc_names_zh_cn import VILLAGER_NAMES,SHOP_NAMES
from embedded_npc_zh_cn import NAMES,TEXTS
ROOT=Path(__file__).resolve().parents[1]
MISSING={
'ornithologist':['我带着飞行属性宝可梦周游世界。','我的飞行属性宝可梦真棒！','不，我出色的宝可梦！','要我说，鸟宝可梦最令人惊叹。','看到了吧，鸟宝可梦更胜一筹！','哎呀。算了，该起飞了！','我很想看看你有哪些宝可梦。','飞行属性宝可梦很强，难怪你会输。','看来其他属性的宝可梦也挺酷。'],
'femalerocketgrunt':['小鬼，交出你的宝可梦！','我赢了，现在交出宝可梦。','可恶，你怎么打败我的！','不打败我就别想通过！','你休想通过！','有胆量就过去吧。','你在这里干什么？！','离开这里。','糟糕，我输了！','这是重要的行动，不需要你到处窥探！','马上滚出去！','不可能！','火箭队最强！','我说过我们最强！','早知道该加入熔岩队。','准备好迎接麻烦！','我还能让麻烦加倍。','宝可梦真没用！','宝可梦是工具，善待它们有什么用？','看吧，它们毫无用处。','没用的东西！','我们是火箭队！','你是谁？','你为什么在这里？','你以为自己在这里干什么？','马上离开！','赶紧离开，别让我告诉老大！','别挡我的路！','我说了，走开！','不可能！','竟敢挑战我，你胆子不小！','你永远赢不了我。','你打败了我？！不——！','你赢不了，我是排名第二十七的队员！','太好了，现在能升到第二十六名！','糟了，现在要掉到第二十八名！','你的宝可梦能在黑市卖不少钱！','你的宝可梦真差劲！','它们可能比我想象的更值钱！','这里没什么可疑的，走开！','小鬼，你根本不知道自己在对付谁！','老大会对你很不满意。']}

def replace(v,values):
    if isinstance(v,str):return next(values)
    if isinstance(v,list):return [replace(x,values) for x in v]
    if isinstance(v,dict):return {k:replace(x,values) for k,x in v.items()}
    return v

def pair(en,zh,result):
    a,b=list(leaves(en)),list(leaves(zh));assert len(a)==len(b),(len(a),len(b))
    for x,y in zip(a,b):
        assert not re.search('[A-Za-z]',y) or re.search('[\u4e00-\u9fff]',y),(x,y)
        result.setdefault(x,y)

def main():
    p=argparse.ArgumentParser();p.add_argument('--pixelmon',required=True);args=p.parse_args()
    definitions={};stock={};texts=dict(TEXTS);names=dict(NAMES);total=0;named=0
    with zipfile.ZipFile(args.pixelmon) as jar:
        for path in jar.namelist():
            if not path.startswith('data/pixelmon/npcs/') or not path.endswith('_en_us.json'):continue
            target=ROOT/'resources'/path.replace('_en_us','_zh_cn');en=json.loads(jar.read(path))
            native_path=path.replace('_en_us','_zh_cn');native=json.loads(jar.read(native_path)) if native_path in jar.namelist() else en
            zh=json.loads(target.read_text(encoding='utf-8')) if target.exists() else json.loads(jar.read(path.replace('_en_us','_zh_cn')))
            kind=path.split('/')[3];key=Path(path).name[:-11]
            if kind=='trainers' and key in MISSING:zh['chat']=replace(en['chat'],iter(MISSING[key]))
            # Locale files must share English indices and gameplay, including native files.
            localized_chat=zh['chat'];localized_names=zh.get('names');zh=copy.deepcopy(en);zh['chat']=localized_chat
            table=VILLAGER_NAMES if kind=='villagers' else SHOP_NAMES if kind=='shopkeepers' else {}
            if key in table:
                translated=table[key].split('|');assert len(translated)==len(en['names']),(key,len(translated),len(en['names']))
                for source,row,text in zip(en['names'],zh['names'],translated):row['name']=text;names.setdefault(source['name'],text)
                assert {k:v for k,v in en.items() if k not in ('names','chat')}=={k:v for k,v in zh.items() if k not in ('names','chat')},path
                named+=1
            pair(en['chat'],zh['chat'],texts);total+=len(list(leaves(zh['chat'])))
            target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps(zh,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
            definitions[path[len('data/pixelmon/npcs/'):].replace('_en_us','_zh_cn')[:-5]]=zh
            stock[path[len('data/pixelmon/npcs/'):].replace('_en_us','_zh_cn')[:-5]]={k:native[k] for k in ('names','chat') if k in native}
    assert len(definitions)==178 and named==88,(len(definitions),named)
    output={'definitions':definitions,'stock':stock,'texts':texts,'names':names}
    (ROOT/'resources/backport/npc_localization.json').write_text(json.dumps(output,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    locale=ROOT/'resources/assets/pixelmon/lang/zh_cn.json'
    language=json.loads(locale.read_text(encoding='utf-8')) if locale.exists() else {}
    language.update(texts);language.update(names)
    locale.parent.mkdir(parents=True,exist_ok=True);locale.write_text(json.dumps(language,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'definitions':len(definitions),'lines':total,'named_definitions':named,'known_names':len(names),'known_texts':len(texts),'mechanics_unchanged':True}))

if __name__=='__main__':main()
