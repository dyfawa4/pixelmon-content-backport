package local.pixelmon.backport.mixin;

import java.util.concurrent.CompletableFuture;
import com.mojang.datafixers.util.Either;
import net.minecraft.world.chunk.*;
import net.minecraft.world.server.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerChunkProvider.class)
public interface CompassChunkRequestInvoker {
    /** Unlike the public wrapper, this does not managed-block the server thread. */
    @Invoker("func_217233_c")
    CompletableFuture<Either<IChunk,ChunkHolder.IChunkLoadingError>> backport$requestChunk(int x,int z,ChunkStatus status,boolean required);
}
