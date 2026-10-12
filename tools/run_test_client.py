"""Developer-only isolated client; no user account or original save access.
Uses an existing Java 11 installation and launcher version JSON/libraries.
"""
import argparse,json,pathlib,subprocess,shutil,os,uuid
root=pathlib.Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('--version-json',type=pathlib.Path,required=True)
p.add_argument('--minecraft-root',type=pathlib.Path,required=True)
p.add_argument('--pixelmon',type=pathlib.Path,required=True)
p.add_argument('--instance-name',help='Separate private instance folder name; letters, digits and hyphens only')
p.add_argument('--manual',action='store_true',help='Launch a separate normal menu for manual acceptance, with current helper and initial maps; no automatic QA')
p.add_argument('--map-helper',type=pathlib.Path,help='Current compiled manual map helper JAR')
p.add_argument('--memory-gb',type=int,choices=range(2,17),default=5,help='Maximum Java heap in GiB')
p.add_argument('--field-wheel-preview',action='store_true',help='Private field wheel UI fixture; requires a separate instance name')
p.add_argument('--full-pack',action='store_true',help='Copy enabled pack mods/configuration into a separate QA instance')
p.add_argument('--ui-preview',action='store_true',help='Run private Tera settings input and screenshot checks at the menu')
p.add_argument('--gym-map',choices=['normal','fire','water','electric','grass','ice','fighting','poison','ground','flying','psychic','bug','rock','ghost','dragon','dark','steel','fairy'],help='Open one isolated gym inspection world with the feedback client test')
a=p.parse_args()
if a.manual and (not a.instance_name or not a.map_helper or a.ui_preview or a.field_wheel_preview or a.gym_map):
    p.error('--manual requires --instance-name and --map-helper, and cannot use automatic preview modes')
a.version_json=a.version_json.resolve();a.minecraft_root=a.minecraft_root.resolve()
build=json.loads((root/'.cache/last-build.json').read_text(encoding='utf-8-sig'))
version=json.loads(a.version_json.read_text(encoding='utf-8-sig'));gamedir=root/('.cache/test-client-full' if a.full_pack else '.cache/test-client')
if a.instance_name:
    if not __import__('re').fullmatch(r'[a-z0-9-]+',a.instance_name):raise ValueError('Invalid isolated instance name')
    gamedir=root/'.cache'/a.instance_name
gamedir.mkdir(exist_ok=True);(gamedir/'mods').mkdir(exist_ok=True)
(gamedir/'local').mkdir(exist_ok=True)
old=str(a.pixelmon.resolve())
if a.full_pack:
    # Original saves, accounts, options and logs are never copied or modified.
    for source in (a.version_json.parent/'mods').glob('*.jar'):
        if source.resolve()==pathlib.Path(old).resolve() or source.name.startswith(('pixelmon-content-backport-','backport-test-maps-','backport-client-test','backport-feedback-','backport-field-')):continue
        shutil.copy2(source,gamedir/'mods'/source.name)
    for name in ['config','defaultconfigs','kubejs','scripts','shaderpacks']:
        source=a.version_json.parent/name
        if source.is_dir():shutil.copytree(source,gamedir/name,dirs_exist_ok=True)
    if not a.manual:shutil.copy2(root/'.cache/test-client/mods/backport-client-test.jar',gamedir/'mods/backport-client-test.jar')
if a.manual:
    shutil.copy2(a.map_helper,gamedir/'mods'/a.map_helper.name)
    for folder in ['saves','gym-saves-v9']:
        for source in (root/'test-maps'/folder).iterdir():
            destination=gamedir/'saves'/source.name
            if source.is_dir() and not destination.exists():shutil.copytree(source,destination)
for src,name in [(old,'Pixelmon-legacy.jar'),(build['jar'],'backport-dev.jar')]:
    target=gamedir/'mods'/name
    if not target.exists() or pathlib.Path(src).stat().st_mtime>target.stat().st_mtime:shutil.copy2(src,target)
if a.instance_name and a.gym_map and not (gamedir/'mods/backport-client-test.jar').exists():
    shutil.copy2(root/'.cache/test-client/mods/backport-feedback-client-test.jar.disabled',gamedir/'mods/backport-feedback-client-test.jar')
if a.field_wheel_preview:
    if not a.instance_name:raise ValueError('Field UI QA requires a separate developer instance')
    shutil.copy2(root/'.cache/field-ui-mod.jar',gamedir/'mods/field-ui-mod.jar')
    source=root/'test-maps/saves/backport-qa-single';destination=gamedir/'saves/backport-qa-single'
    if not destination.exists():shutil.copytree(source,destination)
