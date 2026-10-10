package local.pixelmon.backport.mixin;
import local.pixelmon.backport.ReferenceItems;
import com.pixelmonmod.pixelmon.api.pokemon.stats.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=BattleStats.class,remap=false)
public abstract class ClearAmuletMixin {
    @Shadow private PixelmonWrapper pixelmon;
    @Inject(method="decreaseStat(ILcom/pixelmonmod/pixelmon/api/pokemon/stats/BattleStatsType;Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;ZZ)Z",at=@At("HEAD"),cancellable=true)
    private void backport$block(int amount,BattleStatsType stat,PixelmonWrapper source,boolean attack,boolean messages,CallbackInfoReturnable<Boolean> cir){
        if(source!=null && source!=pixelmon && amount>0 && pixelmon.getUsableHeldItem() instanceof ReferenceItems.Amulet)cir.setReturnValue(false);
    }
}
