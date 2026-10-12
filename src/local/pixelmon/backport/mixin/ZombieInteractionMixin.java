package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.pixelmon.interactions.InteractionZombie;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Native code compares carved pumpkin; the documented catalyst is a lit pumpkin. */
@Mixin(value=InteractionZombie.class,remap=false)
public abstract class ZombieInteractionMixin {
    @Redirect(method="processInteract",at=@At(value="FIELD",target="Lnet/minecraft/block/Blocks;field_196625_cS:Lnet/minecraft/block/Block;",opcode=178))
    private Block backport$litPumpkin(){return Blocks.field_196628_cT;}
    @Inject(method="processInteract",at=@At("RETURN"))
    private void backport$completed(PixelmonEntity entity,PlayerEntity player,Hand hand,ItemStack item,CallbackInfoReturnable<Boolean> result){
        if(result.getReturnValue() && player instanceof ServerPlayerEntity)
            local.pixelmon.backport.AppearanceAdvancements.zombieConverted((ServerPlayerEntity)player,entity.getPokemon());
    }
}
