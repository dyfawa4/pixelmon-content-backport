package local.pixelmon.backport;

import java.util.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.pixelmonmod.pixelmon.world.structure.PixelmonStructures;
import com.pixelmonmod.pixelmon.world.structure.type.GymConfig;
import com.pixelmonmod.pixelmon.world.structure.type.GymStructure;
import net.minecraft.command.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.world.IStructureReader;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.*;
import net.minecraft.world.gen.feature.structure.*;
import net.minecraft.world.gen.settings.StructureSeparationSettings;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;
import local.pixelmon.backport.mixin.CompassChunkRequestInvoker;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.*;

/** Search aliases index native gym starts; they are never added to biome generation. */
public final class GymLocator {
    public static final DeferredRegister<Structure<?>> ALIASES=DeferredRegister.create(ForgeRegistries.STRUCTURE_FEATURES,Content.NS);
    static {for(String type:GymThemePaths.TYPES)ALIASES.register(type+"_gym",()->new TypedGym(type));TeraDenLocator.registerAliases(ALIASES);AuthorBossLocator.registerAliases(ALIASES);}
    /** Forge registry IDs alone do not populate the legacy structure NBT name map. */
    public static void registerNames(){
        int repaired=0;
        for(var entry:ALIASES.getEntries()){
            Structure<?> value=entry.get();String key=entry.getId().toString();
            Structure<?> previous=Structure.field_236365_a_.get(key);
            if(previous!=null && previous!=value)throw new IllegalStateException("Conflicting structure save name "+key);
            if(value.func_143025_a()==null)repaired++;
            Structure.field_236365_a_.put(key,value);
        }
        org.apache.logging.log4j.LogManager.getLogger("pixelmon_backport").info("Registered {} structure save names; repaired {} missing legacy names",ALIASES.getEntries().size(),repaired);
    }
    private static final Map<CommandSource,Search> jobs=new LinkedHashMap<>();
    public static final class TypedGym extends GymStructure {
        public final String type;
        TypedGym(String type){super(GymConfig.CODEC);this.type=type;}
        @Override public boolean isSuitable(Biome.Category category,String name){return false;}
    }
    public static Structure<?> nativeGym(){return PixelmonStructures.GYMS.get();}
    public static StructureSeparationSettings settings(ServerWorld world){return world.func_72863_F().func_201711_g().func_235957_b_().func_236197_a_(nativeGym());}
    public static boolean available(ServerWorld world){
        return world.func_73046_m().func_240793_aU_().func_230418_z_().func_236222_c_()
            && settings(world)!=null && world.func_72863_F().func_201711_g().func_202090_b().func_205004_a(nativeGym());
    }
    public static boolean matches(StructureStart<?> start,String type){
        if(start==null || !start.func_75069_d())return false;
        if(type.equals("any"))return true;
        List<String> paths=new ArrayList<>();for(StructurePiece piece:start.func_186161_c())paths.add(piece.toString());
        return type.equals(GymThemePaths.classify(paths));
    }
    public static boolean available(ServerWorld world,String type){
        if(type.equals("any"))return available(world);
        if(type.equals("psychic")&&!world.func_234923_W_().func_240901_a_().toString().equals("minecraft:the_end"))return false;
        if(!available(world))return false;
        for(Biome biome:world.func_72863_F().func_201711_g().func_202090_b().func_235203_c_())
            for(var supplier:biome.func_242440_e().func_242487_a()){
                var feature=supplier.get();
                if(feature.field_236268_b_==nativeGym() && feature.field_236269_c_ instanceof GymConfig){
                    String pool=((GymConfig)feature.field_236269_c_).func_242810_c().get().func_214947_b().toString();
                    Set<String> configured=GymThemePaths.poolTypes(pool);
                    // Custom datapacks may use an unknown pool; inspect real starts in that case.
                    if(configured.isEmpty() || configured.contains(type))return true;
                }
            }
        return false;
    }
    /** Also used by the optional compass adapter; query the existing gym index. */
    public static StructureStart<?> filteredStart(StructureManager manager,SectionPos pos,Structure<?> requested,IStructureReader chunk){
        StructureStart<?> start=manager.func_235013_a_(pos,requested instanceof TypedGym?nativeGym():requested,chunk);
        return requested instanceof TypedGym && !matches(start,((TypedGym)requested).type)?null:start;
    }
    @SubscribeEvent public void commands(RegisterCommandsEvent event){
        var command=Commands.func_197057_a("locategym");
        for(int i=0;i<GymThemePaths.TYPES.length;i++){
            final String type=GymThemePaths.TYPES[i];
            for(String spelling:new String[]{type,GymThemePaths.CHINESE[i]})command.then(Commands.func_197057_a(spelling)
                .executes(ctx->begin(ctx.getSource(),type,8192)).then(Commands.func_197056_a("radius",IntegerArgumentType.integer(512,32768))
                .executes(ctx->begin(ctx.getSource(),type,IntegerArgumentType.getInteger(ctx,"radius")))));
        }
        command.then(Commands.func_197057_a("cancel").executes(ctx->{cancel(ctx.getSource());ctx.getSource().func_197030_a(new TranslationTextComponent("backport.gym.locate.cancelled"),false);return 1;}));
        event.getDispatcher().register(command);
    }
    private static boolean sameSource(CommandSource a,CommandSource b){return a.func_197022_f()!=null?a.func_197022_f()==b.func_197022_f():a.func_197037_c().equals(b.func_197037_c());}
    public static void cancel(CommandSource source){jobs.entrySet().removeIf(e->{if(!sameSource(e.getKey(),source))return false;e.getValue().close();return true;});}
    public static int begin(CommandSource source,String type,int radius){
        ServerWorld world=source.func_197023_e();
        if(!available(world,type)){source.func_197021_a(new TranslationTextComponent("backport.gym.locate.unavailable"));return 0;}
        cancel(source);
        jobs.put(source,new Search(world,new BlockPos(source.func_197036_d()),type,radius));
        source.func_197030_a(new TranslationTextComponent("backport.gym.locate.searching",GymThemePaths.chinese(type),radius),false);return 1;
    }
    @SubscribeEvent public void unload(WorldEvent.Unload event){jobs.entrySet().removeIf(e->{if(e.getValue().world!=event.getWorld())return false;e.getValue().close();return true;});}
    @SubscribeEvent public void stopped(net.minecraftforge.fml.event.server.FMLServerStoppedEvent event){jobs.values().forEach(Search::close);jobs.clear();}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        long deadline=System.nanoTime()+8_000_000L;
        for(Iterator<Map.Entry<CommandSource,Search>> it=jobs.entrySet().iterator();it.hasNext();){
            Map.Entry<CommandSource,Search> entry=it.next();CommandSource source=entry.getKey();Search search=entry.getValue();
            if(source.func_197022_f()!=null && (source.func_197022_f().field_70128_L || source.func_197022_f().field_70170_p!=search.world)){search.close();it.remove();continue;}
            BlockPos result;
            try{result=search.step();}catch(RuntimeException failure){
                search.close();it.remove();source.func_197021_a(new TranslationTextComponent("backport.gym.locate.failed"));
                org.apache.logging.log4j.LogManager.getLogger("pixelmon_backport").warn("Gym locate request failed",failure);continue;
            }
            if(result!=null){
                String tp="/tp @s "+result.func_177958_n()+" ~ "+result.func_177952_p();
                IFormattableTextComponent message=new TranslationTextComponent("backport.gym.locate.found",GymThemePaths.chinese(search.type),result.func_177958_n(),result.func_177952_p())
                    .func_240699_a_(TextFormatting.GREEN).func_240700_a_(style->style.func_240715_a_(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,tp)));
                source.func_197030_a(message,false);search.close();it.remove();
            }else if(search.finished()){source.func_197021_a(new TranslationTextComponent("backport.gym.locate.missing",GymThemePaths.chinese(search.type),search.radius));search.close();it.remove();}
            if(System.nanoTime()>=deadline)break;
        }
    }
    public static final class Search implements AutoCloseable {
        final ServerWorld world;final String type;final int radius;final List<ChunkPos> candidates=new ArrayList<>();final BlockPos origin;int cursor;
        private StartRequest pending;
        private boolean closed;
        public Search(ServerWorld world,BlockPos origin,String type,int radius){
            this.world=world;this.origin=origin;this.type=type;this.radius=radius;
            StructureSeparationSettings settings=settings(world);int spacing=settings.func_236668_a_();
            int cx=Math.floorDiv(origin.func_177958_n()>>4,spacing),cz=Math.floorDiv(origin.func_177952_p()>>4,spacing),range=radius/(spacing*16)+2;
            SharedSeedRandom random=new SharedSeedRandom();
            for(int x=cx-range;x<=cx+range;x++)for(int z=cz-range;z<=cz+range;z++){
                ChunkPos pos=nativeGym().func_236392_a_(settings,world.func_72905_C(),random,x*spacing,z*spacing);
                if(distance(pos)<=(long)(radius+16)*(radius+16))candidates.add(pos);
            }
            candidates.sort(Comparator.comparingLong(this::distance));
        }
        long distance(ChunkPos p){long dx=(long)p.field_77276_a*16+8-origin.func_177958_n(),dz=(long)p.field_77275_b*16+8-origin.func_177952_p();return dx*dx+dz*dz;}
        public boolean finished(){return closed||pending==null&&cursor>=candidates.size();}
        public int examined(){return cursor;}
        private void release(){if(pending!=null){pending.close();pending=null;}}
        public void close(){release();closed=true;}
        public BlockPos step(){
            if(finished())return null;
            if(pending==null){pending=new StartRequest(world,candidates.get(cursor++));return null;}
            if(!pending.ready())return null;
            StructureStart<?> start;
            try{start=pending.start();}finally{release();}
            if(!matches(start,type))return null;
            BlockPos found=start.func_204294_a();long dx=(long)found.func_177958_n()-origin.func_177958_n(),dz=(long)found.func_177952_p()-origin.func_177952_p();
            return dx*dx+dz*dz<=(long)radius*radius?found:null;
        }
    }
    /** Request only STRUCTURE_STARTS, never terrain, decoration, lighting or entities. */
    private static final class StartRequest implements AutoCloseable {
        private static final TicketType<Long> TICKET=TicketType.func_219484_a("backport_gym_search",Long::compare);
        private static long sequence;
        final ServerWorld world;final ChunkPos pos;final long id=++sequence,started=System.nanoTime();
        final int distance=-ChunkStatus.func_222599_a(ChunkStatus.field_222606_b);
        final java.util.concurrent.CompletableFuture<com.mojang.datafixers.util.Either<IChunk,net.minecraft.world.server.ChunkHolder.IChunkLoadingError>> future;
        StartRequest(ServerWorld world,ChunkPos pos){
            this.world=world;this.pos=pos;world.func_72863_F().func_217228_a(TICKET,pos,distance,id);
            try{future=((CompassChunkRequestInvoker)world.func_72863_F()).backport$requestChunk(pos.field_77276_a,pos.field_77275_b,ChunkStatus.field_222606_b,true);}
            catch(RuntimeException error){close();throw error;}
        }
        boolean ready(){if(System.nanoTime()-started>15_000_000_000L)throw new IllegalStateException("Gym structure-start request timed out at "+pos);return future.isDone();}
        StructureStart<?> start(){var chunk=future.join().left().orElseThrow(()->new IllegalStateException("Gym structure start unavailable at "+pos));return world.func_241112_a_().func_235013_a_(SectionPos.func_218156_a(pos,0),nativeGym(),chunk);}
        public void close(){world.func_72863_F().func_217222_b(TICKET,pos,distance,id);}
    }
}
