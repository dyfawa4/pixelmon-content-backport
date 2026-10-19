"""Generate only legendary/mythical palette achievements present in the supplied quest book."""
import argparse,json
from pathlib import Path
from zipfile import ZipFile
from audit_quest_money import Parser

ROOT=Path(__file__).resolve().parents[1]
def collect(quests,pixelmon):
    with ZipFile(pixelmon) as jar:
        species={d['dex']:d for name in jar.namelist() if name.startswith('data/pixelmon/species/') and name.endswith('.json') for d in [json.loads(jar.read(name))]}
    result=[]
    for path in sorted(Path(quests).glob('*.snbt')):
        for quest in Parser(path.read_text(encoding='utf-8-sig')).parse().get('quests',[]):
            for task in quest.get('tasks',[]):
                item=task.get('item',{})
                if not isinstance(item,dict) or item.get('id')!='pixelmon:pixelmon_sprite':continue
                tag=item.get('tag',{});palette=tag.get('palette');dex=tag.get('ndex')
                if palette in (None,'none','shiny') or dex not in species:continue
                data=species[dex]
                if not any(t in data['forms'][0].get('tags',[]) for t in ('legendary','mythical')):continue
                form=tag.get('form','');definition=next((f for f in data['forms'] if f['name']==form),None)
                if definition is None or not any(p['name']==palette for g in definition.get('genderProperties',data['forms'][0].get('genderProperties',[])) for p in g.get('palettes',[])):
                    raise ValueError((dex,form,palette,'quest appearance not present in actual Pixelmon data'))
                row=dict(id=f'palette_{dex}_{palette}'+('_'+form if form else ''),title=quest['title'],quest=quest['id'],chapter=path.name,dex=dex,species=data['name'],palette=palette,form=form,gender=tag.get('gender',2))
                if any(r['id']==row['id'] for r in result):raise ValueError(('duplicate appearance task',row))
                result.append(row)
    return result

def main():
    ap=argparse.ArgumentParser();ap.add_argument('--quests',type=Path,required=True);ap.add_argument('--pixelmon',type=Path,required=True);args=ap.parse_args()
    rows=collect(args.quests,args.pixelmon)
    def write(path,obj):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    write(ROOT/'resources/backport/special_palette_advancements.json',{'appearances':rows})
    names=json.loads((ROOT/'resources/backport/species_names_zh.json').read_text(encoding='utf-8'))
    for r in rows:
        nbt='{ndex:%ss,palette:"%s",form:"%s",gender:%sb}'%(r['dex'],r['palette'],r['form'],r['gender'])
        write(ROOT/f'resources/data/pixelmon_backport/advancements/legends/{r["id"]}.json',{
            'parent':'pixelmon_backport:legends/forms',
            'display':{'icon':{'item':'pixelmon:pixelmon_sprite','nbt':nbt},'title':{'text':r['title']},'description':{'text':f'获得任务书中对应特殊材质的{names.get("pixelmon."+r["species"].lower(),r["species"])}，并存入自己的队伍或电脑。'},'frame':'challenge' if r['id']=='palette_249_drowned' else 'task','show_toast':True,'announce_to_chat':True,'hidden':True},
            'criteria':{'earned':{'trigger':'minecraft:impossible'}}})
    print(f'Generated {len(rows)} quest-named special legendary palette advancements.')
if __name__=='__main__':main()
