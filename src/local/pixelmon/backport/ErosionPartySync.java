package local.pixelmon.backport;
import java.util.*;
import java.util.function.Supplier;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.*;
/** Small independent party status snapshot, including explicit cures; no native status replacement. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Content.NS)
public final class ErosionPartySync {
 private static final Map<ServerPlayerEntity,String> LAST=new WeakHashMap<>();
 private static State snapshot(ServerPlayerEntity player){State s=new State();for(var p:StorageProxy.getParty(player).getAll())if(p!=null){int reduction=p.getPersistentData().func_74762_e(DrownedErosion.TAG);s.rows.put(p.getUUID(),reduction);if(reduction>0){int[] stats=new int[6];for(int i=0;i<6;i++)stats[i]=p.getStat(DrownedErosion.STATS[i]);s.stats.put(p.getUUID(),stats);s.forms.put(p.getUUID(),p.getForm().getName());s.levels.put(p.getUUID(),p.getPokemonLevel());}}return s;}
 private static void send(ServerPlayerEntity player,State state){LAST.put(player,state.signature());TeraNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->player),state);}
 public static void finished(com.pixelmonmod.pixelmon.battles.controller.BattleController battle){
  if(battle.simulateMode)return;
  for(var participant:battle.participants)if(participant instanceof com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant){
   boolean changed=false;for(var p:participant.allPokemon)if(p.getSpecies().getDex()==1024||DrownedErosion.active(p.pokemon))changed=true;
   if(changed){
    // Native cleanup can still consult the departing wrapper's cached maximum
    // after reverting its form. Bound HP against the final stored form instead.
    for(var wrapper:participant.allPokemon){var pokemon=wrapper.pokemon;int maximum=pokemon.getStat(com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType.HP);if(pokemon.getHealth()>maximum)pokemon.setHealth(maximum);}
    var player=((com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant)participant).player;StorageProxy.getParty(player).notifyListenerAll(player,com.pixelmonmod.pixelmon.comm.EnumUpdateType.CLIENT);send(player,snapshot(player));
   }
  }
 }
 @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
  if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayerEntity)||e.player.field_70173_aa%10!=0)return;
  ServerPlayerEntity player=(ServerPlayerEntity)e.player;State state=snapshot(player);
  if(player.field_70173_aa%100==0||!state.signature().equals(LAST.get(player)))send(player,state);
 }
 public static final class State {
  public final Map<UUID,Integer> rows=new LinkedHashMap<>();
  public final Map<UUID,int[]> stats=new LinkedHashMap<>();public final Map<UUID,String> forms=new LinkedHashMap<>();public final Map<UUID,Integer> levels=new LinkedHashMap<>();
  private String signature(){StringBuilder b=new StringBuilder(rows.toString()).append(forms).append(levels);for(var row:stats.entrySet())b.append(row.getKey()).append(Arrays.toString(row.getValue()));return b.toString();}
  public static void encode(State s,PacketBuffer b){b.writeByte(s.rows.size());for(var row:s.rows.entrySet()){UUID id=row.getKey();b.func_179252_a(id);b.writeInt(row.getValue());if(row.getValue()>0){b.func_180714_a(s.forms.get(id));b.writeInt(s.levels.get(id));for(int value:s.stats.get(id))b.writeInt(value);}}}
  public static State decode(PacketBuffer b){State s=new State();int count=b.readUnsignedByte();if(count>6)throw new IllegalArgumentException("Erosion party size");for(int i=0;i<count;i++){UUID id=b.func_179253_g();int reduction=b.readInt();if(reduction<0||reduction>255)throw new IllegalArgumentException("Erosion reduction");s.rows.put(id,reduction);if(reduction>0){s.forms.put(id,b.func_150789_c(64));int level=b.readInt();if(level<1||level>1000000)throw new IllegalArgumentException("Erosion level");s.levels.put(id,level);int[] values=new int[6];for(int j=0;j<6;j++){values[j]=b.readInt();if(values[j]<1||values[j]>1000000)throw new IllegalArgumentException("Erosion stat");}s.stats.put(id,values);}}return s;}
  public static void handle(State s,Supplier<NetworkEvent.Context> supplied){var c=supplied.get();c.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->ErosionClient.party(s)));c.setPacketHandled(true);}
 }
}
