"""Package built addon and public documents; no user instance or account files."""
import argparse, hashlib, pathlib, shutil, zipfile, subprocess

root = pathlib.Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--version', default='1.3.0')
args = parser.parse_args()
version = args.version
dist = root / 'dist'
addon = dist / f'pixelmon-content-backport-1.16.5-{version}.jar'
maps = dist / f'Pixelmon-Backport-test-maps-{version}.zip'
gyms = dist / f'Pixelmon-Backport-gym-test-maps-{version}.zip'
if not addon.is_file() or not maps.is_file() or not gyms.is_file():
    raise SystemExit('Build the addon and maps first.')
package = dist / f'Pixelmon-Backport-{version}.zip'
with zipfile.ZipFile(package, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    archive.write(addon, f'mods/{addon.name}')
    for name in ['README.md', 'HANDBOOK.md', 'TEST_MAPS.md', 'GYM_TEST_MAPS.md', 'GYM_GENERATION.md', 'FEATURE_TEST_MAPS.md', 'BUILDING.md', f'docs/RELEASE_{version}.md', f'docs/VALIDATION_{version}.md', 'LICENSE.txt', 'THIRD_PARTY.md']:
        archive.write(root / name, name)
handbook = dist / f'Pixelmon-Backport-Handbook-{version}.md'
shutil.copy2(root / 'HANDBOOK.md', handbook)
checksums = dist / f'SHA256SUMS-{version}.txt'
source = dist / f'Pixelmon-Backport-source-{version}.zip'
files=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z'],cwd=root).decode('utf-8').split('\0')
with zipfile.ZipFile(source,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as archive:
    for name in sorted(set(files)):
        if not name:continue
        path=root/name
        if not path.is_file() or not path.resolve().is_relative_to(root.resolve()):continue
        archive.write(path,name)
checksums.write_text(''.join(f'{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.name}\n'
                             for p in [addon, package, maps, gyms, handbook, source]), encoding='utf-8')
print(f'Packaged addon, feature/city maps, handbook, complete source and checksums for {version}.')
