"""Package the reviewed r8 core and an exact archive of committed 1.9.0 sources."""
import argparse
import hashlib
import io
import json
import shutil
import struct
import subprocess
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

from package_release_161 import verify_source

ROOT = Path(__file__).resolve().parents[1]
DIST = ROOT / 'dist'
VERSION = '1.9.0'
CORE_SHA256 = 'e29d13eda8603db8fd60bbd969dcf52acd4fb450044c9caff59f6619ec0b352f'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source-ref', required=True)
    args = parser.parse_args()
    ref = subprocess.check_output(['git', 'rev-parse', args.source_ref + '^{commit}'], cwd=ROOT).decode().strip()
    core = DIST / f'pixelmon-content-backport-1.16.5-{VERSION}.jar'
    data = core.read_bytes()
    assert sha(data) == CORE_SHA256, 'Reviewed r8 core must remain unchanged'
    with ZipFile(io.BytesIO(data)) as jar:
        assert jar.testzip() is None
        assert 'version="1.9.0"' in jar.read('META-INF/mods.toml').decode()
        names = jar.namelist()
        classes = [n for n in names if n.endswith('.class')]
        assert len(classes) == 838
        assert all(struct.unpack('>H', jar.read(n)[6:8])[0] == 55 for n in classes)
        assert not any(n.startswith('qa/') or any(x in n.lower() for x in ('autoreel', 'autofishing', 'super_rod_auto_reel')) for n in names)
        for path in (ROOT / 'resources').rglob('*'):
            if path.is_file():
                name = path.relative_to(ROOT / 'resources').as_posix()
                if name != 'META-INF/MANIFEST.MF':
                    assert jar.read(name) == path.read_bytes(), name

    entries = {'mods/' + core.name: data,
               'mods/' + core.name + '.sha256': (sha(data) + '  ' + core.name + '\n').encode()}
    documents = ['README.md', 'HANDBOOK.md', 'BUILDING.md', 'LICENSE.txt', 'THIRD_PARTY.md',
                 'docs/RELEASE_1.9.0.md', 'docs/VALIDATION_1.9.0_RELEASE.md',
                 'docs/DIVINE_SWORD_1.9.0.md']
    for name in documents:
        entries[name] = (ROOT / name).read_bytes()
    config = ROOT / 'pack-overrides/config/ironfurnaces-client.toml'
    assert 'updates = false' in config.read_text('utf-8')
    entries['config/ironfurnaces-client.toml'] = config.read_bytes()
    for name in ['divine-sword-190.png','divine-sword-190-r1.png','divine-sword-190-r2.png']:
        entries['docs/previews/'+name] = (ROOT/'docs/previews'/name).read_bytes()
    addon = DIST / 'ohmygod-greninja-1.16.5-1.0.2.jar'
    with ZipFile(addon) as jar:
        assert jar.testzip() is None
        metadata = jar.read('META-INF/mods.toml').decode()
        assert '1.0.2' in metadata and '[1.7.0,1.10.0)' in metadata
    entries['可选模组/' + addon.name] = addon.read_bytes()
    entries['可选模组/安装说明.md'] = (ROOT / 'addons/ohmygod-greninja/README.md').read_bytes()
    entries['安装说明.txt'] = (
        '重铸内容回迁 1.9.0\n'
        'Minecraft 1.16.5 / Forge 36.2.42 / Java 11 / Pixelmon 9.1.12～9.1.13。\n'
        '关闭游戏及服务端后，双方删除旧回迁核心，只保留mods中的1.9.0。保留Pixelmon本体、依赖和存档。\n'
        '把config目录一并覆盖到实际客户端实例并重启，以关闭万能熔炉更新提醒；仅更新JAR不会应用配置。\n'
        '可选模组默认不启用。启用时双方另行安装可选目录中的1.0.2，替换旧1.0.0／1.0.1。\n'
        '无需独立资源包；不覆盖个人任务进度。\n'
        '本包不是完整整合包，不含游戏本体、个人存档或自动测试助手。完整更新见docs/RELEASE_1.9.0.md。\n'
    ).encode('utf-8-sig')
    entries['可选模组/' + addon.name + '.sha256'] = (sha(addon.read_bytes())+'  '+addon.name+'\n').encode()
    entries['构建信息.json'] = json.dumps({
        'version': VERSION, 'source_commit': ref, 'core_sha256': sha(data),
        'java11_classes': len(classes), 'network_protocol': 19, 'reviewed_candidate': 'r8',
        'optional_addon_enabled_by_default': False, 'whole_regression_rerun_for_publication': False,
        'includes_game_or_player_data': False,
    }, ensure_ascii=False, indent=2).encode()
    entries['SHA256SUMS.txt'] = ''.join(sha(value)+'  '+name+'\n' for name,value in entries.items()).encode()
    package = DIST / f'Pixelmon-Backport-{VERSION}.zip'
    with ZipFile(package, 'w', ZIP_DEFLATED, compresslevel=6) as bundle:
        for name, content in entries.items():
            bundle.writestr(name, content)
    with ZipFile(package) as bundle:
        assert bundle.testzip() is None
        assert len(bundle.namelist()) == len(entries)
        for name, content in entries.items():
            assert bundle.read(name) == content, name
        jars = {n for n in bundle.namelist() if n.endswith('.jar')}
        assert jars == {'mods/' + core.name, '可选模组/' + addon.name}

    source = DIST / f'Pixelmon-Backport-source-{VERSION}.zip'
    subprocess.run(['git', '-c', 'core.autocrlf=false', '-c', 'core.eol=lf', 'archive',
                    '--format=zip', f'--output={source}', ref], cwd=ROOT, check=True)
    verify_source(source, ref)
    with ZipFile(source) as bundle:
        assert not any(n.startswith('local-only/') for n in bundle.namelist())
    with ZipFile(source) as bundle:
        assert 'src/local/pixelmon/backport/BossSpawnCheckItem.java' in bundle.namelist()
        assert 'resources/assets/pixelmon_backport/textures/item/boss_spawn_checker.png' in bundle.namelist()
        assert not any(n.startswith('docs/data/') or n.startswith('docs/AI_') or 'SESSION_HANDOFF_2026-10' in n for n in bundle.namelist())
    artifacts = [core, package, source, addon]
    for origin, label in [('HANDBOOK.md', 'Handbook'), ('docs/RELEASE_1.9.0.md', 'Release-Notes')]:
        target = DIST / f'Pixelmon-Backport-{label}-{VERSION}.md'
        shutil.copyfile(ROOT / origin, target)
        artifacts.append(target)
    checksum = DIST / f'SHA256SUMS-{VERSION}.txt'
    checksum.write_text(''.join(sha(p.read_bytes()) + '  ' + p.name + '\n' for p in artifacts), 'utf-8')
    manifest = {
        'version': VERSION, 'source_commit': ref, 'core_sha256': sha(data),
        'artifacts': [{'path': str(p), 'size': p.stat().st_size, 'sha256': sha(p.read_bytes())}
                      for p in artifacts + [checksum]],
        'package_integrity': 'PASS', 'source_blobs': 'PASS', 'reviewed_core_unchanged': True,
        'new_game_or_regression_run': False,
    }
    (ROOT / '.cache/release-190-assets.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), 'utf-8')
    print(json.dumps(manifest, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
