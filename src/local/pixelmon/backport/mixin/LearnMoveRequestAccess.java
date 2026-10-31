package local.pixelmon.backport.mixin;
import java.util.UUID;
import java.util.function.Predicate;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(targets="com.pixelmonmod.pixelmon.api.pokemon.LearnMoveController$LearnMove",remap=false)
public interface LearnMoveRequestAccess {
 @Accessor("pokemon") UUID backport$pokemon();
 @Accessor("move") ImmutableAttack backport$move();
 @Accessor("condition") Predicate<?> backport$condition();
}
