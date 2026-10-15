"""Record the installed 1.16.5 DesertPyramidPiece bytecode into a reusable NBT.

No game/server starts. The native generator runs against local-coordinate block
recorders. Ground below its fixed foundation is solid (so fill-down is a no-op).
The generated room, traps, stair facings and chest loot are retained.
"""
import hashlib, json, pathlib, re, subprocess, sys, zipfile
import nbtlib as n

ROOT=pathlib.Path(__file__).resolve().parents[1]
PACKAGE='net/minecraft/world/gen/feature/structure/'
BLOCKS={'field_150322_A':'sandstone','field_150335_W':'tnt','field_150350_a':'air',
        'field_150372_bz':'sandstone_stairs','field_150456_au':'stone_pressure_plate',
        'field_196583_aj':'chiseled_sandstone','field_196585_ak':'cut_sandstone',
        'field_196640_bx':'sandstone_slab','field_196778_fp':'orange_terracotta',
        'field_196797_fz':'blue_terracotta'}

def export():
    build=json.loads((ROOT/'.cache/last-build.json').read_text(encoding='utf8'))
    cp=build['classpath'];java=pathlib.Path(build['java']);stage=ROOT/'.cache/vanilla-pyramid-export'
    sources=stage/'src';classes=stage/'classes';sources.mkdir(parents=True,exist_ok=True);classes.mkdir(exist_ok=True)
    native=next(pathlib.Path(p) for p in cp.split(';') if p.endswith('client-1.16.5-20210115.111550-srg.jar'))
    with zipfile.ZipFile(native) as jar: bytecode=jar.read(PACKAGE+'DesertPyramidPiece.class')
    def source(name,body):
        p=sources/(name.replace('.','/')+'.java');p.parent.mkdir(parents=True,exist_ok=True)
        package,_,simple=name.rpartition('.');p.write_text(('package '+package+';\n' if package else '')+body,encoding='utf8');return str(p)
    files=[]
    for name in ['world.ISeedReader','world.gen.ChunkGenerator','world.gen.feature.structure.StructureManager','world.gen.feature.template.TemplateManager','util.math.MutableBoundingBox','util.math.ChunkPos','util.math.BlockPos']:
        simple=name.rsplit('.',1)[-1];files.append(source('net.minecraft.'+name,'public '+('interface' if simple=='ISeedReader' else 'class')+' '+simple+' {}'))
    files.append(source('net.minecraft.nbt.CompoundNBT','public class CompoundNBT {public boolean func_74767_n(String k){return false;}public void func_74757_a(String k,boolean v){}}'))
    files.append(source('net.minecraft.state.Property','public class Property {public String name;public Property(String n){name=n;}}'))
    files.append(source('net.minecraft.state.DirectionProperty','public class DirectionProperty extends Property {public DirectionProperty(){super("facing");}}'))
    files.append(source('net.minecraft.block.StairsBlock','public class StairsBlock {public static final net.minecraft.state.DirectionProperty field_176309_a=new net.minecraft.state.DirectionProperty();}'))
    files.append(source('net.minecraft.block.BlockState','''public class BlockState {
      public String name;public java.util.Map<String,String> props=new java.util.TreeMap<>();
      public BlockState(String n){name="minecraft:"+n;
       if(n.endsWith("_stairs")){props.put("facing","north");props.put("half","bottom");props.put("shape","straight");props.put("waterlogged","false");}
       if(n.endsWith("_slab")){props.put("type","bottom");props.put("waterlogged","false");}
       if(n.equals("tnt"))props.put("unstable","false");if(n.equals("stone_pressure_plate"))props.put("powered","false");}
      public Object func_206870_a(net.minecraft.state.Property p,Comparable v){BlockState copy=new BlockState(name.substring(10));copy.props.putAll(props);copy.props.put(p.name,v.toString().toLowerCase(java.util.Locale.ROOT));return copy;}
    }'''))
    files.append(source('net.minecraft.block.Block','public class Block {String name;public Block(String n){name=n;}public BlockState func_176223_P(){return new BlockState(name);}}'))
    files.append(source('net.minecraft.block.Blocks','public class Blocks {'+''.join('public static final Block '+key+'=new Block("'+value+'");' for key,value in BLOCKS.items())+'}'))
    files.append(source('net.minecraft.util.Direction','''public enum Direction {
      SOUTH(0,0,1),WEST(1,-1,0),NORTH(2,0,-1),EAST(3,1,0);
      final int index,x,z;Direction(int i,int X,int Z){index=i;x=X;z=Z;}
      public int func_176736_b(){return index;}public int func_82601_c(){return x;}public int func_82599_e(){return z;}
      public enum Plane implements Iterable<Direction>{HORIZONTAL;public java.util.Iterator<Direction> iterator(){return java.util.Arrays.asList(SOUTH,WEST,NORTH,EAST).iterator();}}
    }'''))
    files.append(source('net.minecraft.util.ResourceLocation','public class ResourceLocation {String id;public ResourceLocation(String n){id=n;}public String toString(){return id;}}'))
    files.append(source('net.minecraft.loot.LootTables','public class LootTables {public static final net.minecraft.util.ResourceLocation field_186429_k=new net.minecraft.util.ResourceLocation("minecraft:chests/desert_pyramid");}'))
    files.append(source('net.minecraft.world.gen.feature.structure.IStructurePieceType','public interface IStructurePieceType {IStructurePieceType field_214766_M=new IStructurePieceType(){};}'))
    files.append(source('record.Capture','''public class Capture {
      public static final java.util.Map<String,java.util.Map<String,Object>> blocks=new java.util.TreeMap<>();
      public static int fills,sets,foundations,chests;
      public static void put(int x,int y,int z,net.minecraft.block.BlockState state,java.util.Map<String,Object> tag){
       java.util.Map<String,Object> b=new java.util.LinkedHashMap<>();b.put("pos",new int[]{x,y,z});b.put("name",state.name);b.put("properties",state.props);if(tag!=null)b.put("nbt",tag);blocks.put(x+","+y+","+z,b);}
    }'''))
    files.append(source('net.minecraft.world.gen.feature.structure.ScatteredStructurePiece','''public class ScatteredStructurePiece {
      protected int field_202581_a,field_202582_b,field_202583_c;
      protected ScatteredStructurePiece(IStructurePieceType t,java.util.Random r,int x,int y,int z,int w,int h,int d){field_202581_a=w;field_202582_b=h;field_202583_c=d;}
      protected ScatteredStructurePiece(IStructurePieceType t,net.minecraft.nbt.CompoundNBT n){}
      protected void func_143011_b(net.minecraft.nbt.CompoundNBT n){}
      protected void func_175804_a(net.minecraft.world.ISeedReader world,net.minecraft.util.math.MutableBoundingBox box,int x,int y,int z,int X,int Y,int Z,net.minecraft.block.BlockState outer,net.minecraft.block.BlockState inner,boolean skipAir){
       if(skipAir)throw new AssertionError("World-dependent fill needs review");record.Capture.fills++;
       for(int yy=y;yy<=Y;yy++)for(int xx=x;xx<=X;xx++)for(int zz=z;zz<=Z;zz++)record.Capture.put(xx,yy,zz,xx==x||xx==X||yy==y||yy==Y||zz==z||zz==Z?outer:inner,null);}
      protected void func_175811_a(net.minecraft.world.ISeedReader w,net.minecraft.block.BlockState state,int x,int y,int z,net.minecraft.util.math.MutableBoundingBox box){record.Capture.sets++;record.Capture.put(x,y,z,state,null);}
      protected void func_175808_b(net.minecraft.world.ISeedReader w,net.minecraft.block.BlockState state,int x,int y,int z,net.minecraft.util.math.MutableBoundingBox box){if(y!=-5)throw new AssertionError("Unexpected fill-down start");record.Capture.foundations++;}
      protected boolean func_186167_a(net.minecraft.world.ISeedReader w,net.minecraft.util.math.MutableBoundingBox box,java.util.Random random,int x,int y,int z,net.minecraft.util.ResourceLocation loot){
       record.Capture.chests++;net.minecraft.block.BlockState state=new net.minecraft.block.BlockState("chest");
       state.props.put("facing",x<10?"east":x>10?"west":z<10?"south":"north");state.props.put("type","single");state.props.put("waterlogged","false");
       java.util.Map<String,Object> tag=new java.util.LinkedHashMap<>();tag.put("id","minecraft:chest");tag.put("LootTable",loot.toString());tag.put("LootTableSeed",random.nextLong());record.Capture.put(x,y,z,state,tag);return true;}
    }'''))
    files.append(source('VanillaPyramidExport','''import com.google.gson.Gson; public class VanillaPyramidExport {public static void main(String[] args){
      boolean ok=new net.minecraft.world.gen.feature.structure.DesertPyramidPiece(new java.util.Random(0),0,0).func_230383_a_(null,null,null,new java.util.Random(143),null,null,null);
      if(!ok||record.Capture.chests!=4)throw new AssertionError("Incomplete native generation");
      java.util.Map<String,Object> result=new java.util.LinkedHashMap<>();result.put("blocks",record.Capture.blocks.values());result.put("fills",record.Capture.fills);result.put("sets",record.Capture.sets);result.put("foundationColumns",record.Capture.foundations);result.put("chests",record.Capture.chests);System.out.println(new Gson().toJson(result));}}
    '''))
    subprocess.run([str(java.with_name('javac.exe')),'--release','11','-encoding','UTF-8','-proc:none','-cp',cp,'-d',str(classes),*files],check=True)
    target=classes/(PACKAGE+'DesertPyramidPiece.class');target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(bytecode)
    result=json.loads(subprocess.check_output([str(java),'-cp',str(classes)+';'+cp,'VanillaPyramidExport'],text=True,encoding='utf8'))
    blocks=result['blocks'];minimum=min(b['pos'][1] for b in blocks);maximum=max(b['pos'][1] for b in blocks)
    assert min(b['pos'][0] for b in blocks)==0 and max(b['pos'][0] for b in blocks)==20
    assert min(b['pos'][2] for b in blocks)==0 and max(b['pos'][2] for b in blocks)==20
    palette=[];ids={};entries=[]
    for block in sorted(blocks,key=lambda b:(b['pos'][1],b['pos'][0],b['pos'][2])):
        state={'Name':n.String(block['name'])}
        if block['properties']:state['Properties']=n.Compound({k:n.String(v) for k,v in block['properties'].items()})
        key=json.dumps(state,sort_keys=True);index=ids.get(key)
        if index is None:index=len(palette);ids[key]=index;palette.append(n.Compound(state))
        x,y,z=block['pos'];entry=n.Compound({'pos':n.List[n.Int]([x,y-minimum,z]),'state':n.Int(index)})
        if 'nbt' in block:entry['nbt']=n.Compound({k:n.Long(v) if isinstance(v,int) else n.String(v) for k,v in block['nbt'].items() if k!='LootTableSeed'})
        entries.append(entry)
    value=n.File({'DataVersion':n.Int(2586),'size':n.List[n.Int]([21,maximum-minimum+1,21]),'palette':n.List[n.Compound](palette),'blocks':n.List[n.Compound](entries),'entities':n.List[n.Compound](), 'BackportVanillaBaseY':n.Int(-minimum)})
    out=ROOT/'resources/backport/structures/vanilla_desert_pyramid.nbt';out.parent.mkdir(parents=True,exist_ok=True)
    import io,gzip
    stream=io.BytesIO();value.write(stream);out.write_bytes(gzip.compress(stream.getvalue(),mtime=0))
    metadata={k:v for k,v in result.items() if k!='blocks'}
    metadata.update({'source':'Minecraft 1.16.5 DesertPyramidPiece.func_230383_a_','sourceClassSha256':hashlib.sha256(bytecode).hexdigest(),'coordinateMode':'unrotated local coordinates; solid terrain below foundation; chest facings point into chamber','lootSeeds':'omitted to roll fresh loot for each placed chest','nativeMinY':minimum,'nativeMaxY':maximum,'footprint':[21,21],'recordedBlocks':len(entries),'outputSha256':hashlib.sha256(out.read_bytes()).hexdigest()})
    (out.with_suffix('.json')).write_text(json.dumps(metadata,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print(json.dumps(metadata,ensure_ascii=False,indent=2))

if __name__=='__main__':export()
