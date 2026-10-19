package local.pixelmon.backport.mixin;

import local.pixelmon.backport.Erosion;
import com.pixelmonmod.pixelmon.api.pokemon.stats.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Substitute unmodified base calculations before native status/ability/item/stage modifiers. */
@Mixin(value=BattleStats.class,remap=false)
public abstract class ErosionStatsMixin {
    @Shadow private PixelmonWrapper pixelmon;
    @Redirect(method="getBaseBattleStats",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/stats/PermanentStats;getAttack()I"))
    private int attack(PermanentStats s){return Erosion.stat(pixelmon,BattleStatsType.ATTACK,s.getAttack());}
    @Redirect(method="getBaseBattleStats",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/stats/PermanentStats;getDefense()I"))
    private int defense(PermanentStats s){return Erosion.stat(pixelmon,BattleStatsType.DEFENSE,s.getDefense());}
    @Redirect(method="getBaseBattleStats",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/stats/PermanentStats;getSpecialAttack()I"))
    private int specialAttack(PermanentStats s){return Erosion.stat(pixelmon,BattleStatsType.SPECIAL_ATTACK,s.getSpecialAttack());}
    @Redirect(method="getBaseBattleStats",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/stats/PermanentStats;getSpecialDefense()I"))
    private int specialDefense(PermanentStats s){return Erosion.stat(pixelmon,BattleStatsType.SPECIAL_DEFENSE,s.getSpecialDefense());}
    @Redirect(method="getBaseBattleStats",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/stats/PermanentStats;getSpeed()I"))
    private int speed(PermanentStats s){return Erosion.stat(pixelmon,BattleStatsType.SPEED,s.getSpeed());}
    @Redirect(method="setBattleStatsForCurrentForm",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/stats/PermanentStats;calculateStat(Lcom/pixelmonmod/pixelmon/api/pokemon/stats/BattleStatsType;Lcom/pixelmonmod/pixelmon/api/pokemon/Nature;Lcom/pixelmonmod/pixelmon/api/pokemon/species/Stats;I)I"))
    private int form(PermanentStats s,BattleStatsType t,com.pixelmonmod.pixelmon.api.pokemon.Nature n,com.pixelmonmod.pixelmon.api.pokemon.species.Stats f,int l){return Erosion.stat(pixelmon,t,s.calculateStat(t,n,f,l));}
}
