package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.blocks.tileentity.PokeStopTileEntity;
import java.lang.ref.WeakReference;
import local.pixelmon.backport.client.DetectorLineVisibility;
import local.pixelmon.backport.client.DetectorPokeStopLines;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo @Mixin(targets="pixelmondetector.marker.PKMarker", remap=false)
public abstract class DetectorPokeStopMarkerMixin implements DetectorLineVisibility {
    @Unique private WeakReference<PokeStopTileEntity> backport$pokeStop;

    @Inject(method="<init>(Lpixelmondetector/entities/EntityType;Lnet/minecraft/tileentity/TileEntity;Ljava/lang/String;Ljava/lang/String;)V", at=@At("RETURN"))
    private void backport$rememberPokeStop(@Coerce Object category, TileEntity tile, String name, String description, CallbackInfo ci) {
        if (tile instanceof PokeStopTileEntity) backport$pokeStop = new WeakReference<>((PokeStopTileEntity) tile);
    }

    public boolean backport$hideLine() {
        if (backport$pokeStop == null) return false;
        PokeStopTileEntity stop = backport$pokeStop.get();
        return stop == null || DetectorPokeStopLines.hide(stop);
    }
}
