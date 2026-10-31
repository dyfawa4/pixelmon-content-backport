"""Create a local, allowlisted 1.10.0 candidate; never publish or include player data."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import hashlib
import json

ROOT = Path(__file__).resolve().parents[1]
DIST = ROOT / 'dist'


def main():
    audit = json.loads((ROOT / '.cache/110/artifact-audit.json').read_text('utf-8'))
    core = DIST / audit['jar']
    assert hashlib.sha256(core.read_bytes()).hexdigest() == audit['sha256']
    assert (ROOT / audit['runtime_evidence']).read_text('utf-8').startswith('PASS ')
    addon = DIST / 'ohmygod-greninja-1.16.5-1.0.3.jar'
    files = {
        'mods/' + core.name: core,
        'mods/' + core.name + '.sha256': core.with_suffix('.jar.sha256'),
        '可选模组/' + addon.name: addon,
        '可选模组/' + addon.name + '.sha256': addon.with_suffix('.jar.sha256'),
        '可选模组/安装说明.md': ROOT / 'addons/ohmygod-greninja/README.md',
        '安装与更新说明.md': ROOT / 'docs/CANDIDATE_1.10.0.md',
        '新增MegaBoss刷新地点表.md': ROOT / 'docs/MEGA_BOSS_LOCATIONS_1.10.0.md',
        '全部MegaBoss刷新地点表.md': ROOT / 'docs/ALL_MEGA_BOSS_LOCATIONS_1.10.0.md',
        '食物与药品获取来源说明.md': ROOT / 'docs/ACQUISITION_JEI_1.10.0.md',
        '咖喱食材配方.md': ROOT / 'docs/CURRY_RECIPES_1.10.0.md',
        '钓鱼产物查询说明.md': ROOT / 'docs/FISHING_SOURCES_1.10.0.md',
        '果实收集器与说明更新.md': ROOT / 'docs/FRUIT_COLLECTOR_1.10.0.md',
        '进化条件说明.md': ROOT / 'docs/EVOLUTION_GUIDE_1.10.0.md',
        '钓竿附魔规则.md': ROOT / 'docs/FISHING_ENCHANTMENT_PLAN_1.10.0.md',
        'LICENSE.txt': ROOT / 'LICENSE.txt',
        'THIRD_PARTY.md': ROOT / 'THIRD_PARTY.md',
    }
    entries = {name: path.read_bytes() for name, path in files.items()}
    entries['SHA256SUMS.txt'] = ''.join(hashlib.sha256(data).hexdigest() + '  ' + name + '\n' for name, data in entries.items()).encode('utf-8')
    bundle = DIST / 'Pixelmon-Backport-1.10.0-candidate-r12.zip'
    with ZipFile(bundle, 'w', ZIP_DEFLATED) as z:
        for name, data in entries.items():
            z.writestr(name, data)
    with ZipFile(bundle) as z:
        assert z.testzip() is None
        assert set(z.namelist()) == set(entries)
        for name, data in entries.items():
            assert z.read(name) == data
    bundle.with_suffix('.zip.sha256').write_text(hashlib.sha256(bundle.read_bytes()).hexdigest() + '  ' + bundle.name + '\n', encoding='utf-8')
    print(f'Local candidate: {bundle}; {len(entries)} verified entries')


if __name__ == '__main__':
    main()
