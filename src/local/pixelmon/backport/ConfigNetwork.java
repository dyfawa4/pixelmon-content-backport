package local.pixelmon.backport;
import com.google.gson.*;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.api.distmarker.Dist;

/** Config writes run on the authoritative server thread and require command permission level 2. */
public final class ConfigNetwork {
    private ConfigNetwork(){}
    public static final class Request {
        public int id,page,size=8;public String group,search,path="",expected="",value="";
        public Request(int id,String group,String search,int page){this.id=id;this.group=group;this.search=search;this.page=page;}
        public static void encode(Request s,PacketBuffer b){b.writeInt(s.id);b.func_180714_a(s.group);b.func_180714_a(s.search);b.writeInt(s.page);b.writeByte(s.size);b.func_180714_a(s.path);b.func_180714_a(s.expected);b.func_180714_a(s.value);}
        public static Request decode(PacketBuffer b){Request s=new Request(b.readInt(),b.func_150789_c(32),b.func_150789_c(128),b.readInt());s.size=b.readUnsignedByte();s.path=b.func_150789_c(256);s.expected=b.func_150789_c(12000);s.value=b.func_150789_c(12000);return s;}
        public static void handle(Request s,Supplier<NetworkEvent.Context> context){var c=context.get();c.enqueueWork(()->{
            var p=c.getSender();if(p==null)return;Reply r=new Reply();r.id=s.id;
            try{
                var source=p.func_184102_h();boolean owner=source.func_71264_H()&&source.func_213199_b(p.func_146103_bH());
                if(!owner&&!p.func_195051_bN().func_197034_c(2))throw new IllegalStateException("服务器配置仅房主或管理员可修改或查看；画面设置可在本机修改");
                if(s.group.equals("graphics"))throw new IllegalArgumentException("画面设置属于客户端");
                if(!s.path.isEmpty()){
                    // Reconfiguration cannot alter the rules underneath an active battle.
                    var server=p.func_184102_h();for(var player:server.func_184103_al().func_181057_v())if(com.pixelmonmod.pixelmon.battles.BattleRegistry.getBattle(player)!=null)throw new IllegalStateException("请等待当前玩家战斗结束后修改服务器配置");
                    r.message=ConfigEditor.save(s.group,s.path,new JsonParser().parse(s.expected),new JsonParser().parse(s.value));
                    com.pixelmonmod.pixelmon.api.config.PixelmonServerConfig.load();
                    com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper.sendToAll(new com.pixelmonmod.pixelmon.comm.packetHandlers.ServerConfigListPacket());
                }
                r.data=ConfigEditor.page(s.group,s.search,s.page,s.size).toString();
            }catch(Exception e){r.message=e.getMessage()==null?"配置操作失败":e.getMessage();r.error=true;}
            TeraNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),r);
        });c.setPacketHandled(true);}
    }
    public static final class Reply {
        public int id;public boolean error;public String data="",message="";
        public static void encode(Reply s,PacketBuffer b){b.writeInt(s.id);b.writeBoolean(s.error);b.func_180714_a(s.data);b.func_180714_a(s.message);}
        public static Reply decode(PacketBuffer b){Reply s=new Reply();s.id=b.readInt();s.error=b.readBoolean();s.data=b.func_150789_c(32767);s.message=b.func_150789_c(2048);return s;}
        public static void handle(Reply s,Supplier<NetworkEvent.Context> context){var c=context.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->BackportConfigScreen.accept(s)));c.setPacketHandled(true);}
    }
}
