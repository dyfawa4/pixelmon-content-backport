"""Native Minecraft JSON geometry; GUI art and held models share the item id."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
MODELS=ROOT/'resources/assets/pixelmon_backport/models/item'

def box(low,high,texture):
    return {'from':low,'to':high,'faces':{face:{'uv':[0,0,16,16],'texture':'#'+texture} for face in ('north','south','east','west','up','down')}}

def octagon(radius,cut,z0,z1,texture):
    # Three adjacent prisms form a stepped eight-sided cross section without overlapping faces.
    return [box([8-radius+cut,8-radius,z0],[8+radius-cut,8+radius,z1],texture),
            box([8-radius,8-radius+cut,z0],[8-radius+cut,8+radius-cut,z1],texture),
            box([8+radius-cut,8-radius+cut,z0],[8+radius,8+radius-cut,z1],texture)]

for name,color in [('shrinking_lamp','red'),('enlarging_lamp','green')]:
    elements=[]
    for radius,cut,z0,z1,texture in [(2.1,.55,12.8,15,'dark'),(2.1,.55,5.5,12.8,'body'),(2.2,.55,10.3,10.8,'dark'),(3,.8,3.5,5.8,'body'),(4,1,1.2,3.6,'silver'),(3.45,.85,.95,1.2,'dark'),(2.8,.7,.78,.95,'lens')]:
        elements+=octagon(radius,cut,z0,z1,texture)
    elements.append(box([7.3,10.1,7.2],[8.7,10.55,8.8],'dark'))
    model={'ambientocclusion':False,'gui_light':'side','textures':{'particle':f'minecraft:block/{color}_concrete','body':f'minecraft:block/{color}_concrete','dark':'minecraft:block/gray_concrete','silver':'minecraft:block/iron_block','lens':'minecraft:block/sea_lantern'},'elements':elements,
           'display':{'firstperson_righthand':{'rotation':[0,-15,0],'translation':[1,1,-1],'scale':[.8,.8,.8]},'firstperson_lefthand':{'rotation':[0,15,0],'translation':[1,1,-1],'scale':[.8,.8,.8]},
                      'thirdperson_righthand':{'rotation':[75,-10,0],'translation':[0,2,0],'scale':[.65,.65,.65]},'thirdperson_lefthand':{'rotation':[75,10,0],'translation':[0,2,0],'scale':[.65,.65,.65]},
                      'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.5,.5,.5]},'fixed':{'rotation':[0,90,0],'translation':[0,0,0],'scale':[.8,.8,.8]},'gui':{'rotation':[30,135,0],'translation':[0,0,0],'scale':[.9,.9,.9]}}}
    outputs={name+'_handheld':model,name+'_icon':{'parent':'minecraft:item/generated','gui_light':'front','textures':{'layer0':'pixelmon_backport:item/'+name}},
             name:{'loader':'forge:separate-perspective','ambientocclusion':False,'textures':{'particle':'pixelmon_backport:item/'+name},'base':{'parent':'pixelmon_backport:item/'+name+'_handheld'},'perspectives':{'gui':{'parent':'pixelmon_backport:item/'+name+'_icon'}}}}
    for filename,data in outputs.items():(MODELS/(filename+'.json')).write_text(json.dumps(data,indent=2)+'\n','utf8')
    print(name,len(elements),'3D elements + GUI sprite')
