package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.items.IMedicine;
import com.pixelmonmod.pixelmon.items.medicine.MedicineItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=MedicineItem.class,remap=false)
public interface MedicineMethodsAccess {
    @Accessor("healMethods") IMedicine[] backport$methods();
}
