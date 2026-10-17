"""Contact sheets and descriptive body-only metrics from real client screenshots.
No synthetic game rendering and no claim of all-species gameplay acceptance.
"""
import argparse
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import numpy as np

TYPES = '一般 火 水 草 电 冰 格斗 毒 地面 飞行 超能力 虫 岩石 幽灵 龙 恶 钢 妖精 星晶'.split()


def views(path):
    image = Image.open(path).convert('RGB')
    # These dedicated review instances explicitly use guiScale=2.
    gw, gh = image.width / 2, image.height / 2
    fit = min((gw-16)/800, (gh-16)/450)
    ox, oy = (gw-800*fit)/2, (gh-450*fit)/2
    def crop(x0, x1):
        return image.crop(tuple(round(v) for v in ((ox+x0*fit)*2, (oy+105*fit)*2, (ox+x1*fit)*2, (oy+350*fit)*2)))
    original, crystal = crop(209, 411), crop(413, 615)
    a = np.asarray(original).astype(float)
    mask = np.max(np.abs(a-np.array([32,55,75])), axis=2) > 20
    # Exclude silhouette antialiasing and the crown (not in the original mask).
    mask_image = Image.fromarray((mask*255).astype('uint8')).filter(ImageFilter.MinFilter(5))
    return original, crystal.resize(original.size), np.asarray(mask_image)>0


def tile(source, box):
    original, crystal, mask = source
    mask_image = Image.fromarray((mask*255).astype('uint8'))
    bounds = mask_image.getbbox()
    if bounds:
        x0,y0,x1,y1 = bounds
        crystal = crystal.crop((max(0,x0-4),max(0,y0-4),min(crystal.width,x1+4),min(crystal.height,y1+4)))
    crystal.thumbnail((box[0],box[1]))
    out = Image.new('RGB',box,(32,55,75))
    out.paste(crystal,((box[0]-crystal.width)//2,(box[1]-crystal.height)//2))
    return out


def main():
    p=argparse.ArgumentParser()
    p.add_argument('--baseline',type=Path,required=True)
    p.add_argument('--candidate',type=Path,required=True)
    p.add_argument('--output',type=Path,required=True)
    args=p.parse_args();args.output.mkdir(parents=True,exist_ok=True)
    baseline=json.loads((args.baseline/'tera-review-results.json').read_text(encoding='utf-8'))
    candidate=json.loads((args.candidate/'tera-review-results.json').read_text(encoding='utf-8'))
    assert len(baseline)==len(candidate) and not any(r['error'] for r in baseline+candidate)
    old={r['id']:views(args.baseline/r['screenshot']) for r in baseline}
    new={r['id']:views(args.candidate/r['screenshot']) for r in candidate}
    assert old.keys()==new.keys()
    font=ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',18)
    subjects=sorted({key.rsplit('-',2)[0] for key in new})
    metrics={}
    for subject in subjects:
        sheet=Image.new('RGB',(1250,960),(10,20,32));draw=ImageDraw.Draw(sheet)
        delta=[]
        for t in range(19):
            key=f'{subject}-{t}-40'
            x,y=(t%5)*250,(t//5)*240
            draw.text((x+8,y+6),TYPES[t],font=font,fill=(218,238,245))
            sheet.paste(tile(new[key],(242,205)),(x+4,y+30))
            orig,crystal,mask=new[key]
            diff=np.abs(np.asarray(crystal).astype(float)-np.asarray(orig).astype(float)).mean(axis=2)
            delta.append(float(np.median(diff[mask])))
        sheet.save(args.output/f'{subject}-all-19.png')
        comparisons={}
        for a,b in [(2,4),(13,17),(0,16),(2,5),(10,17)]:
            values=[]
            for images in [old,new]:
                va=images[f'{subject}-{a}-40'];vb=images[f'{subject}-{b}-40']
                mask=va[2]&vb[2]
                difference=np.abs(np.asarray(va[1]).astype(float)-np.asarray(vb[1]).astype(float)).mean(axis=2)
                values.append(round(float(np.mean(difference[mask])),3))
            comparisons[f'{TYPES[a]}/{TYPES[b]}']={'r9BodyRgbDifference':values[0],'candidateBodyRgbDifference':values[1]}
        metrics[subject]={'pairs':comparisons,'medianSkinChangeByType':dict(zip(TYPES,delta))}
        for angle in [40,110]:
            sheet=Image.new('RGB',(1200,880),(10,20,32));draw=ImageDraw.Draw(sheet)
            for row,t in enumerate([2,4,13,17]):
                for col,images in enumerate([old,new]):
                    x,y=col*600,row*220
                    draw.text((x+10,y+5),('r9' if col==0 else 'candidate')+' '+TYPES[t],font=font,fill=(218,238,245))
                    sheet.paste(tile(images[f'{subject}-{t}-{angle}'],(580,185)),(x+10,y+30))
            sheet.save(args.output/f'{subject}-before-after-{angle}.png')
    report={'casesPerBuild':len(candidate),'subjects':metrics,'note':'Descriptive RGB differences in actual body screenshots. Crowns and UI excluded. Not an all-species acceptance result.'}
    (args.output/'pixel-review.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({'casesPerBuild':len(candidate),'subjects':len(subjects),'output':str(args.output)},ensure_ascii=False))


if __name__=='__main__':main()
