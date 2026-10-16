package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.client.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Integrated servers deliver every player's dimension event to ClientProxy. */
@Mixin(value=ClientProxy.class,remap=false)
public abstract class BattleDimensionClientMixin {
    @Inject(method="onDimensionChange",at=@At("HEAD"),cancellable=true)
    private void backport$localDimension(PlayerEvent.PlayerChangedDimensionEvent event,CallbackInfo ci){
        var local=Minecraft.func_71410_x().field_71439_g;
        // Client and integrated-server player objects differ; compare UUIDs.
        if(local==null || event.getPlayer()==null
            || !local.func_110124_au().equals(event.getPlayer().func_110124_au()))ci.cancel();
    }
}
