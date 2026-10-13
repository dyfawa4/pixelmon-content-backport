package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.raids.RaidData;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.enums.TriBoolean;
import local.pixelmon.backport.AcquisitionAdvancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Both native and Tera raids share this actual insertion, after all cancellation checks. */
@Mixin(value=RaidData.class,remap=false)
public abstract class RaidAcquisitionMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method="removePlayer",at=@At("RETURN"))
    private void backport$clearRaidBonus(net.minecraft.entity.player.ServerPlayerEntity player,org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci){local.pixelmon.backport.BurstRaidCapture.finish((com.pixelmonmod.pixelmon.battles.raids.RaidData)(Object)this,player.func_110124_au());}
    @org.spongepowered.asm.mixin.injection.Inject(method={"kickAll","startRaid"},at=@At("HEAD"))
    private void backport$resetRaidBonus(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){local.pixelmon.backport.BurstRaidCapture.clear((com.pixelmonmod.pixelmon.battles.raids.RaidData)(Object)this);}
    @org.spongepowered.asm.mixin.injection.Inject(method="processAction",at=@At("RETURN"))
    private void backport$consumeRaidBonus(int action,int index,net.minecraft.item.ItemStack item,net.minecraft.entity.player.ServerPlayerEntity player,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){var raid=(com.pixelmonmod.pixelmon.battles.raids.RaidData)(Object)this;var p=raid.getPlayer(player);if(p==null || p.catchAttempted)local.pixelmon.backport.BurstRaidCapture.finish(raid,player.func_110124_au());}
    @Redirect(method="processAction",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/storage/PlayerPartyStorage;addSilently(Lcom/pixelmonmod/pixelmon/api/pokemon/Pokemon;)Lcom/pixelmonmod/pixelmon/api/enums/TriBoolean;"))
    private TriBoolean backport$stored(PlayerPartyStorage storage,Pokemon pokemon){
        TriBoolean result=storage.addSilently(pokemon);
        if(result!=null && result!=TriBoolean.NULL)AcquisitionAdvancements.raidStored(pokemon.getOwnerPlayer(),pokemon);
        return result;
    }
}
