"""Original native cuboid sword geometry and a small color atlas, with no vanilla item carrier."""
from pathlib import Path
import json
import struct
import zlib

ROOT=Path(__file__).resolve().parents[1]/'resources/assets/pixelmon_backport'
LANG={
    'zh_cn':{
        'item.pixelmon_backport.divine_sword':'无敌神剑',
        'item.pixelmon_backport.divine_sword.desc':'一击击败末影龙或凋零；成功消耗1次，其他目标无效。',
        'item.pixelmon_backport.divine_sword.uses':'剩余使用次数：%s / 3',
        'item.pixelmon_backport.divine_sword.shop':'任务商店专用 · 无法附魔或修复',
    },
    'en_us':{
        'item.pixelmon_backport.divine_sword':'Invincible Divine Sword',
        'item.pixelmon_backport.divine_sword.desc':'Defeats an Ender Dragon or Wither in one hit. Costs one use on success.',
        'item.pixelmon_backport.divine_sword.uses':'Uses remaining: %s / 3',
        'item.pixelmon_backport.divine_sword.shop':'Quest shop item - cannot be enchanted or repaired',
    },
}
for lang,entries in LANG.items():
    path=ROOT/f'lang/{lang}.json'
    data=json.loads(path.read_text('utf-8'))
    data.update(entries)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n','utf-8')

colors=[(76,48,99),(86,116,147),(194,214,233),(238,250,255),
        (238,207,136),(154,108,55),(79,215,233),(156,119,224)]
raw=b''.join(b'\x00'+b''.join(bytes(colors[x//2])+b'\xff' for x in range(16)) for y in range(16))
def chunk(kind,content):
    return struct.pack('>I',len(content))+kind+content+struct.pack('>I',zlib.crc32(kind+content)&0xffffffff)
png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',16,16,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b'')
(ROOT/'textures/item/divine_sword.png').write_bytes(png)
elements=[]
def box(start,end,color,rotation=None,side=None):
    faces={}
    for face in ['down','up','north','south','west','east']:
        c=side if side is not None and face in ['down','east','west'] else color
        faces[face]={'uv':[c*2+.25,.25,c*2+1.75,15.75],'texture':'#palette'}
    element={'from':start,'to':end,'faces':faces}
    if rotation:
        axis,angle,origin=rotation
        element['rotation']={'axis':axis,'angle':angle,'origin':origin,'rescale':False}
    elements.append(element)

# Continuous violet leather grip, metal collars, and a framed diamond pommel.
box([7.5,1.35,7.5],[8.5,4.8,8.5],0)
for y in [1.6,2.35,3.1,3.85,4.5]:
    box([7.43,y,7.43],[8.57,y+.14,8.57],5)
box([7.35,1.2,7.35],[8.65,1.7,8.65],4,side=5)
box([7.3,.45,7.35],[8.7,1.85,8.65],4,('z',45,[8,1.15,8]),side=5)
box([7.64,.79,7.2],[8.36,1.51,8.8],7,('z',45,[8,1.15,8]))

# Compact swept quillons; a narrow silver edge keeps them from reading as a broad gold bar.
box([6.65,4.7,7.25],[9.35,5.55,8.75],5)
box([6.6,5.05,7.15],[9.4,5.6,8.85],4,side=5)
for left in [True,False]:
    rot=('z',-22.5 if left else 22.5,[6.9 if left else 9.1,5.25,8])
    x0,x1=(4.8,6.95) if left else (9.05,11.2)
    box([x0,5.0,7.35],[x1,5.65,8.65],4,rot,side=5)
    box([x0+.12,5.52,7.25],[x1-.12,5.7,8.75],3,rot,side=4)

# Broad silver cutting edges and two angled blade facets meeting at a raised central ridge.
box([6.7,5.9,7.9],[9.3,14,8.1],1)
box([6.85,6.05,7.8],[8,14,8.2],3,('y',22.5,[8,8,8]),side=2)
box([8,6.05,7.8],[9.15,14,8.2],2,('y',-22.5,[8,8,8]),side=1)
box([7.82,6.05,7.57],[8.18,14.1,8.43],2,side=3)

# A true diagonal diamond tip instead of a square cap or staircase.
box([7.08,13.08,7.9],[8.92,14.92,8.1],1,('z',45,[8,14,8]))
box([7.25,13.25,7.72],[8.75,14.75,8.28],3,('z',45,[8,14,8]),side=2)

# Short cyan inscriptions leave the cutting edges metallic rather than filling the entire blade.
for y,length in [(6.8,.65),(7.85,1.2),(9.45,1.85),(11.7,.65)]:
    box([7.91,y,7.50],[8.09,y+length,8.50],6)
for y in [7.1,8.35,10.05,12.0]:
    box([7.65,y,7.48],[8.35,y+.15,8.52],6)

# Framed violet gem and silver blade collar connect hilt and blade.
box([7.15,5.35,7.25],[8.85,6.25,8.75],4,side=5)
box([7.3,4.9,7.05],[8.7,6.3,8.95],5,('z',45,[8,5.6,8]))
box([7.45,5.05,6.98],[8.55,6.15,9.02],4,('z',45,[8,5.6,8]))
box([7.62,5.22,6.9],[8.38,5.98,9.1],7,('z',45,[8,5.6,8]))
box([7.83,5.56,6.85],[8.1,5.83,9.15],3,('z',45,[8,5.6,8]))
display={
    'gui':{'rotation':[0,-22.5,-40],'translation':[0,0,0],'scale':[1,1,1]},
    'thirdperson_righthand':{'rotation':[0,-90,0],'translation':[0,0,.5],'scale':[1,1,1]},
    'thirdperson_lefthand':{'rotation':[0,90,0],'translation':[0,0,.5],'scale':[1,1,1]},
    'firstperson_righthand':{'rotation':[0,-90,25],'translation':[1.13,3.2,1.13],'scale':[.9,.9,.9]},
    'firstperson_lefthand':{'rotation':[0,90,-25],'translation':[1.13,3.2,1.13],'scale':[.9,.9,.9]},
    'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.5,.5,.5]},
    'fixed':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]},
}
model={'textures':{'palette':'pixelmon_backport:item/divine_sword','particle':'pixelmon_backport:item/divine_sword'},
       'elements':elements,'gui_light':'front','display':display}
(ROOT/'models/item/divine_sword.json').write_text(json.dumps(model,indent=2)+'\n','utf-8')
