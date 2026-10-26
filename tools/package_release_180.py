"""Package the reviewed r15 core and an exact archive of committed 1.8.0 sources."""
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
VERSION = '1.8.0'
CORE_SHA256 = '72ae745baacbed75f85eb1c6a386cbcbc453447e5453a7611d71bbeb14e8f260'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source-ref', required=True)
    args = parser.parse_args()
    ref = subprocess.check_output(['git', 'rev-parse', args.source_ref + '^{commit}'], cwd=ROOT).decode().strip()
    core = DIST / f'pixelmon-content-backport-1.16.5-{VERSION}.jar'
    data = core.read_bytes()
    assert sha(data) == CORE_SHA256, 'Reviewed r15 core must remain unchanged'
    with ZipFile(io.BytesIO(data)) as jar:
        assert jar.testzip() is None
        assert 'version="1.8.0"' in jar.read('META-INF/mods.toml').decode()
        names = jar.namelist()
        classes = [n for n in names if n.endswith('.class')]
        assert len(classes) == 802
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
                 'docs/RELEASE_1.8.0.md', 'docs/VALIDATION_1.8.0.md',
                 'docs/VALIDATION_1.8.0_R13.md', 'docs/VALIDATION_1.8.0_R15.md']
    for name in documents:
        entries[name] = (ROOT / name).read_bytes()
    config = ROOT / 'pack-overrides/config/ironfurnaces-client.toml'
    assert 'updates = false' in config.read_text('utf-8')
    entries['config/ironfurnaces-client.toml'] = config.read_bytes()
    for path in (ROOT / 'pack-overrides').rglob('*.snbt'):
        # Optional quest chapters stay separate so an extraction cannot replace progress.
        entries['可选任务书/' + path.relative_to(ROOT / 'pack-overrides').as_posix()] = path.read_bytes()
    addon = DIST / 'ohmygod-greninja-1.16.5-1.0.1.jar'
    with ZipFile(addon) as jar:
        assert jar.testzip() is None
        metadata = jar.read('META-INF/mods.toml').decode()
        assert '1.0.1' in metadata and '[1.7.0,1.9.0)' in metadata
    entries['可选模组/' + addon.name] = addon.read_bytes()
    entries['可选模组/安装说明.md'] = (ROOT / 'addons/ohmygod-greninja/README.md').read_bytes()
    entries['安装说明.txt'] = (
        '重铸内容回迁 1.8.0\n'
        'Minecraft 1.16.5 / Forge 36.2.42 / Java 11 / Pixelmon 9.1.12～9.1.13。\n'
        '关闭游戏及服务端后，双方删除旧回迁核心，只保留mods中的1.8.0。保留Pixelmon本体、依赖和存档。\n'
        '把config目录一并覆盖到实际客户端实例并重启，以关闭万能熔炉更新提醒；仅更新JAR不会应用配置。\n'
        '可选模组默认不启用。启用时双方另行安装可选目录中的1.0.1，替换旧1.0.0。\n'
        '可选任务书仅供按需合并，不覆盖个人任务进度。无需独立资源包。\n'
        '本包不是完整整合包，不含游戏本体、个人存档或自动测试助手。完整更新见docs/RELEASE_1.8.0.md。\n'
    ).encode('utf-8-sig')
    entries['构建信息.json'] = json.dumps({
        'version': VERSION, 'source_commit': ref, 'core_sha256': sha(data),
        'java11_classes': len(classes), 'network_protocol': 19, 'reviewed_candidate': 'r15',
        'optional_addon_enabled_by_default': False, 'whole_regression_rerun_for_publication': False,
        'includes_game_or_player_data': False,
    }, ensure_ascii=False, indent=2).encode()
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
    artifacts = [core, package, source, addon]
    for origin, label in [('HANDBOOK.md', 'Handbook'), ('docs/RELEASE_1.8.0.md', 'Release-Notes')]:
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
    (ROOT / '.cache/release-180-assets.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), 'utf-8')
    print(json.dumps(manifest, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
