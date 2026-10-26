package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.*;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.attackModifiers.MultipleHit;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.items.heldItems.*;

/** Conservative planning only: never roll or consume the opponent's real survival item. */
public final class NpcSurvival {
    private NpcSurvival(){}
    public static int minimumHits(MoveChoice choice){
        int hits=1;
        if(choice.user.usingZ)return hits;
        for(var effect:choice.attack.getMove().effects)if(effect instanceof MultipleHit){
            var multiple=(MultipleHit)effect;
            hits=choice.user.getBattleAbility() instanceof SkillLink?multiple.maxHits:multiple.minHits;
            if(AshWaterBoost.eligible(choice.user)&&choice.attack.getMove().getAttackName().equals("Water Shuriken"))hits=3;
        }
        return hits;
    }
    private static double minimumDamage(MoveChoice choice){
        double damage=Math.max(choice.result.damage,choice.result.fullDamage);
        for(var effect:choice.attack.getMove().effects)if(effect instanceof MultipleHit){
            var hits=(MultipleHit)effect;
            if(hits.minHits==2&&hits.maxHits==5&&minimumHits(choice)==2)damage*=2/3.168;
        }
        return damage*.9;
    }
    public static double chance(MoveChoice choice,PixelmonWrapper target,double health){
        var view=NpcTurnKnowledge.itemView(target);
        var item=view.getUsableHeldItem();
        if(AuthorHeldItems.blocked(view,item,"modifyDamageIncludeFixed:"+choice.user.getPokemonUUID()))return 1;
        if(item instanceof FocusBandItem)return .9; // A final lethal hit can still be survived, even in a multi-hit move.
        boolean full=health>=view.getMaxHealth();
        boolean sash=full&&item instanceof FocusSashItem;
        if(sash&&!(minimumHits(choice)>1&&minimumDamage(choice)>=health))return 0;
        return 1;
    }
    public static boolean reliable(MoveChoice choice,PixelmonWrapper target,double health){return chance(choice,target,health)>=1;}
    /** Upper damage forecast may cap a guaranteed one-hit survival, but never gamble on Focus Band. */
    public static double incoming(MoveChoice choice,PixelmonWrapper target,double raw){
        var view=NpcTurnKnowledge.itemView(target);
        if(minimumHits(choice)>1)return raw;
        if(view.hasFullHealth()&&view.getUsableHeldItem() instanceof FocusSashItem&&!AuthorHeldItems.blocked(view,view.getUsableHeldItem(),"modifyDamageIncludeFixed:"+choice.user.getPokemonUUID()))
            return Math.min(raw,Math.max(0,view.getHealth()-1));
        return raw;
    }
}
