package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.pokeballs.*;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native capture resets friendship after the success event. Apply after actual storage. */
@Mixin(value=EmptyPokeBallEntity.class,remap=false)
public abstract class CapCaptureFriendshipMixin extends PokeBallEntity {
    protected CapCaptureFriendshipMixin(EntityType<? extends PokeBallEntity> type,World world){super(type,world);}
    @Inject(method="storeCapture",at=@At("RETURN"))
    private void backport$friendship(CallbackInfo ci){if(pixelmon!=null)local.pixelmon.backport.CapBallPreference.stored(pixelmon.getPokemon());}
}
