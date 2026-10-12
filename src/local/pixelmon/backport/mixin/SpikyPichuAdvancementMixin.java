package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.PixelmonEventHandler;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Keep native shrine conditions and award only after its new Pokemon was stored. */
@Mixin(value=PixelmonEventHandler.class,remap=false)
public abstract class SpikyPichuAdvancementMixin {
    @Redirect(method="onMoveSkillUsed",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/storage/PlayerPartyStorage;add(Lcom/pixelmonmod/pixelmon/api/pokemon/Pokemon;)Z"))
    private boolean backport$received(PlayerPartyStorage party,Pokemon pokemon){
        boolean added=party.add(pokemon);
        if(added)local.pixelmon.backport.AppearanceAdvancements.spikyReceived(party.getPlayer(),pokemon);
        return added;
    }
}
