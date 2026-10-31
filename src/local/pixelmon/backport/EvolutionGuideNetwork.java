package local.pixelmon.backport;
import java.util.*;import java.util.function.Supplier;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.network.simple.SimpleChannel;
/** Queries never evolve a Pokemon, alter storage, or consume an item. */
public final class EvolutionGuideNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(Content.NS,"evolution_guide"),()->"1","1"::equals,"1"::equals);
    public static void register(){
        CHANNEL.registerMessage(0,Request.class,Request::encode,Request::decode,Request::handle,Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1,Reply.class,Reply::encode,Reply::decode,Reply::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static final class Request {
        public final UUID pokemon;public final int sequence;
        public Request(UUID pokemon,int sequence){this.pokemon=pokemon;this.sequence=sequence;}
        static void encode(Request r,PacketBuffer b){b.func_179252_a(r.pokemon);b.writeInt(r.sequence);}
        static Request decode(PacketBuffer b){return new Request(b.func_179253_g(),b.readInt());}
        static void handle(Request r,Supplier<NetworkEvent.Context> supplied){var c=supplied.get();c.enqueueWork(()->{
            var player=c.getSender();if(player==null)return;
            var p=StorageProxy.getParty(player).get(r.pokemon);if(p==null)p=StorageProxy.getPCForPlayer(player).find(r.pokemon);
            JsonObject data=new JsonObject();
            if(p!=null)try{data=EvolutionGuide.build(p);}catch(RuntimeException failure){com.pixelmonmod.pixelmon.Pixelmon.LOGGER.warn("Unable to read evolution guide for {}",r.pokemon,failure);}
            CHANNEL.send(PacketDistributor.PLAYER.with(()->player),new Reply(r.pokemon,r.sequence,data));
        });c.setPacketHandled(true);}
    }
    public static final class Reply {
        public final UUID pokemon;public final int sequence;public final JsonObject data;
        public Reply(UUID pokemon,int sequence,JsonObject data){this.pokemon=pokemon;this.sequence=sequence;this.data=data;}
        static void encode(Reply r,PacketBuffer b){b.func_179252_a(r.pokemon);b.writeInt(r.sequence);byte[] bytes=r.data.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);b.writeInt(bytes.length);b.writeBytes(bytes);}
        static Reply decode(PacketBuffer b){UUID id=b.func_179253_g();int sequence=b.readInt(),length=b.readInt();if(length<0||length>262144||length>b.readableBytes())throw new IllegalArgumentException("Evolution guide size");byte[] bytes=new byte[length];b.readBytes(bytes);return new Reply(id,sequence,new JsonParser().parse(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());}
        static void handle(Reply r,Supplier<NetworkEvent.Context> supplied){var c=supplied.get();c.enqueueWork(()->local.pixelmon.backport.client.EvolutionGuideScreen.accept(r));c.setPacketHandled(true);}
    }
}
