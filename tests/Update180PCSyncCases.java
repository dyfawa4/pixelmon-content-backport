package qa;
import java.util.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.comm.EnumUpdateType;
import com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper;
import com.pixelmonmod.pixelmon.comm.packetHandlers.clientStorage.newStorage.ClientSetPacket;
import com.pixelmonmod.pixelmon.comm.packetHandlers.clientStorage.newStorage.pc.ClientInitializePCPacket;
import com.pixelmonmod.pixelmon.client.storage.ClientStorageManager;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import io.netty.buffer.Unpooled;

/** Actual native packets over the integrated server channel; no storage stubs. */
public final class Update180PCSyncCases {
 static UUID owner,first,second,third,capture,raid;static PCStorage screen;static PCBox box;static volatile boolean serverStep;
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static void send(ServerPlayerEntity p,int b,int i,EnumUpdateType... fields){var pc=StorageProxy.getPCForPlayer(p);var pos=new StoragePosition(b,i);NetworkHelper.sendPacket(new ClientSetPacket(pc,pos,pc.get(pos),fields),p);}
 public static void server(ServerPlayerEntity p)throws Exception{
  owner=p.func_110124_au();var pc=StorageProxy.getPCForPlayer(p);check(pc.getBoxCount()>=3,"At least3 native PC boxes");
  pc.shouldSendUpdates=false;for(int b=0;b<3;b++)for(int i=0;i<30;i++)pc.set(b,i,null);
  for(int i=0;i<6;i++)StorageProxy.getParty(p).set(i,Update180Cases.mon(25,"Tackle"));
  var a=Update180Cases.mon(383,"Tackle");var b=Update180Cases.mon(1000,"Tackle");var c=Update180Cases.mon(716,"Tackle");first=a.getUUID();second=b.getUUID();third=c.getUUID();pc.set(0,4,a);pc.set(1,8,b);pc.set(2,9,c);pc.setLastBox(0);
  var request=new StorageSnapshot.Request(owner,2,0,true);var buffer=new PacketBuffer(Unpooled.buffer());var encode=StorageSnapshot.Request.class.getDeclaredMethod("encode",StorageSnapshot.Request.class,PacketBuffer.class);encode.setAccessible(true);encode.invoke(null,request,buffer);var decode=StorageSnapshot.Request.class.getDeclaredMethod("decode",PacketBuffer.class);decode.setAccessible(true);var copy=(StorageSnapshot.Request)decode.invoke(null,buffer);check(copy.initialize&&copy.box==2&&copy.storage.equals(owner),"Missing metadata flag packet roundtrip");buffer.release();
  System.out.println("PC_SYNC_SERVER_SETUP_PASS native storage, broadcast disabled, full party");
 }
 static ServerPlayerEntity player(){return net.minecraft.client.Minecraft.func_71410_x().func_71401_C().func_184103_al().func_181057_v().get(0);}
 static void server(Runnable r){net.minecraft.client.Minecraft.func_71410_x().func_71401_C().func_213167_f(r);}
 static Pokemon at(int b,int i){var pc=ClientStorageManager.pcs.get(owner);return pc==null?null:pc.get(b,i);}
 static void has(int b,int i,UUID id,String why){var mon=at(b,i);check(mon!=null&&id.equals(mon.getUUID()),why);}
 public static void client(int age)throws Exception{
  var mc=net.minecraft.client.Minecraft.func_71410_x();
  if(age==20){mc.func_147108_a(null);ClientStorageManager.pcs.remove(owner);ClientStorageManager.openPC=null;local.pixelmon.backport.client.StoragePacketGuard.logout(null);server(()->send(player(),0,4,EnumUpdateType.HP));}
  if(age==80){has(0,4,first,"Missing PC delta recovered metadata and full mon without reconnect");check(ClientStorageManager.openPC==ClientStorageManager.pcs.get(owner),"OpenPC/map share canonical object");screen=ClientStorageManager.openPC;box=screen.getBox(0);System.out.println("PC_SYNC_MISSING_CACHE_PASS native partial packet recovered metadata+Pokemon");
   // Two requests in the same server tick must both survive the cooldown; preserve stale-empty deletion too.
   screen.set(1,20,Update180Cases.mon(150,"Tackle"));screen.set(1,8,null);screen.set(2,9,null);serverStep=false;server(()->{var p=player();StorageSnapshot.send(p,owner,1,0);StorageSnapshot.send(p,owner,2,0);serverStep=true;});}
  if(age==140){check(serverStep,"Queued server step ran");has(1,8,second,"Burst request box1");has(2,9,third,"Rate-limited box2 eventually delivered");check(at(1,20)==null,"Authoritative empty snapshot removes ghost");check(screen==ClientStorageManager.openPC&&screen==ClientStorageManager.pcs.get(owner)&&box==screen.getBox(0),"Repeated initializations preserve screen and box references");System.out.println("PC_SYNC_QUEUE_PASS throttled burst eventually delivered, empty slots clear ghosts, screen references stable");
   // Native capture event precedes the real full-party add; no explicit recovery request here.
   mc.func_147108_a(new com.pixelmonmod.pixelmon.client.gui.pc.PCScreen());server(()->{var p=player();var mon=Update180Cases.mon(143,"Tackle");capture=mon.getUUID();var entity=mon.getOrSpawnPixelmon(p);com.pixelmonmod.pixelmon.Pixelmon.EVENT_BUS.post(new com.pixelmonmod.pixelmon.api.events.CaptureEvent.SuccessfulCapture(p,entity,null));check(StorageProxy.getParty(p).add(mon),"Native full-party capture insert");check(StorageProxy.getPCForPlayer(p).find(capture)==mon,"Capture canonical PC entry");entity.func_70106_y();});}
  if(age==185){check(capture!=null,"Capture setup ran");var capturedClient=screen.find(capture);check(capturedClient!=null,"Normal capture auto-published without reconnect or reopening");check(mc.field_71462_r instanceof com.pixelmonmod.pixelmon.client.gui.pc.PCScreen,"Native PC screen remains open while capture arrives");check(screen==ClientStorageManager.openPC&&screen==ClientStorageManager.pcs.get(owner),"Live native PC screen keeps storage reference");System.out.println("PC_SYNC_CAPTURE_PASS native successful capture before full-party PC insert auto-published into open PC screen");
   server(()->{var p=player();var mon=Update180Cases.mon(6,"Tackle");raid=mon.getUUID();com.pixelmonmod.pixelmon.Pixelmon.EVENT_BUS.post(new com.pixelmonmod.pixelmon.api.events.CaptureEvent.SuccessfulRaidCapture(p,mon,null));check(StorageProxy.getParty(p).add(mon),"Raid native full-party insert");});}
  if(age==225){check(raid!=null&&screen.find(raid)!=null,"Raid capture auto-published");System.out.println("PC_SYNC_RAID_PASS native raid success before canonical insert auto-published");
   // Full removal then a genuinely first initialization must request contents as well.
   ClientStorageManager.pcs.remove(owner);ClientStorageManager.openPC=null;local.pixelmon.backport.client.StoragePacketGuard.logout(null);server(()->NetworkHelper.sendPacket(new ClientInitializePCPacket(StorageProxy.getPCForPlayer(player())),player()));}
  if(age==285){has(0,4,first,"First initialization also recovers contents");check(ClientStorageManager.openPC.find(capture)!=null&&ClientStorageManager.openPC.find(raid)!=null,"Captures retained on first fresh initialization");System.out.println("PC_SYNC_FIRST_INIT_PASS missing first cache recovered all initial visible contents");
   server(()->{var p=player();var pc=StorageProxy.getPCForPlayer(p);var before=pc.writeToNBT(new net.minecraft.nbt.CompoundNBT()).toString();StorageSnapshot.send(p,UUID.randomUUID(),0,0);StorageSnapshot.send(p,owner,pc.getBoxCount(),0);StorageSnapshot.send(p,owner,-2,0);StorageSnapshot.send(p,owner,0,30);check(before.equals(pc.writeToNBT(new net.minecraft.nbt.CompoundNBT()).toString()),"Invalid requests never change canonical storage");pc.shouldSendUpdates=true;System.out.println("PC_SYNC_AUTHORITY_PASS foreign UUID/out-of-bounds rejected; readonly storage unchanged");});}
  if(age==300){
   // A delta for an expanded, unknown box must request metadata before writing out of range.
   screen=ClientStorageManager.openPC;box=screen.getBox(0);var tiny=new PCStorage(owner,1);tiny.shouldSendUpdates=false;screen=local.pixelmon.backport.client.PCStorageSync.initialize(tiny);check(screen.getBoxCount()==1,"Native cache shrink fixture");local.pixelmon.backport.client.StoragePacketGuard.logout(null);server(()->send(player(),2,9,EnumUpdateType.HP));
  }
  if(age==355){has(2,9,third,"Unknown expanded box recovered metadata then actual slot");check(ClientStorageManager.openPC==screen&&screen.getBox(0)==box,"Expansion retains native screen and existing box references");System.out.println("PC_SYNC_EXPANSION_PASS incoming unknown box recovers metadata without losing live screen reference");}
  if(age==365){mc.func_147108_a(null);server(()->{var p=player();var pc=StorageProxy.getPCForPlayer(p);var mon=Update180Cases.mon(132,"Tackle");var id=mon.getUUID();var entity=mon.getOrSpawnPixelmon(p);com.pixelmonmod.pixelmon.Pixelmon.EVENT_BUS.post(new com.pixelmonmod.pixelmon.api.events.CaptureEvent.SuccessfulCapture(p,entity,null));entity.func_70106_y(); // deliberately never inserted
    serverStep=false;});}
  if(age>395){check(!serverStep,"Uninserted event fixture completed");System.out.println("PC_SYNC_CLIENT_PASS all targeted native PC network checks without reconnect");mc.func_71400_g();}
 }
}

