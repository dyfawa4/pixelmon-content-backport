package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.npcs.registry.DropItemRegistry;
import com.google.gson.JsonElement;
import net.minecraft.util.ResourceLocation;
import local.pixelmon.backport.PokeChestLoot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=DropItemRegistry.class,remap=false)
public abstract class PokeChestRegistryMixin {
    @Inject(method="getDropsForPokemon",at=@At("RETURN"),cancellable=true)
    private static void backport$capRewards(com.pixelmonmod.pixelmon.entities.pixelmon.AbstractHoldsItemsEntity entity,CallbackInfoReturnable<java.util.List<net.minecraft.item.ItemStack>> ci){
        var before=ci.getReturnValue();var after=local.pixelmon.backport.CapDrops.ensure(entity,before);if(after!=before)ci.setReturnValue(after);
    }
    @Inject(method="registerDropItems",at=@At("RETURN"))
    private static void backport$validTier3(ResourceLocation id,JsonElement data,CallbackInfo ci){if(id.func_110623_a().contains("pokechestdrops"))PokeChestLoot.cleanTier3();}
}
