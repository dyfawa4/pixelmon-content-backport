package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.entities.npcs.*;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
/** Old indexed NPCs don't synchronize their generated personal name to rendering clients. */
@Mod.EventBusSubscriber(modid=Content.NS)
public final class NpcNameSync {
    @SubscribeEvent public static void join(EntityJoinWorldEvent event){
        if(event.getWorld().field_72995_K||!(event.getEntity() instanceof IndexedNPCEntity))return;
        var npc=(IndexedNPCEntity)event.getEntity();
        if(npc.func_200201_e()!=null||npc.getNickName()!=null&&!npc.getNickName().isBlank())return;
        String name=npc instanceof NPCShopkeeper?((NPCShopkeeper)npc).getShopkeeperName("en_us"):npc.getName("en_us");
        if(name!=null&&!name.isBlank())npc.setName(name);
    }
}
