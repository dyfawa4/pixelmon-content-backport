package local.pixelmon.backport.mixin;
import java.util.*;
import com.pixelmonmod.pixelmon.entities.DenEntity;
import com.pixelmonmod.pixelmon.battles.raids.*;
import com.pixelmonmod.pixelmon.init.registry.RaidRegistry;
import local.pixelmon.backport.RaidSpawnExclusions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=RaidRandomizer.class,remap=false)
public abstract class RaidCapExclusionMixin {
    @ModifyArg(method="getRandomRaid",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/util/helpers/RandomHelper;getRandomElementFromCollection(Ljava/util/Collection;)Ljava/lang/Object;"),index=0)
    private static Collection<RaidRegistry.Raid> backport$noCapPool(Collection<RaidRegistry.Raid> nativePool){return RaidSpawnExclusions.candidates(nativePool);}
    @Inject(method="getRandomRaid",at=@At("RETURN"),cancellable=true)
    private static void backport$noCapEventOverride(DenEntity den,boolean forced,CallbackInfoReturnable<Optional<RaidData>> cir){
        Optional<RaidData> result=cir.getReturnValue();
        if(result!=null&&result.isPresent()&&!RaidSpawnExclusions.allowed(result.get().getSpecies().getDex()))cir.setReturnValue(Optional.empty());
    }
}
