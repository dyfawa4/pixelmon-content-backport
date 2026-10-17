"""Fetch only official 9.4.1 visual research entries using verified ZIP ranges.

Research classes stay in ignored .cache; no decompiled upstream code is shipped.
ZIP CRC checks are performed by zipfile; source hashes are recorded separately.
"""
import io
import json
import urllib.request
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CACHE = ROOT / '.cache/tera-visual-research'


class RemoteZip(io.RawIOBase):
    def __init__(self, file):
        self.file, self.pos, self.total = file, 0, 0
        self.blocks = {}

    def seekable(self):
        return True

    def tell(self):
        return self.pos

    def seek(self, n, whence=0):
        self.pos = n if whence == 0 else self.pos+n if whence == 1 else self.file['size']+n
        return self.pos

    def read(self, n=-1):
        n = min(self.file['size']-self.pos, n if n >= 0 else self.file['size'])
        if not n:
            return b''
        for start, block in self.blocks.items():
            if start <= self.pos and self.pos+n <= start+len(block):
                result = block[self.pos-start:self.pos-start+n]
                self.pos += n
                return result
        start = self.pos
        fetch = min(self.file['size']-start, max(n, 512*1024))
        request = urllib.request.Request(
            self.file['url'] + '?backport_block=' + str(start) + '_' + str(fetch),
            headers={'Range': f'bytes={start}-{start+fetch-1}',
                     'User-Agent': 'Pixelmon-Backport-Asset-Research'})
        with urllib.request.urlopen(request, timeout=45) as response:
            if response.status != 206:
                raise RuntimeError('Server ignored Range; refusing an unexpected full download')
            data = response.read(fetch+1)
        if len(data) != fetch:
            raise IOError('Truncated ZIP range')
        self.blocks[start] = data
        self.pos += n
        self.total += fetch
        return data[:n]


def main():
    CACHE.mkdir(parents=True,exist_ok=True)
    if not (CACHE/'pixelmon-941.json').exists():
        request=urllib.request.Request('https://api.modrinth.com/v2/version/tY57GRtu',headers={'User-Agent':'Pixelmon-Backport-Asset-Research'})
        with urllib.request.urlopen(request,timeout=45) as response:
            (CACHE/'pixelmon-941.json').write_bytes(response.read())
    metadata = json.loads((CACHE/'pixelmon-941.json').read_text(encoding='utf-8'))
    remote = RemoteZip(next(f for f in metadata['files'] if f['primary']))
    with zipfile.ZipFile(remote) as archive:
        names = archive.namelist()
        selected = [n for n in names if not n.endswith('/') and (
            n.startswith('assets/pixelmon/textures/hat/tera_jewel/') or
            (n.startswith('assets/pixelmon/shaders/core/') and any(s in n for s in ('terrastalization', 'stellar'))) or
            (n.endswith('.class') and any(s in n.lower() for s in (
                'terajewel', 'terarender', 'rendereffect', 'modelattachment', 'attachmentpoint'))) or
            (n.endswith('.json') and any(s in n.lower() for s in ('tera_jewel', 'attachment', 'render_effect'))))]
        for name in selected:
            output = CACHE/'upstream'/name
            if not output.exists():
                output.parent.mkdir(parents=True, exist_ok=True)
                output.write_bytes(archive.read(name))
            print(name, flush=True)
    print('Network ZIP bytes:', remote.total)


if __name__ == '__main__':
    main()
