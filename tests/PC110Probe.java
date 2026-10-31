package qa;
import java.nio.file.*;import java.util.*;import java.net.*;
import net.minecraftforge.fml.common.Mod;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.event.TickEvent;import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraft.client.Minecraft;import net.minecraft.nbt.CompoundNBT;
import com.pixelmonmod.pixelmon.api.pokemon.*;import com.pixelmonmod.pixelmon.api.storage.*;import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.comm.EnumUpdateType;import com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper;
import com.pixelmonmod.pixelmon.comm.packetHandlers.clientStorage.newStorage.*;import com.pixelmonmod.pixelmon.comm.packetHandlers.clientStorage.newStorage.pc.ClientInitializePCPacket;
@Mod("backport_pc110_probe") @Mod.EventBusSubscriber(modid="backport_pc110_probe")
public class PC110Probe {
 static boolean opened,hidden,done,seeded,requested;static int ticks,phase,settled;static UUID charId,eeveeId;static Pokemon originalChar,originalEevee;static CompoundNBT charNBT,eeveeNBT;
 static final StoragePosition PARTY=new StoragePosition(-1,0),A=new StoragePosition(0,0),B=new StoragePosition(0,1),C=new StoragePosition(1,29);
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void fail(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("pc110-fail.txt"),t.toString());}catch(Exception ignored){}done=true;}
 static Pokemon mon(int dex){var p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(70);p.initialize();p.heal();return p;}
 static String identity(Pokemon p){return p==null?"empty":p.getUUID()+":"+p.getSpecies().getDex();}
 @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){if(done||e.phase!=TickEvent.Phase.END)return;var s=ServerLifecycleHooks.getCurrentServer();if(s==null||s.func_184103_al().func_181057_v().isEmpty()||++ticks<100)return;try{
  if(!opened){s.func_71229_d(false);int port;try(var socket=new ServerSocket(0,0,InetAddress.getLoopbackAddress())){port=socket.getLocalPort();}s.func_147137_ag().func_151265_a(InetAddress.getLoopbackAddress(),port);Files.writeString(Path.of("../pc110-lan-port.txt"),Integer.toString(port));opened=true;}
  var guest=s.func_184103_al().func_181057_v().stream().filter(p->p.func_146103_bH().getName().equals("BackportGuest")).findFirst().orElse(null);if(guest==null){if(ticks>12000)throw new AssertionError("guest connection timeout");return;}
  var party=StorageProxy.getParty(guest);var pc=StorageProxy.getPCForPlayer(guest);
  if(!seeded){for(int i=0;i<6;i++)party.set(i,null);originalChar=mon(6);originalEevee=mon(133);charId=originalChar.getUUID();eeveeId=originalEevee.getUUID();party.set(0,originalChar);pc.set(A,originalEevee);pc.set(B,null);pc.set(C,null);charNBT=originalChar.writeToNBT(new CompoundNBT());eeveeNBT=originalEevee.writeToNBT(new CompoundNBT());NetworkHelper.sendPacket(new ClientInitializePCPacket(pc),guest);party.notifyListenerAll(guest,EnumUpdateType.CLIENT);for(var pos:List.of(A,B,C))NetworkHelper.sendPacket(new ClientSetPacket(pc,pos,pc.get(pos),EnumUpdateType.CLIENT),guest);Files.writeString(Path.of("../pc110-lan-stage.txt"),"0");seeded=true;return;}
  var expected=phase==0?charId:phase==1?eeveeId:phase==2?charId:charId;Pokemon moved=phase==0?pc.get(A):phase==1?pc.get(B):phase==2?party.get(0):pc.get(C);
  if(moved==null||!moved.getUUID().equals(expected)){if(ticks>10000)throw new AssertionError("swap timeout phase="+phase);return;}
  check(originalChar.getSpecies().getDex()==6&&originalEevee.getSpecies().getDex()==133,"server dex corruption");check(originalChar.writeToNBT(new CompoundNBT()).equals(charNBT),"Charizard NBT changed");check(originalEevee.writeToNBT(new CompoundNBT()).equals(eeveeNBT),"Eevee NBT changed");
  var copy=new PCStorage(pc.uuid);copy.readFromNBT(pc.writeToNBT(new CompoundNBT()));for(var pos:List.of(A,B,C))check(identity(copy.get(pos)).equals(identity(pc.get(pos))),"native PC NBT roundtrip mismatch "+pos);
  if(++settled<20)return;settled=0;Files.writeString(Path.of("pc110-cases.txt"),"PASS TCP guest phase="+phase+" Charizard="+charId+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);
  Files.writeString(Path.of("../pc110-lan-stage.txt"),Integer.toString(phase+1));if(++phase==4){Files.writeString(Path.of("pc110-pass.txt"),"PASS four real TCP guest ServerSwapPacket exchanges; complete Pokemon NBT unchanged; PC native NBT roundtrip");Files.writeString(Path.of("../pc110-lan-done.txt"),"done");done=true;}
 }catch(Throwable t){fail(t);}}
 @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.func_71410_x();boolean guest=System.getProperty("backport.manual.world","").isEmpty();if(done){mc.func_71400_g();return;}mc.field_71474_y.field_82881_y=false;if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}if(!guest&&!opened&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen)mc.func_238191_a_("backport-qa-single");if(guest&&!requested&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen)try{int port=Integer.parseInt(Files.readString(Path.of("../pc110-lan-port.txt")));mc.func_147108_a(new net.minecraft.client.gui.screen.ConnectingScreen(mc.field_71462_r,mc,"127.0.0.1",port));requested=true;}catch(Exception ex){fail(ex);}if(guest&&mc.field_71462_r!=null&&mc.field_71462_r.getClass().getSimpleName().contains("Disconnected")){try{StringBuilder reason=new StringBuilder();for(var f:mc.field_71462_r.getClass().getDeclaredFields())if(net.minecraft.util.text.ITextComponent.class.isAssignableFrom(f.getType())){f.setAccessible(true);reason.append(f.get(mc.field_71462_r)).append("\n");}Files.writeString(Path.of("pc110-disconnected.txt"),reason);}catch(Exception ignored){}done=true;return;}if(mc.field_71439_g==null)return;com.pixelmonmod.pixelmon.client.ServerStorageDisplay.showStarterScreen=false;if(!guest){mc.func_147108_a(null);return;}
  try{if(Files.exists(Path.of("../pc110-lan-done.txt"))){done=true;return;}var party=com.pixelmonmod.pixelmon.client.storage.ClientStorageManager.party;if(party==null||!Files.exists(Path.of("../pc110-lan-stage.txt"))||!Files.readString(Path.of("../pc110-lan-stage.txt")).equals(Integer.toString(phase)))return;for(var pc:com.pixelmonmod.pixelmon.client.storage.ClientStorageManager.pcs.values()){
   var p=party.get(0);var a=pc.get(A);var b=pc.get(B);var c=pc.get(C);StoragePosition dest=null;
   if(phase==0&&p!=null&&p.getSpecies().getDex()==6&&a!=null&&a.getSpecies().getDex()==133)dest=A;
   if(phase==1&&p!=null&&p.getSpecies().getDex()==133&&a!=null&&a.getSpecies().getDex()==6&&b==null)dest=B;
   if(phase==2&&p==null&&a!=null&&a.getSpecies().getDex()==6)dest=A;
   if(phase==3&&p!=null&&p.getSpecies().getDex()==6&&a==null&&c==null)dest=C;
   if(dest!=null){NetworkHelper.sendToServer(new ServerSwapPacket(PARTY,p,dest,pc.get(dest)));phase++;break;}
  }}catch(Throwable t){fail(t);}
 }
}
