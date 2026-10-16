package local.pixelmon.backport.mixin;

import java.util.*;
import local.pixelmon.backport.EditorNameAliases;
import local.pixelmon.backport.client.EditorNames;
import com.pixelmonmod.pixelmon.api.util.ITranslatable;
import com.pixelmonmod.pixelmon.client.gui.widgets.TabCompleteTranslateableTextField;
import com.pixelmonmod.pixelmon.client.gui.pokemoneditor.IndividualEditorBaseScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only editor species/ball fields; unrelated command/search widgets stay native. */
@Mixin(value=TabCompleteTranslateableTextField.class,remap=false)
public abstract class EditorCompletionMixin {
    @Shadow private Collection<ITranslatable> completions;
    @Shadow private List<ITranslatable> cache;
    @Inject(method="createCompletionsFor",at=@At("HEAD"),cancellable=true)
    private void backport$bilingualPrefix(String input,CallbackInfo ci){
        if(!(Minecraft.func_71410_x().field_71462_r instanceof IndividualEditorBaseScreen))return;
        cache.clear();for(var option:completions)if(EditorNameAliases.prefix(input,EditorNames.id(option),option.getLocalizedName()))cache.add(option);
        ci.cancel();
    }
}
