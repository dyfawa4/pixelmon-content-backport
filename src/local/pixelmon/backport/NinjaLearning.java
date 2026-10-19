package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.battles.attack.AttackRegistry;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Offer once after battle exit. Native learn controller retains decline/replacement semantics. */
public final class NinjaLearning {
    private static final String OFFERED="backport_ninja_training_offered";
    private final java.util.Map<java.util.UUID,Integer> idle=new java.util.HashMap<>();
    @SubscribeEvent public void teach(com.pixelmonmod.pixelmon.api.events.npc.TeachMoveEvent.CanLearnMove e){if(e.getAttack()!=null&&NinjaTraining.MOVE.equals(e.getAttack().getAttackName())&&!NinjaTraining.eligible(e.getPokemon()))e.setCanceled(true);}
    @SubscribeEvent public void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayerEntity))return;
        var player=(ServerPlayerEntity)e.player;var id=player.func_110124_au();
        if(BattleRegistry.getBattle(player)!=null){idle.put(id,0);return;}
        int ticks=idle.getOrDefault(id,0)+1;idle.put(id,Math.min(40,ticks));if(ticks<40||player.field_70173_aa%20!=0)return;
        if(!local.pixelmon.backport.mixin.NinjaLearnAccess.backport$pending().get(id).isEmpty())return;
        for(Pokemon p:StorageProxy.getParty(player).getAll())if(NinjaTraining.eligible(p)&&!p.isEgg()&&!p.getPersistentData().func_74767_n(OFFERED)){
            var move=AttackRegistry.getAttackBase(NinjaTraining.MOVE);if(!move.isPresent())return;
            p.getPersistentData().func_74757_a(OFFERED,true);p.markDirty();
            if(p.getMoveset().stream().anyMatch(NinjaTraining::move))continue;
            LearnMoveController.sendLearnMove(player,p.getUUID(),move.get(),owner->NinjaTraining.eligible(StorageProxy.getParty(owner).get(p.getUUID())));
            idle.put(id,0);return;
        }
    }
    @SubscribeEvent public void logout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){idle.remove(e.getPlayer().func_110124_au());}
}
