package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.species.palette.locator.ModelLocator;
import com.pixelmonmod.pixelmon.api.util.ResourceWithFallback;
import com.pixelmonmod.pixelmon.client.models.*;
import local.pixelmon.backport.client.StatefulFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=ModelLocator.class,remap=false)
public abstract class ModelFactoryMixin {
    @Redirect(method="getFactory",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/client/models/ModelFactoryFactory;get([Lcom/pixelmonmod/pixelmon/api/util/ResourceWithFallback;)Lcom/pixelmonmod/pixelmon/client/models/PixelmonSmdFactory;"))
    private PixelmonSmdFactory backport$states(ModelFactoryFactory factory,ResourceWithFallback[] resources){return StatefulFactory.wrap(factory.get(resources),resources);}
}
