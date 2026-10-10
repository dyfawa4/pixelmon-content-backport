"""Developer-only API inspection, using locally supplied JARs."""
import argparse, pathlib, zipfile, subprocess
root=pathlib.Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--jar',required=True);p.add_argument('--java',required=True);p.add_argument('--tag',default='old');p.add_argument('classes',nargs='+');a=p.parse_args()
with zipfile.ZipFile(a.jar) as z:
    for name in a.classes:
        name=name.replace('.', '/')
        target=root/'.cache'/f'{a.tag}-{name.split("/")[-1]}.class'
        target.write_bytes(z.read(name+'.class'))
        subprocess.run([a.java,'-jar',str(root/'.cache/cfr.jar'),str(target),'--outputdir',str(root/'.cache'/('decompiled-'+a.tag)), '--silent','true'],check=True)
