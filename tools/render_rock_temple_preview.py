"""Offline block geometry preview, not a game screenshot."""
import pathlib,nbtlib as n
from PIL import Image,ImageDraw,ImageFont
from vanilla_rock_temple import ROOT,CANONICAL

def main():
    source=n.load(CANONICAL);base=int(source['BackportVanillaBaseY'])
    blocks={tuple([int(b['pos'][0]),int(b['pos'][1])-base,int(b['pos'][2])]):str(source['palette'][int(b['state'])]['Name']) for b in source['blocks']}
    colors={'sandstone':'#d3bc83','chiseled_sandstone':'#b9a26b','cut_sandstone':'#e1cd9b','sandstone_stairs':'#cdb77e','sandstone_slab':'#d9c58f','orange_terracotta':'#b66a36','blue_terracotta':'#51557d','chest':'#b98030','tnt':'#bd4b44','stone_pressure_plate':'#858988'}
    def color(name):return colors.get(name.split(':')[-1],'#cec5b1')
    def shade(c,f):return tuple(int(int(c[i:i+2],16)*f) for i in (1,3,5))
    image=Image.new('RGB',(1560,980),'#f4f1e9');draw=ImageDraw.Draw(image)
    font=lambda size:ImageFont.truetype('C:/Windows/Fonts/msyh.ttc',size)
    draw.text((42,24),'岩石城市 · 原版沙漠神殿',font=font(34),fill='#292d30')
    draw.text((44,77),'离线模板预览｜1.16.5 原版生成布局｜非游戏截图，楼梯以方块体积示意',font=font(18),fill='#62696a')
    scale=20;origin=(70,450)
    def project(x,y,z):return (origin[0]+(x+z)*scale,origin[1]+(x-z)*scale*.5-y*scale)
    visible={p:v for p,v in blocks.items() if p[1]>=0 and v!='minecraft:air'}
    for (x,y,z),name in sorted(visible.items(),key=lambda row:(row[0][0]-row[0][2]+row[0][1],row[0][1])):
        c=color(name)
        for delta,vertices,tint in [((0,0,-1),[(x,y,z),(x+1,y,z),(x+1,y+1,z),(x,y+1,z)],.74),((1,0,0),[(x+1,y,z),(x+1,y,z+1),(x+1,y+1,z+1),(x+1,y+1,z)],.87),((0,1,0),[(x,y+1,z),(x+1,y+1,z),(x+1,y+1,z+1),(x,y+1,z+1)],1)]:
            if tuple(p+q for p,q in zip((x,y,z),delta)) not in visible:draw.polygon([project(*p) for p in vertices],fill=shade(c,tint),outline=shade(c,.67))
    draw.text((55,794),'21 × 21 格完整主体：双塔、阶梯屋顶、橙色纹样与原版入口',font=font(20),fill='#303a3b')
    draw.text((55,831),'地下部分通过独立结构连接；保持地面高度与原有城市道路衔接。',font=font(18),fill='#62696a')
    draw.line((970,130,970,925),fill='#d4d3ca',width=2)
    draw.text((1010,132),'地下藏宝室 · y = -11',font=font(22),fill='#303a3b')
    cell=37;left,top=1035,190
    for z in range(6,15):
        for x in range(6,15):
            name=blocks.get((x,-11,z),'minecraft:air');fill=color(name) if name!='minecraft:air' else '#eeeeea'
            box=(left+(x-6)*cell,top+(z-6)*cell,left+(x-5)*cell-1,top+(z-5)*cell-1);draw.rectangle(box,fill=fill,outline='#d3cfbf')
            if name=='minecraft:chest':draw.text((box[0]+5,box[1]+7),'箱',font=font(20),fill='#382c21')
            if name=='minecraft:stone_pressure_plate':draw.text((box[0]+5,box[1]+7),'板',font=font(20),fill='#ffffff')
    draw.text((1010,552),'4 个宝箱：原版沙漠神殿战利品表',font=font(18),fill='#414b4c')
    draw.text((1010,587),'保留压力板与下方 3 × 3 TNT 陷阱',font=font(18),fill='#414b4c')
    draw.text((1010,650),'城市适配',font=font(22),fill='#303a3b')
    for i,line in enumerate(['两种广场均为 35 × 35 格，四周留白 7 格。','清除旧砂岩地框，使用连续城市铺装。','原有服务与连接点数据保留。','地上、地下合并后逐格核对原版布局。','调整后布局仍待游戏内人工验收。']):draw.text((1010,696+i*35),line,font=font(17),fill='#62696a')
    target=ROOT/'.cache/rock-vanilla-preview.png';image.save(target);print(target)

if __name__=='__main__':main()
