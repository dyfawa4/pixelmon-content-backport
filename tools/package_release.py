"""Package built addon and public documents; no user instance or account files."""
import argparse, hashlib, pathlib, shutil, zipfile

root = pathlib.Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--version', default='1.1.0')
args = parser.parse_args()
version = args.version
dist = root / 'dist'
addon = dist / f'pixelmon-content-backport-1.16.5-{version}.jar'
maps = dist / f'Pixelmon-Backport-test-maps-{version}.zip'
if not addon.is_file() or not maps.is_file():
    raise SystemExit('Build the addon and maps first.')
package = dist / f'Pixelmon-Backport-{version}.zip'
with zipfile.ZipFile(package, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    archive.write(addon, f'mods/{addon.name}')
    for name in ['README.md', 'HANDBOOK.md', 'TEST_MAPS.md', 'LICENSE.txt', 'THIRD_PARTY.md']:
        archive.write(root / name, name)
handbook = dist / f'Pixelmon-Backport-Handbook-{version}.md'
shutil.copy2(root / 'HANDBOOK.md', handbook)
checksums = dist / f'SHA256SUMS-{version}.txt'
checksums.write_text(''.join(f'{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.name}\n'
                             for p in [addon, package, maps, handbook]), encoding='utf-8')
print(f'Packaged {package.name}, {maps.name}, {handbook.name}, {checksums.name}')
