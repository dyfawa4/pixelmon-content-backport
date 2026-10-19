package local.pixelmon.backport.mixin;

import local.pixelmon.backport.UltraPlantAchievement;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(targets="com.pixelmonmod.pixelmon.world.listener.UltraSpaceListener",remap=false)
public abstract class UltraPlantMiningBoltMixin {
    @Redirect(method="onBlockBreak",at=@At(value="INVOKE",target="Lnet/minecraft/world/World;func_217376_c(Lnet/minecraft/entity/Entity;)Z"))
    private static boolean backport$markMiningBolt(World world,Entity entity,BlockEvent.BreakEvent event){
        return UltraPlantAchievement.spawn(world,entity,event);
    }
}
