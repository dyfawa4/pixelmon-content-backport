package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.events.PokeLootEvent;
import com.pixelmonmod.pixelmon.blocks.enums.EnumPokeChestType;
import com.pixelmonmod.pixelmon.blocks.tileentity.PokeChestTileEntity;
import net.minecraft.item.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
public final class PokeChestLoot {
    private PokeChestLoot(){}
    public static boolean applies(PokeChestTileEntity chest){return chest!=null && chest.getChestType()==EnumPokeChestType.MASTERBALL && chest.getOwner()==null && !chest.isCustomDrop();}
    public static boolean valid(ItemStack item){return item!=null && !item.func_190926_b() && item.func_190916_E()>0 && item.func_77973_b()!=Items.field_190931_a;}
    public static void cleanTier3(){
        var pool=com.pixelmonmod.pixelmon.entities.npcs.registry.DropItemRegistry.tier3;
        if(pool==null)return;pool.removeIf(item->!valid(item));if(pool.isEmpty())pool.add(fallback());
    }
    private static ItemStack fallback(){
        Item candy=ForgeRegistries.ITEMS.getValue(new ResourceLocation("pixelmon","rare_candy"));
        return new ItemStack(candy==null || candy==Items.field_190931_a?Items.field_151045_i:candy);
    }
    public static ItemStack[] drops(PokeLootEvent.GetDrops event){
        ItemStack[] original=event.getDrops();if(!applies(event.chest))return original;
        List<ItemStack> valid=new ArrayList<>();if(original!=null)for(ItemStack item:original)if(valid(item))valid.add(item);
        if(valid.isEmpty())valid.add(fallback());return valid.toArray(new ItemStack[0]);
    }
    public static ItemStack drop(PokeLootEvent.Drop event){ItemStack item=event.getDrop();return applies(event.chest) && !valid(item)?fallback():item;}
}
