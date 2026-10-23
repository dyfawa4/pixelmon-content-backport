package local.pixelmon.backport;
import java.util.*;
import java.util.function.Supplier;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import net.minecraft.network.PacketBuffer;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

/** Independent status data, without changing the native main-status packet. */
public final class ErosionNetwork {
    private ErosionNetwork(){}
    public static void sync(BattleController b,PixelmonWrapper resized){
        if(b.simulateMode)return;
        LightningOathNetwork.sync(b);
        State s=new State();s.battle=b.battleIndex;PixelmonWrapper bonus=NinjaTraining.bonus(b);if(bonus!=null)s.bonus=bonus.getPokemonUUID();
        for(BattleParticipant part:b.participants)for(PixelmonWrapper p:part.allPokemon)if(Erosion.remaining(p)>0)s.turns.put(p.getPokemonUUID(),Erosion.remaining(p));
        if(resized!=null){s.resized=resized.getPokemonUUID();s.hp=resized.getHealth();s.max=resized.getMaxHealth();}
        Set<ServerPlayerEntity> audience=new LinkedHashSet<>(b.getPlayerEntities());
        for(Spectator p:b.spectators)audience.add(p.getEntity());
        for(ServerPlayerEntity p:audience)TeraNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),s);
    }
    public static final class State {
        public int battle,hp,max;public UUID resized,bonus;public final Map<UUID,Integer> turns=new LinkedHashMap<>();
        public static void encode(State s,PacketBuffer b){b.writeInt(s.battle);b.writeBoolean(s.bonus!=null);if(s.bonus!=null)b.func_179252_a(s.bonus);b.writeShort(s.turns.size());for(var e:s.turns.entrySet()){b.func_179252_a(e.getKey());b.writeByte(e.getValue());}b.writeBoolean(s.resized!=null);if(s.resized!=null){b.func_179252_a(s.resized);b.writeInt(s.hp);b.writeInt(s.max);}}
        public static State decode(PacketBuffer b){State s=new State();s.battle=b.readInt();if(b.readBoolean())s.bonus=b.func_179253_g();int n=b.readUnsignedShort();if(n>1024)throw new IllegalArgumentException("Erosion party size");for(int i=0;i<n;i++){UUID id=b.func_179253_g();int turns=b.readUnsignedByte();if(turns!=255&&(turns<1||turns>20))throw new IllegalArgumentException("Erosion duration");s.turns.put(id,turns);}if(b.readBoolean()){s.resized=b.func_179253_g();s.hp=b.readInt();s.max=b.readInt();if(s.max<1||s.hp<0||s.hp>s.max)throw new IllegalArgumentException("Erosion health");}return s;}
        public static void handle(State s,Supplier<NetworkEvent.Context> context){var c=context.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->ErosionClient.accept(s)));c.setPacketHandled(true);}
    }
}
