"""Package the accepted 1.6.1 R3 core, documentation and committed source."""
import argparse
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import struct
import subprocess
from zipfile import ZipFile, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parents[1]
DIST = ROOT / 'dist'
VERSION = '1.6.1'
ACCEPTED_CORE_SHA256 = '68c0b3c4eb791a8a24378340e963c823cb35eb2fa208c96570f2b84adea4ca4c'


def sha(data):
    return hashlib.sha256(data).hexdigest()


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT)


def check_core(data):
    assert sha(data) == ACCEPTED_CORE_SHA256, 'Core differs from accepted R3'
    with ZipFile(io.BytesIO(data)) as jar:
        assert jar.testzip() is None
        assert 'version="1.6.1"' in jar.read('META-INF/mods.toml').decode()
        assert json.loads(jar.read('pack.mcmeta'))['pack']['pack_format'] == 6
        classes = [n for n in jar.namelist() if n.endswith('.class')]
        assert len(classes) == 490
        assert all(struct.unpack('>H', jar.read(n)[6:8])[0] == 55 for n in classes)
        for path in (ROOT / 'resources').rglob('*'):
            name = path.relative_to(ROOT / 'resources').as_posix()
            if path.is_file() and name != 'META-INF/MANIFEST.MF':
                assert jar.read(name) == path.read_bytes(), ('stale core resource', name)


def verify_source(path, ref):
    committed = {}
    for entry in git('ls-tree', '-r', '-z', ref).split(b'\0'):
        if not entry:
            continue
        header, name = entry.split(b'\t', 1)
        mode, kind, oid = header.split()
        assert kind == b'blob' and mode in {b'100644', b'100755'}
        committed[name.decode()] = oid.decode()
    algorithm = git('rev-parse', '--show-object-format').decode().strip()
    with ZipFile(path) as source:
        assert source.testzip() is None
        names = {n for n in source.namelist() if not n.endswith('/')}
        assert names == set(committed), 'Source archive omits committed files'
        for name in sorted(names):
            parts = PurePosixPath(name).parts
            assert not any(p in {'.git', '.cache', 'dist', '__pycache__', 'playerdata'} for p in parts)
            assert not name.endswith(('.jar', '.class', '.log', '.pyc'))
            # Compare committed blobs, preserving Windows line-ending rules.
            data = source.read(name)
            blob = b'blob ' + str(len(data)).encode() + b'\0' + data
            assert hashlib.new(algorithm, blob).hexdigest() == committed[name], name
            if parts[0] in {'src', 'resources'} or name in {'build.ps1', 'check.ps1'}:
                assert source.read(name).replace(b'\r\n', b'\n') == (ROOT / name).read_bytes().replace(b'\r\n', b'\n'), ('uncommitted implementation', name)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--source-ref', required=True)
    args = parser.parse_args()
    ref = git('rev-parse', args.source_ref + '^{commit}').decode().strip()
    core = DIST / f'pixelmon-content-backport-1.16.5-{VERSION}.jar'
    data = core.read_bytes()
    check_core(data)
    source = DIST / f'Pixelmon-Backport-source-{VERSION}.zip'
    subprocess.run(['git', '-c', 'core.autocrlf=false', '-c', 'core.eol=lf',
                    'archive', '--format=zip', f'--output={source}', ref], cwd=ROOT, check=True)
    verify_source(source, ref)

    package = DIST / f'Pixelmon-Backport-{VERSION}.zip'
    documents = ['README.md', 'HANDBOOK.md', 'BUILDING.md', 'LICENSE.txt', 'THIRD_PARTY.md']
    documents += [p.relative_to(ROOT).as_posix() for p in (ROOT / 'docs').rglob('*.md')]
    with ZipFile(package, 'w', ZIP_DEFLATED, compresslevel=9) as bundle:
        bundle.writestr('mods/' + core.name, data)
        bundle.writestr('mods/' + core.name + '.sha256', sha(data) + '  ' + core.name + '\n')
        for name in sorted(set(documents)):
            bundle.write(ROOT / name, name)
        for p in sorted((ROOT / 'pack-overrides').rglob('*.snbt')):
            bundle.write(p, p.relative_to(ROOT / 'pack-overrides').as_posix())
        bundle.writestr('安装说明.txt', (
            'Pixelmon Content Backport 1.6.1\n'
            '客户端与服务端同时更新；移除旧回迁核心，只保留 mods 中这一份。\n'
            '保留原生 Pixelmon、依赖、配置和存档。Java 11 / Forge 36.2.42 / Minecraft 1.16.5。\n'
            '所有外观和汉化已在核心内，不需要独立资源包。网络协议为 13。\n'
            '本包不是完整整合包，不含世界或玩家数据；可选 config/ftbquests 为任务书章节，按需合并。\n'
            '完整更新见 docs/RELEASE_1.6.1.md；场内捕捉规则保持不变。\n'))
        bundle.writestr('构建信息.json', json.dumps({
            'version': VERSION, 'source_commit': ref, 'core_sha256': sha(data),
            'accepted_candidate': '1.6.1-test-r3', 'network_protocol': 13,
            'includes_game_or_player_data': False}, ensure_ascii=False, indent=2))
    with ZipFile(package) as bundle:
        assert bundle.testzip() is None
        assert bundle.read('mods/' + core.name) == data
        assert not any(n.startswith(('saves/', 'logs/', 'resourcepacks/')) for n in bundle.namelist())
    copies = {'HANDBOOK.md': 'Handbook', 'docs/RELEASE_1.6.1.md': 'Release-Notes'}
    artifacts = [core, package, source]
    for name, label in copies.items():
        out = DIST / f'Pixelmon-Backport-{label}-{VERSION}.md'
        out.write_bytes((ROOT / name).read_bytes())
        artifacts.append(out)
    manifest = DIST / f'SHA256SUMS-{VERSION}.txt'
    manifest.write_text(''.join(f'{sha(p.read_bytes())}  {p.name}\n' for p in artifacts), encoding='utf-8')
    for line in manifest.read_text().splitlines():
        digest, name = line.split('  ', 1)
        assert sha((DIST / name).read_bytes()) == digest
    (ROOT / '.cache/release-161-assets.json').write_text(
        json.dumps([str(p) for p in artifacts] + [str(manifest)], indent=2), encoding='utf-8')
    print(f'PASS 1.6.1 release: {len(artifacts)} assets + checksums; accepted R3 core; all committed source blobs verified; source={ref}')


if __name__ == '__main__':
    main()
