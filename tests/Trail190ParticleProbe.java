package qa.trailmixin;
import qa.Trail190Cases;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particles.IParticleData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Observe native particle emission only in the disposable QA jar. */
@Mixin(value=ClientWorld.class,remap=false)
public abstract class Trail190ParticleProbe {
 @Inject(method="func_195594_a(Lnet/minecraft/particles/IParticleData;DDDDDD)V",at=@At("HEAD"))
 private void observe(IParticleData data,double x,double y,double z,double vx,double vy,double vz,CallbackInfo ci){if(Trail190Cases.recording){Trail190Cases.count++;Trail190Cases.last=data;}}
}
