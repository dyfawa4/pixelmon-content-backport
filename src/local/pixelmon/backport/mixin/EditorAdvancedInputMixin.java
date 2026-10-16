package local.pixelmon.backport.mixin;

import java.util.Collection;
import local.pixelmon.backport.client.EditorNames;
import com.pixelmonmod.pixelmon.client.gui.pokemoneditor.PokemonEditorAdvancedScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=PokemonEditorAdvancedScreen.class,remap=false)
public abstract class EditorAdvancedInputMixin {
    @Shadow private TextFieldWidget heldText;
    @Shadow private ItemStack heldItem;
    @Inject(method="updateHeldItem",at=@At("HEAD"),cancellable=true)
    private void backport$unfinishedChineseItem(CallbackInfo ci){
        String input=heldText.func_146179_b();
        // IME/paste can produce incomplete names. Do not feed those to an ID
        // parser or silently save the previous held item after its exception.
        if(input.codePoints().anyMatch(c->c>127)&&EditorNames.item(input).equals(input)){
            heldItem=ItemStack.field_190927_a;ci.cancel();
        }
    }
    @ModifyArg(method="updateHeldItem",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/util/helpers/ResourceLocationHelper;of(Ljava/lang/String;)Lnet/minecraft/util/ResourceLocation;"),index=0)
    private String backport$heldItemName(String input){return EditorNames.item(input);}
    @ModifyArg(method="func_231160_c_",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/client/gui/widgets/TabCompleteTextField;setCompletions(Ljava/util/Collection;)Lcom/pixelmonmod/pixelmon/client/gui/widgets/TabCompleteTextField;",ordinal=0),index=0)
    private Collection<String> backport$heldCompletions(Collection<String> current){return EditorNames.items(current);}
    @ModifyArg(method="func_231160_c_",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/client/gui/widgets/TabCompleteTextField;setCompletions(Ljava/util/Collection;)Lcom/pixelmonmod/pixelmon/client/gui/widgets/TabCompleteTextField;",ordinal=1),index=0)
    private Collection<String> backport$natureCompletions(Collection<String> current){return EditorNames.natures(current);}
    @Inject(method="func_231160_c_",at=@At("RETURN"))
    private void backport$heldLabel(CallbackInfo ci){if(!heldItem.func_190926_b())heldText.func_146180_a(heldItem.func_200301_q().getString());}
}
