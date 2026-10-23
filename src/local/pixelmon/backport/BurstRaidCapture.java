package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.events.raids.EndRaidEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.catching.CaptureValues;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import com.pixelmonmod.pixelmon.battles.raids.RaidData;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** One raid's own final capture may inherit its field, never another battle. */
public final class BurstRaidCapture {
    private static final Map<RaidData,Set<UUID>> PENDING=Collections.synchronizedMap(new WeakHashMap<>());
    @SubscribeEvent public void end(EndRaidEvent event){
        PENDING.remove(event.getRaid());if(!event.didRaidersWin())return;
        var bc=event.getRaidParticipant().bc;if(bc==null || bc.simulateMode)return;
        var terrain=bc.globalStatusController.getTerrain();if(!(terrain instanceof BurstTerrain))return;
        int side=((BurstTerrain)terrain).team;Set<UUID> eligible=new HashSet<>();
        for(var participant:event.getAllyParticipants())if(participant.team==side && participant instanceof com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant)
            eligible.add(((com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant)participant).player.func_110124_au());
        if(!eligible.isEmpty())PENDING.put(event.getRaid(),eligible);
    }
    public static void apply(RaidData raid,ServerPlayerEntity player,Pokemon target,PokeBall ball,CaptureValues values){
        if(raid==null || player==null || target==null || values==null || CapBallPreference.cap(target))return;
        Set<UUID> pending=PENDING.get(raid);var capture=raid.getPlayer(player);
        if(pending!=null && pending.contains(player.func_110124_au()) && capture!=null && capture.canCatch && capture.possibleCatch==target)
            values.setBallBonus(BurstPolicy.catchBonus(values.getBallBonus(),true,BurstCapture.forbidden(ball,target)));
    }
    @SubscribeEvent public void capture(com.pixelmonmod.pixelmon.api.events.CaptureEvent.StartRaidCapture event){apply(event.getRaid(),event.getPlayer(),event.getRaidPokemon(),null,event.getCaptureValues());}
    public static void finish(RaidData raid,UUID player){Set<UUID> pending=PENDING.get(raid);if(pending!=null){pending.remove(player);if(pending.isEmpty())PENDING.remove(raid);}}
    public static void clear(RaidData raid){PENDING.remove(raid);}
    public static void preview(RaidData raid,RaidData.RaidPlayer player){
        if(player.playerEntity==null)return;Set<UUID> pending=PENDING.get(raid);List<String> balls=Collections.emptyList();
        if(pending!=null && pending.contains(player.player) && player.canCatch && player.possibleCatch!=null)
            balls=BurstCapture.outlinedBalls(player.playerEntity,player.possibleCatch,com.pixelmonmod.pixelmon.entities.pokeballs.PokeBallMode.EMPTY);
        TeraNetwork.CHANNEL.send(net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(()->player.playerEntity),new TeraNetwork.RaidBurst(raid.uuid,balls));
    }
}
