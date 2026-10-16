package local.pixelmon.backport.mixin;

import java.util.Collection;
import local.pixelmon.backport.client.EditorNames;
import com.pixelmonmod.pixelmon.client.gui.pokemoneditor.IndividualEditorBaseScreen;
import com.pixelmonmod.pixelmon.client.gui.widgets.DropDown;
import com.pixelmonmod.pixelmon.client.gui.widgets.TabCompleteTextField;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.species.palette.PaletteProperties;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=IndividualEditorBaseScreen.class,remap=false)
public abstract class EditorBaseInputMixin {
    @Shadow private DropDown<PaletteProperties> paletteDropdown;
    @Shadow private TabCompleteTextField[] tbMoves;
    @Shadow protected Pokemon p;
    @ModifyArg(method="*",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/registries/PixelmonSpecies;get(Ljava/lang/String;)Ljava/util/Optional;"),index=0)
    private String backport$speciesName(String input){return EditorNames.species(input);}
    @Redirect(method="checkFields",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/widget/TextFieldWidget;func_146179_b()Ljava/lang/String;"))
    private String backport$moveName(TextFieldWidget field){
        for(int i=0;i<tbMoves.length;i++)if(field==tbMoves[i])return EditorNames.move(field.func_146179_b(),p.getMoveset().get(i));
        return field.func_146179_b();
    }
    @ModifyArg(method="func_231160_c_",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/client/gui/widgets/TabCompleteTextField;setCompletions(Ljava/util/Collection;)Lcom/pixelmonmod/pixelmon/client/gui/widgets/TabCompleteTextField;"),index=0)
    private Collection<String> backport$moveCompletions(Collection<String> current){return EditorNames.moves(current);}
    @Inject(method="func_231160_c_",at=@At("RETURN"))
    private void backport$paletteLabels(CallbackInfo ci){paletteDropdown.setGetOptionString(p->EditorNames.label(p,p.getName()));}
}
