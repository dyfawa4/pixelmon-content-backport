package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.command.impl.HealCommand;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=HealCommand.class,remap=false)
public abstract class StatusCommandHealMixin {
 @Redirect(method="execute",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/storage/PlayerPartyStorage;heal()V"))
 private void backport$explicitHeal(PlayerPartyStorage party){for(var p:party.getAll())if(p!=null){local.pixelmon.backport.Erosion.cured(p);local.pixelmon.backport.Calamity.cured(p);}party.heal();for(var p:party.getAll())if(p!=null)local.pixelmon.backport.CalamityPartySync.correct(p);}
}
