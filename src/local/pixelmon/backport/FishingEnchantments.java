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
/** Extend the original fishing pipeline without changing encounter eligibility or the displayed rarity definition. */
public final class FishingEnchantments {
    public static ItemStack rod(ServerPlayerEntity player,HookEntity hook){
        for(var stack:new ItemStack[]{player.func_184614_ca(),player.func_184592_cb()})if(stack.func_77973_b() instanceof FishingRodItem&&((FishingRodItem)stack.func_77973_b()).getRodType()==hook.getRodType())return stack;
        return ItemStack.field_190927_a;
    }
    public static int level(String id,ItemStack stack){var enchantment=ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation("minecraft",id));return enchantment==null?0:Math.max(0,Math.min(3,EnchantmentHelper.func_77506_a(enchantment,stack)));}
    public static int waitTicks(int original,int level){return level<=0?original:Math.max(20,(int)Math.ceil(original/(1.0+Math.min(3,level))));}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void cast(FishingEvent.Cast event){event.setTicksUntilCatch(waitTicks(event.getTicksUntilCatch(),level("lure",rod(event.player,event.fishHook))));}
    public static float rarity(SpawnInfo info,AbstractSpawner spawner,SpawnLocation location,float original){
        if(spawner!=PixelmonSpawning.fishingSpawner||location==null||!(location.cause instanceof ServerPlayerEntity)||!(original>0)||!Float.isFinite(original)||info.calculateNominalRarity()>=5)return original;
        var player=(ServerPlayerEntity)location.cause;if(!(player.field_71104_cf instanceof HookEntity))return original;
        var hook=(HookEntity)player.field_71104_cf;if(hook.func_234616_v_()!=player||hook.field_70128_L)return original;
        int luck=level("luck_of_the_sea",rod(player,hook));return original*(1+.5f*luck);
    }
}
