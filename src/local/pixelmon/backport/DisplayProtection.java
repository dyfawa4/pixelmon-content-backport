package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.blocks.tileentity.PokeDisplayTileEntity;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TranslationTextComponent;
/** Validate the complete exchange before native code can clear either storage slot. */
public final class DisplayProtection {
 private DisplayProtection(){}
 public static boolean valid(Pokemon p){return p!=null&&p.getSpecies()!=null&&p.getSpecies().getDex()>0&&p.getForm()!=null;}
 public static boolean allow(ServerPlayerEntity player,BlockPos pos,int slot){
  if(player==null||pos==null||!player.field_70170_p.func_175667_e(pos)||player.func_70092_e(pos.func_177958_n()+.5,pos.func_177956_o()+.5,pos.func_177952_p()+.5)>64)return false;
  var raw=player.field_70170_p.func_175625_s(pos);if(!(raw instanceof PokeDisplayTileEntity))return false;
  var tile=(PokeDisplayTileEntity)raw;if(!tile.isLockedBy(player))return false;
  if(slot==-1)return true;
  var party=StorageProxy.getParty(player);var old=tile.getStoredPoke().orElse(null);
  boolean ok=party!=null&&slot>=0&&slot<6&&(old==null||valid(old));
  if(ok){var incoming=party.get(slot);ok=incoming==null?old!=null:valid(incoming);}
  if(ok&&old==null&&party.get(slot)!=null&&party.get(slot).getHealth()>0&&party.countAblePokemon()<2)ok=false;
  if(!ok){tile.setLocked(null);player.func_146105_b(new TranslationTextComponent("backport.display.rejected"),false);}
  return ok;
 }
}
