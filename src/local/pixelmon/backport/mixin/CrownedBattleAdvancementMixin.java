package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.items.HeldItem;
import local.pixelmon.backport.CrownedAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=BattleController.class,remap=false)
public abstract class CrownedBattleAdvancementMixin {
    @Redirect(method="initBattle",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/items/HeldItem;onStartOfBattle(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;)V"))
    private void backport$completedHeldForm(HeldItem item,PixelmonWrapper actor){
        String before=actor.getForm().getName();
        item.onStartOfBattle(actor);
        CrownedAdvancements.changed(actor,before);
    }
}
