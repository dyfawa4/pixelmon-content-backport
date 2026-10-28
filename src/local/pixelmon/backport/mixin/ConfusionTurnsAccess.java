package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.status.Confusion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=Confusion.class,remap=false)
public interface ConfusionTurnsAccess {
    @Accessor("effectTurns") int backport$turns();
}
