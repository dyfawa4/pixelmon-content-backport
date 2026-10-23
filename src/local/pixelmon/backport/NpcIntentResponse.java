package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.battles.attacks.*;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.*;
import com.pixelmonmod.pixelmon.battles.controller.BattlePriorityHelper;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.log.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.*;
import local.pixelmon.backport.mixin.NpcDamagePreviewAccess;

/** One shared response pass for all registered attacks; native effects retain legality and immunity rules. */
public final class NpcIntentResponse {
    private NpcIntentResponse(){}
    private static Attack submitted(PixelmonWrapper target){
        List<Attack> moves=NpcTurnKnowledge.moves(target);return moves!=null&&moves.size()==1?moves.get(0):null;
    }
    private static final Set<String> NO_COPY=new HashSet<>(Arrays.asList("Focus Punch","Beak Blast","Belch","Counter","Covet","Metal Burst","Mirror Coat","Shell Trap","Struggle","Chatter","Thief"));
    private static boolean first(PixelmonWrapper a,Attack aa,PixelmonWrapper b,Attack ba){
        if(aa==null)return false;if(ba==null)return true;
        boolean simulation=a.bc.simulateMode;a.bc.simulateMode=true;
        try(var x=NpcTurnKnowledge.preview(a,aa);var y=NpcTurnKnowledge.preview(b,ba)){
            MoveChoice ac=new MoveChoice(a,aa,Collections.singletonList(b)),bc=new MoveChoice(b,ba,Collections.singletonList(a));
            ac.result=new MoveResults(b);ac.result.priority=BattlePriorityHelper.calculatePriority(a);
            bc.result=new MoveResults(a);bc.result.priority=BattlePriorityHelper.calculatePriority(b);
            return !MoveChoice.canOutspeed(Collections.singletonList(bc),a,ac.createList());
        }finally{a.bc.simulateMode=simulation;}
    }
    public static MoveChoice raw(BattleAIBase ai,PixelmonWrapper user,Attack attack,PixelmonWrapper target){
        boolean mode=user.bc.simulateMode;user.bc.simulateMode=true;
        var saved=user.targets;boolean evolve=user.willEvolve,z=user.usingZ;
        try(var action=NpcTurnKnowledge.preview(user,attack)){
            MoveChoice hit=new MoveChoice(user,attack.deepCopy(),Collections.singletonList(target));
            ((NpcDamagePreviewAccess)ai).backport$damage(user,hit);return hit;
        }finally{user.targets=saved;user.willEvolve=evolve;user.usingZ=z;user.bc.simulateMode=mode;}
    }
    public static void failed(MoveChoice choice){
        if(choice.result==null)choice.result=new MoveResults(choice.targets.isEmpty()?choice.user:choice.targets.get(0));
        choice.result.damage=choice.result.fullDamage=0;choice.result.result=AttackResult.failed;choice.lowerTier(0);choice.weight=0;
    }
    /** Correct action-dependent damage that the native simulation intentionally approximates. */
    public static void conditional(BattleAIBase ai,MoveChoice hit){
        if(!NpcTurnKnowledge.active(hit.user.bc)||hit.targets.size()!=1||hit.attack==null)return;
        var user=hit.user;var target=hit.targets.get(0);Attack response=submitted(target);
        if(response==null&&NpcTurnKnowledge.offensive(target)>=0)response=target.attack;
        int known=NpcTurnKnowledge.offensive(target);
        for(var effect:hit.attack.getMove().effects){
            if(effect instanceof SuckerPunch&&(known==0||known==1&&response!=null&&!first(user,hit.attack,target,response))){failed(hit);return;}
            if(effect instanceof BackportEffects.UpperHand&&known>=0){
                if(known==0||response==null||response.getMove().getPriority(target)<=0||!first(user,hit.attack,target,response)){failed(hit);return;}
            }
            if(effect instanceof FocusPunch&&known==1&&response!=null&&first(target,response,user,hit.attack)){
                MoveChoice incoming=raw(ai,target,response,user);
                if(incoming.result!=null&&incoming.result.result!=AttackResult.failed&&incoming.result.damage>0){failed(hit);return;}
            }
            if(effect.getClass().getSimpleName().equals("ShellTrap")&&known>=0){
                if(known!=1||response==null||response.getAttackCategory()!=AttackCategory.PHYSICAL||!first(target,response,user,hit.attack)){failed(hit);return;}
                MoveChoice incoming=raw(ai,target,response,user);
                if(incoming.result==null||incoming.result.result==AttackResult.failed||incoming.result.damage<=0||incoming.result.fullDamage>=user.getHealth()){failed(hit);return;}
            }
            if(effect instanceof MeFirst&&known>=0){
                if(known!=1||response==null||NO_COPY.contains(response.getMove().getAttackName())||!first(user,hit.attack,target,response)){failed(hit);return;}
                MoveChoice copied=raw(ai,user,response,target);
                if(copied.result!=null){hit.result=copied.result;hit.result.fullDamage=(int)(hit.result.fullDamage*1.5);hit.result.damage=Math.min(target.getHealth(),hit.result.fullDamage);hit.raiseTier(hit.result.damage>=target.getHealth()?3:2);hit.weight=100f*hit.result.fullDamage/Math.max(1,target.getMaxHealth());}
            }
            if(effect instanceof DamageReflect&&known>=0){
                DamageReflect reflect=(DamageReflect)effect;
                if(known!=1||response==null||!reflect.isCorrectCategory(response.getAttackCategory())||!first(target,response,user,hit.attack)){failed(hit);return;}
                MoveChoice incoming=raw(ai,target,response,user);int damage=incoming.result==null?0:Math.max(incoming.result.damage,incoming.result.fullDamage);
                if(damage<=0||damage>=user.getHealth()||incoming.result.result==AttackResult.failed){failed(hit);return;}
                int reflected=(int)(damage*reflect.multiplier);hit.result.fullDamage=reflected;hit.result.damage=Math.min(target.getHealth(),reflected);
                hit.result.result=AttackResult.hit;hit.raiseTier(reflected>=target.getHealth()?3:2);hit.weight=100f*reflected/Math.max(1,target.getMaxHealth());
            }
        }
    }
    /** Forecast fast self-defence on detached targets; never execute a global effect or send packets. */
    private static PixelmonWrapper defended(PixelmonWrapper target,Attack defence,PixelmonWrapper user,Attack attack){
        if(defence==null||!first(target,defence,user,attack))return target;
        List<StatusBase> states=new ArrayList<>();
        for(var effect:defence.getMove().effects)if(effect instanceof ProtectVariation||effect instanceof ProtectVariationTeam||effect instanceof MagicCoat||effect instanceof Snatch)
            states.add(((StatusBase)effect).copy());
        if(states.isEmpty())return target;
        PixelmonWrapper copy=new PixelmonWrapper(target);copy.pokemon=PokemonFactory.copy(target.pokemon);copy.entity=null;
        for(var state:states){copy.getStatuses().removeIf(old->old.type==state.type);copy.getStatuses().add(state);}
        return copy;
    }
    public static void weight(PixelmonWrapper own,List<MoveChoice> choices){
        if(!NpcTurnKnowledge.active(own.bc))return;
        var ai=own.getBattleAI();
        for(var choice:choices){
            if(!choice.isAttack()||choice.tier<=0)continue;
            try(var action=NpcTurnKnowledge.preview(own,choice.attack)){
                if(choice.targets.size()==1){
                    var target=choice.targets.get(0);Attack response=submitted(target);
                    if(!own.isAlly(target)){
                        var departing=NpcTurnKnowledge.departure(target);
                        if(departing!=null&&choice.attack.getMove().effects.stream().anyMatch(e->e instanceof Pursuit)){
                            MoveChoice intercept=raw(ai,own,choice.attack,departing);choice.result=intercept.result;choice.tier=intercept.tier;choice.weight=intercept.weight;choice.targets=Collections.singletonList(departing);target=departing;response=submitted(target);
                        }
                        PixelmonWrapper protectedTarget=defended(target,response,own,choice.attack);
                        if(protectedTarget!=target){
                            MoveChoice result=raw(ai,own,choice.attack,protectedTarget);
                            if(choice.isOffensiveMove()||result.result!=null&&result.result.result==AttackResult.failed){choice.result=result.result;choice.tier=result.tier;choice.weight=result.weight;}
                            // Native result owns the projected target; keep the actual slot for execution.
                        }
                        conditional(ai,choice);
                        if(response!=null&&first(target,response,own,choice.attack)){
                            var intent=NpcMoveIntent.read(response);
                            // The actual native status decides legality. This is a
                            // risk penalty, not an unconditional immunity bypass.
                            if(intent.has(NpcMoveIntent.Trait.PUNISHES_KO)&&choice.result!=null&&choice.result.damage>=target.getHealth()&&own.getHealthPercent()>35){choice.lowerTier(2);choice.weight*=.1f;}
                        }
                    }
                }
                for(var enemy:own.getOpponentPokemon()){
                    Attack response=submitted(enemy);
                    if(response==null||!NpcTurnKnowledge.targets(enemy,own)||!first(enemy,response,own,choice.attack))continue;
                    var intent=NpcMoveIntent.read(response);
                    if(intent.has(NpcMoveIntent.Trait.BLOCKS_STATUS)&&choice.attack.getAttackCategory()==AttackCategory.STATUS)choice.weight*=.1f;
                    if(own.lastAttack!=null){boolean same=own.lastAttack.getMove().equals(choice.attack.getMove());if(intent.has(NpcMoveIntent.Trait.FORCES_LAST_MOVE)&&!same||intent.has(NpcMoveIntent.Trait.DISABLES_LAST_MOVE)&&same)choice.weight*=.15f;}
                    if(intent.has(NpcMoveIntent.Trait.STEALS_BUFF)&&choice.attack.getAttackCategory()==AttackCategory.STATUS&&choice.targets.contains(own))choice.weight*=.3f;
                }
            }
        }
    }
}
