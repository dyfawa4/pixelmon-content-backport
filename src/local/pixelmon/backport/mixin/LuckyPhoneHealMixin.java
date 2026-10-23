package local.pixelmon.backport.mixin;
import local.pixelmon.backport.TeraItems;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="pokelucky.Process",remap=false)
public abstract class LuckyPhoneHealMixin {
    @Redirect(method="heal",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/storage/PlayerPartyStorage;heal()V"))
    private static void backport$curePhone(com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage party){for(var p:party.getAll())if(p!=null)local.pixelmon.backport.DrownedErosion.cured(p);party.heal();}
    @Inject(method="heal",at=@At("RETURN"))
    private static void backport$chargeAfterHeal(ServerPlayerEntity player,CallbackInfo ci){TeraItems.recharge(player);}
}
