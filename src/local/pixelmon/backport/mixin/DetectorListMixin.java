package local.pixelmon.backport.mixin;
import local.pixelmon.backport.client.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="com.mrcrayfish.configured.client.screen.EditListScreen",remap=false)
public abstract class DetectorListMixin implements DetectorListContext {
    @Unique private Object backport$holder;
    @Inject(method="<init>",at=@At("RETURN"))
    private void backport$context(Screen parent,ITextComponent title,@Coerce Object holder,ResourceLocation background,CallbackInfo ci){backport$holder=holder;}
    @Override public String backport$listText(String raw){return DetectorConfigText.listValue(backport$holder,raw);}
}
