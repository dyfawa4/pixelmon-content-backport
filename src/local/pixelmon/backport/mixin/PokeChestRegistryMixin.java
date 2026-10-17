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
        var before=ci.getReturnValue();var after=local.pixelmon.backport.RewardSanitizer.stacks(local.pixelmon.backport.CapDrops.ensure(entity,before));if(after!=before)ci.setReturnValue(after);
    }
    @Inject(method="registerTierDrops",at=@At("RETURN"))
    private static void backport$validTiers(com.google.gson.JsonObject json,String name,java.util.List<net.minecraft.item.ItemStack> pool,CallbackInfo ci){pool.removeIf(item->!PokeChestLoot.valid(item));}
    @Redirect(method={"getTier1Drop","getTier2Drop","getTier3Drop","getUltraSpaceDrop"},at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/util/helpers/RandomHelper;getRandomElementFromCollection(Ljava/util/Collection;)Ljava/lang/Object;"))
    private static Object backport$nonemptyPool(java.util.Collection<net.minecraft.item.ItemStack> pool){pool.removeIf(item->!PokeChestLoot.valid(item));return pool.isEmpty()?net.minecraft.item.ItemStack.field_190927_a:com.pixelmonmod.pixelmon.api.util.helpers.RandomHelper.getRandomElementFromCollection(pool);}
    @Inject(method="registerDropItems",at=@At("RETURN"))
    private static void backport$validTier3(ResourceLocation id,JsonElement data,CallbackInfo ci){if(id.func_110623_a().contains("pokechestdrops"))PokeChestLoot.cleanTier3();}
}
