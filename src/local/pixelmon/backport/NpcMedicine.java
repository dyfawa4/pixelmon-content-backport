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
    private static final class Bag {int heals=2,revives=1,lastTurn=-1;boolean leader;ItemStack planned;UUID target;}
    private static final Map<BattleParticipant,Bag> BAGS=Collections.synchronizedMap(new WeakHashMap<>());
    public static void prepare(TrainerParticipant p){Bag bag=new Bag();bag.leader=GymCities.isLeader(p.trainer)||AuthorBossTeams.is(p.trainer);BAGS.put(p,bag);}
    public static double value(PixelmonWrapper member,PixelmonWrapper own){
        double score=0;for(var attack:member.getMoveset())if(attack.getAttackCategory()!=AttackCategory.STATUS)
            for(PixelmonWrapper enemy:own.getOpponentPokemon())score=Math.max(score,attack.getMove().getBasePower()*Element.getTotalEffectiveness(enemy.type,attack.getType()));
        double bulk=member.getMaxHealth()/300.0;
        return score*(1+Math.min(1,bulk)*.2)+(member.pokemon.getPersistentData().func_74767_n(GymAcePolicy.MARKER)?45:0);
    }
    public static boolean plan(PixelmonWrapper p){
        return plan(p,null);
    }
    public static boolean plan(PixelmonWrapper p,MoveChoice choice){
        Bag bag=BAGS.get(p.getParticipant());
        if(bag==null || p.bc.simulateMode || !p.isAlive() || !p.onBattlefield || p.willUseItemInStack!=null
            || bag.lastTurn==p.bc.battleTurn || p.bc.rules.hasClause("bagclause"))return false;
        List<MoveChoice> threats=NpcTactics.threats(p);
        if(NpcTactics.finishFirst(p,choice,threats))return false;
        // A last survivor can spend this item before falling and keep the team in battle.
        // Do not apply the normal safe-window/HP gates or let self-healing consume this chance.
        if(bag.leader&&Arrays.stream(p.getParticipant().allPokemon).filter(PixelmonWrapper::isAlive).count()==1){
            if(bag.revives>0){
                PixelmonWrapper fallen=null;double best=Double.NEGATIVE_INFINITY;boolean ace=false;
                for(var member:p.getParticipant().allPokemon)if(!member.isAlive()&&!member.onBattlefield){
                    boolean marked=member.pokemon.getPersistentData().func_74767_n(GymAcePolicy.MARKER);double score=value(member,p);
                    if(fallen==null||marked&&!ace||marked==ace&&score>best){fallen=member;best=score;ace=marked;}
                }
                if(fallen!=null)return queue(p,bag,fallen,new ItemStack(PixelmonItems.max_revive));
            }
            if(bag.heals>0&&p.getHealthDeficit()>0)return queue(p,bag,p,new ItemStack(PixelmonItems.max_potion));
            return false;
        }
        double incoming;
        try(var item=NpcTurnKnowledge.preview(p,null)){incoming=NpcTactics.incoming(p,NpcTactics.threats(p));}
        double future=NpcTurnKnowledge.active(p.bc)?NpcTactics.incoming(p,NpcTactics.threats(p,true)):incoming;
        PixelmonWrapper target=null;ItemStack medicine=null;
        boolean status=p.getStatuses().stream().anyMatch(s->Arrays.asList("Sleep","Freeze","Paralysis","Burn","Poison","PoisonBad").contains(s.type.toString()));
        boolean canActFirst=choice!=null && choice.isAttack() && !MoveChoice.canOutspeed(threats,p,choice.createList());
        boolean window=NpcTurnKnowledge.active(p.bc)&&incoming<p.getHealth()*.15&&future>=p.getHealth()*.65&&p.getHealthPercent()<65;
        if(bag.heals>0 && ((p.getHealthPercent()<=35||window) && p.getHealthDeficit()>=50 || !bag.leader&&status && p.getHealthPercent()<=80)
            && !(choice!=null && NpcMoves.recovery(choice.attack)) && NpcSwitchPolicy.healingBuysAction(p.getMaxHealth(),incoming,canActFirst)){
            ItemStack candidate=new ItemStack(bag.leader?PixelmonItems.max_potion:PixelmonItems.full_restore);
            var healed=NpcMedicineForecast.project(p,candidate,-1);
            // Unscoped trainers retain native observed-move knowledge; its target
            // references are the live wrapper rather than our detached copy.
            var riskTarget=NpcTurnKnowledge.active(p.bc)?healed:p;
            double immediate;
            try(var item=NpcTurnKnowledge.preview(riskTarget,null)){immediate=NpcTactics.medicineDamage(riskTarget,NpcTactics.threats(riskTarget));}
            double following=NpcTurnKnowledge.active(p.bc)?NpcTactics.medicineDamage(healed,NpcTactics.threats(healed,true)):immediate;
            if(healed.getHealth()>immediate&&healed.getHealth()-immediate>following*(canActFirst?.15:1.05)){
                target=p;medicine=candidate;
            }
        }else if(bag.revives>0 && p.getHealthPercent()>55 && incoming<p.getHealth()*.65){
            double best=-1,reserve=0;int alive=0;
            for(PixelmonWrapper member:p.getParticipant().allPokemon)if(member.isAlive()){alive++;reserve=Math.max(reserve,value(member,p));}
            for(PixelmonWrapper member:p.getParticipant().allPokemon)if(!member.isAlive() && !member.onBattlefield){
                double value=value(member,p);
                if(value>best){best=value;target=member;}
            }
            if(target!=null&&(!bag.leader||NpcRevivePolicy.worthwhile(p.getHealth(),Math.max(incoming,future*.35),alive,best,reserve)))medicine=new ItemStack(bag.leader?PixelmonItems.max_revive:PixelmonItems.revive);
            else target=null;
        }
        if(target==null)return false;
        return queue(p,bag,target,medicine);
    }
    private static boolean queue(PixelmonWrapper p,Bag bag,PixelmonWrapper target,ItemStack medicine){
        Tera.cancelSelection(p);p.willUseItemPokemon=target.getPokemonUUID();p.willUseItemInStack=medicine;p.willUseItemInStackInfo=-1;
        p.willEvolve=false;p.usingZ=false;bag.planned=medicine;bag.target=target.getPokemonUUID();
        bag.lastTurn=p.bc.battleTurn;return true;
    }
    public static boolean use(PixelmonWrapper p){
        if(p.bc==null || p.bc.simulateMode || !(p.getParticipant() instanceof TrainerParticipant) || p.willUseItemInStack==null)return false;
        Bag bag=BAGS.get(p.getParticipant());if(bag==null || bag.planned!=p.willUseItemInStack || !Objects.equals(bag.target,p.willUseItemPokemon))return false;
        ItemStack stack=p.willUseItemInStack;PixelmonWrapper target=p.getParticipant().getPokemonFromParty(p.willUseItemPokemon);
        bag.planned=null;bag.target=null;
        boolean revive=stack.func_77973_b()==PixelmonItems.revive||stack.func_77973_b()==PixelmonItems.max_revive;
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
