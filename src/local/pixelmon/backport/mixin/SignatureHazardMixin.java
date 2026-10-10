package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.EntryHazard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A full hazard stack must not invalidate a damaging signature move. */
@Mixin(value=EntryHazard.class,remap=false)
public abstract class SignatureHazardMixin {
    @Shadow private int maxLayers;

    @Inject(method="applyHazards",at=@At("HEAD"),cancellable=true)
    private void backport$keepDamageResult(PixelmonWrapper user,PixelmonWrapper target,CallbackInfo ci){
        if(user.attack==null)return;
        String name=user.attack.getMove().getAttackName();
        if(!"Ceaseless Edge".equals(name) && !"Stone Axe".equals(name))return;
        EntryHazard self=(EntryHazard)(Object)this;
        if(target.getStatus(self.type) instanceof EntryHazard &&
           ((EntryHazard)target.getStatus(self.type)).getNumLayers()>=maxLayers)ci.cancel();
    }
}
