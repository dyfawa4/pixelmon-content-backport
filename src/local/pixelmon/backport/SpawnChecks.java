package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.spawning.AbstractSpawner;
import com.pixelmonmod.pixelmon.spawning.LegendarySpawner;
import com.pixelmonmod.pixelmon.spawning.PixelmonSpawning;
import net.minecraft.entity.player.ServerPlayerEntity;

/** Direct, read-only native query entry; never grants command permissions. */
public final class SpawnChecks {
    private static final ThreadLocal<ServerPlayerEntity> VIEWER=new ThreadLocal<>();
    private SpawnChecks(){}
    public static List<ServerPlayerEntity> players(List<ServerPlayerEntity> original){
        ServerPlayerEntity p=VIEWER.get();return p==null?original:Collections.singletonList(p);
    }
    public static List<ServerPlayerEntity> players(List<ServerPlayerEntity> original,net.minecraft.command.CommandSource source){
        List<ServerPlayerEntity> scoped=players(original);if(scoped!=original)return scoped;
        return source!=null&&source.func_197022_f() instanceof ServerPlayerEntity?Collections.singletonList((ServerPlayerEntity)source.func_197022_f()):original;
    }
    public static boolean run(ServerPlayerEntity player,boolean legendary){
        if(PixelmonSpawning.coordinator==null)return false;
        AbstractSpawner spawner=PixelmonSpawning.coordinator.getSpawner(legendary?"legendary":player.func_200200_C_().getString());
        if(spawner==null||spawner.checkSpawns==null||(spawner instanceof LegendarySpawner)!=legendary)return false;
        ServerPlayerEntity previous=VIEWER.get();VIEWER.set(player);
        try{spawner.checkSpawns.checkSpawns(spawner,player.func_195051_bN(),Collections.emptyList());return true;}
        finally{if(previous==null)VIEWER.remove();else VIEWER.set(previous);}
    }
}
