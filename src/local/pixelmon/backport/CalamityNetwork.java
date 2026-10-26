package local.pixelmon.backport;
import java.util.*;
import java.util.function.Supplier;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.*;
/** Calamity's own battle status snapshot; no timed-skill state or packet dependency. */
public final class CalamityNetwork {
 public static void sync(BattleController battle,PixelmonWrapper resized){if(battle.simulateMode)return;var s=new State();s.battle=battle.battleIndex;for(var part:battle.participants)for(var p:part.allPokemon)if(CalamityBattle.active(p))s.active.add(p.getPokemonUUID());if(resized!=null){s.resized=resized.getPokemonUUID();s.hp=resized.getHealth();s.max=resized.getMaxHealth();}Set<ServerPlayerEntity> audience=new LinkedHashSet<>(battle.getPlayerEntities());for(var spectator:battle.spectators)audience.add(spectator.getEntity());for(var player:audience)TeraNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->player),s);}
 public static final class State {
  public int battle,hp,max;public UUID resized;public final Set<UUID> active=new LinkedHashSet<>();
  public static void encode(State s,PacketBuffer b){b.writeInt(s.battle);b.writeShort(s.active.size());for(var id:s.active)b.func_179252_a(id);b.writeBoolean(s.resized!=null);if(s.resized!=null){b.func_179252_a(s.resized);b.writeInt(s.hp);b.writeInt(s.max);}}
  public static State decode(PacketBuffer b){var s=new State();s.battle=b.readInt();int n=b.readUnsignedShort();if(n>1024)throw new IllegalArgumentException("Calamity party size");for(int i=0;i<n;i++)s.active.add(b.func_179253_g());if(b.readBoolean()){s.resized=b.func_179253_g();s.hp=b.readInt();s.max=b.readInt();if(s.max<1||s.hp<0||s.hp>s.max)throw new IllegalArgumentException("Calamity health");}return s;}
  public static void handle(State s,Supplier<NetworkEvent.Context> context){var c=context.get();c.enqueueWork(()->net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,()->()->CalamityClient.accept(s)));c.setPacketHandled(true);}
 }
}
