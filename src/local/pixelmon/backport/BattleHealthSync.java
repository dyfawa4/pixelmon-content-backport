package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
/** Read-only authoritative health snapshots after native faint/switch bookkeeping. */
public final class BattleHealthSync {
 private static final Map<PixelmonWrapper,String> SENT=new WeakHashMap<>();
 public static void publish(BattleController b){if(b.simulateMode||b.battleEnded)return;for(var part:b.participants)for(var p:part.allPokemon)if(!Erosion.isCopy(p)&&(Erosion.active(p)||Calamity.active(p.pokemon))){String value=p.getHealth()+"/"+p.getMaxHealth();if(!value.equals(SENT.put(p,value))){p.update(com.pixelmonmod.pixelmon.comm.EnumUpdateType.HP);ErosionNetwork.sync(b,p);CalamityNetwork.sync(b,p);}}}
}
