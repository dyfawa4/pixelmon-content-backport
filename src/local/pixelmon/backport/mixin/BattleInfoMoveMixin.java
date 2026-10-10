package local.pixelmon.backport.mixin;
import local.pixelmon.backport.BattleInfoCompat;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.client.ClientProxy;
import java.util.List;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets="com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.ChooseAttack$MoveButton",priority=1100,remap=false)
public abstract class BattleInfoMoveMixin {
    @Shadow private Attack attack;
    @Redirect(method="pmbattleinfo$getEffectivenessColor",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/Element;getTotalEffectiveness(Ljava/util/List;Lcom/pixelmonmod/pixelmon/api/pokemon/Element;Z)F"),require=0)
    private float backport$display(List<Element> types,Element type,boolean inverse){
        return (float)BattleInfoCompat.multiplier(attack,ClientProxy.battleManager.getCurrentPokemon(),ClientProxy.battleManager.displayedEnemyPokemon[0],inverse);
    }
}
