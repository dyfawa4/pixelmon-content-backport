package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.items.PokeBallItem;
import com.pixelmonmod.pixelmon.items.PokeBallPart;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Resolve descriptions by ball subtype; preserve explicit stack tooltip overrides. */
@Mixin(value=PokeBallItem.class,remap=false)
public abstract class BallDescriptionMixin {
    @Inject(method="getTooltipText(Lnet/minecraft/item/ItemStack;)Ljava/lang/String;",at=@At("HEAD"),cancellable=true)
    private void backport$ballDescription(ItemStack stack,CallbackInfoReturnable<String> cir){
        if(stack.func_77942_o()&&stack.func_77978_p().func_74764_b("tooltip"))return;
        PokeBallPart.getPokeBall(stack).ifPresent(ball->{
            String name=ball.getName();
            if(name.equals("origin_ball")||name.equals("park_ball")||name.equals(local.pixelmon.backport.RadiantBallLogic.NAME)||name.equals(local.pixelmon.backport.SpectralBallLogic.NAME)||java.util.Set.of("gigaton_ball","ancient_heavy_ball","leaden_ball","feather_ball","jet_ball","wing_ball").contains(name))
                cir.setReturnValue(I18n.func_135052_a("pixelmon.backport.ball.description."+name));
        });
    }
}
