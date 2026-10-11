"""Remove malformed parameter annotation metadata from the Forge compile API only.

Some 1.16.5 SRG jars contain parameter counts javac 11 rejects (inner/enum
constructors). Game jars and runtime class files remain unchanged.
"""
import pathlib, struct, sys, zipfile

def clean(data):
    p = 8
    def u2():
        nonlocal p
        v = struct.unpack_from('>H', data, p)[0]; p += 2; return v
    count = u2(); names = {}; i = 1
    while i < count:
        tag = data[p]; p += 1
        if tag == 1:
            size = u2(); names[i] = data[p:p+size].decode('utf-8', errors='replace'); p += size
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18): p += 4
        elif tag in (5, 6): p += 8; i += 1
        elif tag in (7, 8, 16, 19, 20): p += 2
        elif tag == 15: p += 3
        else: raise ValueError(tag)
        i += 1
    p += 6; interfaces = u2(); p += 2*interfaces
    out = bytearray(data[:p])
    for kind in range(2):
        members = u2(); out.extend(struct.pack('>H', members))
        for _ in range(members):
            out.extend(data[p:p+6]); p += 6
            attributes = u2(); kept = []
            for _ in range(attributes):
                start = p; name = u2(); size = struct.unpack_from('>I', data, p)[0]; p += 4+size
                if names[name] not in ('RuntimeInvisibleParameterAnnotations', 'RuntimeVisibleParameterAnnotations'):
                    kept.append(data[start:p])
            out.extend(struct.pack('>H', len(kept))); out.extend(b''.join(kept))
    out.extend(data[p:]); return out

source, target = map(pathlib.Path, sys.argv[1:3])
if not target.exists() or target.stat().st_mtime < max(source.stat().st_mtime, pathlib.Path(__file__).stat().st_mtime):
    target.parent.mkdir(parents=True, exist_ok=True)
    staging=target.with_suffix('.tmp.jar')
    with zipfile.ZipFile(source) as src, zipfile.ZipFile(staging, 'w', zipfile.ZIP_DEFLATED) as dest:
        for entry in src.infolist():
            value = src.read(entry)
            dest.writestr(entry, clean(value) if entry.filename.endswith('.class') else value)
    staging.replace(target)
print(target)
