package local.pixelmon.backport.mixin;
import java.util.function.Predicate;
import com.pixelmonmod.pixelmon.entities.DenEntity;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.battles.raids.RaidData;
import local.pixelmon.backport.RaidSpawnExclusions;
import local.pixelmon.backport.TeraDenEntity;
import local.pixelmon.backport.TeraRaidHabitats;
import net.minecraft.nbt.CompoundNBT;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.item.Item;
import net.minecraft.entity.player.PlayerEntity;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import java.util.*;

@Mixin(value=DenEntity.class,remap=false)
public abstract class DenCapExclusionMixin {
    @Shadow private boolean lockout;
    @Unique private List<Species> backport$wishingCandidates(Predicate<Species> nativeRule){
        DenEntity den=(DenEntity)(Object)this;
        TeraRaidHabitats.Context habitat=den instanceof TeraDenEntity?new TeraRaidHabitats.Context(den.field_70170_p,den.func_233580_cy_()):null;
        List<Species> choices=new ArrayList<>();
        for(int dex:PixelmonSpecies.getLegendaries(false)){
            Species species=PixelmonSpecies.fromNationalDex(dex);
            if(species!=null&&RaidSpawnExclusions.allowed(dex)&&nativeRule.test(species)&&(habitat==null?TeraRaidHabitats.allowedNative(den.field_70170_p,den.func_233580_cy_(),dex,species.getDefaultForm().getName()):habitat.allowed(Integer.toString(dex))))choices.add(species);
        }
        return choices;
    }
    @Inject(method="attemptWishingPiece",at=@At("HEAD"),cancellable=true)
    private void backport$emptyLegendaryWish(Item item,PlayerEntity player,boolean consumeHeldItem,CallbackInfoReturnable<Boolean> ci){
        DenEntity den=(DenEntity)(Object)this;
        if(!(den instanceof TeraDenEntity)||!local.pixelmon.backport.TeraWishes.item(item))return;
        boolean success=local.pixelmon.backport.TeraWishes.attempt((TeraDenEntity)den,item,player,consumeHeldItem);
        if(success)lockout=true;
        ci.setReturnValue(success);
    }
    @Redirect(method="attemptWishingPiece",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/registries/PixelmonSpecies;getRandomLegendary(Ljava/util/function/Predicate;)Lcom/pixelmonmod/pixelmon/api/pokemon/species/Species;"))
    private Species backport$noCapLegendary(Predicate<Species> nativeRule){
        DenEntity den=(DenEntity)(Object)this;
        if(!(den instanceof TeraDenEntity))return PixelmonSpecies.getRandomLegendary(s->s!=null&&RaidSpawnExclusions.allowed(s.getDex())&&nativeRule.test(s)&&TeraRaidHabitats.allowedNative(den.field_70170_p,den.func_233580_cy_(),s.getDex(),s.getDefaultForm().getName()));
        List<Species> choices=backport$wishingCandidates(nativeRule);
        return choices.get(den.func_70681_au().nextInt(choices.size()));
    }
    @ModifyVariable(method="setData",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private RaidData backport$allowedData(RaidData data){
        if(data==null||data.getStars()<0)return data;
        DenEntity den=(DenEntity)(Object)this;
        if(RaidSpawnExclusions.allowed(data.getSpecies().getDex())&&(den.field_70170_p.field_72995_K||(den instanceof TeraDenEntity?((TeraDenEntity)den).allowedEncounter(data):TeraRaidHabitats.allowedNative(den.field_70170_p,den.func_233580_cy_(),data.getSpecies().getDex(),data.getForm().getName()))))return data;
        // The native empty-den sentinel remains safe for sync, lobby and saving.
        return new RaidData(((DenEntity)(Object)this).func_145782_y(),-1,1,"");
    }
    @Inject(method="func_70037_a",at=@At("RETURN"))
    private void backport$excludeLoadedCap(CompoundNBT saved,CallbackInfo ci){
        DenEntity den=(DenEntity)(Object)this;
        if(!den.field_70170_p.field_72995_K&&den.getData().isPresent()){
            var data=den.getServerData();if(!RaidSpawnExclusions.allowed(data.getSpecies().getDex())||!TeraRaidHabitats.allowedNative(den.field_70170_p,den.func_233580_cy_(),data.getSpecies().getDex(),data.getForm().getName()))den.clearData();
        }
    }
}
