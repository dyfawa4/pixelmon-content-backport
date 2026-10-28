package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.OHKO;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.api.util.helpers.RandomHelper;
import local.pixelmon.backport.AuthorForesight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Share native OHKO's inner roll with its current-action forecast. */
@Mixin(value=OHKO.class,remap=false)
public abstract class NpcOhkoRollMixin {
    @Redirect(method="applyEffectDuring",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/util/helpers/RandomHelper;getRandomChance(I)Z"),require=1)
    private boolean backport$ohko(int percent,PixelmonWrapper user,PixelmonWrapper target){
        if(AuthorForesight.forecast(user)&&!user.bc.simulateMode)return AuthorForesight.ohko(user,target,percent);
        return RandomHelper.getRandomChance(percent);
    }
}
