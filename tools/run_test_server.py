"""Run only the isolated server, preserve stdin, stop gracefully after tests."""
import argparse, pathlib, subprocess, time
root=pathlib.Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--java',required=True);p.add_argument('--timeout',type=int,default=180)
p.add_argument('--server-directory',required=True,type=pathlib.Path)
p.add_argument('--pixelmon',required=True,type=pathlib.Path);a=p.parse_args()
server=a.server_directory.resolve();log=root/'.cache/live-test.log'
old=str(a.pixelmon.resolve())
log.parent.mkdir(parents=True,exist_ok=True)
with log.open('w',encoding='utf-8') as output:
    proc=subprocess.Popen([a.java,'-Xms1G','-Xmx4G','-Dbackport.test.live=true',f'-Dbackport.test.oldJar={old}',
                           '-jar','forge-1.16.5-36.2.42.jar','nogui'],cwd=server,stdin=subprocess.PIPE,stdout=output,stderr=subprocess.STDOUT,text=True)
    success=False;deadline=time.monotonic()+a.timeout
    try:
        while proc.poll() is None and time.monotonic()<deadline:
            contents=log.read_text(encoding='utf-8',errors='replace')
            if 'BACKPORT_LIVE_PASS' in contents:success=True;break
            if 'BACKPORT_LIVE_FAIL' in contents:break
            time.sleep(1)
    finally:
        if proc.poll() is None:
            proc.stdin.write('stop\n');proc.stdin.flush()
            try:proc.wait(timeout=35)
            except subprocess.TimeoutExpired:proc.terminate();proc.wait(timeout=15)
        proc.stdin.close()
print(('PASS' if success else 'FAIL')+': '+str(log))
raise SystemExit(0 if success else 1)
