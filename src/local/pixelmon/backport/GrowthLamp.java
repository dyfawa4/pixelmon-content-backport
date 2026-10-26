package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.items.PixelmonItem;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.enums.EnumGrowth;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import net.minecraft.item.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.*;
import net.minecraft.util.*;
import net.minecraft.util.text.TranslationTextComponent;
/** A consumable step on the native growth ladder, independent of enum declaration order. */
public final class GrowthLamp extends PixelmonItem {
    public final int direction;
    public GrowthLamp(int direction){super(new Item.Properties().func_200916_a(Backport.CREATIVE_TAB));this.direction=direction<0?-1:1;}
    public static EnumGrowth next(EnumGrowth current,int direction){
        var ladder=new ArrayList<>(Arrays.asList(EnumGrowth.values()));ladder.sort(Comparator.comparingDouble(g->g.scaleValue));
        int index=ladder.indexOf(current)+(direction<0?-1:1);return index<0||index>=ladder.size()?null:ladder.get(index);
    }
    @Override public ActionResultType func_111207_a(ItemStack stack,PlayerEntity player,LivingEntity target,Hand hand){
        if(!(target instanceof PixelmonEntity))return ActionResultType.PASS;
        if(player.field_70170_p.field_72995_K)return ActionResultType.SUCCESS;
        var entity=(PixelmonEntity)target;var pokemon=entity.getPokemon();
        if(!(player instanceof ServerPlayerEntity)||pokemon==null||pokemon.isEgg()||!player.func_110124_au().equals(pokemon.getOwnerPlayerUUID()))return fail(player,"owned");
        if(BattleRegistry.getBattle(player)!=null||entity.battleController!=null&&!entity.battleController.battleEnded)return fail(player,"battle");
        var next=next(pokemon.getGrowth(),direction);if(next==null)return fail(player,direction<0?"smallest":"largest");
        pokemon.setGrowth(next);
        if(!player.field_71075_bZ.field_75098_d)stack.func_190918_g(1);
        player.func_146105_b(new TranslationTextComponent("pixelmon.backport.growth_lamp.changed",pokemon.getLocalizedName(),new TranslationTextComponent(next.getTranslationKey())),true);
        return ActionResultType.SUCCESS;
    }
    private static ActionResultType fail(PlayerEntity player,String reason){player.func_146105_b(new TranslationTextComponent("pixelmon.backport.growth_lamp."+reason),true);return ActionResultType.FAIL;}
}
