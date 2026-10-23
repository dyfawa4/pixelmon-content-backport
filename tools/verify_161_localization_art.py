"""Audit the actual installed FD version and embedded ball art, without launching Minecraft."""
import argparse
import hashlib
import json
import re
import zipfile
from pathlib import Path

def read(z, name):
    return json.loads(z.read(name).decode('utf-8-sig'))

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--mods', type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    resources = root / 'resources'
    installed = []
    for jar in args.mods.glob('*.jar'):
        with zipfile.ZipFile(jar) as z:
            namespaces = {n.split('/')[1] for n in z.namelist() if n.startswith('assets/') and len(n.split('/')) > 2}
            related = {n for n in namespaces if 'delight' in n.lower() or 'farmers' in n.lower()}
            # A compatibility patch can carry this namespace too; only the
            # actual declared mod owns the installed version being audited.
            declared = z.read('META-INF/mods.toml').decode('utf-8-sig') if 'META-INF/mods.toml' in z.namelist() else ''
            if related and re.search(r'(?m)^\s*modId\s*=\s*[\"\']farmersdelight[\"\']', declared):
                installed.append((jar, related))
    assert len(installed) == 1 and installed[0][1] == {'farmersdelight'}, installed
    jar = installed[0][0]
    with zipfile.ZipFile(jar) as z:
        en = read(z, 'assets/farmersdelight/lang/en_us.json')
        old = read(z, 'assets/farmersdelight/lang/zh_cn.json')
        patch = json.loads((resources / 'assets/farmersdelight/lang/zh_cn.json').read_text(encoding='utf-8'))
        merged = dict(old, **patch)
        assert set(en) <= set(merged), sorted(set(en) - set(merged))
        placeholders = lambda s: sorted(re.findall(r'%(?:\d+\$)?[\d.]*[sdf]|%%', s))
        for key, value in en.items():
            assert re.search('[\u3400-\u9fff]', merged[key]), (key, merged[key])
            assert placeholders(value) == placeholders(merged[key]), ('format mismatch', key)
        assert all(merged[k] == v for k, v in old.items()), 'Existing translation overwritten'
        assert len(set(en) - set(old)) == 11, 'Re-audit the installed mod version'
        keys = set()
        def walk(obj):
            if isinstance(obj, dict):
                if 'translate' in obj: keys.add(obj['translate'])
                if 'text' in obj: assert not re.search('[A-Za-z]', obj['text']), obj
                for v in obj.values(): walk(v)
            elif isinstance(obj, list):
                for v in obj: walk(v)
        advancements = 0
        for name in z.namelist():
            if name.startswith('data/farmersdelight/advancements/') and name.endswith('.json'):
                data = read(z, name)
                if 'display' in data:
                    advancements += 1
                    walk(data['display'])
        assert all(k in merged and re.search('[\u3400-\u9fff]', merged[k]) for k in keys), keys-set(merged)
        assert not list((resources / 'data/farmersdelight/advancements').glob('**/*.json')), 'Do not replace native advancement progress/criteria'
    en_patch = json.loads((resources / 'assets/farmersdelight/lang/en_us.json').read_text())
    config_keys = {k for k in patch if k.startswith('farmersdelight.config.')}
    assert config_keys <= en_patch.keys(), config_keys-en_patch.keys()
    assert len([k for k in config_keys if k.endswith('.tooltip')]) == 25
    for k in config_keys: assert re.search('[\u3400-\u9fff]', patch[k]), k
    provenance = json.loads((resources / 'backport/pokeball-item-art-source.json').read_text())
    assert len(provenance['files']) == 76
    for row in provenance['files']:
        path = row['path']; data = (resources / path).read_bytes()
        assert path.startswith('assets/pixelmon/textures/items/pokeballs/')
        assert hashlib.sha256(data).hexdigest() == row['sha256'], path
        assert data.startswith(b'\x89PNG\r\n\x1a\n'), path
    assert len(provenance['ownedFiles']) == 85
    for name in provenance['ownedFiles']:
        data = (resources / f'assets/pixelmon_backport/textures/items/pokeballs/{name}.png').read_bytes()
        assert hashlib.sha256(data).hexdigest() == provenance['ownedHashes'][name], name
        if name.endswith(('_base', '_disc')):
            model = json.loads((resources / f'assets/pixelmon_backport/models/item/ball_parts/{name}.json').read_text())
            assert model['textures']['layer0'] == f'pixelmon_backport:items/pokeballs/{name}'
    report = {'installedRelatedMods': [p.name for p, _ in installed], 'englishKeys': len(en),
              'nativeChineseKeys': len(old), 'missingKeysFixed': sorted(set(en)-set(old)),
              'effectiveChineseKeys': len(merged), 'visibleAdvancements': advancements,
              'configSettings': 25, 'configTranslationKeys': len(config_keys), 'embeddedBallTextures': 76, 'ownedNamespaceBallTextures': 85}
    out = root / '.cache/fixes-161/localization-art-audit.json'
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(report, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
    print(f'PASS Farmer\'s Delight: all {len(en)} English keys translated, 11 omissions fixed, {advancements} advancement displays, format arguments preserved; 25 config labels/tooltips; 76 embedded reference ball item textures verified.')

if __name__ == '__main__':
    main()
