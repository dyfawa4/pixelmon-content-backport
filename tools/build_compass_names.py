"""Build a Chinese catalogue for the actual compass/mod versions; no game access."""
import argparse, json, pathlib, zipfile

ROOT=pathlib.Path(__file__).resolve().parents[1]
def read(path): return json.loads(path.read_text(encoding='utf-8-sig'))
def language(jar, namespace, locale):
    with zipfile.ZipFile(jar) as archive:
        return json.loads(archive.read(f'assets/{namespace}/lang/{locale}.json'))
def write(path, data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def main():
    p=argparse.ArgumentParser()
    for name in ['pixelmon','vanilla-zh','bop','explorers','nature']:p.add_argument('--'+name,type=pathlib.Path,required=True)
    a=p.parse_args();pixel=read(ROOT/'resources/assets/pixelmon/lang/zh_cn.json')
    vanilla=read(a.vanilla_zh);bop=language(a.bop,'biomesoplenty','zh_cn')
    extra={'muskeg':'寒地泥炭沼泽','dense_marsh':'茂密湿地','dense_woodland':'茂密林地','grassland_clover_patch':'三叶草草原','shroomy_wetland':'蘑菇湿地','wetland_forest':'湿地森林','shield':'地盾高地','tall_dead_forest':'高大枯木林'}
    bop.update({'biome.biomesoplenty.'+k:v for k,v in extra.items()})
    structure={k[len('structure.'):].replace('.',':',1):v for k,v in pixel.items() if k.startswith('structure.')}
    explorer=read(ROOT/'resources/assets/explorerscompass/lang/zh_cn.json')
    structure.update({k[len('structure.'):].replace('.',':',1):v for k,v in explorer.items() if k.startswith('structure.')})
    backport=read(ROOT/'resources/assets/pixelmon_backport/lang/zh_cn.json')
    structure.update({k[len('structure.'):].replace('.',':',1):v for k,v in backport.items() if k.startswith('structure.')})
    biome={}
    for lang in [vanilla,bop,language(a.pixelmon,'pixelmon','zh_cn')]:
        biome.update({k[len('biome.'):].replace('.',':',1):v for k,v in lang.items() if k.startswith('biome.')})
    dimensions={'minecraft:overworld':'主世界','minecraft:the_nether':'下界','minecraft:the_end':'末地','pixelmon:ultraspace':'究极空间','pixelmon:ultra_space':'究极空间','pixelmon:drownedworld':'溺水世界','pixelmon:drowned_world':'溺水世界'}
    source={'minecraft':'我的世界','pixelmon':'宝可梦重铸','pixelmon_backport':'重铸内容回迁','biomesoplenty':'超多生物群系'}
    data={'structure':structure,'biome':biome,'dimension':dimensions,'source':source}
    for group in data.values():
        assert all(any('\u4e00'<=c<='\u9fff' for c in value) for value in group.values()), 'Untranslated catalogue value'
    for jar,ns in [(a.explorers,'explorerscompass'),(a.nature,'naturescompass')]:
        target=ROOT/f'resources/assets/{ns}/lang/zh_cn.json'
        zh=read(target) if target.exists() else language(jar,ns,'zh_cn')
        if ns=='naturescompass':zh.update({'string.naturescompass.highHumidity':'高湿度','string.naturescompass.topBlock':'地表方块','string.naturescompass.fillerBlock':'地基方块'})
        en=language(jar,ns,'en_us');assert all(k.startswith('_') or k in zh for k in en),ns
        if ns=='explorerscompass':zh.update({'structure.'+k.replace(':','.',1):v for k,v in structure.items()});zh.update({'dimension.'+k.replace(':','.',1):v for k,v in dimensions.items()})
        else:zh.update({'biome.'+k.replace(':','.',1):v for k,v in biome.items()})
        write(target,zh)
    expected={k for k in language(a.bop,'biomesoplenty','en_us') if k.startswith('biome.')}
    missing=sorted(k for k in expected if k[len('biome.'):].replace('.',':',1) not in biome)
    assert not missing,missing
    write(ROOT/'resources/backport/compass_names.json',data)
    print('Compass Chinese names:',{k:len(v) for k,v in data.items()},'; complete native interface key coverage.')
if __name__=='__main__':main()
