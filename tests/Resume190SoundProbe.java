package qa.trailmixin;
import qa.Resume190Cases;import net.minecraft.world.server.ServerWorld;import net.minecraft.entity.player.PlayerEntity;import net.minecraft.util.*;import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ServerWorld.class,remap=false) public abstract class Resume190SoundProbe {
 @Inject(method="func_184148_a",at=@At("HEAD")) private void sound(PlayerEntity p,double x,double y,double z,SoundEvent s,SoundCategory c,float v,float pitch,CallbackInfo ci){if(s==com.pixelmonmod.pixelmon.init.registry.SoundRegistration.PC.get()){Resume190Cases.sounds++;Resume190Cases.correctSound=c==SoundCategory.BLOCKS&&Math.abs(v-.7f)<.001&&pitch==1f;}}
}