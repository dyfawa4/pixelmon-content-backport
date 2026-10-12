package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.interactions.InteractionRainbowWing;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=InteractionRainbowWing.class,remap=false)
public abstract class RainbowWingAdvancementMixin {
    @Inject(method="processInteract",at=@At("RETURN"))
    private void backport$completed(PixelmonEntity entity,PlayerEntity player,Hand hand,ItemStack item,CallbackInfoReturnable<Boolean> result){
        if(result.getReturnValue() && player instanceof ServerPlayerEntity)
            local.pixelmon.backport.AppearanceAdvancements.rainbowConverted((ServerPlayerEntity)player,entity.getPokemon());
    }
}
