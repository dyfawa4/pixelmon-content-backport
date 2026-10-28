package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.pokeballs.PokeBallEntity;
import local.pixelmon.backport.RadiantBallLogic;
import net.minecraft.particles.RedstoneParticleData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Reuse the native Premier Ball renderer callback, changing only Radiant's trail colour. */
@Mixin(value=PokeBallEntity.class,remap=false)
public abstract class RadiantTrailMixin {
 @Unique private static final RedstoneParticleData backport$goldDust=new RedstoneParticleData(1f,.74f,.12f,1f);
 @Unique private int backport$lastTrailTick=Integer.MIN_VALUE;
 @Shadow protected abstract boolean getIsOnGround();
 @Inject(method="premierFlash",at=@At("HEAD"),cancellable=true)
 private void backport$gold(CallbackInfo ci){
  PokeBallEntity ball=(PokeBallEntity)(Object)this;
  if(ball.getBallType()==null||!RadiantBallLogic.NAME.equals(ball.getBallType().getName()))return;
  ci.cancel();
  if(backport$lastTrailTick==ball.field_70173_aa)return;
  if(ball.field_70170_p.field_72995_K&&!ball.field_70128_L&&!ball.getIsWaiting()&&!getIsOnGround()&&ball.func_213322_ci().func_189985_c()>0.0001){
   backport$lastTrailTick=ball.field_70173_aa;
   ball.field_70170_p.func_195594_a(backport$goldDust,ball.func_226277_ct_()+.1,ball.func_226278_cu_()+.05,ball.func_226281_cx_(),0,0,0);
  }
 }
}
