package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import net.minecraft.item.Item;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.*;
/** Reuse the native held/thrown-item paths, while selecting from the independent Tera habitat pool. */
public final class TeraWishes {
 private TeraWishes(){}
 public static boolean item(Item item){return item==PixelmonItems.wishing_piece||item==PixelmonItems.waterdude_wishing_piece;}
 public static boolean attempt(TeraDenEntity den,Item item,PlayerEntity player,boolean consume){
  if(den.field_70170_p.field_72995_K||den.getData().isPresent()||!item(item)||den.isDecoration())return false;
  boolean legendary=item==PixelmonItems.waterdude_wishing_piece;
  int r=den.func_70681_au().nextInt(100),stars=legendary?6:r<20?1:r<40?2:r<60?3:r<80?4:r<95?5:6;
  if(!den.wish(stars,legendary)){if(player!=null)player.func_145747_a(new TranslationTextComponent("raid.interact.wishfailed"),Util.field_240973_b_);return false;}
  if(player!=null){if(consume&&!player.func_184812_l_())player.func_184614_ca().func_190918_g(1);player.func_145747_a(new TranslationTextComponent("raid.interact.wished"),Util.field_240973_b_);}
  den.field_70170_p.func_184133_a(null,den.func_233580_cy_(),SoundEvents.field_187616_bj,SoundCategory.BLOCKS,1,1);
  return true;
 }
}
