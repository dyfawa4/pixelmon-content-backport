package local.pixelmon.backport.mixin;

import java.util.List;
import net.minecraft.entity.Entity;
import net.minecraft.tileentity.TileEntity;
import local.pixelmon.backport.client.DetectorTargets;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

@Pseudo @Mixin(targets="pixelmondetector.entities.EntityFinder",remap=false)
public abstract class DetectorActiveTargetsMixin {
    @ModifyArg(method="getAndDraw",at=@At(value="INVOKE",target="Lpixelmondetector/entities/EntityFinder;addTileList(Ljava/util/List;)V"),index=0)
    private static List<TileEntity> backport$activeTiles(List<TileEntity> tiles){return DetectorTargets.tiles(tiles);}
    @ModifyArg(method="getAndDraw",at=@At(value="INVOKE",target="Lpixelmondetector/entities/EntityFinder;addEntityList(Ljava/lang/Iterable;)V"),index=0)
    private static Iterable<Entity> backport$activeEntities(Iterable<Entity> entities){return DetectorTargets.entities(entities);}
}
