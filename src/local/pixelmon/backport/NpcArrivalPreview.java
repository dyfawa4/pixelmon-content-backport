package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
/** A reversible field view for native AI. Never calls doSwitch or entry effects. */
public final class NpcArrivalPreview implements AutoCloseable {
    private final BattleController battle;
    private final List<Slot> slots=new ArrayList<>();
    private final List<Runnable> restores=new ArrayList<>();
    private final Map<PixelmonWrapper,PixelmonWrapper> originals=new IdentityHashMap<>();
    private static final class Flags {
        final PixelmonWrapper mon;final boolean field,switched,taken,seen,wait;
        final boolean evolve,z;final com.pixelmonmod.pixelmon.battles.attacks.Attack attack,selected;
        final List<PixelmonWrapper> targets;final UUID arrival,itemTarget;final net.minecraft.item.ItemStack item;final int info;
        Flags(PixelmonWrapper p){mon=p;field=p.onBattlefield;switched=p.switchedThisTurn;taken=p.takenTurn;seen=p.hasBeenInBattle;wait=p.wait;
            evolve=p.willEvolve;z=p.usingZ;attack=p.attack;selected=p.selectedAttack;targets=p.targets;arrival=p.newPokemonUUID;itemTarget=p.willUseItemPokemon;item=p.willUseItemInStack;info=p.willUseItemInStackInfo;}
        void restore(){mon.onBattlefield=field;mon.switchedThisTurn=switched;mon.takenTurn=taken;mon.hasBeenInBattle=seen;mon.wait=wait;
            mon.willEvolve=evolve;mon.usingZ=z;mon.attack=attack;mon.selectedAttack=selected;mon.targets=targets;mon.newPokemonUUID=arrival;mon.willUseItemPokemon=itemTarget;mon.willUseItemInStack=item;mon.willUseItemInStackInfo=info;}
    }
    private static final class Slot {
        final List<PixelmonWrapper> list;final int index;final Flags old,next;
        Slot(List<PixelmonWrapper> list,int index,PixelmonWrapper next){this.list=list;this.index=index;old=new Flags(list.get(index));this.next=new Flags(next);}
        void show(){list.set(index,next.mon);old.mon.onBattlefield=false;next.mon.onBattlefield=true;next.mon.wait=false;}
        void restore(){list.set(index,old.mon);old.restore();next.restore();}
    }
    private NpcArrivalPreview(BattleController battle){this.battle=battle;}
    public static NpcArrivalPreview open(BattleController battle){
        NpcArrivalPreview preview=new NpcArrivalPreview(battle);
        if(!NpcTurnKnowledge.active(battle))return preview;
        try{
            for(var party:battle.participants)if(party instanceof PlayerParticipant)for(var member:party.allPokemon){
                Flags flags=new Flags(member);preview.restores.add(flags::restore);
            }
            for(var party:battle.participants)if(party instanceof PlayerParticipant)for(int i=0;i<party.controlledPokemon.size();i++){
                PixelmonWrapper old=party.controlledPokemon.get(i),arrival=NpcTurnKnowledge.arrival(old),next=NpcTurnKnowledge.itemView(arrival);
                if(next==old||!next.isAlive()||arrival!=old&&arrival.onBattlefield||preview.originals.containsKey(next))continue;
                Slot slot=new Slot(party.controlledPokemon,i,next);preview.slots.add(slot);preview.originals.put(next,old);
                NpcTurnKnowledge.bindArrival(old,next);slot.show();
            }
            // Bench medicines change that member only. Make native roster queries
            // see its detached state too, without treating it as the active target.
            for(var party:battle.participants)if(party instanceof PlayerParticipant)for(int i=0;i<party.allPokemon.length;i++){
                PixelmonWrapper old=party.allPokemon[i],next=NpcTurnKnowledge.itemView(old);if(next==old)continue;
                final int index=i;preview.restores.add(()->party.allPokemon[index]=old);party.allPokemon[i]=next;
            }
            return preview;
        }catch(RuntimeException|Error failure){preview.close();throw failure;}
    }
    @Override public void close(){
        // Native execution owns a target slot. Keep that original reference so
        // real switching (including intercepting moves) performs its normal retargeting.
        for(var party:battle.participants)if(party instanceof TrainerParticipant)for(var mon:party.allPokemon)
            if(mon.targets!=null&&!mon.targets.isEmpty()){
                List<PixelmonWrapper> targets=new ArrayList<>();for(var target:mon.targets)targets.add(originals.getOrDefault(target,target));mon.targets=targets;
            }
        for(int i=slots.size()-1;i>=0;i--)slots.get(i).restore();slots.clear();originals.clear();
        for(int i=restores.size()-1;i>=0;i--)restores.get(i).run();restores.clear();
    }
}
