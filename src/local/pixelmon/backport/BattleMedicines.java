package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.items.*;
import com.pixelmonmod.pixelmon.items.medicine.MedicineItem;
import com.pixelmonmod.pixelmon.items.heldItems.*;
import net.minecraft.item.ItemStack;

/** Bag-use classification; passive held-item effects never enter this path. */
public final class BattleMedicines {
    private BattleMedicines(){}
    public static boolean medicine(ItemStack stack){
        if(stack==null)return false;
        var item=stack.func_77973_b();
        return item instanceof MedicineItem || item instanceof PPRestoreItem
            || item instanceof BattleItem || item instanceof BerryItem || item instanceof BerryJuiceItem
            || item instanceof WhiteHerbItem || item instanceof MentalHerbItem || item instanceof SacredAshItem;
    }
}
