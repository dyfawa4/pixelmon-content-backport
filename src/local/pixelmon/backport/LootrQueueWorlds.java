package local.pixelmon.backport;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

/** Lootr's static insertion queue can outlive an integrated server. */
public final class LootrQueueWorlds {
    private LootrQueueWorlds(){}
    public static void discardRetired(List<?> queue){
        var server=ServerLifecycleHooks.getCurrentServer();
        queue.removeIf(value->{
            if(!(value instanceof Entity))return true;
            Entity entity=(Entity)value;
            if(entity.field_70128_L || !(entity.field_70170_p instanceof ServerWorld) || server==null)return true;
            ServerWorld world=(ServerWorld)entity.field_70170_p;
            return world.func_73046_m()!=server || server.func_71218_a(world.func_234923_W_())!=world;
        });
    }
}
