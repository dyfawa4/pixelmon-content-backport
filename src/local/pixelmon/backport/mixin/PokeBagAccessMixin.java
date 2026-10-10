package local.pixelmon.backport.mixin;
import local.pixelmon.backport.PokeInventory;
import com.pixelmonmod.pixelmon.container.PokeBagContainer;
import com.pixelmonmod.pixelmon.items.PokeBagItem;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep active bag GUI and its backing stack coherent during healer/use events. */
@Mixin(value=PokeBagContainer.class,remap=false)
public abstract class PokeBagAccessMixin implements PokeInventory.OpenBag {
    @Shadow @Final private IInventory container;
    @Unique private ItemStack backport$stack;
    @Inject(method="<init>(ILnet/minecraft/entity/player/PlayerInventory;Lcom/pixelmonmod/pixelmon/items/PokeBagItem$PokeBag;)V",at=@At("RETURN"))
    private void backport$init(int id,PlayerInventory player,PokeBagItem.PokeBag bag,CallbackInfo ci){backport$stack=bag.getSlot()>=0?player.func_70301_a(bag.getSlot()):ItemStack.field_190927_a;}
    public ItemStack backport$bagStack(){return backport$stack;}
    public IInventory backport$bagInventory(){return container;}
}
