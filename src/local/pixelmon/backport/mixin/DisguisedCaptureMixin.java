package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.pokeballs.EmptyPokeBallEntity;
import com.pixelmonmod.pixelmon.entities.pokeballs.PokeBallEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.world.World;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import local.pixelmon.backport.DisguisedCapture;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=EmptyPokeBallEntity.class,remap=false)
public abstract class DisguisedCaptureMixin extends PokeBallEntity {
    protected DisguisedCaptureMixin(EntityType<? extends PokeBallEntity> type,World world){super(type,world);}
    @Unique private boolean backport$revealed;
    @Inject(method="checkReallyADitto",at=@At("RETURN"))
    private void backport$reveal(CallbackInfoReturnable<Boolean> cir){backport$revealed=Boolean.TRUE.equals(cir.getReturnValue()) && pixelmon!=null && pixelmon.getSpecies().getDex()==132;}
    @Inject(method="storeCapture",at=@At("RETURN"))
    private void backport$stored(CallbackInfo ci){
        if(thrower instanceof ServerPlayerEntity && pixelmon!=null)DisguisedCapture.stored((ServerPlayerEntity)thrower,pixelmon.getPokemon(),backport$revealed);
        backport$revealed=false;
    }
    @Inject(method="failCapture",at=@At("HEAD"))
    private void backport$failed(CallbackInfo ci){backport$revealed=false;}
}
