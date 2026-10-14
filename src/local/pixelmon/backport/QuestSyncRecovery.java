package local.pixelmon.backport;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/** Resend authoritative quest/team data; never rerun login rewards or reset progress. */
public final class QuestSyncRecovery {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(Content.NS,"quest_sync_recovery"),()->"1",QuestSyncRecovery::compatible,QuestSyncRecovery::compatible);
    private static final Map<ServerPlayerEntity,Long> LAST=new WeakHashMap<>();
    private QuestSyncRecovery(){}
    private static boolean compatible(String value){return "1".equals(value)||NetworkRegistry.ABSENT.equals(value)||NetworkRegistry.ACCEPTVANILLA.equals(value);}
    public static void register(){CHANNEL.registerMessage(0,Request.class,(m,b)->{},b->new Request(),Request::handle,Optional.of(NetworkDirection.PLAY_TO_SERVER));}
    public static final class Request {
        public static void handle(Request message,Supplier<NetworkEvent.Context> supplier){
            NetworkEvent.Context context=supplier.get();ServerPlayerEntity player=context.getSender();
            if(player!=null)context.enqueueWork(()->resend(player));context.setPacketHandled(true);
        }
    }
    private static void resend(ServerPlayerEntity player){
        if(!ModList.get().isLoaded("ftbquests"))return;
        long now=System.currentTimeMillis();if(now-LAST.getOrDefault(player,0L)<5000)return;LAST.put(player,now);
        try{
            Class<?> fileType=Class.forName("dev.ftb.mods.ftbquests.quest.QuestFile");
            Object file=Class.forName("dev.ftb.mods.ftbquests.quest.ServerQuestFile").getField("INSTANCE").get(null);
            if(file==null)return;
            // Match only the native login sync packets, not playerLoggedIn(),
            // which also installs listeners and checks/completes tasks.
            Object self=fileType.getMethod("getData",Entity.class).invoke(file,player);
            Object sync=Class.forName("dev.ftb.mods.ftbquests.net.SyncQuestsMessage").getConstructor(fileType).newInstance(file);
            sync.getClass().getMethod("sendTo",ServerPlayerEntity.class).invoke(sync,player);
            Class<?> teamType=Class.forName("dev.ftb.mods.ftbquests.quest.TeamData");
            var ctor=Class.forName("dev.ftb.mods.ftbquests.net.SyncTeamDataMessage").getConstructor(teamType,boolean.class);
            for(Object row:(Collection<?>)fileType.getMethod("getAllData").invoke(file)){
                Object teamSync=ctor.newInstance(row,row==self);teamSync.getClass().getMethod("sendTo",ServerPlayerEntity.class).invoke(teamSync,player);
            }
        }catch(ReflectiveOperationException failure){org.apache.logging.log4j.LogManager.getLogger(Content.NS).warn("Quest client data resync unavailable",failure);}
    }
}
