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
cap = dist / f'Pixelmon-Backport-cap-test-map-{version}.zip'
if not addon.is_file() or not maps.is_file() or not gyms.is_file() or (version in {'1.4.0', '1.4.1', '1.4.2'} and not cap.is_file()):
    raise SystemExit('Build the addon and maps first.')
package = dist / f'Pixelmon-Backport-{version}.zip'
with zipfile.ZipFile(package, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    archive.write(addon, f'mods/{addon.name}')
    documents = ['README.md', 'HANDBOOK.md', 'TEST_MAPS.md', 'GYM_TEST_MAPS.md', 'GYM_GENERATION.md', 'FEATURE_TEST_MAPS.md', 'BUILDING.md', f'docs/RELEASE_{version}.md', f'docs/VALIDATION_{version}.md', 'LICENSE.txt', 'THIRD_PARTY.md']
    if version in {'1.4.0', '1.4.1', '1.4.2'}: documents += ['CAP_TEST_MAP.md', f'docs/CAP_PIKACHU_{version}.md', 'docs/CITY_FEEDBACK_1.3.0.md']
    if version in {'1.4.1', '1.4.2'}: documents += [f'docs/GRENINJA_{version}.md', 'docs/MEGA_BOSS_AVATAR_AUDIT_1.4.1.md', 'docs/RAYQUAZA_MEGA_1.4.1.md']
    if version == '1.4.2': documents += ['docs/RELEASE_1.4.1.md', 'docs/VALIDATION_1.4.1.md', 'docs/RELEASE_1.4.0.md', 'docs/GRENINJA_1.4.1.md', 'docs/CAP_PIKACHU_1.4.1.md']
    for name in documents:
        archive.write(root / name, name)
handbook = dist / f'Pixelmon-Backport-Handbook-{version}.md'
shutil.copy2(root / 'HANDBOOK.md', handbook)
cap_guide = dist / f'Pixelmon-Backport-Cap-Pikachu-{version}.md'
if version in {'1.4.0', '1.4.1', '1.4.2'}: shutil.copy2(root / f'docs/CAP_PIKACHU_{version}.md', cap_guide)
greninja_guide = dist / f'Pixelmon-Backport-Greninja-{version}.md'
if version in {'1.4.1', '1.4.2'}: shutil.copy2(root / f'docs/GRENINJA_{version}.md', greninja_guide)
checksums = dist / f'SHA256SUMS-{version}.txt'
source = dist / f'Pixelmon-Backport-source-{version}.zip'
files=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z'],cwd=root).decode('utf-8').split('\0')
with zipfile.ZipFile(source,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as archive:
    for name in sorted(set(files)):
        if not name:continue
        parts = pathlib.PurePosixPath(name).parts
        if any(part in {'.git', '.cache', 'dist', '__pycache__'} for part in parts): continue
        if pathlib.PurePosixPath(name).suffix in {'.jar', '.class', '.log', '.pyc', '.pyo'}: continue
        path=root/name
        if not path.is_file() or not path.resolve().is_relative_to(root.resolve()):continue
        archive.write(path,name)
artifacts = [addon, package, maps, gyms, handbook, source]
if version in {'1.4.0', '1.4.1', '1.4.2'}: artifacts += [cap, cap_guide]
if version in {'1.4.1', '1.4.2'}: artifacts += [greninja_guide]
checksums.write_text(''.join(f'{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.name}\n'
                             for p in artifacts), encoding='utf-8')
print(f'Packaged addon, feature/city maps, handbook, complete source and checksums for {version}.')
