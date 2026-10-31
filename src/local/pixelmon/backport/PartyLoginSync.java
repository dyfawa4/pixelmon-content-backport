package local.pixelmon.backport;
import java.util.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.comm.EnumUpdateType;
import com.pixelmonmod.pixelmon.comm.packetHandlers.clientStorage.newStorage.*;
import com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper;
/** Read-only recovery of the sender's authoritative party; never heals or recreates Pokemon. */
public final class PartyLoginSync {
 public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(Content.NS,"party_login_sync"),()->"1",PartyLoginSync::compatible,PartyLoginSync::compatible);
 private static final Map<ServerPlayerEntity,Long> LAST=new WeakHashMap<>();
 private static boolean compatible(String v){return "1".equals(v)||NetworkRegistry.ABSENT.equals(v)||NetworkRegistry.ACCEPTVANILLA.equals(v);}
 public static void register(){CHANNEL.registerMessage(0,Request.class,(m,b)->{},b->new Request(),(m,c)->{var context=c.get();context.enqueueWork(()->{var p=context.getSender();if(p==null)return;long now=System.nanoTime();Long last=LAST.get(p);if(last!=null&&now-last<2_000_000_000L)return;LAST.put(p,now);refresh(p);});context.setPacketHandled(true);},Optional.of(NetworkDirection.PLAY_TO_SERVER));}
 public static void refresh(ServerPlayerEntity player){
  var party=StorageProxy.getParty(player);
  NetworkHelper.sendPacket(new SetTempMode(party.inTemporaryMode(),party.getTempPartyColor()),player);
  for(int i=0;i<6;i++)NetworkHelper.sendPacket(new ClientSetPacket(party,new StoragePosition(-1,i),party.get(i),EnumUpdateType.CLIENT),player);
 }
 public static final class Request {}
}
