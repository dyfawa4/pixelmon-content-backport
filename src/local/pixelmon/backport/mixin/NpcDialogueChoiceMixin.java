package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.dialogue.Choice;
import local.pixelmon.backport.client.NpcDialogueDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Display/measure the translated label; choice IDs and handlers remain native. */
@Mixin(targets="com.pixelmonmod.pixelmon.client.gui.custom.dialogue.DialogueScreen",remap=false)
public abstract class NpcDialogueChoiceMixin {
    @Redirect(method={"loadButtons","getLargestWidth"},at=@At(value="FIELD",target="Lcom/pixelmonmod/pixelmon/api/dialogue/Choice;text:Ljava/lang/String;",opcode=org.objectweb.asm.Opcodes.GETFIELD))
    private String backport$choiceLabel(Choice choice){return NpcDialogueDisplay.text(choice.text);}
}
