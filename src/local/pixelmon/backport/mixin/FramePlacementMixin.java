package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.items.PaintingItem;
import local.pixelmon.backport.FramePlacement;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=PaintingItem.class,remap=false)
public abstract class FramePlacementMixin {
    @Redirect(method="func_195939_a",at=@At(value="INVOKE",target="Lnet/minecraft/world/World;func_217376_c(Lnet/minecraft/entity/Entity;)Z"))
    private boolean backport$framePlaceSound(World world,Entity frame){return FramePlacement.spawn(world,frame);}
}
