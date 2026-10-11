package local.pixelmon.backport.mixin;
import java.util.ArrayList;
import com.pixelmonmod.pixelmon.api.moveskills.MoveSkill;
import com.pixelmonmod.pixelmon.client.gui.moveskills.MoveSkillSelectScreen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=MoveSkillSelectScreen.class,remap=false)
public abstract class FieldSkillScreenMixin {
    @Shadow ArrayList<MoveSkill> moveSkills;
    @Shadow int currentlySelectedIndex;
    @Inject(method="func_231160_c_",at=@At("RETURN"))
    private void backport$nativeSkills(CallbackInfo ci){
        if((Object)this instanceof local.pixelmon.backport.FieldMegaScreen.Skills){
            moveSkills.removeIf(s->s.id.equals("change_form"));currentlySelectedIndex=-1;
        }
    }
}
