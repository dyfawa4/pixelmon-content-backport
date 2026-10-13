package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy;
import com.pixelmonmod.pixelmon.api.events.spawning.LegendarySpawnEvent;
import com.pixelmonmod.pixelmon.api.util.helpers.RandomHelper;
import com.pixelmonmod.pixelmon.api.util.helpers.WorldHelper;
import net.minecraftforge.eventbus.api.*;

public final class CapSpawns {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void spawn(LegendarySpawnEvent.DoSpawn event){
        if(event.isCanceled() || event.getLegendary().getDex()!=CapSpecies.DEX)return;
        var location=event.action.spawnLocation.location;var world=location.world;var pos=location.pos;
        // Check the actual final spawn location as well as the datapack condition.
        if(!(world instanceof net.minecraft.world.server.ServerWorld)
            || !new net.minecraft.util.ResourceLocation("minecraft:plains").equals(world.func_226691_t_(pos).getRegistryName())
            || !WorldHelper.insideStructure((net.minecraft.world.server.ServerWorld)world,"minecraft:village",pos)
            || !RandomHelper.getRandomChance((float)BurstPolicy.spawnRetention(PixelmonConfigProxy.getSpawning().getLegendarySpawnChance())))event.setCanceled(true);
    }
}
