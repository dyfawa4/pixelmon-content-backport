package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.tasks.EnforcedSwitchTask;
import com.pixelmonmod.pixelmon.enums.heldItems.EnumHeldItems;
import com.pixelmonmod.pixelmon.items.HeldItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;

/** Deferred forced switching adapted to the legacy held-item callback contract. */
public final class EjectPack extends HeldItem {
    private static final Map<PixelmonWrapper,String> PENDING=new WeakHashMap<>();
    private static final Map<BattleController,String> SWITCHED=new WeakHashMap<>();
    public EjectPack(){super(EnumHeldItems.ejectPack,new Item.Properties());}
    private static String action(BattleController b){return b.battleTurn+":"+b.turn+":"+(NinjaTraining.bonus(b)!=null);}
    @Override public void onStatDecrease(PixelmonWrapper holder,PixelmonWrapper affected,int amount,BattleStatsType stat){
        if(AiEffectEvaluation.active()||holder!=affected||amount<=0||holder.bc==null||holder.bc.simulateMode||holder.bc.simulateStats||!holder.isAlive()||!holder.onBattlefield)return;
        // The legacy callback is broadcast to every active held item, not only the target.
        BattleController b=holder.bc;
        if(b.turn>=0&&b.turn<b.turnList.size()){
            var actor=b.turnList.get(b.turn);
            if(actor.attack!=null&&actor.attack.getActualMove()!=null&&"Parting Shot".equals(actor.attack.getActualMove().getAttackName()))return;
        }
        if(ready(holder)&&!action(b).equals(SWITCHED.get(b)))PENDING.put(holder,action(b));
    }
    private static boolean ready(PixelmonWrapper p){
        var part=p.getParticipant();
        return !AiEffectEvaluation.active()&&p.bc!=null&&!p.bc.battleEnded&&!p.bc.simulateMode&&!p.bc.simulateStats&&p.isAlive()&&p.onBattlefield
            &&p.getUsableHeldItem() instanceof EjectPack&&!p.stopsForcedSwitches(p)&&part!=null
            &&part.getType()!=ParticipantType.WildPokemon&&part.hasMorePokemonReserve()&&part.switchingOut.isEmpty();
    }
    private void flush(PixelmonWrapper holder){
        if(holder==null||!PENDING.containsKey(holder))return;
        if(!ready(holder)){PENDING.remove(holder);return;}
        BattleController b=holder.bc;String key=PENDING.get(holder);
        if(key.equals(SWITCHED.get(b))){PENDING.remove(holder);return;}
        PixelmonWrapper fastest=holder;
        for(var p:b.getActiveUnfaintedPokemon())if(PENDING.containsKey(p)&&ready(p)&&p.getBattleStats().speedStat>fastest.getBattleStats().speedStat)fastest=p;
        if(fastest!=holder)return;
        SWITCHED.put(b,key);
        PENDING.keySet().removeIf(p->p.bc==b);
        var part=holder.getParticipant();
        b.sendToAll("pixelmon.helditems.ejectpack",holder.getNickname());holder.consumeItem();holder.setUpSwitchMove();
        if(part.getType()==ParticipantType.Player)NetworkHelper.sendPacket(new EnforcedSwitchTask(b.getPositionOfPokemon(holder,part),true),(ServerPlayerEntity)holder.getPlayerOwner());
        else b.switchPokemon(holder.getPokemonUUID(),holder.getBattleAI().getNextSwitch(holder),true);
    }
    @Override public void applyRepeatedEffectAfterStatus(PixelmonWrapper holder){flush(holder);}
    public static void flushAll(BattleController battle){
        if(battle.simulateMode)return;
        if(battle.battleEnded){PENDING.keySet().removeIf(p->p.bc==battle);SWITCHED.remove(battle);return;}
        for(PixelmonWrapper p:new ArrayList<>(PENDING.keySet()))if(p.bc==battle){
            if(p.getUsableHeldItem() instanceof EjectPack)((EjectPack)p.getUsableHeldItem()).flush(p);
            else PENDING.remove(p);
        }
    }
    public static void end(BattleController battle){if(!battle.simulateMode){PENDING.keySet().removeIf(p->p.bc==battle);SWITCHED.remove(battle);}}
}
