package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractClientEntity;
import com.pixelmonmod.pixelmon.comm.packetHandlers.evolution.EvolutionStage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Temporary render-local stage only. Never sends evolution packets or changes size. */
@Mixin(value=AbstractClientEntity.class,remap=false)
public interface TeraEvolutionAccess {
    @Accessor("evoStage") void backport$visualEvolutionStage(EvolutionStage stage);
}
