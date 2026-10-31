package local.pixelmon.backport.mixin;
import java.util.UUID;
import com.google.common.collect.ListMultimap;
import com.pixelmonmod.pixelmon.api.pokemon.LearnMoveController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=LearnMoveController.class,remap=false)
public interface LearnMoveControllerAccess {
 @Accessor("userMap") static ListMultimap<UUID,?> backport$requests(){throw new AssertionError();}
}
