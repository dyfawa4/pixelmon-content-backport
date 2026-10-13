package local.pixelmon.backport.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets="com.pixelmonmod.pixelmon.client.gui.machines.infuser.InfuserScreen",remap=false)
public abstract class InfuserTitleMixin {
    @Redirect(method="func_230451_b_",at=@At(value="INVOKE",target="Lnet/minecraft/client/resources/I18n;func_135052_a(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"))
    private String backport$name(String key,Object[] args){return local.pixelmon.backport.InfuserNames.translate(key,args);}
}
