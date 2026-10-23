package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.storage.PCBox;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(targets="com.pixelmonmod.pixelmon.api.storage.PCStorage",remap=false)
public interface PCStorageAccess {
    @Accessor("boxes") void backport$boxes(PCBox[] boxes);
}
