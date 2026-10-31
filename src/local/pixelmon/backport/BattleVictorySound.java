package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.battles.BattleResults;
import com.pixelmonmod.pixelmon.api.events.battles.BattleEndEvent;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.*;
import net.minecraftforge.eventbus.api.*;

/** A real winning player's own sound; event replays cannot play it again for that battle. */
public final class BattleVictorySound {
    private static final Map<BattleController,Set<UUID>> PLAYED=Collections.synchronizedMap(new WeakHashMap<>());
    public static final ResourceLocation SOUND=new ResourceLocation("minecraft:ui.toast.challenge_complete");
    public static boolean eligible(PlayerParticipant player,BattleEndEvent event){
        var battle=event.getBattleController();
        if(battle==null||battle.simulateMode||event.isAbnormal()||event.getCause()!=com.pixelmonmod.pixelmon.api.battles.BattleEndCause.NORMAL||!battle.participants.contains(player)||event.getResults().get(player)!=BattleResults.VICTORY)return false;
        for(var opponent:player.getOpponents()){
            if(opponent instanceof TrainerParticipant){var npc=((TrainerParticipant)opponent).trainer;
                if(npc!=null&&(npc.isGymLeader||AuthorBossTeams.is(npc)||GymCities.isLeader(npc)))return true;
            }
            if(opponent instanceof WildPixelmonParticipant)for(var pokemon:opponent.allPokemon)if(pokemon!=null&&pokemon.entity!=null&&pokemon.entity.getBossTier().isBoss())return true;
        }
        return false;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void ended(BattleEndEvent event){
        var battle=event.getBattleController();if(battle==null||battle.simulateMode)return;
        for(var entry:event.getResults().entrySet())if(entry.getKey() instanceof PlayerParticipant){
            var participant=(PlayerParticipant)entry.getKey();ServerPlayerEntity player=participant.player;
            if(player==null||player.field_70170_p.field_72995_K||!eligible(participant,event))continue;
            if(!PLAYED.computeIfAbsent(battle,k->new HashSet<>()).add(player.func_110124_au()))continue;
            player.func_213823_a(net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(SOUND),SoundCategory.PLAYERS,1f,1f);
        }
    }
}
