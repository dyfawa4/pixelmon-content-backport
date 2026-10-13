"""Build an original cap mesh on the native Pikachu skeleton. No stat changes."""
import argparse, copy, io, json, math, pathlib, struct, zipfile
from PIL import Image, ImageDraw
import numpy as np
from convert_content import Bmd, legacy_bmd
ROOT=pathlib.Path(__file__).resolve().parents[1]

def render_sprite(model, texture, target):
    """Render the real cap mesh and UVs to a transparent inventory sprite."""
    size=256; angle=math.pi/7; elevation=.12
    c,s=math.cos(angle),math.sin(angle); ce,se=math.cos(elevation),math.sin(elevation)
    triangles=[]
    for face in model.faces:
        vertices=[]
        for coords,weights in face:
            x,y,z=coords[:3]; xx=x*c-y*s; yy=x*s+y*c
            vertices.append((xx,z*ce+yy*se,yy*ce-z*se,coords[6],coords[7]))
        triangles.append(vertices)
    points=np.array(triangles); low=points[:,:,:2].min(axis=(0,1)); high=points[:,:,:2].max(axis=(0,1))
    scale=(size-16)/max(high-low); center=(high+low)/2
    pixels=np.zeros((size,size,4),dtype=np.uint8); depth=np.full((size,size),np.inf)
    atlas=np.array(texture); th,tw=atlas.shape[:2]
    for triangle in triangles:
        v=np.array(triangle); xy=(v[:,:2]-center)*scale
        xy[:,0]+=size/2; xy[:,1]=size/2-xy[:,1]
        x0,y0=np.maximum(0,np.floor(xy.min(axis=0)).astype(int)); x1,y1=np.minimum(size-1,np.ceil(xy.max(axis=0)).astype(int))
        if x1<x0 or y1<y0:continue
        ax,ay=xy[0];bx,by=xy[1];cx,cy=xy[2]
        den=(by-cy)*(ax-cx)+(cx-bx)*(ay-cy)
        if abs(den)<1e-8:continue
        yy,xx=np.mgrid[y0:y1+1,x0:x1+1];xx=xx+.5;yy=yy+.5
        a=((by-cy)*(xx-cx)+(cx-bx)*(yy-cy))/den
        b=((cy-ay)*(xx-cx)+(ax-cx)*(yy-cy))/den;d=1-a-b
        z=a*v[0,2]+b*v[1,2]+d*v[2,2]
        u=a*v[0,3]+b*v[1,3]+d*v[2,3];vv=a*v[0,4]+b*v[1,4]+d*v[2,4]
        colors=atlas[np.clip(((1-vv)*th).astype(int),0,th-1),np.clip((u*tw).astype(int),0,tw-1)]
        visible=(a>=-1e-6)&(b>=-1e-6)&(d>=-1e-6)&(z<depth[y0:y1+1,x0:x1+1])&(colors[:,:,3]>0)
        depth[y0:y1+1,x0:x1+1][visible]=z[visible]
        pixels[y0:y1+1,x0:x1+1][visible]=colors[visible]
    Image.fromarray(pixels).resize((128,128),Image.Resampling.LANCZOS).save(target)

