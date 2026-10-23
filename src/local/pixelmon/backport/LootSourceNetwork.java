package local.pixelmon.backport;

import java.util.*;
import java.util.function.Supplier;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/** The server's loaded tables are authoritative, including world datapacks. */
public final class LootSourceNetwork {
 public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new net.minecraft.util.ResourceLocation(Content.NS,"loot_sources"),()->"1","1"::equals,"1"::equals);
 private static final Map<MinecraftServer,List<LootSourceCatalog.Source>> CACHE=new WeakHashMap<>();
 public static void register(){CHANNEL.registerMessage(0,Batch.class,Batch::encode,Batch::decode,Batch::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new LootSourceNetwork());}
 @SubscribeEvent public void sync(OnDatapackSyncEvent event){
  MinecraftServer server=net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;
  if(event.getPlayer()==null)CACHE.remove(server);
  var sources=CACHE.computeIfAbsent(server,LootSourceCatalog::build);UUID generation=UUID.randomUUID();int total=(sources.size()+3)/4;
  for(var player:event.getPlayer()==null?event.getPlayerList().func_181057_v():List.of(event.getPlayer()))for(int i=0;i<total;i++)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Batch(generation,i,total,sources.subList(i*4,Math.min(sources.size(),i*4+4))));
 }
 public static final class Batch {
  public final UUID generation;public final int index,total;public final List<LootSourceCatalog.Source> sources;
  public Batch(UUID generation,int index,int total,List<LootSourceCatalog.Source> sources){this.generation=generation;this.index=index;this.total=total;this.sources=List.copyOf(sources);}
  public static void encode(Batch m,PacketBuffer b){b.func_179252_a(m.generation);b.writeInt(m.index);b.writeInt(m.total);b.writeInt(m.sources.size());for(var s:m.sources){b.func_180714_a(s.group);b.func_180714_a(s.id);b.func_180714_a(s.title);b.func_180714_a(s.detail);b.writeInt(s.entries.size());for(var e:s.entries){b.func_150788_a(e.stack);b.func_180714_a(e.note);}}}
  private static int bound(int n,int max){if(n<0||n>max)throw new IllegalArgumentException("Loot catalogue size");return n;}
  public static Batch decode(PacketBuffer b){UUID id=b.func_179253_g();int index=bound(b.readInt(),16383),total=bound(b.readInt(),16384);if(total==0||index>=total)throw new IllegalArgumentException("Loot catalogue batch");List<LootSourceCatalog.Source> sources=new ArrayList<>();int count=bound(b.readInt(),4);for(int i=0;i<count;i++){String group=b.func_150789_c(32),key=b.func_150789_c(512),title=b.func_150789_c(512),detail=b.func_150789_c(1024);List<LootSourceCatalog.Entry> entries=new ArrayList<>();int size=bound(b.readInt(),4096);for(int j=0;j<size;j++)entries.add(new LootSourceCatalog.Entry(b.func_150791_c(),b.func_150789_c(4096)));sources.add(new LootSourceCatalog.Source(group,key,title,detail,entries));}return new Batch(id,index,total,sources);}
  static void handle(Batch m,Supplier<NetworkEvent.Context> supplier){var c=supplier.get();c.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->{if(net.minecraftforge.fml.ModList.get().isLoaded("jei"))local.pixelmon.backport.client.LootSourceJei.receive(m);}));c.setPacketHandled(true);}
 }
}
