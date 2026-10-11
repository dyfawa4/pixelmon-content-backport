package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.battles.UseBattleItemEvent;
import com.pixelmonmod.pixelmon.api.pokemon.stats.links.WrapperLink;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.items.medicine.MedicineItem;
import net.minecraft.item.ItemStack;

/** Finite native medicines, used through the normal item turn instead of an extra heal/attack. */
public final class NpcMedicine {
    private NpcMedicine(){}
    private static final class Bag {int heals=2,revives=1,lastTurn=-1;}
    private static final Map<BattleParticipant,Bag> BAGS=Collections.synchronizedMap(new WeakHashMap<>());
    public static void prepare(TrainerParticipant p){BAGS.put(p,new Bag());}
    public static boolean plan(PixelmonWrapper p){
        Bag bag=BAGS.get(p.getParticipant());
        if(bag==null || p.bc.simulateMode || bag.lastTurn==p.bc.battleTurn || p.bc.rules.hasClause("bagclause"))return false;
        PixelmonWrapper target=null;ItemStack medicine=null;
        if(bag.heals>0 && p.getHealthPercent()<=30 && p.getHealthDeficit()>=50){target=p;medicine=new ItemStack(PixelmonItems.hyper_potion);}
        else if(bag.revives>0 && p.getHealthPercent()>55){
            for(PixelmonWrapper member:p.getParticipant().allPokemon)if(!member.isAlive() && !member.onBattlefield){target=member;medicine=new ItemStack(PixelmonItems.revive);break;}
        }
        if(target==null)return false;
        Tera.cancelSelection(p);p.willUseItemPokemon=target.getPokemonUUID();p.willUseItemInStack=medicine;p.willUseItemInStackInfo=-1;
        bag.lastTurn=p.bc.battleTurn;return true;
    }
    public static boolean use(PixelmonWrapper p){
        if(!(p.getParticipant() instanceof TrainerParticipant) || p.willUseItemInStack==null)return false;
        Bag bag=BAGS.get(p.getParticipant());if(bag==null)return false;
        ItemStack stack=p.willUseItemInStack;PixelmonWrapper target=p.getParticipant().getPokemonFromParty(p.willUseItemPokemon);
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
