package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.StatsEffect;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.*;
import com.pixelmonmod.pixelmon.battles.status.*;

/** Describe effect semantics, independently of move names, species and the chosen response. */
public final class NpcMoveIntent {
    public enum Trait {ATTACK,STATUS,NEEDS_ATTACK,PRIORITY_INTERCEPT,INTERRUPTED_BY_HIT,RETALIATION,STEALS_ATTACK,
        NEEDS_PHYSICAL_HIT,GUARD,REFLECTS_STATUS,STEALS_BUFF,BLOCKS_STATUS,FORCES_LAST_MOVE,DISABLES_LAST_MOVE,
        PUNISHES_KO,PUNISHES_SWITCH,BOOSTS_SELF,HEALS_SELF}
    private final EnumSet<Trait> traits=EnumSet.noneOf(Trait.class);
    public boolean has(Trait trait){return traits.contains(trait);}
    public static NpcMoveIntent read(Attack attack){
        NpcMoveIntent intent=new NpcMoveIntent();if(attack==null)return intent;
        intent.traits.add(attack.getAttackCategory()==AttackCategory.STATUS?Trait.STATUS:Trait.ATTACK);
        for(var effect:attack.getMove().effects){
            if(effect instanceof SuckerPunch)intent.traits.add(Trait.NEEDS_ATTACK);
            if(effect instanceof BackportEffects.UpperHand)intent.traits.add(Trait.PRIORITY_INTERCEPT);
            if(effect instanceof FocusPunch)intent.traits.add(Trait.INTERRUPTED_BY_HIT);
            if(effect instanceof DamageReflect)intent.traits.add(Trait.RETALIATION);
            if(effect instanceof MeFirst)intent.traits.add(Trait.STEALS_ATTACK);
            if(effect.getClass().getSimpleName().equals("ShellTrap"))intent.traits.add(Trait.NEEDS_PHYSICAL_HIT);
            if(effect instanceof ProtectVariation||effect instanceof ProtectVariationTeam)intent.traits.add(Trait.GUARD);
            if(effect instanceof MagicCoat)intent.traits.add(Trait.REFLECTS_STATUS);
            if(effect instanceof Snatch)intent.traits.add(Trait.STEALS_BUFF);
            if(effect instanceof Taunt)intent.traits.add(Trait.BLOCKS_STATUS);
            if(effect instanceof Encore)intent.traits.add(Trait.FORCES_LAST_MOVE);
            if(effect instanceof Disable)intent.traits.add(Trait.DISABLES_LAST_MOVE);
            if(effect instanceof DestinyBond)intent.traits.add(Trait.PUNISHES_KO);
            if(effect instanceof Pursuit)intent.traits.add(Trait.PUNISHES_SWITCH);
            if(effect instanceof StatsEffect){var stats=(StatsEffect)effect;if(stats.isUser&&stats.amount>0)intent.traits.add(Trait.BOOSTS_SELF);}
            if(effect instanceof Recover)intent.traits.add(Trait.HEALS_SELF);
        }
        return intent;
    }
}
