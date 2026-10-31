package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.yesNo.YesNoReplaceMove;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Native arguments are old move, new move; use one unambiguous local key. */
@Mixin(value=YesNoReplaceMove.class,remap=false)
public abstract class MoveReplacementTextMixin {
 @Redirect(method="drawConfirmText",at=@At(value="INVOKE",target="Lnet/minecraft/client/resources/I18n;func_135052_a(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"))
 private String backport$replaceWords(String key,Object[] args){return I18n.func_135052_a("gui.yesno.replace".equals(key)?"pixelmon.backport.move.replace_confirm":key,args);}
}
