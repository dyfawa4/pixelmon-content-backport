package local.pixelmon.backport;
import java.util.*;
import java.util.concurrent.*;
import com.pixelmonmod.pixelmon.world.structure.PixelmonStructures;
import net.minecraft.block.*;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.gen.feature.structure.StructureStart;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Remove old chorus only in actual End psychic city footprints, without loading chunks. */
public final class EndGymVegetation {
    private static final class Pending {
        final Map<Long,MutableBoundingBox> cities=new HashMap<>();
        final Set<Long> chunks=new LinkedHashSet<>();
    }
    private final Map<ServerWorld,Pending> worlds=new IdentityHashMap<>();
    private final Map<ServerWorld,ConcurrentLinkedQueue<ChunkPos>> incoming=new ConcurrentHashMap<>();
    @SubscribeEvent public void loaded(ChunkEvent.Load event){
        if(!(event.getWorld() instanceof ServerWorld))return;
        ServerWorld world=(ServerWorld)event.getWorld();
        if(!world.func_234923_W_().equals(World.field_234920_i_))return;
        var pos=event.getChunk().func_76632_l();
        // Defer until the world tick: load events can precede the full chunk's
        // insertion into the provider and can originate on a worker thread.
        incoming.computeIfAbsent(world,w->new ConcurrentLinkedQueue<>()).add(pos);
    }
    private void observe(ServerWorld world,ChunkPos pos){
            var chunk=world.func_72863_F().func_225313_a(pos.field_77276_a,pos.field_77275_b);
            if(chunk==null)return;
            Pending pending=worlds.computeIfAbsent(world,w->new Pending());
            StructureStart<?> start=chunk.func_230342_a_(PixelmonStructures.GYMS.get());
            if(start!=null&&start.func_75069_d()&&start.func_186161_c().stream().anyMatch(p->p.toString().contains("/gym_psychic]"))){
                MutableBoundingBox box=start.func_75071_a();
                if(box.field_78893_d-box.field_78897_a<512&&box.field_78892_f-box.field_78896_c<512&&pending.cities.putIfAbsent(pos.func_201841_a(),box)==null){
                    for(int x=box.field_78897_a>>4;x<=box.field_78893_d>>4;x++)for(int z=box.field_78896_c>>4;z<=box.field_78892_f>>4;z++)
                        if(world.func_72863_F().func_225313_a(x,z)!=null)pending.chunks.add(ChunkPos.func_77272_a(x,z));
                }
            }
            for(var box:pending.cities.values())if(GymTerrainProtection.intersects(pos.field_77276_a*16,pos.field_77275_b*16,pos.field_77276_a*16+15,pos.field_77275_b*16+15,box.field_78897_a,box.field_78896_c,box.field_78893_d,box.field_78892_f)){
                pending.chunks.add(pos.func_201841_a());break;
            }
    }
    @SubscribeEvent public void unloaded(WorldEvent.Unload event){worlds.remove(event.getWorld());incoming.remove(event.getWorld());}
    @SubscribeEvent public void tick(TickEvent.WorldTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.world instanceof ServerWorld))return;
        ServerWorld world=(ServerWorld)event.world;
        var queue=incoming.get(world);if(queue!=null)for(int i=0;i<16;i++){ChunkPos pos=queue.poll();if(pos==null)break;observe(world,pos);}
        Pending pending=worlds.get(world);
        if(pending==null||pending.chunks.isEmpty())return;
        var it=pending.chunks.iterator();long key=it.next();it.remove();
        int x=ChunkPos.func_212578_a(key),z=ChunkPos.func_212579_b(key);
        clean(world,new ChunkPos(x,z),pending.cities.values());
    }
    /** Caller supplies only verified city bounds; querying an unloaded chunk never loads it. */
    public static int clean(ServerWorld world,ChunkPos location,Collection<MutableBoundingBox> cities){
        if(!world.func_234923_W_().equals(World.field_234920_i_))return 0;
        int x=location.field_77276_a,z=location.field_77275_b,removed=0;
        var chunk=world.func_72863_F().func_225313_a(x,z);if(chunk==null)return 0;
        for(MutableBoundingBox box:cities){
            if(!GymTerrainProtection.intersects(x*16,z*16,x*16+15,z*16+15,box.field_78897_a,box.field_78896_c,box.field_78893_d,box.field_78892_f))continue;
            for(int bx=Math.max(x*16,box.field_78897_a);bx<=Math.min(x*16+15,box.field_78893_d);bx++)
                for(int bz=Math.max(z*16,box.field_78896_c);bz<=Math.min(z*16+15,box.field_78892_f);bz++)
                    for(int y=0;y<256;y++){
                        BlockPos pos=new BlockPos(bx,y,bz);Block block=chunk.func_180495_p(pos).func_177230_c();
                        if(block instanceof ChorusPlantBlock||block instanceof ChorusFlowerBlock){world.func_180501_a(pos,Blocks.field_150350_a.func_176223_P(),18);removed++;}
                    }
        }
        return removed;
    }
}
