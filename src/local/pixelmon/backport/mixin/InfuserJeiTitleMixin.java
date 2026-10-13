package local.pixelmon.backport.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets="com.strangeone101.pixeltweaks.integration.jei.category.InfuserRecipeCategory",remap=false)
public abstract class InfuserJeiTitleMixin {
    @Redirect(method="getTitle",at=@At(value="INVOKE",target="Lnet/minecraft/client/resources/I18n;func_135052_a(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;"))
    private String backport$name(String key,Object[] args){return local.pixelmon.backport.InfuserNames.translate(key,args);}
}
