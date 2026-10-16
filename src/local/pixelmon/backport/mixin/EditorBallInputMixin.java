package local.pixelmon.backport.mixin;

import local.pixelmon.backport.client.EditorNames;
import com.pixelmonmod.pixelmon.client.gui.pokemoneditor.PokemonEditorIndividualScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=PokemonEditorIndividualScreen.class,remap=false)
public abstract class EditorBallInputMixin {
    @ModifyArg(method="*",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/item/pokeball/PokeBallRegistry;getPokeBall(Ljava/lang/String;)Lcom/pixelmonmod/api/registry/RegistryValue;"),index=0)
    private String backport$ballName(String input){return EditorNames.ball(input);}
}
