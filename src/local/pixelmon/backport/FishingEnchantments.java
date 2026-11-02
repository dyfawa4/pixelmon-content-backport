package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.events.FishingEvent;
import com.pixelmonmod.pixelmon.api.spawning.*;
import com.pixelmonmod.pixelmon.entities.projectiles.HookEntity;
import com.pixelmonmod.pixelmon.items.FishingRodItem;
import com.pixelmonmod.pixelmon.spawning.PixelmonSpawning;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.registries.ForgeRegistries;
import com.pixelmonmod.pixelmon.enums.items.EnumRodType;
import com.pixelmonmod.pixelmon.api.spawning.calculators.SelectionAlgorithm;
import net.minecraft.enchantment.Enchantment;
import java.util.*;
/** Fishing bonuses operate on eligible encounters, never by relabeling ordinary catches. */
public final class FishingEnchantments {
    private static final Map<HookEntity,ItemStack> CAST_RODS=new WeakHashMap<>();
    public static void bind(HookEntity hook,ItemStack stack){CAST_RODS.putIfAbsent(hook,stack);}
    public static boolean supported(ItemStack stack){
        if(!(stack.func_77973_b() instanceof FishingRodItem))return false;
        var type=((FishingRodItem)stack.func_77973_b()).getRodType();
        return type==EnumRodType.OldRod||type==EnumRodType.GoodRod||type==EnumRodType.SuperRod;
    }
    public static boolean allowed(Enchantment enchantment){
        var id=enchantment.getRegistryName();
        return id!=null&&id.func_110624_b().equals("minecraft")&&Set.of("luck_of_the_sea","lure","unbreaking","mending","vanishing_curse","fortune","looting").contains(id.func_110623_a());
    }
    public static boolean rejectsBook(ItemStack rod,ItemStack book){
        return supported(rod)&&book.func_77973_b()==net.minecraft.item.Items.field_151134_bR
            &&EnchantmentHelper.func_82781_a(book).keySet().stream().anyMatch(e->!allowed(e));
    }
    public static boolean bookOnly(Enchantment enchantment){
        var id=enchantment.getRegistryName();return new ResourceLocation("minecraft:fortune").equals(id)||new ResourceLocation("minecraft:looting").equals(id);
    }
    public static int total(ItemStack stack){return supported(stack)?level("luck_of_the_sea",stack)+level("fortune",stack)+level("looting",stack):0;}
    public static int catchDamage(ItemStack stack){int total=total(stack);return total==0?0:total+1;}
    public static ItemStack rod(ServerPlayerEntity player,HookEntity hook){
        var casting=CAST_RODS.get(hook);
        if(casting!=null)return player.func_184614_ca()==casting||player.func_184592_cb()==casting?casting:ItemStack.field_190927_a;
        for(var stack:new ItemStack[]{player.func_184614_ca(),player.func_184592_cb()})if(stack.func_77973_b() instanceof FishingRodItem&&((FishingRodItem)stack.func_77973_b()).getRodType()==hook.getRodType())return stack;
        return ItemStack.field_190927_a;
    }
    public static int level(String id,ItemStack stack){var enchantment=ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation("minecraft",id));return enchantment==null?0:Math.max(0,Math.min(3,EnchantmentHelper.func_77506_a(enchantment,stack)));}
    public static int waitTicks(int original,int level){return level<=0?original:Math.max(20,(int)Math.ceil(original/(1.0+Math.min(3,level))));}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void cast(FishingEvent.Cast event){event.setTicksUntilCatch(waitTicks(event.getTicksUntilCatch(),level("lure",rod(event.player,event.fishHook))));}
    public static SpawnInfo choose(SelectionAlgorithm algorithm,AbstractSpawner spawner,SpawnLocation location,List<SpawnInfo> candidates){
        if(spawner!=PixelmonSpawning.fishingSpawner||location==null||!(location.cause instanceof ServerPlayerEntity))return algorithm.choose(spawner,location,candidates);
        var player=(ServerPlayerEntity)location.cause;
        if(!(player.field_71104_cf instanceof HookEntity))return algorithm.choose(spawner,location,candidates);
        var hook=(HookEntity)player.field_71104_cf;
        if(hook.func_234616_v_()!=player||hook.field_70128_L)return algorithm.choose(spawner,location,candidates);
        int total=total(rod(player,hook));
        if(total==0)return algorithm.choose(spawner,location,candidates);
        var rare=new ArrayList<SpawnInfo>();var ordinary=new ArrayList<SpawnInfo>();
        for(var info:candidates){
            float weight=info.getAdjustedRarity(spawner,location);
            if(!(weight>0)||!Float.isFinite(weight))continue;
            (info.calculateNominalRarity()<5?rare:ordinary).add(info);
        }
        // Only suitable, positive-weight entries can be selected. An absent rare pool adds no encounter.
        if(rare.isEmpty())return algorithm.choose(spawner,location,candidates);
        var pool=player.func_70681_au().nextInt(10)<total?rare:ordinary;
        return pool.isEmpty()?null:algorithm.choose(spawner,location,pool);
    }
    public static float rarity(SpawnInfo info,AbstractSpawner spawner,SpawnLocation location,float original){
        if(spawner!=PixelmonSpawning.fishingSpawner||location==null||!(location.cause instanceof ServerPlayerEntity)||!(original>0)||!Float.isFinite(original)||info.calculateNominalRarity()>=5)return original;
        var player=(ServerPlayerEntity)location.cause;if(!(player.field_71104_cf instanceof HookEntity))return original;
        var hook=(HookEntity)player.field_71104_cf;if(hook.func_234616_v_()!=player||hook.field_70128_L)return original;
        var stack=rod(player,hook);
        if(supported(stack))return original; // Replaced by the explicit !!! pool probability; do not stack old weights.
        int luck=level("luck_of_the_sea",stack);return original*(1+.5f*luck);
    }
}
