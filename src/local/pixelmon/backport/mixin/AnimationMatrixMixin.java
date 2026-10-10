package local.pixelmon.backport.mixin;
import java.io.*;
import com.pixelmonmod.pixelmon.client.models.smd.*;
import com.pixelmonmod.pixelmon.tools.Matrix4f;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=SmdAnimation.class, remap=false)
public abstract class AnimationMatrixMixin {
    @Inject(method="loadSmdAnim",at=@At("RETURN"))
    private void backport$fullTransforms(ResourceLocation location, CallbackInfo ci) {
        String ref=location.toString();
        if (!ref.startsWith("pixelmon_backport:")) return;
        String path="assets/pixelmon_backport/"+ref.substring(ref.indexOf(':')+1)+".matrices";
        InputStream stream=getClass().getClassLoader().getResourceAsStream(path);
        if(stream==null)return;
        SmdAnimation animation=(SmdAnimation)(Object)this;
        try(DataInputStream in=new DataInputStream(stream)) {
            if(in.readInt()!=1 || in.readInt()!=animation.frames.size() || in.readInt()!=animation.bones.size()) throw new IOException("Matrix stream shape mismatch");
            for(AnimFrame frame:animation.frames) for(int bone=0;bone<animation.bones.size();bone++) {
                Matrix4f m=new Matrix4f();
                m.m00=in.readFloat();m.m10=in.readFloat();m.m20=in.readFloat();m.m30=in.readFloat();
                m.m01=in.readFloat();m.m11=in.readFloat();m.m21=in.readFloat();m.m31=in.readFloat();
                m.m02=in.readFloat();m.m12=in.readFloat();m.m22=in.readFloat();m.m32=in.readFloat();
                m.m03=in.readFloat();m.m13=in.readFloat();m.m23=in.readFloat();m.m33=in.readFloat();
                Matrix4f inverse=Matrix4f.invert(m,null);
                if(inverse==null)throw new IOException("Singular animation bone");
                frame.transforms.set(bone,m);frame.invertTransforms.set(bone,inverse);
            }
            if(in.read()!=-1)throw new IOException("Trailing animation matrices");
        }catch(IOException e){throw new IllegalStateException("Invalid converted animation: "+ref,e);}
    }
}
