package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.species.parameters.ControlledMovementLogic;
import com.pixelmonmod.pixelmon.entities.pixelmon.*;
import local.pixelmon.backport.RidingMotion;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=PixelmonEntity.class,remap=false)
public abstract class RidingTravelMixin extends AbstractInteractionsEntity {
    protected RidingTravelMixin(EntityType<? extends AbstractInteractionsEntity> type,World world){super(type,world);}
    @Override public void func_213352_e(Vector3d input){if(!RidingMotion.travel((PixelmonEntity)(Object)this))super.func_213352_e(input);}
    @Redirect(method="func_70636_d",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/entities/pixelmon/PixelmonEntity;func_184186_bw()Z"))
    private boolean backport$interpolateServerMotion(PixelmonEntity e){return e.field_70170_p.field_72995_K&&RidingMotion.active(e)?false:e.func_184186_bw();}
    @Inject(method="func_110206_u",at=@At("HEAD"),cancellable=true)
    private void backport$noChargedJump(int power,CallbackInfo ci){if(RidingMotion.active((Entity)(Object)this))ci.cancel();}
    @Redirect(method="func_70030_z",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/species/parameters/ControlledMovementLogic;handleMovementLogic(Lcom/pixelmonmod/pixelmon/entities/pixelmon/PixelmonEntity;Lnet/minecraft/entity/player/PlayerEntity;)V"))
    private void backport$noLegacyFlight(ControlledMovementLogic logic,PixelmonEntity e,PlayerEntity p){if(!RidingMotion.active(e))logic.handleMovementLogic(e,p);}
    @Redirect(method="func_70030_z",at=@At(value="INVOKE",target="Lnet/minecraft/entity/Entity;func_70040_Z()Lnet/minecraft/util/math/vector/Vector3d;"))
    private Vector3d backport$noLegacyWaterPitch(Entity passenger){return RidingMotion.active((Entity)(Object)this)?Vector3d.field_186680_a:passenger.func_70040_Z();}
    @Inject(method="func_70030_z",at=@At("TAIL"))
    private void backport$cleanup(CallbackInfo ci){RidingMotion.cleanup((PixelmonEntity)(Object)this);}
}
