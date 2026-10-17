package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.client.models.smd.DeformVertex;
import com.pixelmonmod.pixelmon.tools.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=DeformVertex.class,remap=false)
public interface TeraVertexAccess {
    @Accessor("baseLoc") Vector4f backport$restPosition();
}
