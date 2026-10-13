"""Broaden species collection wording without touching capture-stat or disguise tasks."""
import argparse
import json
from pathlib import Path
from zipfile import ZipFile

ROOT=Path(__file__).resolve().parents[1]

def write(path,data):path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def main(jar):
    audit=[]
    with ZipFile(jar) as z:
        locales={lang:json.loads(z.read(f'assets/pixelmon/lang/{lang}.json')) for lang in ('zh_cn','en_us')}
        overlays={lang:json.loads((ROOT/f'resources/assets/pixelmon_backport/lang/{lang}.json').read_text(encoding='utf-8-sig')) for lang in locales}
        for path in z.namelist():
            if not path.startswith('data/pixelmon/advancements/') or not path.endswith('.json'):continue
            adv=json.loads(z.read(path));display=adv.get('display',{});description=display.get('description',{})
            if not isinstance(description,dict) or 'translate' not in description:continue
            species=False;special=False
            for criterion in adv.get('criteria',{}).values():
                trigger=criterion.get('trigger','');conditions=criterion.get('conditions',{})
                if trigger=='pixelmon:legendary_capture_trigger':species=True
                elif trigger=='pixelmon:spec_trigger':
                    spec=str(conditions.get('pokemon',conditions.get('spec','')))
                    if 'disguise' in spec.lower() or 'ditto' in path.lower():special=True
                    elif spec:species=True
            if not species or special:continue
            key=description['translate'];audit.append((path,key))
            for lang in locales:
                original=overlays[lang].get(key,locales[lang].get(key))
                if not original:continue
                changed=original
                replacements=[('捕捉到','获得'),('捕捉','获得'),('捕获','获得'),('抓住','获得')] if lang=='zh_cn' else [('Successfully capture','Successfully obtain'),('Successfully catch','Successfully obtain'),('Capture','Obtain'),('capture','obtain'),('Catch','Obtain'),('catch','obtain'),('Caught','Obtained'),('caught','obtained')]
                for before,after in replacements:changed=changed.replace(before,after)
                if changed!=original:overlays[lang][key]=changed
        for lang,overlay in overlays.items():
            if lang=='zh_cn':
                for name,label in [('articuno','急冻鸟'),('zapdos','闪电鸟'),('moltres','火焰鸟')]:overlay[f'advancements.{name}_capture.description']=f'获得一只普通形态的{label}。伽勒尔形态不计入。'
            else:
                for name,label in [('articuno','Articuno'),('zapdos','Zapdos'),('moltres','Moltres')]:overlay[f'advancements.{name}_capture.description']=f'Obtain a regular {label}. Galarian forms do not count.'
            write(ROOT/f'resources/assets/pixelmon_backport/lang/{lang}.json',overlay)
    count=0
    for path in (ROOT/'resources/data/pixelmon_backport/advancements/legends').glob('*.json'):
        adv=json.loads(path.read_text(encoding='utf-8-sig'));desc=adv.get('display',{}).get('description',{})
        if 'text' not in desc:continue
        old=desc['text'];desc['text']=old.replace('捕捉','获得').replace('捕获','获得')
        if desc['text']!=old:write(path,adv);count+=1
    (ROOT/'.cache/v14-api/acquisition-text-audit.json').write_text(json.dumps(audit,ensure_ascii=False,indent=2),encoding='utf-8')
    print(f'Updated {count} backport descriptions; inspected {len(audit)} native collection descriptions; capture-stat and disguise tasks preserved.')

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--pixelmon',required=True);main(parser.parse_args().pixelmon)
