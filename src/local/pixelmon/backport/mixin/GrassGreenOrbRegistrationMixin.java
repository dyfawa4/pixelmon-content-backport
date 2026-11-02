package local.pixelmon.backport.mixin;
import org.spongepowered.asm.mixin.Mixin;import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets="com.pixelmonmod.pixelmon.init.registry.ItemRegistration",remap=false)
public abstract class GrassGreenOrbRegistrationMixin {
 @Redirect(method="<clinit>",at=@At(value="INVOKE",target="Lnet/minecraftforge/registries/DeferredRegister;register(Ljava/lang/String;Ljava/util/function/Supplier;)Lnet/minecraftforge/fml/RegistryObject;"))
 private static net.minecraftforge.fml.RegistryObject<?> backport$orb(net.minecraftforge.registries.DeferredRegister<net.minecraft.item.Item> register,String name,java.util.function.Supplier<? extends net.minecraft.item.Item> factory){return register.register(name,name.equals("jade_orb")?local.pixelmon.backport.GrassGreenOrb::new:factory);}
}
