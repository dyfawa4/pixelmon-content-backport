"""Publishable 1.6.0 artifacts from built core and clean, reviewed map packages."""
import argparse
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import shutil
import struct
import subprocess
from zipfile import ZipFile, ZIP_DEFLATED

import nbtlib

ROOT = Path(__file__).resolve().parents[1]
DIST = ROOT / 'dist'
VERSION = '1.6.0'


def check_jar(data):
    with ZipFile(io.BytesIO(data)) as jar:
        assert jar.testzip() is None
        assert 'version="1.6.0"' in jar.read('META-INF/mods.toml').decode().replace(' ', '')
        assert json.loads(jar.read('pack.mcmeta'))['pack']['pack_format'] == 6
        classes = [n for n in jar.namelist() if n.endswith('.class')]
        assert classes and all(struct.unpack('>H', jar.read(n)[6:8])[0] == 55 for n in classes)
        return jar.read('META-INF/mods.toml').decode()


def create_zip(path, entries):
    with ZipFile(path, 'w', ZIP_DEFLATED, compresslevel=6) as z:
        for name, data in sorted(entries.items()):
            z.writestr(name, data)
    with ZipFile(path) as z:
        assert z.testzip() is None


def clean_world(name, data):
    parts = PurePosixPath(name).parts
    assert len(parts) >= 3 and parts[0] == 'saves' and '..' not in parts
    assert not any(p in {'playerdata', 'stats', 'advancements', 'pokemon', 'ftbteams', 'ftbchunks', 'logs'} for p in parts)
    assert parts[-1] not in {'session.lock', 'usercache.json', 'options.txt', 'servers.dat'}
    if parts[-1] == 'level.dat':
        assert 'Player' not in nbtlib.File.from_fileobj(__import__('gzip').GzipFile(fileobj=io.BytesIO(data)))['Data']


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source-ref', required=True, help='Exact committed source to archive')
    args = parser.parse_args()
    core = DIST / 'pixelmon-content-backport-1.16.5-1.6.0.jar'
    core_data = core.read_bytes()
    check_jar(core_data)
    maps = [
        ('test-maps-1.6.0.zip', 9), ('gym-test-maps-1.6.0.zip', 21),
        ('cap-test-map-1.6.0.zip', 1), ('npc-quest-test-map-1.6.0.zip', 1),
        ('tera-visual-test-map-1.6.0-r1.zip', 1), ('riding-test-map-1.6.0-r1.zip', 1),
    ]
    # Give the current Greninja candidate an unambiguous final download name.
    shutil.copy2(DIST / 'Pixelmon-Backport-greninja-test-map-1.6.0-r10.zip',
                 DIST / 'Pixelmon-Backport-greninja-test-map-1.6.0.zip')
    maps.append(('greninja-test-map-1.6.0.zip', 1))
    mega_entries = {'mods/' + core.name: core_data,
                    'mods/backport-mega-test-1.16.5-1.6.0.jar':
                    (ROOT / '.cache/za-mega-160/backport-mega-test-1.16.5-1.6.0.jar').read_bytes()}
    for p in (ROOT / 'test-maps/mega-saves-v160').rglob('*'):
        if p.is_file():
            mega_entries['saves/' + p.relative_to(ROOT / 'test-maps/mega-saves-v160').as_posix()] = p.read_bytes()
    mega_entries['README.md'] = ('''# 1.6.0 Mega 展示测试图

使用独立 Minecraft 1.16.5 / Forge 36.2.42 / Java 11 实例，自备支持的 Pixelmon 本体。
将本包 mods 和 saves 合入实例；正式存档不需要测试辅助模组。
进入“轻快之旅 1.6.0 Mega补全测试”，自动准备 60 个展示位，覆盖 49 个 ZA 条目及来源／闪光对照。
展示对象有专用防清除标记，远离、区块重载后仍保留；不改变其他野生宝可梦的规则。
包含原始颜色非 Mega 玛机雅娜和三种米立龙原形。Mega Boss 禁止捕捉，匹配石头掉落。
仅该命名测试世界会准备场地，其他世界不修改；不会自动开战或替玩家选招。
''').encode()
    create_zip(DIST / 'Pixelmon-Backport-mega-test-map-1.6.0.zip', mega_entries)
    maps.append(('mega-test-map-1.6.0.zip', 1))
    helpers, all_worlds, offline = {}, set(), {}
    for suffix, expected in maps:
        path = DIST / ('Pixelmon-Backport-' + suffix)
        with ZipFile(path) as z:
            assert z.testzip() is None
            assert z.read('mods/' + core.name) == core_data, ('stale core', suffix)
            worlds = {n.split('/')[1] for n in z.namelist() if n.startswith('saves/') and n.endswith('/level.dat')}
            assert len(worlds) == expected, (suffix, worlds)
            for name in z.namelist():
                if name.endswith('/'):
                    continue
                data = z.read(name)
                if name.startswith('mods/') and name.endswith('.jar'):
                    check_jar(data)
                    if name in helpers and helpers[name] != data:
                        with ZipFile(io.BytesIO(helpers[name])) as previous, ZipFile(io.BytesIO(data)) as current:
                            assert {n: previous.read(n) for n in previous.namelist()} == {n: current.read(n) for n in current.namelist()}, ('helper collision', name)
                    helpers[name] = data
                if name.startswith('saves/'):
                    clean_world(name, data)
                    # The offline sampler retains only the user-approved rock city.
                    if suffix.startswith('gym-') and name.split('/')[1] != 'backport-gym-v143-rock-vanilla':
                        continue
                    assert name not in offline
                    offline[name] = data
                    all_worlds.add(name.split('/')[1])
    assert len(all_worlds) == 16
    offline.update(helpers)
    test_mods = dict(helpers)
    test_mods['README.md'] = '可选测试组件：请仅在独立测试实例使用，正式游戏只装核心 JAR。地图请另下载同版本地图包。\n'.encode()
    create_zip(DIST / 'Pixelmon-Backport-test-mods-1.6.0.zip', test_mods)
    documents = ['README.md', 'HANDBOOK.md', 'TEST_MAPS.md', 'GYM_TEST_MAPS.md', 'GYM_GENERATION.md',
                 'FEATURE_TEST_MAPS.md', 'BUILDING.md', 'GRENINJA_TEST_MAP.md', 'RIDING_TEST_MAP.md',
                 'TERA_VISUAL_TEST_MAP.md', 'NPC_QUEST_TEST_MAP.md', 'CAP_TEST_MAP.md', 'LICENSE.txt', 'THIRD_PARTY.md']
    # Include linked historical reference documents without any local logs or client files.
    documents += [p.relative_to(ROOT).as_posix() for p in (ROOT / 'docs').rglob('*') if p.is_file()]
    public_docs = {name: (ROOT / name).read_bytes() for name in documents}
    feature = {'mods/' + core.name: core_data, **public_docs}
    offline.update(public_docs)
    for p in (ROOT / 'pack-overrides').rglob('*.snbt'):
        name = p.relative_to(ROOT / 'pack-overrides').as_posix()
        feature[name] = offline[name] = p.read_bytes()
    offline['README.md'] = ('''# 1.6.0 离线测试包

包含同版本核心、6 个可选测试辅助模组和 16 张干净地图：9 张功能、已验收岩石城、帽皮、NPC任务、太晶外观、乘骑、忍蛙、Mega图鉴。
自备 Minecraft 1.16.5、Forge 36.2.42、Java 11 及支持的 Pixelmon 本体。
使用独立实例；正式存档只装核心 JAR。退出游戏后安装，移除旧回迁与旧测试组件。
不含账号、玩家进度或游戏本体。忍蛙与更新详细说明见 docs/GRENINJA_1.6.0.md 和 docs/RELEASE_1.6.0.md。
''').encode()
    offline['构建信息.json'] = json.dumps({'version': VERSION, 'source_commit': args.source_ref,
        'core_sha256': hashlib.sha256(core_data).hexdigest(), 'worlds': sorted(all_worlds),
        'player_data_included': False}, ensure_ascii=False, indent=2).encode()
    create_zip(DIST / 'Pixelmon-Backport-1.6.0.zip', feature)
    create_zip(DIST / 'Pixelmon-Backport-offline-test-1.6.0.zip', offline)
    copies = {'HANDBOOK.md': 'Handbook', 'docs/CAP_PIKACHU_1.6.0.md': 'Cap-Pikachu', 'docs/GRENINJA_1.6.0.md': 'Greninja',
              'docs/RELEASE_1.6.0.md': 'Release-Notes'}
    for source, label in copies.items():
        shutil.copy2(ROOT / source, DIST / f'Pixelmon-Backport-{label}-1.6.0.md')
    source_zip = DIST / 'Pixelmon-Backport-source-1.6.0.zip'
    subprocess.run(['git', 'archive', '--format=zip', f'--output={source_zip}', args.source_ref], cwd=ROOT, check=True)
    with ZipFile(source_zip) as z:
        assert z.testzip() is None
        assert not any(n.startswith(('.git/', '.cache/', 'dist/')) or n.endswith(('.jar', '.class', '.log')) for n in z.namelist())
        for name in ('src/local/pixelmon/backport/Erosion.java', 'resources/backport/content.json',
                     'docs/GRENINJA_1.6.0.md', 'tools/package_release_160.py'):
            assert z.read(name) == (ROOT / name).read_bytes()
    artifacts = [core, DIST / 'Pixelmon-Backport-1.6.0.zip', source_zip,
                 DIST / 'Pixelmon-Backport-offline-test-1.6.0.zip', DIST / 'Pixelmon-Backport-test-mods-1.6.0.zip']
    artifacts += [DIST / ('Pixelmon-Backport-' + s) for s, _ in maps]
    artifacts += [DIST / f'Pixelmon-Backport-{label}-1.6.0.md' for label in copies.values()]
    lines = []
    for p in artifacts:
        line = f'{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.name}\n'
        p.with_name(p.name + '.sha256').write_text(line, encoding='utf-8')
        lines.append(line)
    manifest = DIST / 'SHA256SUMS-1.6.0.txt'
    manifest.write_text(''.join(lines), encoding='utf-8')
    (ROOT / '.cache/release-160-assets.json').write_text(json.dumps([str(p) for p in artifacts] + [str(manifest)], indent=2), encoding='utf-8')
    print(f'PASS 1.6.0 release: {len(artifacts)} assets + checksums; exact core in every map; Java 11; 16 clean offline worlds; source={args.source_ref}')


if __name__ == '__main__':
    main()
