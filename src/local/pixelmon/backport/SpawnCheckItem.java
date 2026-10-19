package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.items.PixelmonItem;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

public final class SpawnCheckItem extends PixelmonItem {
    private final boolean legendary;
    public SpawnCheckItem(boolean legendary){super(new Item.Properties().func_200916_a(ItemGroup.field_78040_i).func_200917_a(1));this.legendary=legendary;}
    @Override public ActionResult<ItemStack> func_77659_a(World world,PlayerEntity player,Hand hand){
        ItemStack stack=player.func_184586_b(hand);
        if(world.field_72995_K)return ActionResult.func_226250_c_(stack);
        if(!(player instanceof ServerPlayerEntity)||player.func_184811_cZ().func_185141_a(this))return ActionResult.func_226250_c_(stack);
        player.func_184811_cZ().func_185145_a(this,60);
        boolean ready;
        try{ready=SpawnChecks.run((ServerPlayerEntity)player,legendary);}
        catch(RuntimeException error){com.pixelmonmod.pixelmon.Pixelmon.LOGGER.warn("Spawn checker could not calculate the current area",error);ready=false;}
        if(!ready)player.func_146105_b(new TranslationTextComponent("pixelmon.backport.spawn_check.unavailable"),true);
        return ActionResult.func_226248_a_(stack);
    }
}
