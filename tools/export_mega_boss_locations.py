"""Export the shipped extra Mega spawn sets without changing spawning rules."""
from pathlib import Path
from zipfile import ZipFile
import argparse
import json
import re

ROOT = Path(__file__).resolve().parents[1]
LABELS = dict(forests='森林类', roofed='黑森林类', birches='白桦林类', taigas='针叶林类',
    mountainous='山地类', arid='干旱类', swamps='沼泽类', beaches='沙滩类', beach='沙滩类',
    mesas='恶地类', lakes='湖泊类', end='末地类', savannas='热带草原类', mushroom='蘑菇类',
    flowery='花卉类', magical='魔法类', freezing='冰冻类', jungles='丛林类', plains='平原类',
    deep_ocean='深海', deep_warm_ocean='暖水深海', deep_lukewarm_ocean='温水深海')
LABELS.update({'mountainous forests':'山地森林类','freezing forests':'冰冻森林类','freezing mountains':'冰冻山地类',
    'minecraft:river':'河流','minecraft:end_barrens':'末地荒地','minecraft:snowy_beach':'积雪沙滩',
    'minecraft:desert_hills':'沙漠丘陵','minecraft:savanna_plateau':'热带高原',
    'end_highlands':'末地高地','small_end_islands':'末地小型岛屿'})
LABELS.update({'all forests':'所有森林类','evil':'阴森类','oceanic':'海洋类','redwoods':'红杉林类',
    'minecraft:badlands':'恶地','minecraft:beach':'沙滩','minecraft:end_highlands':'末地高地',
    'minecraft:eroded_badlands':'风蚀恶地','minecraft:ice_spikes':'冰刺之地',
    'minecraft:modified_jungle':'丛林变种','minecraft:snowy_tundra':'积雪的冻原'})
TIMES = dict(DAY='白天', NIGHT='夜晚', DAWN='黎明', DUSK='黄昏', AFTERNOON='下午')
LOCATIONS = dict(Land='陆地', Water='水中', Air='空中', Underground='地下', Seafloor='海床', SurfaceWater='水面')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--native', type=Path, required=True)
    parser.add_argument('--all', action='store_true', help='Merge native and backported Mega spawn sets')
    args = parser.parse_args()
    with ZipFile(args.native) as z:
        lang = json.loads(z.read('assets/pixelmon/lang/zh_cn.json'))
        native = {n.rsplit('/', 1)[-1]: json.loads(z.read(n)) for n in z.namelist()
                  if n.startswith('data/pixelmon/spawning/megas/') and n.endswith('.json')}
    lang.update(json.loads((ROOT / 'resources/assets/pixelmon_backport/lang/zh_cn.json').read_text('utf-8')))
    rows = []
    paths = sorted((ROOT / 'resources/data/pixelmon/spawning/megas').glob('*.json'))
    entries = {name: (data, 'Pixelmon本体') for name, data in native.items()} if args.all else {}
    for path in paths:
        entries[path.name] = (json.loads(path.read_text('utf-8')), '回迁新增')
    primal_count = 0
    for filename, (data, source) in sorted(entries.items(), key=lambda pair: pair[1][0]['id']):
        for row in data['spawnInfos']:
            spec = row['spec']
            assert isinstance(spec, str), path
            species = re.search(r'species:([^ ]+)', spec).group(1)
            form = re.search(r'form:([^ ]+)', spec)
            name = lang.get('pixelmon.' + species.lower(), species)
            if filename.startswith('primal_'):
                name = '原始' + name
                primal_count += 1
            elif form and form.group(1) == 'megaz':
                name += ' Mega Z'
            else:
                name += ' Mega'
                if form and form.group(1) not in ('mega', 'az', 'complete'):
                    name += '（' + form.group(1) + '）'
            condition = row.get('condition', {})
            biomes = '、'.join((LABELS.get(b, b) + (' `' + b + '`' if b in LABELS else '')) for b in condition.get('stringBiomes', [])) or '未限定群系'
            times = '／'.join(TIMES.get(t, t) for t in condition.get('times', [])) or '未限定'
            places = '／'.join(LOCATIONS.get(t, t) for t in row.get('stringLocationTypes', [])) or '未指定'
            extra = {k: v for k, v in condition.items() if k not in ('stringBiomes', 'times')}
            if row.get('anticondition'):
                extra['排除条件'] = row['anticondition']
            details = []
            for key, value in extra.items():
                if key == 'maxY': details.append('最高Y：' + str(value))
                elif key == 'minY': details.append('最低Y：' + str(value))
                elif key == 'maxLightLevel': details.append('最高光照：' + str(value))
                else: details.append(key + '：' + json.dumps(value, ensure_ascii=False))
            row_values = (name, biomes, times, places, '；'.join(details) if details else '无额外限定', str(row.get('rarity', '')), filename)
            rows.append(row_values + (source,) if args.all else row_values)
    out = ROOT / ('docs/ALL_MEGA_BOSS_LOCATIONS_1.10.0.md' if args.all else 'docs/MEGA_BOSS_LOCATIONS_1.10.0.md')
    header = '# ' + ('全部' if args.all else '新增') + ' Mega Boss 刷新地点表（1.10.0）\n\n'
    if args.all:
        header += f'基于 Pixelmon 9.1.13 与回迁 1.10.0 当前配置，合并 {len(native)} 份本体表和 {len(paths)} 份回迁表，共 {len(entries)} 份有效配置、{len(rows)} 条记录。其中含同一 Mega Boss 生成器使用的 {primal_count} 条原始回归记录。\n\n'
        header += '按物种与原表明确指定的形态列出。原表未指定 X／Y 等形态的记录不拆成虚构的独立刷新表；本体和回迁中同名资源以回迁配置为准。本表汇总专用 Mega Boss 生成表，普通野生宝可梦随机成为 Boss 的路径还取决于其普通刷新表。\n\n'
    else:
        header += f'从当前随包的 {len(paths)} 份额外 Mega 刷怪配置逐条导出，共 {len(rows)} 条。本表不含 Pixelmon 本体原有的完整 Mega 清单，也不新增或修改刷怪规则。\n\n'
    header += '同一行的群系条件为候选范围；带“类”的是 Pixelmon 群系分类，可能涵盖多个原版或模组群系，保留原始标识供核对。未安装的模组群系不会凭空出现。时段“未限定”指该表没有额外限定；位置、地形和其他生成规则仍需满足。`rarity` 是候选权重，不是百分比，也不代表到点必出。\n\n'
    header += '来源：Pixelmon 本体 `data/pixelmon/spawning/megas/` 与回迁包同路径的配置文件。模组群系保留注册标识，便于准确区分不同模组的同名群系。\n\n'
    header += '| 宝可梦 | 群系／地点 | 时段 | 刷新位置 | 其他条件 | 权重 | 源配置 |' + (' 来源 |' if args.all else '') + '\n'
    header += '|---|---|---|---|---|---|---|' + ('---|' if args.all else '') + '\n'
    out.write_text(header + ''.join('| ' + ' | '.join(row) + ' |\n' for row in rows), encoding='utf-8')
    assert len(rows) == sum(len(data['spawnInfos']) for data, source in entries.values())
    print(f'{out}: {len(entries)} sets / {len(rows)} rows')


if __name__ == '__main__':
    main()
