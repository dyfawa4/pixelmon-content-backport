"""Package built addon and public documents; no user instance or account files."""
import argparse, hashlib, pathlib, shutil, zipfile, subprocess

root = pathlib.Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--version', default='1.3.0')
parser.add_argument('--source-ref', help='Package exact committed source instead of the working tree')
args = parser.parse_args()
version = args.version
dist = root / 'dist'
addon = dist / f'pixelmon-content-backport-1.16.5-{version}.jar'
maps = dist / f'Pixelmon-Backport-test-maps-{version}.zip'
gyms = dist / f'Pixelmon-Backport-gym-test-maps-{version}.zip'
cap = dist / f'Pixelmon-Backport-cap-test-map-{version}.zip'
if not addon.is_file() or not maps.is_file() or not gyms.is_file() or (version in {'1.4.0', '1.4.1', '1.4.2', '1.4.3', '1.4.4', '1.5.0'} and not cap.is_file()):
    raise SystemExit('Build the addon and maps first.')
package = dist / f'Pixelmon-Backport-{version}.zip'
with zipfile.ZipFile(package, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    archive.write(addon, f'mods/{addon.name}')
    documents = ['README.md', 'HANDBOOK.md', 'TEST_MAPS.md', 'GYM_TEST_MAPS.md', 'GYM_GENERATION.md', 'FEATURE_TEST_MAPS.md', 'BUILDING.md', f'docs/RELEASE_{version}.md', f'docs/VALIDATION_{version}.md', 'LICENSE.txt', 'THIRD_PARTY.md']
    if version in {'1.4.0', '1.4.1', '1.4.2', '1.4.3', '1.4.4', '1.5.0'}: documents += ['CAP_TEST_MAP.md', f'docs/CAP_PIKACHU_{version}.md', 'docs/CITY_FEEDBACK_1.3.0.md']
    if version in {'1.4.1', '1.4.2', '1.4.3', '1.4.4', '1.5.0'}: documents += [f'docs/GRENINJA_{version}.md', 'docs/MEGA_BOSS_AVATAR_AUDIT_1.4.1.md', 'docs/RAYQUAZA_MEGA_1.4.1.md']
    if version in {'1.4.2', '1.4.3', '1.4.4', '1.5.0'}: documents += ['docs/RELEASE_1.4.1.md', 'docs/VALIDATION_1.4.1.md', 'docs/RELEASE_1.4.0.md', 'docs/GRENINJA_1.4.1.md', 'docs/CAP_PIKACHU_1.4.1.md']
    if version in {'1.4.3', '1.4.4', '1.5.0'}: documents += ['docs/REPAIR_1.4.3.md', 'docs/ROCK_VANILLA_TEMPLE_1.4.3.md', 'docs/DEN_AUDIT_2026-10-15.md', 'docs/MAGEARNA_MANSIONS_1.4.3.md', 'docs/REVISION2_1.4.3.md']
    if version in {'1.4.4', '1.5.0'}: documents += ['docs/LEGENDARY_ADVANCEMENTS_1.4.4.md', 'docs/REQUEST_LEGENDARY_ADVANCEMENTS_1.4.4_2026-10-15.md', 'docs/CRASH_Z_PREVIEW_1.4.4.md', 'docs/BOSS_CAPTURE_1.4.4.md', 'docs/SPECIAL_APPEARANCE_1.4.4.md']
    if version in {'1.4.4', '1.5.0'}: documents += ['docs/CAP_TERA_LEARNING_2026-10-16.md', 'docs/BATTLE_PP_CRASH_2026-10-16.md', 'docs/QUEST_MONEY_2026-10-16.md', 'docs/QUEST_MONEY_DETAILS_2026-10-16.json', 'docs/JAVA11_GATE_2026-10-16.md', 'docs/BATTLE_TELEPORT_CRASH_2026-10-16.md', 'docs/EDITOR_CHINESE_2026-10-16.md', 'docs/TERA_RAID_CAPTURE_1.4.4.md', 'docs/LOCAL_CLEANUP_SAFETY_2026-10-16.md']
    if version == '1.5.0': documents += ['RIDING_TEST_MAP.md','TERA_VISUAL_TEST_MAP.md','NPC_QUEST_TEST_MAP.md','docs/RIDING_REWORK_1.5.0.md','docs/MENU_BACKGROUND_1.5.0.md','docs/REWARD_SAFETY_1.5.0.md','docs/NPC_DIALOGUE_GYM_1.5.0.md','docs/CAP_BALL_GUARD_1.4.5.md','docs/LATE_BLOOMER_ADVANCEMENTS_1.4.5.md','docs/VALIDATION_1.4.5.md']
    for name in documents:
        archive.write(root / name, name)
    if version in {'1.4.4', '1.5.0'}:
        for path in sorted((root/'pack-overrides').rglob('*.snbt')):
            archive.write(path, path.relative_to(root/'pack-overrides').as_posix())
handbook = dist / f'Pixelmon-Backport-Handbook-{version}.md'
shutil.copy2(root / 'HANDBOOK.md', handbook)
cap_guide = dist / f'Pixelmon-Backport-Cap-Pikachu-{version}.md'
if version in {'1.4.0', '1.4.1', '1.4.2', '1.4.3', '1.4.4', '1.5.0'}: shutil.copy2(root / f'docs/CAP_PIKACHU_{version}.md', cap_guide)
greninja_guide = dist / f'Pixelmon-Backport-Greninja-{version}.md'
if version in {'1.4.1', '1.4.2', '1.4.3', '1.4.4', '1.5.0'}: shutil.copy2(root / f'docs/GRENINJA_{version}.md', greninja_guide)
checksums = dist / f'SHA256SUMS-{version}.txt'
source = dist / f'Pixelmon-Backport-source-{version}.zip'
files=subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z'],cwd=root).decode('utf-8').split('\0')
if args.source_ref:
    subprocess.run(['git','archive','--format=zip',f'--output={source}',args.source_ref],cwd=root,check=True)
with zipfile.ZipFile(source,'a' if args.source_ref else 'w',zipfile.ZIP_DEFLATED,compresslevel=6) as archive:
    for name in ([] if args.source_ref else sorted(set(files))):
        if not name:continue
        parts = pathlib.PurePosixPath(name).parts
        if any(part in {'.git', '.cache', 'dist', '__pycache__'} for part in parts): continue
        if pathlib.PurePosixPath(name).suffix in {'.jar', '.class', '.log', '.pyc', '.pyo'}: continue
        path=root/name
        if not path.is_file() or not path.resolve().is_relative_to(root.resolve()):continue
        archive.write(path,name)
artifacts = [addon, package, maps, gyms, handbook, source]
if version in {'1.4.0', '1.4.1', '1.4.2', '1.4.3', '1.4.4', '1.5.0'}: artifacts += [cap, cap_guide]
if version in {'1.4.1', '1.4.2', '1.4.3', '1.4.4', '1.5.0'}: artifacts += [greninja_guide]
if version == '1.5.0':
    artifacts += [dist/'Pixelmon-Backport-offline-test-1.5.0.zip', dist/'Pixelmon-Backport-npc-quest-test-map-1.5.0.zip', dist/'Pixelmon-Backport-tera-visual-test-map-1.5.0-r10.zip', dist/'Pixelmon-Backport-riding-test-map-1.5.0-r9.zip']
checksums.write_text(''.join(f'{hashlib.sha256(p.read_bytes()).hexdigest()}  {p.name}\n'
                             for p in artifacts), encoding='utf-8')
print(f'Packaged addon, feature/city maps, handbook, complete source and checksums for {version}.')
