package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.entities.npcs.NPCChatting;
import local.pixelmon.backport.NpcLocalization;
import java.util.ArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=NPCChatting.class,remap=false)
public abstract class NpcDialogueLocaleMixin {
    @Inject(method="getName",at=@At("RETURN"),cancellable=true)
    private void backport$name(String locale,CallbackInfoReturnable<String> cir){cir.setReturnValue(NpcLocalization.translate(cir.getReturnValue(),locale));}
    @Inject(method="getChat",at=@At("RETURN"),cancellable=true)
    private void backport$oldPages(String locale,CallbackInfoReturnable<ArrayList<String>> cir){
        if(!"zh_cn".equalsIgnoreCase(locale) || cir.getReturnValue()==null)return;
        ArrayList<String> pages=new ArrayList<>();for(String page:cir.getReturnValue())pages.add(NpcLocalization.translate(page,locale));cir.setReturnValue(pages);
    }
}
