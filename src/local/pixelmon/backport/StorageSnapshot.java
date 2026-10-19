package local.pixelmon.backport;
import java.util.*;
import java.util.function.Supplier;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.comm.EnumUpdateType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

/** Read-only recovery: authenticated sender's own storage, bounded and rate limited. */
public final class StorageSnapshot {
    private static final Map<ServerPlayerEntity,Long> LAST=new WeakHashMap<>();
    private StorageSnapshot(){}
    public static void send(ServerPlayerEntity player,UUID id,int box,int slot){
        if(player==null || slot<0 || slot>=(box==-1?6:30) || box< -1)return;
        long now=player.field_70170_p.func_82737_E();Long previous=LAST.get(player);
        if(previous!=null && now>=previous && now-previous<10)return;
        var party=StorageProxy.getParty(player);
        if(box==-1){
            if(!party.uuid.equals(id))return;LAST.put(player,now);
            party.notifyListenerAll(player,EnumUpdateType.CLIENT);return;
        }
        var pc=StorageProxy.getPCForPlayer(player);
        if(!pc.uuid.equals(id) || box>=pc.getBoxCount())return;LAST.put(player,now);
        for(int i=0;i<30;i++){StoragePosition position=new StoragePosition(box,i);
            pc.notifyListener(player,position,pc.get(position),EnumUpdateType.CLIENT);
        }
    }
    public static final class Request {
        public final UUID storage;public final int box,slot;
        public Request(UUID id,int box,int slot){storage=id;this.box=box;this.slot=slot;}
        static void encode(Request m,PacketBuffer b){b.func_179252_a(m.storage);b.writeShort(m.box);b.writeByte(m.slot);}
        static Request decode(PacketBuffer b){return new Request(b.func_179253_g(),b.readShort(),b.readUnsignedByte());}
        static void handle(Request m,Supplier<NetworkEvent.Context> supplied){
            NetworkEvent.Context c=supplied.get();c.enqueueWork(()->send(c.getSender(),m.storage,m.box,m.slot));c.setPacketHandled(true);
        }
    }
}
