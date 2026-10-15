package local.pixelmon.backport.mixin;
import local.pixelmon.backport.*;
import net.minecraft.entity.Entity;
import net.minecraft.client.resources.I18n;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo @Mixin(targets="pixelmondetector.marker.PKMarker",remap=false)
public abstract class DetectorDenMixin {
    @Shadow public String name;
    @Shadow public String desc;
    @Inject(method="<init>(Lpixelmondetector/entities/EntityType;Lnet/minecraft/entity/Entity;Ljava/lang/String;Ljava/lang/String;)V",at=@At("RETURN"))
    private void backport$teraLabel(@Coerce Object category,Entity entity,String label,String details,CallbackInfo ci){if(entity instanceof TeraDenEntity){TeraDenEntity den=(TeraDenEntity)entity;name=I18n.func_135052_a("entity.pixelmon_backport.tera_den");desc=den.getData().map(r->r.getSpecies().getLocalizedName()+" R"+r.getStars()+" · "+I18n.func_135052_a("pixelmon.backport.tera.type."+TeraItems.NAMES[den.teraType()])).orElse("");}}
}
