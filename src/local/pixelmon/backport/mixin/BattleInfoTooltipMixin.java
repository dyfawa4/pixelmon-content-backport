package local.pixelmon.backport.mixin;
import local.pixelmon.backport.BattleInfoCompat;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.client.ClientProxy;
import com.pixelmonmod.pixelmon.client.gui.battles.PixelmonClientData;
import java.util.List;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="com.inhumanity.pmbattleinfo.util.Tooltip",remap=false)
public abstract class BattleInfoTooltipMixin {
    @Inject(method="getOurTypes",at=@At("HEAD"),cancellable=true)
    private static void backport$ours(CallbackInfoReturnable<List<Element>> c){c.setReturnValue(BattleInfoCompat.defense(ClientProxy.battleManager.displayedOurPokemon[0]));}
    @Inject(method="getEnemyTypes",at=@At("HEAD"),cancellable=true)
    private static void backport$enemy(CallbackInfoReturnable<List<Element>> c){c.setReturnValue(BattleInfoCompat.defense(ClientProxy.battleManager.displayedEnemyPokemon[0]));}
    @Redirect(method="getTooltipEnemyAlly",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/attacks/Attack;getType()Lcom/pixelmonmod/pixelmon/api/pokemon/Element;"))
    private static Element backport$move(Attack attack,PixelmonClientData user,boolean enemy){return BattleInfoCompat.moveType(attack,user);}
}
