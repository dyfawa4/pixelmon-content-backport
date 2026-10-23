package local.pixelmon.backport;
import java.util.*;
import java.util.function.Supplier;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import net.minecraft.network.PacketBuffer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.network.simple.SimpleChannel;
/** Separate versioned channel, leaving all existing packet layouts intact. */
public final class LightningOathNetwork {
 private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(Content.NS,"lightning_oath"),()->"1","1"::equals,"1"::equals);
 private LightningOathNetwork(){}
 public static void register(){CHANNEL.registerMessage(0,State.class,State::encode,State::decode,State::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));}
 public static void sync(BattleController battle){if(battle.simulateMode)return;State state=new State();state.battle=battle.battleIndex;for(var part:battle.participants)for(var p:part.allPokemon)if(LightningOath.remaining(p)>0)state.turns.put(p.getPokemonUUID(),LightningOath.remaining(p));Set<ServerPlayerEntity> audience=new LinkedHashSet<>(battle.getPlayerEntities());for(var spectator:battle.spectators)audience.add(spectator.getEntity());for(var player:audience)CHANNEL.send(PacketDistributor.PLAYER.with(()->player),state);}
 public static final class State {
  public int battle;public final Map<UUID,Integer> turns=new LinkedHashMap<>();
  public static void encode(State state,PacketBuffer buf){buf.writeInt(state.battle);buf.writeShort(state.turns.size());for(var row:state.turns.entrySet()){buf.func_179252_a(row.getKey());buf.writeByte(row.getValue());}}
  public static State decode(PacketBuffer buf){State state=new State();state.battle=buf.readInt();int count=buf.readUnsignedShort();if(count>1024)throw new IllegalArgumentException("Oath party size");for(int i=0;i<count;i++){UUID id=buf.func_179253_g();int turns=buf.readUnsignedByte();if(turns<1||turns>5)throw new IllegalArgumentException("Oath duration");state.turns.put(id,turns);}return state;}
  public static void handle(State state,Supplier<NetworkEvent.Context> supplied){var context=supplied.get();context.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->LightningOathClient.accept(state)));context.setPacketHandled(true);}
 }
}
