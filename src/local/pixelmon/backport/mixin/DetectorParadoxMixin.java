package local.pixelmon.backport.mixin;
import java.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Pseudo @Mixin(targets="pixelmondetector.entities.EntityFinder",remap=false)
public abstract class DetectorParadoxMixin {
    @Redirect(method="addEntityList",at=@At(value="INVOKE",target="Ljava/util/Set;contains(Ljava/lang/Object;)Z"))
    private static boolean backport$newParadox(Set<?> original,Object value){return original.contains(value) || value instanceof String && Arrays.asList("gougingfire","ragingbolt","ironboulder","ironcrown").contains(((String)value).toLowerCase(Locale.ROOT).replace(" ",""));}
}
