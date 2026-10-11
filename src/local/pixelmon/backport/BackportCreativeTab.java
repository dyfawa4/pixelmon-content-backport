package local.pixelmon.backport;

import net.minecraft.item.*;
import net.minecraft.util.NonNullList;
import com.pixelmonmod.pixelmon.items.TechnicalMoveItem;
import com.pixelmonmod.pixelmon.enums.TMType;

/** One discoverable native creative tab; preserves all existing Pixelmon tabs. */
public final class BackportCreativeTab extends ItemGroup {
    public BackportCreativeTab(){super(Content.NS);}
    @Override public ItemStack func_78016_d(){return new ItemStack(Backport.REGISTERED.get("tera_orb").get());}
    @Override public void func_78018_a(NonNullList<ItemStack> items){
        java.util.Set<Item> seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        Backport.REGISTERED.forEach((name,item)->{Item canonical=Backport.item(name);if(seen.add(canonical))items.add(new ItemStack(canonical));});
        for(int id:new int[]{171,207,210,225,226,227,228,229})
            items.add(TechnicalMoveItem.of(TMType.TM9,BackportTechnicalMoves.resolve(TMType.TM9,id)));
    }
}