cp=[]
def allowed(rules):
    result=not rules
    for rule in rules:
        osrule=rule.get('os',{})
        if osrule.get('name','windows')!='windows':continue
        if osrule.get('arch','x86_64') not in ('x86_64','amd64'):continue
        result=rule['action']=='allow'
    return result
for lib in version['libraries']:
    if not allowed(lib.get('rules',[])):continue
    path=lib.get('downloads',{}).get('artifact',{}).get('path')
    if not path:
        group,name,ver,*classifier=lib['name'].split(':')
        suffix=('-'+classifier[0] if classifier else '')
        path=f'{group.replace(".","/")}/{name}/{ver}/{name}-{ver}{suffix}.jar'
    target=a.minecraft_root/'libraries'/path
    if not target.exists():raise FileNotFoundError(target)
    if str(target) not in cp:cp.append(str(target))
    if lib['name'].startswith('net.minecraftforge:forge:'):
        for suffix in ['client','universal']:
            extra=target.with_name(target.stem+'-'+suffix+'.jar')
            if not extra.exists():raise FileNotFoundError(extra)
            cp.append(str(extra))
# Launcher inheritance replaces earlier versions of duplicate libraries.
# Retaining both log4j 2.8 and 2.15 creates method linkage errors.
selected={}
for path in cp:
    key=str(path) if pathlib.Path(path).name.startswith('forge-') else str(pathlib.Path(path).parent.parent)
    selected[key]=path
cp=list(selected.values())
cp.append(str(a.version_json.with_suffix('.jar')))
natives=a.version_json.parent/'natives-windows-x86_64'
if not natives.exists():raise FileNotFoundError(natives)
gui_scale=2
if a.full_pack:
    # Read only this display preference; do not import accounts, servers or
    # the entire original options file into the private test instance.
    options=a.version_json.parent/'options.txt'
    if options.exists():
        for line in options.read_text(encoding='utf-8').splitlines():
            if line.startswith('guiScale:'):
                try:gui_scale=max(0,min(6,int(line.split(':',1)[1])))
                except ValueError:pass
if not (gamedir/'options.txt').exists():
    (gamedir/'options.txt').write_text(f'guiScale:{gui_scale}\nlang:zh_cn\nrenderDistance:{10 if a.gym_map else 3}\nmaxFps:60\nsoundCategory_master:0.0\n',encoding='utf-8')
if a.gym_map:
    source=root/f'test-maps/gym-saves-v9/backport-gym-v9-{a.gym_map}'
    destination=gamedir/f'saves/backport-gym-v9-{a.gym_map}'
    if not destination.exists():shutil.copytree(source,destination)
world=root/'.cache/test-server/backport-test';destination=gamedir/'saves/backport-test'
if not a.manual and not destination.exists():shutil.copytree(world,destination)
args=[build['java'],'-Xms1G',f'-Xmx{a.memory_gb}G',f'-Djava.library.path={natives}',
      '-Dminecraft.launcher.brand=Backport-Manual' if a.manual else '-Dminecraft.launcher.brand=Backport-QA','-Dminecraft.launcher.version=1','-Dmixin.debug.export=true','-Dbackport.network.test='+str(not a.manual).lower(),
      '-Dbackport.gallery.enabled='+str(not a.manual and not a.full_pack).lower(),
      '-Dbackport.ui.preview='+str(a.ui_preview).lower(),
      '-Dbackport.gym.preview='+(a.gym_map or ''),
      '--add-exports=java.base/sun.security.util=ALL-UNNAMED','--add-opens=java.base/java.util.jar=ALL-UNNAMED',
      '-cp',os.pathsep.join(cp),version['mainClass'],'--username','BackportVisualTest',
      '--version','Backport-Java11-QA','--gameDir',str(gamedir),'--assetsDir',str(a.minecraft_root/'assets'),
      '--assetIndex',version['assets'],'--uuid',uuid.uuid3(uuid.NAMESPACE_DNS,'BackportVisualTest').hex,
      '--accessToken','0','--userType','legacy','--versionType','release','--width','1280','--height','800',
      '--launchTarget','fmlclient','--fml.forgeVersion','36.2.42','--fml.mcVersion','1.16.5',
      '--fml.forgeGroup','net.minecraftforge','--fml.mcpVersion','20210115.111550']
log=gamedir/'manual-launch.log' if a.manual else root/('.cache/client-full-launch.log' if a.full_pack else '.cache/client-launch.log')
with log.open('w',encoding='utf-8') as out:
    process=subprocess.Popen(args,cwd=gamedir,stdout=out,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
(gamedir/'manual-process.json' if a.manual else root/'.cache/client-process.json').write_text(json.dumps({'pid':process.pid,'gameDir':str(gamedir)}),encoding='utf-8')
print(f'Isolated Java 11 client started: PID {process.pid}')
