package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.npcs.registry.BaseShopItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=BaseShopItem.class,remap=false)
public interface ShopPriceSourcesAccess {
 @Accessor("buy") int backport$buy();
}
