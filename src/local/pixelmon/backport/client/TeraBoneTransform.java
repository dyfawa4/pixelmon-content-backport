package local.pixelmon.backport.client;

/** Pixelmon's legacy skinning math uses column-major field names; its
 * toVanilla() copies names without converting that convention. */
public final class TeraBoneTransform {
    private TeraBoneTransform(){}
    public static net.minecraft.util.math.vector.Matrix4f toRender(com.pixelmonmod.pixelmon.tools.Matrix4f bone){
        return bone.copy().transpose().toVanilla();
    }
}
