import json,urllib.request,pathlib,hashlib,time
p=pathlib.Path(__file__).resolve().parents[1]/'.cache'
p.mkdir(parents=True,exist_ok=True)
v=next(x for x in json.load(urllib.request.urlopen('https://api.modrinth.com/v2/project/pixelmon/version')) if x['version_number']=='9.4.1')
f=next(x for x in v['files'] if x['primary'])
(p/'upstream.json').write_text(json.dumps(v,indent=2),encoding='utf-8')
target=p/f['filename']
if not target.exists():
 partial=target.with_suffix('.part')
 for attempt in range(8):
  size=partial.stat().st_size if partial.exists() else 0
  if size==f['size']: break
  try:
   req=urllib.request.Request(f['url'],headers={'Range':f'bytes={size}-','User-Agent':'Pixelmon-Backport-Builder/1.0'})
   with urllib.request.urlopen(req,timeout=25) as src:
    if size and src.status!=206: raise RuntimeError('Server did not honor Range')
    with partial.open('ab') as out:
     prev=time.time()
     while b:=src.read(256*1024):
      out.write(b);size+=len(b)
      if time.time()-prev>10: print(f'Download {size}/{f["size"]}',flush=True);prev=time.time()
  except (TimeoutError,OSError) as e: print('Resume after:',str(e),flush=True)
 if not partial.exists() or partial.stat().st_size!=f['size']: raise RuntimeError('Incomplete download; rerun to resume')
 h=hashlib.sha512()
 with partial.open('rb') as inp:
  while b:=inp.read(1024*1024): h.update(b)
 if h.hexdigest()!=f['hashes']['sha512']: raise RuntimeError('hash mismatch')
 partial.replace(target)
print('Verified upstream:',target,flush=True)
