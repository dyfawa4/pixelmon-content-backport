package local.pixelmon.backport.mixin;
import local.pixelmon.backport.TeraItems;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="pokelucky.Process",remap=false)
public abstract class LuckyPhoneHealMixin {
    @Inject(method="heal",at=@At("RETURN"))
    private static void backport$chargeAfterHeal(ServerPlayerEntity player,CallbackInfo ci){TeraItems.recharge(player);}
}
