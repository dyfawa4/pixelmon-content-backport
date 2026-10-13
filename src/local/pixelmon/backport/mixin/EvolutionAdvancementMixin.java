package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.entities.pixelmon.helpers.EvolutionQuery;
import local.pixelmon.backport.AcquisitionAdvancements;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only a real completed species evolution counts; cancelled early returns keep the old dex. */
@Mixin(value=EvolutionQuery.class,remap=false)
public abstract class EvolutionAdvancementMixin {
    @Shadow Pokemon pokemon;
    @Shadow ServerPlayerEntity player;
    @Shadow boolean normalEvolution;
    @Unique private int backport$beforeDex;
    @Inject(method="doEvoSwitch",at=@At("HEAD"),cancellable=true)
    private void backport$beforeEvolution(CallbackInfo ci){
        backport$beforeDex=pokemon==null?-1:pokemon.getSpecies().getDex();
        if(normalEvolution && backport$beforeDex==local.pixelmon.backport.CapSpecies.DEX){((EvolutionQuery)(Object)this).abort();ci.cancel();}
    }
    @Inject(method="doEvoSwitch",at=@At("RETURN"))
    private void backport$afterEvolution(CallbackInfo ci){if(normalEvolution)AcquisitionAdvancements.evolved(player,pokemon,backport$beforeDex);}
}
