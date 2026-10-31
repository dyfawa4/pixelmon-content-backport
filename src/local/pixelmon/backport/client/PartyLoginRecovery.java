package local.pixelmon.backport.client;
import local.pixelmon.backport.PartyLoginSync;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Bounded recovery after login, only while the entire client party is empty. */
@Mod.EventBusSubscriber(modid="pixelmon_backport",value=Dist.CLIENT)
public final class PartyLoginRecovery {
 private static boolean active;private static int age;
 @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggedInEvent e){active=true;age=0;}
 @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggedOutEvent e){active=false;age=0;}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
  if(!active||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.func_71410_x();if(mc.field_71439_g==null||mc.field_71441_e==null||mc.func_147114_u()==null)return;
  if(++age>240){active=false;return;}if(age!=40&&age!=120&&age!=240)return;
  var party=com.pixelmonmod.pixelmon.client.storage.ClientStorageManager.party;
  if(party!=null)for(var pokemon:party.getAll())if(pokemon!=null)return;
  if(PartyLoginSync.CHANNEL.isRemotePresent(mc.func_147114_u().func_147298_b()))PartyLoginSync.CHANNEL.sendToServer(new PartyLoginSync.Request());
 }
}
