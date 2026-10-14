package local.pixelmon.backport.mixin;
import java.util.function.Predicate;
import com.pixelmonmod.pixelmon.entities.DenEntity;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.battles.raids.RaidData;
import local.pixelmon.backport.RaidSpawnExclusions;
import net.minecraft.nbt.CompoundNBT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=DenEntity.class,remap=false)
public abstract class DenCapExclusionMixin {
    @ModifyArg(method="attemptWishingPiece",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/registries/PixelmonSpecies;getRandomLegendary(Ljava/util/function/Predicate;)Lcom/pixelmonmod/pixelmon/api/pokemon/species/Species;"),index=0)
    private Predicate<Species> backport$noCapLegendary(Predicate<Species> nativeRule){
        return species->species!=null&&RaidSpawnExclusions.allowed(species.getDex())&&nativeRule.test(species);
    }
    @ModifyVariable(method="setData",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private RaidData backport$allowedData(RaidData data){
        if(data==null||data.getStars()<0||RaidSpawnExclusions.allowed(data.getSpecies().getDex()))return data;
        // The native empty-den sentinel remains safe for sync, lobby and saving.
        return new RaidData(((DenEntity)(Object)this).func_145782_y(),-1,1,"");
    }
    @Inject(method="func_70037_a",at=@At("RETURN"))
    private void backport$excludeLoadedCap(CompoundNBT saved,CallbackInfo ci){
        DenEntity den=(DenEntity)(Object)this;
        if(!den.field_70170_p.field_72995_K&&den.getData().isPresent()&&!RaidSpawnExclusions.allowed(den.getServerData().getSpecies().getDex()))den.clearData();
    }
}
