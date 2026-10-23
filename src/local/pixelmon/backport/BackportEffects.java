package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.MagicGuard;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.util.helpers.RandomHelper;
import com.pixelmonmod.pixelmon.battles.attacks.*;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.*;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.multiTurn.MultiTurnCharge;
import com.pixelmonmod.pixelmon.battles.controller.log.AttackResult;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.*;
import com.pixelmonmod.pixelmon.enums.heldItems.EnumHeldItems;

public final class BackportEffects {
    private BackportEffects(){}
    public static void register(){
        for(Class<? extends EffectBase> type:new Class[]{AlluringVoice.class,BurningBulwark.class,DragonCheer.class,ElectroShot.class,FickleBeam.class,HardPress.class,IvyCudgel.class,PsychicNoise.class,SupercellSlam.class,SyrupBomb.class,TeraStarstorm.class,TeraBlast.class,UpperHand.class,MightyCleave.class,BurstField.class,NinjaTraining.class,LightningOath.class}) {
            String key="Backport"+type.getSimpleName();
            Class<?> present=EffectTypeAdapter.EFFECTS.putIfAbsent(key,type);
            if(present!=null && present!=type)throw new IllegalStateException("Effect id collision: "+key);
        }
    }
    public static class LightningOath extends SpecialAttackBase {
        @Override public void applyEffect(PixelmonWrapper user,PixelmonWrapper target){if(local.pixelmon.backport.LightningOath.signature(user,user.attack))local.pixelmon.backport.LightningOath.apply(user,target);}
    }
    public static class NinjaTraining extends SpecialAttackBase {
        @Override public AttackResult applyEffectStart(PixelmonWrapper user,PixelmonWrapper target){return local.pixelmon.backport.NinjaTraining.allowed(user)?AttackResult.proceed:AttackResult.failed;}
        @Override public AttackResult applyEffectDuring(PixelmonWrapper user,PixelmonWrapper target){
            if(!local.pixelmon.backport.NinjaTraining.allowed(user))return AttackResult.failed;
            if(!AuthorChallenge.buff(user))return AttackResult.failed;
            user.getBattleStats().modifyStat(2,BattleStatsType.SPECIAL_ATTACK,user,true);
            local.pixelmon.backport.NinjaNextPriority.grant(user);
            local.pixelmon.backport.NinjaTraining.grant(user);return AttackResult.proceed;
        }
    }
    public static class BurstField extends SpecialAttackBase {
        @Override public AttackResult applyEffectStart(PixelmonWrapper user,PixelmonWrapper target){
            if(user.getSpecies().getDex()==CapSpecies.DEX && user.attack!=null && user.attack.isZ && user.isAlive() && !user.bc.battleEnded){
                user.bc.globalStatusController.addGlobalStatus(user,new BurstTerrain(user.getParticipant().team));
                user.bc.sendToAll("pixelmon.backport.burst.start");
            }
            return AttackResult.proceed;
        }
    }
    public static class AlluringVoice extends SpecialAttackBase implements BattleMechanics.TargetSecondary {
        @Override public void applyEffect(PixelmonWrapper user,PixelmonWrapper target){
            if(checkChance() && target.getBattleStats().isRaisedThisTurn())Confusion.confuse(user,target);
        }
    }
    public static class FickleBeam extends SpecialAttackBase {
        @Override public AttackResult applyEffectStart(PixelmonWrapper user,PixelmonWrapper target){
            if(RandomHelper.getRandomChance(.3f))user.attack.setOverridePower(user.attack.getMove().getBasePower()*2);
            return AttackResult.proceed;
        }
    }
    public static class HardPress extends SpecialAttackBase {
        @Override public AttackResult applyEffectStart(PixelmonWrapper user,PixelmonWrapper target){
            user.attack.setOverridePower(Math.max(1,100*target.getHealth()/target.getMaxHealth()));return AttackResult.proceed;
        }
    }
    public static class DragonCheer extends SpecialAttackBase {
        @Override public AttackResult applyEffectStart(PixelmonWrapper user,PixelmonWrapper target){
            return target.getBattleStats().increaseCritStage(target.hasType(Element.DRAGON)?2:1,true)?AttackResult.proceed:AttackResult.failed;
        }
    }
    public static class IvyCudgel extends SpecialAttackBase {
        @Override public AttackResult applyEffectStart(PixelmonWrapper user,PixelmonWrapper target){
            if(user.getSpecies().getDex()==1017){
                String form=user.getForm().getName();
                user.attack.overrideType(form.contains("wellspring")?Element.WATER:form.contains("hearthflame")?Element.FIRE:form.contains("cornerstone")?Element.ROCK:Element.GRASS);
            }
            return AttackResult.proceed;
        }
    }
    public static class UpperHand extends SpecialAttackBase {
        @Override public AttackResult applyEffectStart(PixelmonWrapper user,PixelmonWrapper target){
            int known=NpcTurnKnowledge.offensive(target);
            if(user.bc.simulateMode&&known>=0){
                float priority=com.pixelmonmod.pixelmon.battles.controller.BattlePriorityHelper.calculatePriority(target);
                return known==1&&priority>0&&priority<4?AttackResult.proceed:AttackResult.failed;
            }
            return target.attack!=null && target.attack.getAttackCategory()!=AttackCategory.STATUS && !target.hasMoved() && target.priority>0 && target.priority<4 ? AttackResult.proceed:AttackResult.failed;
        }
    }
    public static class ElectroShot extends MultiTurnCharge {
        public ElectroShot(){super("pixelmon.effect.electroshotcharging");}
        private boolean rain(PixelmonWrapper user){return UmbrellaHooks.backport$weather(user.bc.globalStatusController,user) instanceof Rainy;}
        @Override public AttackResult applyEffectDuring(PixelmonWrapper user,PixelmonWrapper target){
            if(!doesPersist(user)){setPersists(user,true);setTurnCount(user,2);}
            decrementTurnCount(user);
            if(getTurnCount(user)==1){
                user.getBattleStats().modifyStat(1,BattleStatsType.SPECIAL_ATTACK);
                if(!rain(user) && !user.getUsableHeldItem().affectMultiturnMove(user))return AttackResult.charging;
            }
            setPersists(user,false);return AttackResult.proceed;
        }
        @Override public boolean isCharging(PixelmonWrapper user,PixelmonWrapper target){return !rain(user)&&super.isCharging(user,target);}
        @Override public boolean shouldNotLosePP(PixelmonWrapper user){return !rain(user)&&super.shouldNotLosePP(user);}
    }
    public static class SupercellSlam extends SpecialAttackBase {
        @Override public void applyMissEffect(PixelmonWrapper user,PixelmonWrapper target){
            if(!(user.getBattleAbility() instanceof MagicGuard))user.doBattleDamage(user,user.getPercentMaxHealth(50f),DamageTypeEnum.CRASH);
        }
    }
    public static class MightyCleave extends HyperDrill {}
    public static class BurningBulwark extends Protect {
        @Override protected boolean addStatus(PixelmonWrapper user){return user.addStatus(new BurningBulwark(),user);}
        @Override public boolean stopsIncomingAttack(PixelmonWrapper target,PixelmonWrapper user){return user.attack.getAttackCategory()!=AttackCategory.STATUS && super.stopsIncomingAttack(target,user);}
        @Override public void stopsIncomingAttackMessage(PixelmonWrapper target,PixelmonWrapper user){
            super.stopsIncomingAttackMessage(target,user);
            if(user.attack.getMove().getMakesContact() && !(user.getBattleAbility() instanceof com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.LongReach) && user.getUsableHeldItem().getHeldItemType()!=EnumHeldItems.protectivePads)Burn.burn(target,user,null,false);
        }
    }
    public static class PsychicNoise extends SpecialAttackBase implements BattleMechanics.TargetSecondary {
        @Override public void applyEffect(PixelmonWrapper user,PixelmonWrapper target){
            if(!target.isAlive() || !checkChance())return;
            for(StatusBase status:target.getStatuses())if(status instanceof BriefHealBlock){((BriefHealBlock)status).turns=2;return;}
            if(!target.hasStatus(StatusType.HealBlock))target.addStatus(new BriefHealBlock(),user);
        }
    }
    public static class BriefHealBlock extends HealBlock {
        private int turns=2;
        @Override public boolean isTeamStatus(){return false;}
        @Override public void applyRepeatedEffect(PixelmonWrapper pokemon){if(--turns<=0)pokemon.removeStatus(this);}
        @Override public StatusBase copy(){BriefHealBlock copy=new BriefHealBlock();copy.turns=turns;return copy;}
    }
    public static class SyrupBomb extends StatusBase {
        private transient int turns;private transient PixelmonWrapper source;
        public SyrupBomb(){super(StatusType.None);}
        private SyrupBomb(PixelmonWrapper user){this();source=user;}
        @Override public void applyEffect(PixelmonWrapper user,PixelmonWrapper target){
            if(target.isAlive() && target.getStatuses().stream().noneMatch(s->s instanceof SyrupBomb))target.addStatus(new SyrupBomb(user),user);
        }
        @Override public void applyRepeatedEffect(PixelmonWrapper target){
            if(source==null || source.isFainted() || !source.onBattlefield || turns>=3){target.removeStatus(this);return;}
            target.getBattleStats().modifyStat(-1,BattleStatsType.SPEED,source,false);++turns;
        }
        @Override public StatusBase copy(){SyrupBomb result=new SyrupBomb(source);result.turns=turns;return result;}
    }
    public static class TeraBlast extends SpecialAttackBase {
        @Override public AttackResult applyEffectStart(PixelmonWrapper user,PixelmonWrapper target){
            if(Tera.active(user)){
                user.attack.overrideType(Tera.state(user).type);
                user.attack.overrideAttackCategory(BattleMechanics.adaptiveCategory(user));
                if(Tera.state(user).type==Element.MYSTERY)user.attack.setOverridePower(100);
            }
            return AttackResult.proceed;
        }
        @Override public void applyEffect(PixelmonWrapper user,PixelmonWrapper target){BattleMechanics.connected(user);}
        @Override public void applyEffectAfterAllTargets(PixelmonWrapper user){
            if(BattleMechanics.consumeConnection(user) && user.isAlive() && Tera.active(user) && Tera.state(user).type==Element.MYSTERY){
                user.getBattleStats().modifyStat(-1,BattleStatsType.ATTACK,user,true);
                user.getBattleStats().modifyStat(-1,BattleStatsType.SPECIAL_ATTACK,user,true);
            }
        }
    }
    public static class TeraStarstorm extends SpecialAttackBase {
        private boolean valid(PixelmonWrapper p){return p.getSpecies().getDex()==1024 && Tera.active(p) && Tera.state(p).type==Element.MYSTERY;}
        @Override public AttackResult applyEffectStart(PixelmonWrapper user,PixelmonWrapper target){
            if(valid(user)){
                user.attack.overrideType(Element.MYSTERY);
                user.attack.overrideAttackCategory(BattleMechanics.adaptiveCategory(user));
            }
            return AttackResult.proceed;
        }
    }
}
