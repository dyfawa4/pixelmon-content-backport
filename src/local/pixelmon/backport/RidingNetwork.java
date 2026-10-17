package local.pixelmon.backport;
import java.util.*;
import java.util.function.Supplier;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public final class RidingNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(Content.NS,"riding"),()->"3","3"::equals,"3"::equals);
    public static void register(){
        CHANNEL.registerMessage(0,Input.class,Input::encode,Input::decode,Input::handle,Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1,Snapshot.class,Snapshot::encode,Snapshot::decode,Snapshot::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static void send(ServerPlayerEntity p,Snapshot state){CHANNEL.send(PacketDistributor.PLAYER.with(()->p),state);}
    public static final class Input {
        public final UUID mount;public final int entity,sequence,mask;public final float yaw,pitch;
        public Input(UUID mount,int entity,int sequence,int mask,float yaw,float pitch){this.mount=mount;this.entity=entity;this.sequence=sequence;this.mask=mask;this.yaw=yaw;this.pitch=pitch;}
        static void encode(Input m,PacketBuffer b){b.func_179252_a(m.mount);b.writeInt(m.entity);b.writeInt(m.sequence);b.writeByte(m.mask);b.writeFloat(m.yaw);b.writeFloat(m.pitch);}
        static Input decode(PacketBuffer b){Input m=new Input(b.func_179253_g(),b.readInt(),b.readInt(),b.readUnsignedByte(),b.readFloat(),b.readFloat());if(m.mask>127||!Float.isFinite(m.yaw)||!Float.isFinite(m.pitch))throw new IllegalArgumentException("Riding input flags/rotation");return m;}
        static void handle(Input m,Supplier<NetworkEvent.Context> supplier){var c=supplier.get();c.enqueueWork(()->RidingMotion.input(c.getSender(),m));c.setPacketHandled(true);}
    }
    public static final class Snapshot {
        public UUID mount;public int entity,sequence;public double x,y,z,vx,vy,vz;public float yaw;public boolean flying,ground;
        public Snapshot(){}
        public Snapshot(PixelmonEntity e,int sequence){mount=e.func_110124_au();entity=e.func_145782_y();this.sequence=sequence;x=e.func_226277_ct_();y=e.func_226278_cu_();z=e.func_226281_cx_();var v=e.func_213322_ci();vx=v.field_72450_a;vy=v.field_72448_b;vz=v.field_72449_c;yaw=e.field_70177_z;flying=e.isFlying();ground=e.func_233570_aj_();}
        static void encode(Snapshot m,PacketBuffer b){b.func_179252_a(m.mount);b.writeInt(m.entity);b.writeInt(m.sequence);b.writeDouble(m.x);b.writeDouble(m.y);b.writeDouble(m.z);b.writeDouble(m.vx);b.writeDouble(m.vy);b.writeDouble(m.vz);b.writeFloat(m.yaw);b.writeBoolean(m.flying);b.writeBoolean(m.ground);}
        static Snapshot decode(PacketBuffer b){Snapshot m=new Snapshot();m.mount=b.func_179253_g();m.entity=b.readInt();m.sequence=b.readInt();m.x=b.readDouble();m.y=b.readDouble();m.z=b.readDouble();m.vx=b.readDouble();m.vy=b.readDouble();m.vz=b.readDouble();m.yaw=b.readFloat();m.flying=b.readBoolean();m.ground=b.readBoolean();if(!Double.isFinite(m.x+m.y+m.z+m.vx+m.vy+m.vz)||!Float.isFinite(m.yaw))throw new IllegalArgumentException("Riding position/rotation");return m;}
        static void handle(Snapshot m,Supplier<NetworkEvent.Context> supplier){var c=supplier.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->local.pixelmon.backport.client.RidingClient.snapshot(m)));c.setPacketHandled(true);}
    }
}
