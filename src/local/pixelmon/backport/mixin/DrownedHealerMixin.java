package local.pixelmon.backport.mixin;
import java.util.List;
import java.util.function.Consumer;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.blocks.tileentity.HealerTileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Clear only on the server's actual completed healer action, never an inventory heal or GUI-close packet. */
@Mixin(value=HealerTileEntity.class,remap=false)
public abstract class DrownedHealerMixin {
 @Redirect(method="func_73660_a",at=@At(value="INVOKE",target="Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"),require=1)
 private void backport$curePersistentErosion(List<Pokemon> party,Consumer<Pokemon> nativeHealing){for(var pokemon:party){local.pixelmon.backport.DrownedErosion.cured(pokemon);nativeHealing.accept(pokemon);}}
}
