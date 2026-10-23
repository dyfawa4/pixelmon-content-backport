package local.pixelmon.backport.client;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.comm.EnumUpdateType;
import local.pixelmon.backport.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;

/** A delta cannot construct a Pokemon; retain real data and request its server snapshot. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid=Content.NS,value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class StoragePacketGuard {
    private static long last=Long.MIN_VALUE;
    private static Object session;
    private static final LinkedHashMap<String,StorageSnapshot.Request> pending=new LinkedHashMap<>();
    private StoragePacketGuard(){}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent event){reset(null);}
    private static void reset(Object world){session=world;last=Long.MIN_VALUE;pending.clear();}
    @net.minecraftforge.eventbus.api.SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.ClientTickEvent event){
        if(event.phase==net.minecraftforge.event.TickEvent.Phase.END)pump();
    }
    public static void recover(UUID id,StoragePosition pos){
        var world=Minecraft.func_71410_x().field_71441_e;
        if(world!=session)reset(world);
        if(world==null||id==null||pos==null||pos.box < -1||pos.order<0||pos.order>=(pos.box==-1?6:30))return;
        String key=id+":"+pos.box;
        if(pending.size()<256||pending.containsKey(key))pending.putIfAbsent(key,new StorageSnapshot.Request(id,pos.box,0));
        pump();
    }
    private static void pump(){
        var world=Minecraft.func_71410_x().field_71441_e;
        if(world!=session)reset(world);
        if(world==null||pending.isEmpty())return;
        long now=world.func_82737_E();if(last!=Long.MIN_VALUE&&now>=last&&now-last<10)return;
        var iterator=pending.entrySet().iterator();var request=iterator.next().getValue();iterator.remove();
        last=now;TeraNetwork.CHANNEL.sendToServer(request);
    }
    public static boolean guard(UUID id,StoragePosition pos,PacketBuffer data,EnumUpdateType[] types){
        if(data==null)return false; // A real server deletion remains a deletion.
        var storage=com.pixelmonmod.pixelmon.client.storage.ClientStorageManager.getStorage(id,pos);
        if(storage==null){recover(id,pos);return true;}
        if(data.readableBytes()<16){recover(id,pos);return true;}
        int index=data.readerIndex();UUID incoming=new UUID(data.getLong(index),data.getLong(index+8));
        Pokemon existing=storage.get(pos);
        boolean same=existing!=null && existing.getSpecies().getDex()>0 && incoming.equals(existing.getUUID());
        var fields=types==null||types.length==0?EnumSet.allOf(EnumUpdateType.class):EnumSet.copyOf(Arrays.asList(types));
        boolean complete=fields.containsAll(Arrays.asList(EnumUpdateType.Name,EnumUpdateType.Form,EnumUpdateType.Stats,EnumUpdateType.HP));
        if(!complete){if(!same){recover(id,pos);return true;}return false;}
        PacketBuffer copy=new PacketBuffer(data.copy(index,data.readableBytes()));
        try{
            Pokemon fresh=PokemonFactory.create(incoming);fresh.readFromByteBuffer(copy,types);
            if(fresh.getSpecies().getDex()<=0){recover(id,pos);return true;}
            if(!same){storage.set(pos,fresh);return true;}
            return false;
        }finally{copy.release();}
    }
}
