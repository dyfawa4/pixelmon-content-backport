package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.listener.EntityPlayerExtension;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SEntityMetadataPacket;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/** Refresh equipment icons from owned server state without granting or removing charms. */
public final class CharmStateSync {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(Content.NS,"charm_state"),()->"1","1"::equals,"1"::equals);
    private static final Map<UUID,Long> LAST=new WeakHashMap<>();
    private CharmStateSync(){}
    public static void register(){CHANNEL.registerMessage(0,Request.class,(m,b)->{},b->new Request(),(m,c)->{var context=c.get();context.enqueueWork(()->{var player=context.getSender();if(player==null)return;long now=System.nanoTime();Long last=LAST.get(player.func_110124_au());if(last!=null&&now-last<100_000_000L)return;LAST.put(player.func_110124_au(),now);refresh(player);});context.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_SERVER));}
    public static void request(){CHANNEL.sendToServer(new Request());}
    public static void refresh(ServerPlayerEntity player){
        var party=StorageProxy.getParty(player);
        EntityPlayerExtension.updatePlayerExpCharm(player,party.getExpCharm());
        EntityPlayerExtension.updatePlayerShinyCharm(player,party.getShinyCharm());
        EntityPlayerExtension.updatePlayerOvalCharm(player,party.getOvalCharm());
        EntityPlayerExtension.updatePlayerCatchingCharm(player,party.getCatchingCharm());
        EntityPlayerExtension.updatePlayerMarkCharm(player,party.getMarkCharm());
        // A full snapshot also repairs a stale client when the server's data
        // manager already contains the right value and would not mark it dirty.
        // The usual full-snapshot constructor clears live dirty flags. Encode
        // a detached snapshot so other tracking players still receive updates.
        PacketBuffer buffer=new PacketBuffer(io.netty.buffer.Unpooled.buffer());
        try{
            buffer.func_150787_b(player.func_145782_y());
            net.minecraft.network.datasync.EntityDataManager.func_187229_a(player.func_184212_Q().func_187231_c(),buffer);
            var packet=new SEntityMetadataPacket();packet.func_148837_a(buffer);player.field_71135_a.func_147359_a(packet);
        }catch(java.io.IOException e){throw new IllegalStateException("Charm metadata snapshot",e);}finally{buffer.release();}
    }
    public static final class Request {private Request(){}}
}
