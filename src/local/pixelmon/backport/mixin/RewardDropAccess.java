package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.boss.DropSet;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=DropSet.Drop.class,remap=false)
public interface RewardDropAccess {
    @Accessor("item") ItemStack backport$item();
    @Accessor("commands") String[] backport$commands();
}
