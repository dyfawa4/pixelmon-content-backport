package local.pixelmon.backport.mixin;
import java.util.*;
import com.pixelmonmod.pixelmon.entities.DenEntity;
import com.pixelmonmod.pixelmon.battles.raids.*;
import com.pixelmonmod.pixelmon.init.registry.RaidRegistry;
import local.pixelmon.backport.RaidSpawnExclusions;
import local.pixelmon.backport.TeraRaidHabitats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=RaidRandomizer.class,remap=false)
public abstract class RaidCapExclusionMixin {
    @Redirect(method="getRandomRaid",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/util/helpers/RandomHelper;getRandomElementFromCollection(Ljava/util/Collection;)Ljava/lang/Object;"))
    private static Object backport$noCapPool(Collection<RaidRegistry.Raid> nativePool,DenEntity den,boolean forced){
        return com.pixelmonmod.pixelmon.api.util.helpers.RandomHelper.getRandomElementFromCollection(RaidSpawnExclusions.candidates(nativePool,den.field_70170_p,den.func_233580_cy_()));
    }
    @Inject(method="getRandomRaid",at=@At("RETURN"),cancellable=true)
    private static void backport$noCapEventOverride(DenEntity den,boolean forced,CallbackInfoReturnable<Optional<RaidData>> cir){
        Optional<RaidData> result=cir.getReturnValue();
        if(result!=null&&result.isPresent()){
            RaidData data=result.get();if(!RaidSpawnExclusions.allowed(data.getSpecies().getDex())||!TeraRaidHabitats.allowedNative(den.field_70170_p,den.func_233580_cy_(),data.getSpecies().getDex(),data.getForm().getName()))cir.setReturnValue(Optional.empty());
        }
    }
}
