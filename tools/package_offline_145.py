"""1.4.5 handoff: retain the verified 1.4.4 initial maps and add the NPC quest world."""
from datetime import datetime, timedelta, timezone
import hashlib
import io
import json
from pathlib import PurePosixPath
import re
import shutil
import struct
import subprocess
from zipfile import ZipFile, ZIP_DEFLATED


def digest(data):
    return hashlib.sha256(data).hexdigest()


def inspect_jar(data, mod_id, required):
    with ZipFile(io.BytesIO(data)) as jar:
        assert jar.testzip() is None
        meta = jar.read('META-INF/mods.toml').decode()
        assert 'version="1.4.5"' in meta and f'modId="{mod_id}"' in meta
        classes = [n for n in jar.namelist() if n.endswith('.class')]
        assert required in classes
        assert all(struct.unpack('>H', jar.read(n)[6:8])[0] == 55 for n in classes)
        assert not any('Verification' in n or 'Agent' in n or 'RuntimeDenAudit' in n for n in classes)
        if mod_id == 'pixelmon_backport':
            assert 'NpcQuestTestMap.class' not in classes
            assert not any(n.startswith('data/pixelmon/quests/npc145') for n in jar.namelist())
        return len(classes)


def package(root, out, build_id):
    import nbtlib
    assert re.fullmatch(r'1\.4\.5-[A-Za-z0-9_.-]+', build_id)
    assert not out.exists()
    dist = root / 'dist'
    production = dist / 'pixelmon-content-backport-1.16.5-1.4.5.jar'
    main = production.read_bytes()
    class_count = inspect_jar(main, 'pixelmon_backport', 'local/pixelmon/backport/NpcQuestLogic.class')
    # Narrative text in the main addon remains the accepted 1.4.4 text, not QA instructions.
    for locale in ['en_us', 'zh_cn']:
        rel = f'resources/assets/pixelmon_backport/lang/{locale}.json'
        before = json.loads(subprocess.check_output(['git', 'show', 'v1.4.4:' + rel], cwd=root).decode())
        after = json.loads((root / rel).read_text(encoding='utf-8'))
        assert {k: v for k, v in before.items() if k.startswith('quest.')} == {k: v for k, v in after.items() if k.startswith('quest.')}
    base = dist / '1.4.4-离线测试包-最新.zip'
    assert digest(base.read_bytes()) == '0ba3e3b00b33825a2ea19b44990b7deed4446b24008b5143c6fa6f235ae389ba'
    helpers = {}
    with ZipFile(dist / 'Pixelmon-Backport-cap-test-map-1.4.5.zip') as jar:
        assert jar.testzip() is None
        assert jar.read('mods/' + production.name) == main
        name = 'backport-test-maps-1.16.5-1.4.5.jar'
        helpers[name] = jar.read('mods/' + name)
        inspect_jar(helpers[name], 'backport_client_test', 'MapSupport.class')
    npc_archive = dist / 'Pixelmon-Backport-npc-quest-test-map-1.4.5-r2.zip'
    with ZipFile(npc_archive) as jar:
        assert jar.testzip() is None and jar.read('mods/' + production.name) == main
        name = 'backport-npc-quest-test-1.16.5-1.4.5.jar'
        helpers[name] = jar.read('mods/' + name)
        inspect_jar(helpers[name], 'backport_npc_quest_test', 'NpcQuestTestMap.class')
    out.mkdir(parents=True)
    (out / 'mods').mkdir()
    (out / 'mods' / production.name).write_bytes(main)
    for name, data in helpers.items():
        (out / 'mods' / name).write_bytes(data)
    worlds = []
    for archive, expected in [(base, 11), (npc_archive, 1)]:
        with ZipFile(archive) as jar:
            selected = [n.split('/')[1] for n in jar.namelist() if n.startswith('saves/') and n.endswith('/level.dat')]
            assert len(selected) == expected
            worlds.extend(selected)
            for name in jar.namelist():
                parts = PurePosixPath(name).parts
                if len(parts) < 3 or parts[0] != 'saves' or name.endswith('/'):
                    continue
                assert '..' not in parts and not PurePosixPath(name).is_absolute()
                assert not any(p in {'playerdata', 'stats', 'advancements', 'pokemon', 'ftbteams', 'ftbchunks', 'logs'} for p in parts)
                assert parts[-1] not in {'session.lock', 'usercache.json', 'servers.dat', 'options.txt'}
                target = out.joinpath(*parts)
                assert not target.exists()
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(jar.read(name))
    assert len(worlds) == len(set(worlds)) == 12
    for world in worlds:
        level = nbtlib.load(out / 'saves' / world / 'level.dat')
        assert 'Player' not in level['Data']
    rock = out / 'saves/backport-gym-v143-rock-vanilla'
    approved = json.loads((rock / 'APPROVED_LAYOUT.json').read_text())
    assert approved['cityPieces'] == 97 and approved['seed'] == 143143
    for name, sha in approved['files'].items():
        assert digest((rock / name).read_bytes()) == sha
    docs = {'README.md': 'docs/OFFLINE_TEST_1.4.5.md', '测试清单.md': 'docs/OFFLINE_TEST_CHECKLIST_1.4.5.md',
            '测试反馈模板.md': 'docs/OFFLINE_TEST_FEEDBACK_1.4.5.md'}
    for name in ['NPC_QUEST_TEST_MAP.md', 'HANDBOOK.md', 'TEST_MAPS.md', 'FEATURE_TEST_MAPS.md', 'GYM_TEST_MAPS.md',
                 'CAP_TEST_MAP.md', 'LICENSE.txt', 'THIRD_PARTY.md']:
        docs[name] = name
    for name in ['RELEASE_1.4.5.md', 'VALIDATION_1.4.5.md', 'NPC_QUEST_AUDIT_1.4.5.json',
                 'RELEASE_1.4.4.md', 'VALIDATION_1.4.4.md', 'TERA_RAID_CAPTURE_1.4.4.md',
                 'QUEST_MONEY_2026-10-16.md', 'QUEST_MONEY_DETAILS_2026-10-16.json',
                 'ROCK_VANILLA_TEMPLE_1.4.3.md']:
        docs['docs/' + name] = 'docs/' + name
    for target, source in docs.items():
        dest = out / target
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(root / source, dest)
    overrides = []
    for path in sorted((root / 'pack-overrides').rglob('*.snbt')):
        rel = path.relative_to(root / 'pack-overrides')
        assert rel.parts[:4] == ('config', 'ftbquests', 'quests', 'chapters')
        dest = out / rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(path, dest)
        overrides.append(rel.as_posix())
    assert len(overrides) == 2
    identity = {'version': '1.4.5', 'build_id': build_id,
                'built_at': datetime.now(timezone(timedelta(hours=8))).isoformat(timespec='seconds'),
                'status': 'Offline test handoff; user accepted NPC quest map on 2026-10-17; not a GitHub release',
                'minecraft': '1.16.5', 'forge': '36.2.42', 'java': 11,
                'pixelmon': 'file 9.1.13 / internal 9.1.12', 'main_jar_sha256': digest(main),
                'java11_classes': class_count, 'helper_jars_sha256': {k: digest(v) for k, v in helpers.items()},
                'worlds': worlds, 'approved_rock_city_pieces': 97, 'quest_config_overrides': overrides,
                'original_npc_narrative_preserved': True, 'qa_explanations_only_in_map_helper': True,
                'tests': 'Java 11 build and full offline regressions passed; user accepted manual NPC map; see VALIDATION_1.4.5.md for limits',
                'private_diagnostics_included': False, 'original_game_client_included': False}
    (out / '构建信息.json').write_text(json.dumps(identity, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    checksums = {p.relative_to(out).as_posix(): digest(p.read_bytes()) for p in sorted(out.rglob('*')) if p.is_file()}
    (out / 'SHA256SUMS.txt').write_text(''.join(f'{sha}  {name}\n' for name, sha in checksums.items()), encoding='utf-8')
    archive = out.with_name(out.name + '.zip')
    assert not archive.exists()
    with ZipFile(archive, 'w', ZIP_DEFLATED, compresslevel=6) as jar:
        for path in sorted(out.rglob('*')):
            if path.is_file():
                jar.write(path, path.relative_to(out).as_posix())
    with ZipFile(archive) as jar:
        assert jar.testzip() is None
        for name, sha in checksums.items():
            assert digest(jar.read(name)) == sha
        assert len([n for n in jar.namelist() if n.startswith('mods/') and n.endswith('.jar')]) == 3
        assert len([n for n in jar.namelist() if n.startswith('saves/') and n.endswith('/level.dat')]) == 12
    sha = digest(archive.read_bytes())
    archive.with_name(archive.name + '.sha256').write_text(sha + '  ' + archive.name + '\n', encoding='utf-8')
    report = {'version': '1.4.5', 'build_id': build_id, 'archive': str(archive), 'bytes': archive.stat().st_size,
              'sha256': sha, 'main_jar_sha256': digest(main), 'worlds': 12, 'mods': 3,
              'approved_rock_snapshot_verified': True, 'original_npc_narrative_preserved': True,
              'player_files_included': False, 'crc_and_all_file_hashes_verified': True}
    (root / '.cache' / ('offline-test-delivery-' + build_id + '.json')).write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, ensure_ascii=False, indent=2))
