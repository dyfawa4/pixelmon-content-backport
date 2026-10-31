package local.pixelmon.backport;

import java.util.List;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.events.DropEvent;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.item.*;
import net.minecraft.world.World;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.util.text.*;
import net.minecraftforge.eventbus.api.*;

public final class GlowScaleDust extends Item {
    public GlowScaleDust(){super(new Item.Properties().func_200916_a(Backport.CREATIVE_TAB));}
    @Override public boolean func_77636_d(ItemStack stack){return true;}
    @Override public void func_77624_a(ItemStack stack,World world,List<ITextComponent> lines,ITooltipFlag flag){
        lines.add(new TranslationTextComponent("pixelmon.backport.glow_scale_dust.description").func_240699_a_(TextFormatting.GRAY));
    }
    public static boolean qualifies(Pokemon pokemon){
        return pokemon!=null&&!pokemon.isEgg()&&!pokemon.hasNoDrops()&&pokemon.isShiny();
    }
    private static boolean dust(ItemStack stack){return stack!=null&&!stack.func_190926_b()&&stack.func_77973_b() instanceof GlowScaleDust;}
    public static final class Events {
        @SubscribeEvent(priority=EventPriority.LOWEST) public void drops(DropEvent event){
            if(event.isCanceled()||event.player==null||!(event.entity instanceof PixelmonEntity))return;
            PixelmonEntity entity=(PixelmonEntity)event.entity;
            if(entity.field_70170_p.field_72995_K||entity.hasOwner()||entity.getTrainer()!=null||entity.isBossPokemon()||!qualifies(entity.getPokemon()))return;
            for(var drop:event.getDrops())if(dust(drop.item)||drop.drop!=null
                &&dust(((local.pixelmon.backport.mixin.RewardDropAccess)(Object)drop.drop).backport$item()))return;
            event.addDrop(new ItemStack(Backport.item("glow_scale_dust")));
        }
    }
}
