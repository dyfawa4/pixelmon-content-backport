package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.items.HeldItem;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.AuthorHeldItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=PixelmonWrapper.class,remap=false)
public abstract class AuthorItemWeightMixin {
    @Redirect(method="getWeight",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/items/HeldItem;modifyWeight(F)F"),require=1)
    private float backport$weight(HeldItem item,float weight){return AuthorHeldItems.blocked((PixelmonWrapper)(Object)this,item,"weight")?weight:item.modifyWeight(weight);}
}
