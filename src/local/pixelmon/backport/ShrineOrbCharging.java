package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.events.PixelmonKnockoutEvent;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.items.ShrineOrbItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.eventbus.api.*;
/** One actual knockout, every eligible orb. No battle-victory or capture fallback. */
public final class ShrineOrbCharging {
 public static void charge(ServerPlayerEntity player){
  if(player==null||player.field_70170_p.field_72995_K)return;
  try(var inventory=new PokeInventory(player)){for(var stack:inventory.stacks)if(!stack.func_190926_b()&&stack.func_77973_b() instanceof ShrineOrbItem&&stack.func_77952_i()<ShrineOrbItem.full)stack.func_196085_b(Math.min(ShrineOrbItem.full,stack.func_77952_i()+1));}
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public void knockout(PixelmonKnockoutEvent e){
  var defeated=e.pokemon;if(defeated==null||defeated.bc==null||defeated.bc.simulateMode||!defeated.isFainted())return;
  for(var side:defeated.bc.participants)if(side instanceof PlayerParticipant&&side.team!=defeated.getParticipant().team)charge(((PlayerParticipant)side).player);
 }
}
