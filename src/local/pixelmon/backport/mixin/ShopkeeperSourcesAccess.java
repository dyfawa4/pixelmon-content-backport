package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.npcs.registry.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Read candidate stock without rolling the shop's random inventory. */
@Mixin(value=ShopkeeperData.class,remap=false)
public interface ShopkeeperSourcesAccess {
 @Accessor("items") java.util.ArrayList<ShopItem> backport$items();
}