def build(jar):
    z=zipfile.ZipFile(jar);stem='pokemon/025_pikachu/all/base/none/'
    native=Bmd(z.read('assets/pixelmon/textures/'+stem+'pikachu.bmd'))
    head=next(i for i,(_,name) in enumerate(native.bones) if name=='Head')
    cap=[]
    def triangle(points,color):
        a,b,c=points
        u=[b[i]-a[i] for i in range(3)];v=[c[i]-a[i] for i in range(3)]
        n=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]]
        length=math.sqrt(sum(t*t for t in n))
        if length<1e-8:return
        cap.append([(tuple(p)+tuple(t/length for t in n),color) for p in points])
    # Native BMD bind pose: Z is up; negative Y is the face. Ears remain animated.
    def point(ring,segment):
        angle=segment*math.tau/32; elevation=ring*math.pi/2/8
        return (4.15*math.cos(elevation)*math.cos(angle),-1+3.65*math.cos(elevation)*math.sin(angle),18.45+2.7*math.sin(elevation))
    for r in range(8):
        for s in range(32):
            color=1 if math.sin((s+.5)*math.tau/32)<-.55 else 0
            a,b,c,d=point(r,s),point(r,s+1),point(r+1,s+1),point(r+1,s)
            triangle((a,b,c),color);triangle((a,c,d),color)
    # Curved red visor extending forwards, including underside and rim.
    inner=[];outer=[]
    for s in range(17):
        a=math.pi+s*math.pi/16
        inner.append((4.15*math.cos(a),-1+3.65*math.sin(a),18.48))
        outer.append((4.7*math.cos(a),-1+6.0*math.sin(a),18.30))
    for s in range(16):
        a,b,c,d=inner[s],outer[s],outer[s+1],inner[s+1]
        triangle((a,b,c),0);triangle((a,c,d),0)
        triangle((c,b,a),0);triangle((d,c,a),0)
        e=(b[0],b[1],b[2]-.18);f=(c[0],c[1],c[2]-.18)
        triangle((b,e,f),0);triangle((b,f,c),0)
    # Green angular emblem on the front white panel, raised from its surface.
    logo=[(-.9,19.05),(-.45,19.95),(.8,19.95),(.8,19.60),(-.15,19.60),(-.45,19.05)]
    pts=[(x,-1-3.65*math.sqrt(max(.01,1-((zv-18.45)/2.7)**2-(x/4.15)**2))-.04,zv) for x,zv in logo]
    for i in range(1,len(pts)-1):triangle((pts[0],pts[i],pts[i+1]),2)
    output=ROOT/'resources/assets/pixelmon_backport/textures/pokemon/025_ash_cap'
    output.mkdir(parents=True,exist_ok=True)
    # Bmd normalizes the legacy exporter helper bone name. Normalize the same
    # header in reused native animations so body and animation skeletons agree.
    for animation in ['idle','walk']:
        raw=legacy_bmd(z.read('assets/pixelmon/textures/'+stem+animation+'.bmd'))
        assert Bmd(raw).bones==native.bones
        (output/(animation+'.bmd')).write_bytes(raw)
    for palette in ['none','shiny']:
        texture=Image.open(io.BytesIO(z.read('assets/pixelmon/textures/pokemon/025_pikachu/all/base/'+palette+'/texture.png'))).convert('RGBA')
        width=texture.width+16;atlas=Image.new('RGBA',(width,texture.height));atlas.paste(texture,(0,0))
        colors=[(196,34,43,255),(248,247,233,255),(23,124,72,255)]
        for i,col in enumerate(colors):ImageDraw.Draw(atlas).rectangle((texture.width, i*8,width-1,i*8+7),fill=col)
        atlas.save(output/(palette+'.png'))
        faces=[]
        for face in native.faces:
            faces.append([(tuple(v[:6])+ (v[6]*texture.width/width,v[7]),weights) for v,weights in face])
        for face in cap:
            faces.append([(coords+((texture.width+8)/width,1-(color*8+4)/texture.height),[(head,1.)]) for coords,color in face])
        stream=io.BytesIO()
        def w(fmt,*values):stream.write(struct.pack('>'+fmt,*values))
        w('Bh',1,len(native.bones))
        for i,(parent,name) in enumerate(native.bones):w('hh',i,parent);stream.write((name+'\0').encode('utf-16-be'))
        w('hh',1,len(native.bones))
        for i in range(len(native.bones)):w('h6f',i,*native.rest[i])
        w('h',1);stream.write('cap_atlas\0'.encode('utf-16-be'));w('h',len(faces))
        for face in faces:
            w('B',0)
            for coords,weights in face:
                w('h8fB',0,*coords,len(weights))
                for bone,weight in weights:w('hf',bone,weight)
        data=stream.getvalue();check=Bmd(data)
        assert check.bones==native.bones and len(check.faces)>len(native.faces)
        (output/(palette+'.bmd')).write_bytes(data)
        (output/(palette+'.pqc')).write_text('$body pixelmon_backport:textures/pokemon/025_ash_cap/'+palette+'.bmd\n$anim idle pixelmon_backport:textures/pokemon/025_ash_cap/idle.bmd\n$anim walk pixelmon_backport:textures/pokemon/025_ash_cap/walk.bmd\n$scale 0.4\n',encoding='utf-8')
    # Party/minimap icons use the native logical 32px size, not a 128px mesh render.
    from create_cap_sprites import build as build_sprites
    build_sprites(jar)
    species=json.loads(z.read('data/pixelmon/species/025_pikachu.json'))
    form=copy.deepcopy(species['forms'][0]);form['name']='ash_cap';form['defaultBaseForm']=''
    for gender in form['genderProperties']:
        gender['palettes']=[p for p in gender['palettes'] if p['name'] in ['none','shiny']]
        for palette in gender['palettes']:
            name=palette['name'];palette['texture']='pixelmon_backport:pokemon/025_ash_cap/'+name+'.png'
            palette['sprite']='pixelmon_backport:pokemon/025_ash_cap/'+name+'_sprite.png'
            palette['modelLocator']={'factoryType':'NORMAL','pqc':['pixelmon_backport:pokemon/025_ash_cap/'+name+'.pqc']}
    (ROOT/'resources/backport/ash_cap.json').write_text(json.dumps(form,ensure_ascii=False,indent=2),encoding='utf-8')
    print('Ash cap: native skeleton preserved; %d original hat triangles, normal/shiny atlases.'%len(cap))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--pixelmon',required=True);build(p.parse_args().pixelmon)
