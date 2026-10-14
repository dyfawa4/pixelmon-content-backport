package local.pixelmon.backport.mixin;

import java.util.List;
import local.pixelmon.backport.LootrQueueWorlds;
import net.minecraft.world.chunk.*;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo @Mixin(targets="noobanidus.mods.lootr.entity.EntityTicker",remap=false)
public abstract class LootrQueueLifecycleMixin {
    @Shadow @Final private static List<?> entities;
    @Inject(method="onServerTick",at=@At("HEAD"))
    private static void backport$discardOldWorld(TickEvent.ServerTickEvent event,CallbackInfo ci){
        if(event.phase==TickEvent.Phase.END)LootrQueueWorlds.discardRetired(entities);
    }
    @Redirect(method="onServerTick",at=@At(value="INVOKE",target="Lnet/minecraft/world/server/ServerChunkProvider;func_212849_a_(IILnet/minecraft/world/chunk/ChunkStatus;Z)Lnet/minecraft/world/chunk/IChunk;"))
    private static IChunk backport$loadedChestChunk(ServerChunkProvider provider,int x,int z,ChunkStatus status,boolean required){
        // Only insert into an already accessible FULL chunk. Defer other entries.
        return provider.func_225313_a(x,z);
    }
}
