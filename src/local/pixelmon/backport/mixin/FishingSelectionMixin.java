package local.pixelmon.backport.mixin;
import java.util.List;
import com.pixelmonmod.pixelmon.api.spawning.*;
import com.pixelmonmod.pixelmon.api.spawning.calculators.SelectionAlgorithm;
import local.pixelmon.backport.FishingEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=AbstractSpawner.class,remap=false)
public abstract class FishingSelectionMixin {
    @Redirect(method="getWeightedSpawnInfo",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/spawning/calculators/SelectionAlgorithm;choose(Lcom/pixelmonmod/pixelmon/api/spawning/AbstractSpawner;Lcom/pixelmonmod/pixelmon/api/spawning/SpawnLocation;Ljava/util/List;)Lcom/pixelmonmod/pixelmon/api/spawning/SpawnInfo;"))
    private SpawnInfo backport$specialPool(SelectionAlgorithm algorithm,AbstractSpawner spawner,SpawnLocation location,List<SpawnInfo> candidates){return FishingEnchantments.choose(algorithm,spawner,location,candidates);}
}
