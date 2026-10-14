package local.pixelmon.backport.mixin;

import java.util.List;
import java.util.function.Predicate;
import net.minecraftforge.common.ForgeConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Give the detector's existing settings language keys without changing paths or defaults. */
@Pseudo @Mixin(targets="pixelmondetector.config.PDConfig$Common",remap=false)
public abstract class DetectorConfigTranslationMixin {
    @Redirect(method="<init>",at=@At(value="INVOKE",target="Lnet/minecraftforge/common/ForgeConfigSpec$Builder;define(Ljava/lang/String;Z)Lnet/minecraftforge/common/ForgeConfigSpec$BooleanValue;"))
    private ForgeConfigSpec.BooleanValue backport$boolean(ForgeConfigSpec.Builder builder,String name,boolean value){
        return builder.translation("pixelmondetector.config."+name).define(name,value);
    }
    @Redirect(method="<init>",at=@At(value="INVOKE",target="Lnet/minecraftforge/common/ForgeConfigSpec$Builder;defineList(Ljava/lang/String;Ljava/util/List;Ljava/util/function/Predicate;)Lnet/minecraftforge/common/ForgeConfigSpec$ConfigValue;"))
    private <T> ForgeConfigSpec.ConfigValue<List<? extends T>> backport$list(ForgeConfigSpec.Builder builder,String name,List<? extends T> value,Predicate<Object> validator){
        return builder.translation("pixelmondetector.config."+name).defineList(name,value,validator);
    }
    @Redirect(method="<init>",at=@At(value="INVOKE",target="Lnet/minecraftforge/common/ForgeConfigSpec$Builder;defineInRange(Ljava/lang/String;III)Lnet/minecraftforge/common/ForgeConfigSpec$IntValue;"))
    private ForgeConfigSpec.IntValue backport$integer(ForgeConfigSpec.Builder builder,String name,int value,int min,int max){
        return builder.translation("pixelmondetector.config."+name).defineInRange(name,value,min,max);
    }
    @Redirect(method="<init>",at=@At(value="INVOKE",target="Lnet/minecraftforge/common/ForgeConfigSpec$Builder;defineInRange(Ljava/lang/String;DDD)Lnet/minecraftforge/common/ForgeConfigSpec$DoubleValue;"))
    private ForgeConfigSpec.DoubleValue backport$decimal(ForgeConfigSpec.Builder builder,String name,double value,double min,double max){
        return builder.translation("pixelmondetector.config."+name).defineInRange(name,value,min,max);
    }
}
