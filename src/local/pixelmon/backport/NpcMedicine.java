package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.battles.UseBattleItemEvent;
import com.pixelmonmod.pixelmon.api.pokemon.stats.links.WrapperLink;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.items.medicine.MedicineItem;
import net.minecraft.item.ItemStack;

/** Finite native medicines, used through the normal item turn instead of an extra heal/attack. */
public final class NpcMedicine {
    private NpcMedicine(){}
    private static final class Bag {int heals=2,revives=1,lastTurn=-1;ItemStack planned;UUID target;}
    private static final Map<BattleParticipant,Bag> BAGS=Collections.synchronizedMap(new WeakHashMap<>());
    public static void prepare(TrainerParticipant p){BAGS.put(p,new Bag());}
    public static boolean plan(PixelmonWrapper p){
        return plan(p,null);
    }
    public static boolean plan(PixelmonWrapper p,MoveChoice choice){
        Bag bag=BAGS.get(p.getParticipant());
        if(bag==null || p.bc.simulateMode || !p.isAlive() || !p.onBattlefield || p.willUseItemInStack!=null
            || bag.lastTurn==p.bc.battleTurn || p.bc.rules.hasClause("bagclause"))return false;
        List<MoveChoice> threats=NpcTactics.threats(p);
        if(NpcTactics.finishFirst(p,choice,threats))return false;
        double incoming=NpcTactics.incoming(p,threats);
        PixelmonWrapper target=null;ItemStack medicine=null;
        if(bag.heals>0 && p.getHealthPercent()<=30 && p.getHealthDeficit()>=50
            && !(choice!=null && NpcMoves.recovery(choice.attack)) && Math.min(p.getMaxHealth(),p.getHealth()+120)>incoming){
            target=p;medicine=new ItemStack(PixelmonItems.hyper_potion);
        }else if(bag.revives>0 && p.getHealthPercent()>55 && incoming<p.getHealth()*.65){
            double best=-1;
            for(PixelmonWrapper member:p.getParticipant().allPokemon)if(!member.isAlive() && !member.onBattlefield){
                double value=member.pokemon.getPersistentData().func_74767_n(NpcMoves.ACE)?30:0;
                for(var attack:member.getMoveset())if(attack.getAttackCategory()!=AttackCategory.STATUS)
                    for(PixelmonWrapper enemy:p.getOpponentPokemon())value=Math.max(value,attack.getMove().getBasePower()*Element.getTotalEffectiveness(enemy.type,attack.getType()));
                if(value>best){best=value;target=member;}
            }
            if(target!=null)medicine=new ItemStack(PixelmonItems.revive);
        }
        if(target==null)return false;
        Tera.cancelSelection(p);p.willUseItemPokemon=target.getPokemonUUID();p.willUseItemInStack=medicine;p.willUseItemInStackInfo=-1;
        p.willEvolve=false;p.usingZ=false;bag.planned=medicine;bag.target=target.getPokemonUUID();
        bag.lastTurn=p.bc.battleTurn;return true;
    }
    public static boolean use(PixelmonWrapper p){
        if(p.bc==null || p.bc.simulateMode || !(p.getParticipant() instanceof TrainerParticipant) || p.willUseItemInStack==null)return false;
        Bag bag=BAGS.get(p.getParticipant());if(bag==null || bag.planned!=p.willUseItemInStack || !Objects.equals(bag.target,p.willUseItemPokemon))return false;
        ItemStack stack=p.willUseItemInStack;PixelmonWrapper target=p.getParticipant().getPokemonFromParty(p.willUseItemPokemon);
        bag.planned=null;bag.target=null;
        boolean revive=stack.func_77973_b()==PixelmonItems.revive;
        p.willUseItemInStack=null;p.willUseItemInStackInfo=0;p.attack=null;p.selectedAttack=null;
        if(target==null || (revive?bag.revives:bag.heals)<=0)return true;
        UseBattleItemEvent event=new UseBattleItemEvent(p,target,stack,-1);
        if(Pixelmon.EVENT_BUS.post(event) || !(event.getStack().func_77973_b() instanceof MedicineItem) || event.getTarget()==null)return true;
        if(((MedicineItem)event.getStack().func_77973_b()).useMedicine(new WrapperLink(event.getTarget()),-1)){
            if(revive)--bag.revives;else --bag.heals;
            p.bc.sendToAll("pixelmon.backport.npc.medicine",p.getParticipant().getDisplayName(),event.getStack().func_200301_q(),event.getTarget().getNickname());
        }
        return true;
    }
}
