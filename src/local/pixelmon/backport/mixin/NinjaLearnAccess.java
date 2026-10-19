package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.LearnMoveController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=LearnMoveController.class,remap=false)
public interface NinjaLearnAccess {
    @Accessor("userMap") static com.google.common.collect.ListMultimap<java.util.UUID,?> backport$pending(){throw new AssertionError();}
}
