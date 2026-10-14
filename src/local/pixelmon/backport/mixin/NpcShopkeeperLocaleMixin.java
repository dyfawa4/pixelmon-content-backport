package local.pixelmon.backport.mixin;
import local.pixelmon.backport.NpcLocalization;
import com.pixelmonmod.pixelmon.entities.npcs.NPCShopkeeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=NPCShopkeeper.class,remap=false)
public abstract class NpcShopkeeperLocaleMixin {
    @Inject(method="getShopkeeperName",at=@At("RETURN"),cancellable=true)
    private void backport$shopName(String locale,CallbackInfoReturnable<String> ci){ci.setReturnValue(NpcLocalization.translate(ci.getReturnValue(),locale));}
}
