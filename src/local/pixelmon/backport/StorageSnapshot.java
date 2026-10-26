package local.pixelmon.backport;
import java.util.*;
import java.util.function.Supplier;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.comm.EnumUpdateType;
import com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper;
import com.pixelmonmod.pixelmon.comm.packetHandlers.clientStorage.newStorage.ClientSetPacket;
import com.pixelmonmod.pixelmon.comm.packetHandlers.clientStorage.newStorage.pc.ClientInitializePCPacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

/** Read-only recovery of the authenticated sender's own storage. Rate limiting queues, never drops. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Content.NS)
public final class StorageSnapshot {
    private static final Map<ServerPlayerEntity,Long> LAST=new WeakHashMap<>();
    private static final Map<ServerPlayerEntity,LinkedHashMap<Integer,Request>> PENDING=new WeakHashMap<>();
    private StorageSnapshot(){}
    public static void send(ServerPlayerEntity player,UUID id,int box,int slot){request(player,new Request(id,box,slot));}
    private static boolean valid(ServerPlayerEntity player,Request r){
        if(player==null||r.storage==null||r.box< -1||r.slot<0||r.slot>=(r.box==-1?6:30))return false;
        if(r.box==-1)return StorageProxy.getParty(player).uuid.equals(r.storage);
        var pc=StorageProxy.getPCForPlayer(player);
        return pc.uuid.equals(r.storage)&&r.box<pc.getBoxCount();
    }
    private static void request(ServerPlayerEntity player,Request r){
        if(!valid(player,r))return;
        var queue=PENDING.computeIfAbsent(player,key->new LinkedHashMap<>());
        Request old=queue.get(r.box);
        if(old!=null&&old.initialize&&!r.initialize)r=old;
        // At most the sender's real boxes plus its party; a repeated box coalesces.
        queue.put(r.box,r);pump(player);
    }
    private static void pump(ServerPlayerEntity player){
        var queue=PENDING.get(player);if(queue==null||queue.isEmpty())return;
        long now=player.field_70170_p.func_82737_E();Long previous=LAST.get(player);
        if(previous!=null&&now>=previous&&now-previous<10)return;
        var iterator=queue.values().iterator();var r=iterator.next();iterator.remove();
        if(queue.isEmpty())PENDING.remove(player);
        if(!valid(player,r))return;LAST.put(player,now);
        PokemonStorage storage;
        if(r.box==-1)storage=StorageProxy.getParty(player);
        else {
            var pc=StorageProxy.getPCForPlayer(player);storage=pc;
            // Contents alone cannot repair a missing PC object on the client.
            if(r.initialize)NetworkHelper.sendPacket(new ClientInitializePCPacket(pc),player);
        }
        // An explicit recovery is independent of native broadcast/listener flags.
        // Include null slots so moves, releases and deletions cannot leave ghosts.
        for(int i=0;i<(r.box==-1?6:30);i++){
            var pos=new StoragePosition(r.box,i);
            NetworkHelper.sendPacket(new ClientSetPacket(storage,pos,storage.get(pos),EnumUpdateType.CLIENT),player);
        }
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ServerTickEvent e){
        if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END)return;
        for(var player:new ArrayList<>(PENDING.keySet()))pump(player);
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){
        if(e.getPlayer() instanceof ServerPlayerEntity){LAST.remove(e.getPlayer());PENDING.remove(e.getPlayer());}
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void stopped(net.minecraftforge.fml.event.server.FMLServerStoppedEvent e){LAST.clear();PENDING.clear();}
    public static final class Request {
        public final UUID storage;public final int box,slot;public final boolean initialize;
        public Request(UUID id,int box,int slot){this(id,box,slot,false);}
        public Request(UUID id,int box,int slot,boolean initialize){storage=id;this.box=box;this.slot=slot;this.initialize=initialize;}
        static void encode(Request m,PacketBuffer b){b.func_179252_a(m.storage);b.writeShort(m.box);b.writeByte(m.slot);b.writeBoolean(m.initialize);}
        static Request decode(PacketBuffer b){return new Request(b.func_179253_g(),b.readShort(),b.readUnsignedByte(),b.readBoolean());}
        static void handle(Request m,Supplier<NetworkEvent.Context> supplied){
            NetworkEvent.Context c=supplied.get();c.enqueueWork(()->request(c.getSender(),m));c.setPacketHandled(true);
        }
    }
}
