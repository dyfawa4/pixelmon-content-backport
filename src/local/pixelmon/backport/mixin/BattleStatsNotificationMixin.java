package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStats;
import com.pixelmonmod.pixelmon.api.pokemon.ability.Ability;
import com.pixelmonmod.pixelmon.items.HeldItem;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=BattleStats.class,remap=false)
public abstract class BattleStatsNotificationMixin {
    @Shadow private PixelmonWrapper pixelmon;
    @Inject(method="notifyActivePokemonOfStatChange",at=@At("HEAD"),cancellable=true)
    private void backport$detached(CallbackInfo ci){if(pixelmon==null||pixelmon.bc==null)ci.cancel();}
    @Redirect(method="lambda$notifyActivePokemonOfStatChange$1",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/items/HeldItem;onStatModified(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;)V"),require=1)
    private void backport$optionalItem(HeldItem item,PixelmonWrapper user,PixelmonWrapper changed){if(item!=null)item.onStatModified(user,changed);}
    @Redirect(method="lambda$notifyActivePokemonOfStatChange$0",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/ability/Ability;applyEffectOnStatModified(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;)V"),require=1)
    private void backport$optionalAbility(Ability ability,PixelmonWrapper user,PixelmonWrapper changed){if(ability!=null)ability.applyEffectOnStatModified(user,changed);}
}
