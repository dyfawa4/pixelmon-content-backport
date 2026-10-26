package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.events.CaptureEvent;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.comm.EnumUpdateType;
import com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper;
import com.pixelmonmod.pixelmon.comm.packetHandlers.clientStorage.newStorage.ClientSetPacket;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/** Successful capture fires before insertion. Publish only the canonical, already stored PC slot. */
public final class CaptureStorageSync {
    private static final Map<ServerPlayerEntity,LinkedHashMap<UUID,Integer>> PENDING=new WeakHashMap<>();
    private static void defer(ServerPlayerEntity player,UUID id){
        var server=ServerLifecycleHooks.getCurrentServer();
        if(player==null||id==null||server==null||!server.func_213162_bc())return;
        var queue=PENDING.computeIfAbsent(player,key->new LinkedHashMap<>());
        if(queue.size()<128||queue.containsKey(id))queue.put(id,20);
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public void caught(CaptureEvent.SuccessfulCapture e){if(e.getPokemon()!=null)defer(e.getPlayer(),e.getPokemon().getPokemon().getUUID());}
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public void raid(CaptureEvent.SuccessfulRaidCapture e){if(e.getRaidPokemon()!=null)defer(e.getPlayer(),e.getRaidPokemon().getUUID());}
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        var server=ServerLifecycleHooks.getCurrentServer();if(server==null)return;
        var players=PENDING.entrySet().iterator();
        while(players.hasNext()){
            var entry=players.next();var player=entry.getKey();
            if(server.func_184103_al().func_177451_a(player.func_110124_au())!=player){players.remove();continue;}
            var pc=StorageProxy.getPCForPlayer(player);var iterator=entry.getValue().entrySet().iterator();
            while(iterator.hasNext()){
                var captured=iterator.next();var pokemon=pc.find(captured.getKey());
                if(pokemon!=null){
                    var pos=pc.getPosition(pokemon);
                    if(pos!=null&&pc.get(pos)==pokemon)NetworkHelper.sendPacket(new ClientSetPacket(pc,pos,pokemon,EnumUpdateType.CLIENT),player);
                    iterator.remove();
                }else if(StorageProxy.getParty(player).find(captured.getKey())!=null||captured.getValue()<=1)iterator.remove();
                else captured.setValue(captured.getValue()-1);
            }
            if(entry.getValue().isEmpty())players.remove();
        }
    }
    @SubscribeEvent public void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){PENDING.remove(e.getPlayer());}
    @SubscribeEvent public void stopped(net.minecraftforge.fml.event.server.FMLServerStoppedEvent e){PENDING.clear();}
}
