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
    public static boolean masterBall(ItemStack stack){
        if(!valid(stack))return false;
        var id=stack.func_77973_b().getRegistryName();
        return new ResourceLocation("pixelmon","master_ball").equals(id)||
            (new ResourceLocation("pixelmon","poke_ball").equals(id)&&stack.func_77942_o()&&"master_ball".equals(stack.func_77978_p().func_74779_i("PokeBallID")));
    }
    /** Independent one-in-100 gate, unaffected by the size of the ordinary pool. */
    public static ItemStack tier3Drop(Random random){
        boolean master=random.nextInt(100)==0;
        var choices=new ArrayList<ItemStack>();
        var pool=com.pixelmonmod.pixelmon.entities.npcs.registry.DropItemRegistry.tier3;
        if(pool!=null)for(var stack:pool)if(tierEligible(stack)&&masterBall(stack)==master)choices.add(stack);
        if(!choices.isEmpty())return choices.get(random.nextInt(choices.size())).func_77946_l();
        if(master)return com.pixelmonmod.pixelmon.items.PokeBallItem.of(com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry.getPokeBall("master_ball").getValueUnsafe(),1);
        return fallback();
    }
    public static boolean applies(PokeChestTileEntity chest){return chest!=null && chest.getChestType()==EnumPokeChestType.MASTERBALL && chest.getOwner()==null && !chest.isCustomDrop();}
    public static boolean valid(ItemStack item){return item!=null && !item.func_190926_b() && item.func_190916_E()>0 && item.func_77973_b()!=Items.field_190931_a;}
    public static boolean tierEligible(ItemStack item){
        if(!valid(item))return false;
        ResourceLocation id=item.func_77973_b().getRegistryName();
        return id==null||!id.func_110624_b().equals(Content.NS)||!Set.of("metal_alloy","pink_nectar","red_nectar","purple_nectar","yellow_nectar","teal_mask","wellspring_mask","hearthflame_mask","cornerstone_mask","syrupy_apple","gimmighoul_coin").contains(id.func_110623_a());
    }
    public static void cleanTier3(){
        var pool=com.pixelmonmod.pixelmon.entities.npcs.registry.DropItemRegistry.tier3;
        if(pool==null)return;pool.removeIf(item->!tierEligible(item));if(pool.isEmpty())pool.add(fallback());
    }
    private static ItemStack fallback(){
        Item candy=ForgeRegistries.ITEMS.getValue(new ResourceLocation("pixelmon","rare_candy"));
        return new ItemStack(candy==null || candy==Items.field_190931_a?Items.field_151045_i:candy);
    }
    public static ItemStack[] drops(PokeLootEvent.GetDrops event){
        ItemStack[] original=event.getDrops();
        List<ItemStack> valid=new ArrayList<>();if(original!=null)for(ItemStack item:original)if(valid(item))valid.add(item);
        if(valid.isEmpty()&&applies(event.chest))valid.add(fallback());return valid.toArray(new ItemStack[0]);
    }
    public static ItemStack drop(PokeLootEvent.Drop event){ItemStack item=event.getDrop();return valid(item)?item:applies(event.chest)?fallback():ItemStack.field_190927_a;}
}
