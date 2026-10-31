package qa;
import java.nio.file.*;
import java.util.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraft.client.Minecraft;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import local.pixelmon.backport.PartyLoginSync;
@Mod("backport_party110_probe") @Mod.EventBusSubscriber(modid="backport_party110_probe")
public class Party110Probe {
 static boolean opened,hidden,ready,reset,done;static int ticks,clientTicks,afterReset;static volatile String expected="";static String before;static volatile boolean receivedRequest;
 static String ids(com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage party){var out=new ArrayList<String>();for(int i=0;i<6;i++){var p=party.get(i);out.add(p==null?"empty":p.getUUID()+":"+p.getSpecies().getDex()+":"+p.getHealth());}return out.toString();}
 static String state(com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage party){var out=new ArrayList<String>();for(int i=0;i<6;i++){var p=party.get(i);out.add(p==null?"empty":p.writeToNBT(new net.minecraft.nbt.CompoundNBT()).toString());}return out.toString();}
 static void fail(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("party110-fail.txt"),t.toString());}catch(Exception e){}done=true;}
 @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){if(done||e.phase!=TickEvent.Phase.END)return;var s=ServerLifecycleHooks.getCurrentServer();if(s==null||s.func_184103_al().func_181057_v().isEmpty())return;var p=s.func_184103_al().func_181057_v().get(0);try{
  if(++ticks==20){var party=StorageProxy.getParty(p);boolean seeded=Files.exists(Path.of("party110-seeded.txt"));
   if(!seeded){for(int i=0;i<6;i++)party.set(i,null);for(int i:new int[]{0,2,5}){var mon=PokemonFactory.create(PixelmonSpecies.fromNationalDex(i==0?1:i==2?25:801));mon.setLevelNum(50);mon.initialize();mon.setHealth(7+i);party.set(i,mon);}Files.writeString(Path.of("party110-seeded.txt"),ids(party));}
   else if(!Files.readString(Path.of("party110-seeded.txt")).equals(ids(party)))throw new AssertionError("Saved party identity/HP changed across real restart");
   expected=ids(party);before=state(party);PartyLoginSync.refresh(p);if(!before.equals(state(party)))throw new AssertionError("Snapshot mutated server Pokemon");ready=true;
  }
  if(ready){var f=PartyLoginSync.class.getDeclaredField("LAST");f.setAccessible(true);receivedRequest=((Map<?,?>)f.get(null)).containsKey(p);}
  if(ready&&!before.equals(state(StorageProxy.getParty(p))))throw new AssertionError("Recovery changed Pokemon NBT");
  if(ticks>600)throw new AssertionError("Client login recovery timed out");
 }catch(Throwable t){fail(t);}}
 @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}if(!opened&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_("backport-qa-single");}
  if(mc.field_71439_g!=null){com.pixelmonmod.pixelmon.client.ServerStorageDisplay.showStarterScreen=false;mc.func_147108_a(null);clientTicks++;}
  try{var party=com.pixelmonmod.pixelmon.client.storage.ClientStorageManager.party;if(ready&&party!=null&&!reset&&clientTicks>=80&&ids(party).equals(expected)){
    // Reproduce the same cache initialization called by ServerConfigListPacket, after first slot delivery.
    com.pixelmonmod.pixelmon.client.storage.ClientStorageManager.load();reset=true;afterReset=0;Files.writeString(Path.of("party110-reset.txt"),"Cleared client cache after authoritative slots arrived; clientTick="+clientTicks);
   }
   if(reset){afterReset++;var current=com.pixelmonmod.pixelmon.client.storage.ClientStorageManager.party;if(receivedRequest&&afterReset>5&&current!=null&&ids(current).equals(expected)){Files.writeString(Path.of("party110-pass.txt"),"PASS native login/cache reset/recovery; ticks="+afterReset+"; "+expected);done=true;}}
  }catch(Throwable t){fail(t);}
  if(done)mc.func_71400_g();
 }
}
