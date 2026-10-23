package local.pixelmon.backport;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.pixelmonmod.pixelmon.world.structure.type.GymConfig;
import com.pixelmonmod.pixelmon.world.structure.type.GymStructure;
import net.minecraft.util.math.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraft.world.server.*;
import net.minecraftforge.registries.DeferredRegister;
import local.pixelmon.backport.mixin.CompassChunkRequestInvoker;

/** Compass aliases verify real rooms; aliases themselves never generate structures. */
public final class AuthorBossLocator {
    private AuthorBossLocator(){}
    public static void registerAliases(DeferredRegister<Structure<?>> registry){
        registry.register("author_gym",()->new RoomAlias(false));
    }
    public static final class RoomAlias extends GymStructure {
        public final boolean physical;
        RoomAlias(boolean physical){super(GymConfig.CODEC);this.physical=physical;}
        @Override public boolean isSuitable(Biome.Category category,String name){return false;}
    }
    public static boolean available(ServerWorld world,RoomAlias alias){
        if(alias.physical)return false;
        if(!AuthorRoomIndex.get(world).positions(alias.physical).isEmpty())return true;
        String dim=world.func_234923_W_().func_240901_a_().toString();
        if(dim.contains("drowned")||alias.physical&&!dim.equals("minecraft:overworld"))return false;
        return world.func_73046_m().func_240793_aU_().func_230418_z_().func_236222_c_()
            && world.func_72863_F().func_201711_g().func_202090_b().func_235203_c_().stream().anyMatch(b->matches(world,b,alias.physical));
    }
    private static boolean matches(ServerWorld world,Biome biome,boolean physical){
        var id=world.func_241828_r().func_243612_b(net.minecraft.util.registry.Registry.field_239720_u_).func_177774_c(biome);
        String dim=world.func_234923_W_().func_240901_a_().toString(),name=id==null?"":id.toString(),cat=biome.func_201856_r().name().toLowerCase(Locale.ROOT);
        return physical?AuthorBossPolicy.physical(dim,cat,name):!AuthorBossPolicy.physical(dim,cat,name)&&AuthorBossPolicy.theme(dim,cat,name)!=null;
    }
    public static int config(String key,int fallback){
        try{Object general=Class.forName("com.chaosthedude.explorerscompass.config.ConfigHandler").getField("GENERAL").get(null);
            Object value=general.getClass().getField(key).get(general);return ((Number)value.getClass().getMethod("get").invoke(value)).intValue();
        }catch(ReflectiveOperationException e){return fallback;}
    }
    public static final class Search implements AutoCloseable {
        private final ServerWorld world;private final boolean physical;private final BlockPos origin;
        private final int radius,maxSamples,cx,cz,range;private final List<BlockPos> known;private final Set<Long> checked=new HashSet<>();
        private int knownCursor,ring,edge,samples;private Candidate pending;private boolean done;
        public Search(ServerWorld world,BlockPos origin,RoomAlias alias){
            this.world=world;this.origin=origin;physical=alias.physical;radius=Math.max(0,config("maxRadius",8192));maxSamples=Math.max(0,config("maxSamples",50000));
            cx=origin.func_177958_n()>>4;cz=origin.func_177952_p()>>4;range=radius/16+1;
            known=AuthorRoomIndex.get(world).positions(physical);known.removeIf(p->StrongholdLocator.distance(p,origin)>(long)radius*radius);known.sort(Comparator.comparingLong(p->StrongholdLocator.distance(p,origin)));
        }
        public boolean finished(){return done;}
        public void skipFound(){done=false;}
        public BlockPos step(){
            if(done)return null;
            if(pending!=null){
                if(!pending.ready(world))return null;
                ChunkPos chunk=pending.pos;closePending();
                for(BlockPos p:AuthorRoomIndex.get(world).positions(physical))if((p.func_177958_n()>>4)==chunk.field_77276_a&&(p.func_177952_p()>>4)==chunk.field_77275_b
                    &&StrongholdLocator.distance(p,origin)<=(long)radius*radius&&AuthorRoomIndex.intact(world,p,physical)){done=true;return p;}
            }
            while(knownCursor<known.size()){
                BlockPos p=known.get(knownCursor++);ChunkPos chunk=new ChunkPos(p);
                if(!checked.add(chunk.func_201841_a()))continue;
                if(AuthorRoomIndex.intact(world,p,physical)){done=true;return p;}
                pending=new Candidate(world,chunk,p);return null;
            }
            if(!world.func_73046_m().func_240793_aU_().func_230418_z_().func_236222_c_()){done=true;return null;}
            long deadline=System.nanoTime()+1_000_000L;
            for(int budget=0;budget<512&&System.nanoTime()<deadline;budget++){
                if(ring>range||samples>=maxSamples){done=true;return null;}
                ChunkPos chunk=next();
                if(!AuthorBossPolicy.candidate(world.func_72905_C(),chunk.field_77276_a,chunk.field_77275_b,physical))continue;
                BlockPos center=new BlockPos(chunk.field_77276_a*16+8,64,chunk.field_77275_b*16+8);
                if(StrongholdLocator.distance(center,origin)>(long)radius*radius||!checked.add(chunk.func_201841_a()))continue;
                samples++;
                Biome biome=world.func_72863_F().func_201711_g().func_202090_b().func_225526_b_(center.func_177958_n()>>2,16,center.func_177952_p()>>2);
                if(!matches(world,biome,physical))continue;
                pending=new Candidate(world,chunk,null);return null;
            }
            return null;
        }
        private ChunkPos next(){
            if(ring==0){ring=1;return new ChunkPos(cx,cz);}
            int side=edge/(2*ring),offset=edge%(2*ring),x,z;
            if(side==0){x=-ring+offset;z=-ring;}else if(side==1){x=ring;z=-ring+offset;}else if(side==2){x=ring-offset;z=ring;}else{x=-ring;z=ring-offset;}
            if(++edge==8*ring){edge=0;ring++;}return new ChunkPos(cx+x,cz+z);
        }
        private void closePending(){if(pending!=null){pending.close(world);pending=null;}}
        @Override public void close(){closePending();done=true;}
    }
    private static final class Candidate {
        private static final TicketType<Long> TICKET=TicketType.func_223183_a("backport_author_search",Long::compare,400);
        private static final java.util.concurrent.atomic.AtomicLong IDS=new java.util.concurrent.atomic.AtomicLong();
        final ChunkPos pos;final long id=IDS.incrementAndGet(),startedTick,startedNanos=System.nanoTime();final CompletableFuture<?> loaded;
        final List<ChunkPos> tickets=new ArrayList<>();
        long readyAfter=Long.MAX_VALUE;
        Candidate(ServerWorld world,ChunkPos pos,BlockPos known){
            this.pos=pos;startedTick=world.func_82737_E();
            int minX=known==null?pos.field_77276_a:(known.func_177958_n()-6)>>4,maxX=known==null?pos.field_77276_a:(known.func_177958_n()+6)>>4;
            int minZ=known==null?pos.field_77275_b:(known.func_177952_p()-6)>>4,maxZ=known==null?pos.field_77275_b:(known.func_177952_p()+6)>>4;
            List<CompletableFuture<?>> requests=new ArrayList<>();
            try{
                for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
                    ChunkPos chunk=new ChunkPos(x,z);tickets.add(chunk);world.func_72863_F().func_217228_a(TICKET,chunk,2,id);
                    requests.add(((CompassChunkRequestInvoker)world.func_72863_F()).backport$requestChunk(x,z,ChunkStatus.field_222617_m,true)
                        .thenApply(result->{if(result.left().isEmpty())throw new IllegalStateException("Author room candidate unavailable");return result.left().get();}));
                }
                loaded=CompletableFuture.allOf(requests.toArray(new CompletableFuture<?>[0]));
            }
            catch(RuntimeException failure){close(world);throw failure;}
        }
        boolean ready(ServerWorld world){
            if(world.func_82737_E()-startedTick>=300||System.nanoTime()-startedNanos>=15_000_000_000L)throw new IllegalStateException("Author room search candidate timed out");
            if(!loaded.isDone())return false;loaded.join();
            if(readyAfter==Long.MAX_VALUE)readyAfter=world.func_82737_E()+2;
            return world.func_82737_E()>=readyAfter;
        }
        void close(ServerWorld world){for(ChunkPos chunk:tickets)world.func_72863_F().func_217222_b(TICKET,chunk,2,id);tickets.clear();}
    }
}
