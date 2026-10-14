package local.pixelmon.backport;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.pixelmonmod.pixelmon.world.structure.type.GymConfig;
import com.pixelmonmod.pixelmon.world.structure.type.GymStructure;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.feature.structure.*;
import net.minecraft.world.gen.settings.StructureSeparationSettings;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;
import local.pixelmon.backport.mixin.CompassChunkRequestInvoker;
import net.minecraftforge.registries.*;

/** Search aliases use native den starts, then verify the actual converted entity. */
public final class TeraDenLocator {
    private TeraDenLocator(){}
    public static final String[] BIOMES={"badlands","beach","desert","end","forest","ice","jungle","mountain","mushroom","nether","plains","snow","taiga","water","swamp","ultra_deep_sea","ultra_desert","ultra_forest","ultra_jungle","ultra_plant"};
    public static void registerAliases(DeferredRegister<Structure<?>> registry){
        for(String biome:BIOMES)registry.register(biome+"_tera_den",()->new TypedDen(biome));
    }
    public static final class TypedDen extends GymStructure {
        public final String biome;
        TypedDen(String biome){super(GymConfig.CODEC);this.biome=biome;}
        @Override public boolean isSuitable(net.minecraft.world.biome.Biome.Category category,String name){return false;}
        public Structure<?> nativeDen(){return ForgeRegistries.STRUCTURE_FEATURES.getValue(new ResourceLocation("pixelmon",biome+"_den"));}
    }
    public static StructureSeparationSettings settings(ServerWorld world,TypedDen alias){
        Structure<?> nativeDen=alias.nativeDen();return nativeDen==null?null:world.func_72863_F().func_201711_g().func_235957_b_().func_236197_a_(nativeDen);
    }
    public static boolean available(ServerWorld world,TypedDen alias){
        return settings(world,alias)!=null && world.func_72863_F().func_201711_g().func_202090_b().func_205004_a(alias.nativeDen());
    }
    public static final class Candidate {
        private static final TicketType<Long> TICKET=TicketType.func_223183_a("backport_tera_search",Long::compare,400);
        private static final java.util.concurrent.atomic.AtomicLong IDS=new java.util.concurrent.atomic.AtomicLong();
        private final long id=IDS.incrementAndGet();
        private final List<ChunkPos> tickets=new ArrayList<>();
        private final long startedTick;
        private final long startedNanos=System.nanoTime();
        private final AxisAlignedBB box;
        private final CompletableFuture<?> loaded;
        private long readyAfter=Long.MAX_VALUE;
        public Candidate(ServerWorld world,StructureStart<?> start){
            startedTick=world.func_72820_D();
            MutableBoundingBox b=start.func_75071_a();
            box=new AxisAlignedBB(b.field_78897_a-2,b.field_78895_b-4,b.field_78896_c-2,b.field_78893_d+3,b.field_78894_e+5,b.field_78892_f+3);
            int minX=Math.floorDiv(b.field_78897_a-2,16),minZ=Math.floorDiv(b.field_78896_c-2,16),maxX=Math.floorDiv(b.field_78893_d+2,16),maxZ=Math.floorDiv(b.field_78892_f+2,16);
            // Native den templates are small. Reject oversized custom structures rather
            // than turning one compass sample into unrestricted world generation.
            if((long)(maxX-minX+1)*(maxZ-minZ+1)>16)throw new IllegalArgumentException("Oversized den search footprint");
            List<CompletableFuture<?>> chunks=new ArrayList<>();
            try{
                for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
                    ChunkPos pos=new ChunkPos(x,z);tickets.add(pos);
                    world.func_72863_F().func_217228_a(TICKET,pos,2,id);
                    chunks.add(((CompassChunkRequestInvoker)world.func_72863_F()).backport$requestChunk(x,z,ChunkStatus.field_222617_m,true).thenApply(result->{if(result.left().isEmpty())throw new IllegalStateException("Den candidate chunk unavailable");return result.left().get();}));
                }
            }catch(RuntimeException failure){close(world);throw failure;}
            loaded=CompletableFuture.allOf(chunks.toArray(new CompletableFuture<?>[0]));
        }
        public boolean ready(ServerWorld world){
            if(world.func_72820_D()-startedTick>=300 || System.nanoTime()-startedNanos>=15_000_000_000L)throw new IllegalStateException("Tera den search candidate timed out");
            if(!loaded.isDone())return false;
            loaded.join();
            // EntityJoinWorld conversion schedules a replacement on the server queue.
            if(readyAfter==Long.MAX_VALUE)readyAfter=world.func_72820_D()+2;
            return world.func_72820_D()>=readyAfter;
        }
        public void close(ServerWorld world){
            for(ChunkPos pos:tickets)world.func_72863_F().func_217222_b(TICKET,pos,2,id);
            tickets.clear();
        }
        public BlockPos find(ServerWorld world){
            return world.func_175647_a(TeraDenEntity.class,box,den->!den.field_70128_L && !den.isDecoration()).stream()
                .map(TeraDenEntity::func_233580_cy_).min(Comparator.comparingInt(BlockPos::func_177958_n).thenComparingInt(BlockPos::func_177952_p)).orElse(null);
        }
    }
}
