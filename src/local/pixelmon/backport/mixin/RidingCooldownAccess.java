package local.pixelmon.backport.mixin;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=Entity.class,remap=false)
public interface RidingCooldownAccess {
    @Accessor("field_184245_j") void backport$boardingCooldown(int ticks);
}
