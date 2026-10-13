package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import java.util.*;
/** Derive freeze from real battle membership; never save a gravity/flight/permission flag. */
public final class BattleMotion {
    private static final Set<PlayerEntity> HELD=Collections.newSetFromMap(new WeakHashMap<>());
    public static boolean eligible(boolean alive,boolean participant,boolean ended,boolean simulation,boolean spectator){return alive && participant && !ended && !simulation && !spectator;}
    public static boolean heldMovement(Entity entity){
        if(frozen(entity))return true;
        for(Entity passenger:entity.func_184188_bt())if(heldMovement(passenger))return true;
        return false;
    }
    public static boolean frozen(Entity entity){
        if(!(entity instanceof PlayerEntity) || !entity.func_70089_S())return false;
        PlayerEntity player=(PlayerEntity)entity;
        if(player.field_70170_p.field_72995_K)return Boolean.TRUE.equals(DistExecutor.unsafeCallWhenOn(Dist.CLIENT,()->()->BattleMotionClient.frozen(player)));
        var battle=BattleRegistry.getBattleExcludeSpectate(player);
        return battle!=null && eligible(true,battle.getParticipantForEntity(player) instanceof PlayerParticipant,battle.battleEnded,battle.simulateMode,false);
    }
    public static void stop(Entity entity){
        entity.func_213317_d(Vector3d.field_186680_a);entity.field_70143_R=0;
        // Cancelling travel also skips its normal limb-amplitude decay. Leaving
        // old nonzero previous/current values makes partial-tick rendering twitch
        // around a stationary limb phase. Zero both; retain phase and head rotation.
        if(entity instanceof net.minecraft.entity.LivingEntity){
            var living=(net.minecraft.entity.LivingEntity)entity;
            living.field_184618_aE=0;living.field_70721_aZ=0;
        }
    }
    @SubscribeEvent public void tick(TickEvent.PlayerTickEvent event){
        boolean active=frozen(event.player);
        synchronized(HELD){if(active){HELD.add(event.player);stop(event.player);stop(event.player.func_184208_bv());}else if(HELD.remove(event.player))stop(event.player);}
    }
    @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event){synchronized(HELD){HELD.remove(event.getPlayer());}stop(event.getPlayer());}
}
