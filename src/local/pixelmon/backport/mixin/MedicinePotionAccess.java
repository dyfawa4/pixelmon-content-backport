package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.items.IHealHP;
import com.pixelmonmod.pixelmon.items.medicine.MedicinePotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=MedicinePotion.class,remap=false)
public interface MedicinePotionAccess {
    @Accessor("healHP") IHealHP backport$healing();
}
