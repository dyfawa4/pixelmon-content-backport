package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.blocks.PokeChestBlock;
import com.pixelmonmod.pixelmon.api.events.PokeLootEvent;
import local.pixelmon.backport.PokeChestLoot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=PokeChestBlock.class,remap=false)
public abstract class PokeChestLootMixin {
    @Redirect(method="func_225533_a_",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/events/PokeLootEvent$GetDrops;getDrops()[Lnet/minecraft/item/ItemStack;"))
    private ItemStack[] backport$validDrops(PokeLootEvent.GetDrops event){return PokeChestLoot.drops(event);}
    @Redirect(method="func_225533_a_",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/events/PokeLootEvent$Drop;getDrop()Lnet/minecraft/item/ItemStack;"))
    private ItemStack backport$validDrop(PokeLootEvent.Drop event){return PokeChestLoot.drop(event);}
}
