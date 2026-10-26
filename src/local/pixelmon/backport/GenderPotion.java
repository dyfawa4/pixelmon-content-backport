package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.items.PixelmonItem;
import net.minecraft.item.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.*;
import net.minecraft.util.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraft.client.util.ITooltipFlag;
public final class GenderPotion extends PixelmonItem {
    public GenderPotion(){super(new Item.Properties().func_200916_a(Backport.CREATIVE_TAB));}
    public static Gender opposite(Pokemon p){return p.getGender()==Gender.MALE?Gender.FEMALE:p.getGender()==Gender.FEMALE?Gender.MALE:Gender.NONE;}
    public static Stats target(Pokemon p){
        if(p==null||p.isEgg()||p.getSpecies().getDex()==CapSpecies.DEX||p.getGender()==Gender.NONE)return null;
        Gender next=opposite(p);if(AddedLegendGender.gender(p,next)!=next)return null;
        Stats form=p.getForm();if(form.isGenderless())return null;
        // possibleGenders also describes renderable gender properties; a 0/100
        // natural ratio still means a fixed-sex species even if both are listed.
        if(form.getMalePercentage()>0&&form.getMalePercentage()<100&&form.isPossibleGender(Gender.MALE)&&form.isPossibleGender(Gender.FEMALE))return form;
        // Native Indeedee/Basculegion/Oinkologne represent their two sexes as forms.
        if(form.getName().equals("male")||form.getName().equals("female"))for(Stats f:p.getSpecies().getForms())if(f.getName().equals(next==Gender.MALE?"male":"female")&&f.isPossibleGender(next))return f;
        return null;
    }
    @Override public ActionResultType func_111207_a(ItemStack stack,PlayerEntity player,LivingEntity target,Hand hand){
        if(!(target instanceof PixelmonEntity)||stack.func_190926_b())return ActionResultType.PASS;if(player.field_70170_p.field_72995_K)return ActionResultType.SUCCESS;
        PixelmonEntity entity=(PixelmonEntity)target;Pokemon pokemon=entity.getPokemon();
        if(!(player instanceof ServerPlayerEntity)||pokemon==null||!player.func_110124_au().equals(pokemon.getOwnerPlayerUUID()))return fail(player,"owned");
        if(BattleRegistry.getBattle(player)!=null||FieldMega.busy(entity))return fail(player,"battle");
        Stats form=target(pokemon);if(form==null)return fail(player,"fixed");Stats original=pokemon.getForm();Gender before=pokemon.getGender(),next=opposite(pokemon);
        if(form!=pokemon.getForm())pokemon.setForm(form);pokemon.setGender(next);
        if(pokemon.getGender()!=next){pokemon.setForm(original);pokemon.setGender(before);return fail(player,"fixed");}
        pokemon.getStats().recalculateStats();if(pokemon.getHealth()>pokemon.getMaxHealth())pokemon.setHealth(pokemon.getMaxHealth());
        stack.func_190918_g(1);pokemon.markDirty(Pokemon.FORM);player.field_71071_by.func_70296_d();
        com.pixelmonmod.pixelmon.api.storage.StorageProxy.getParty((ServerPlayerEntity)player).notifyListenerAll((ServerPlayerEntity)player,com.pixelmonmod.pixelmon.comm.EnumUpdateType.CLIENT);
        player.func_146105_b(new TranslationTextComponent("pixelmon.backport.gender.changed",pokemon.getFormattedDisplayName(),new TranslationTextComponent(next==Gender.MALE?"pixelmon.backport.gender.male":"pixelmon.backport.gender.female")),true);return ActionResultType.SUCCESS;
    }
    private static ActionResultType fail(PlayerEntity p,String key){p.func_146105_b(new TranslationTextComponent("pixelmon.backport.gender."+key),true);return ActionResultType.FAIL;}
    @Override public void func_77624_a(ItemStack stack,World world,List<ITextComponent> lines,ITooltipFlag flag){lines.add(new TranslationTextComponent("pixelmon.backport.gender.description").func_240699_a_(TextFormatting.GRAY));lines.add(new TranslationTextComponent("pixelmon.backport.gender.limit").func_240699_a_(TextFormatting.GRAY));}
}
