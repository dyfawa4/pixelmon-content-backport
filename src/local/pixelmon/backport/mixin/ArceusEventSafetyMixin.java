package local.pixelmon.backport.mixin;
import java.util.UUID;
import com.pixelmonmod.pixelmon.api.events.legendary.ArceusEvent;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=ArceusEvent.class,remap=false)
public abstract class ArceusEventSafetyMixin {
    @Shadow protected UUID playerUUID;
    @Inject(method="getPlayer",at=@At("HEAD"),cancellable=true)
    private void backport$missingPlayer(CallbackInfoReturnable<ServerPlayerEntity> ci){if(playerUUID==null || net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer()==null)ci.setReturnValue(null);}
}
