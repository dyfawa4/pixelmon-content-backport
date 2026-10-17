package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.client.models.smd.Bone;
import com.pixelmonmod.pixelmon.tools.Matrix4f;
import local.pixelmon.backport.client.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=Bone.class,remap=false)
public abstract class TeraBonePoseMixin implements TeraBonePose {
    @Shadow public Matrix4f modified;
    @Unique private net.minecraft.util.math.vector.Matrix4f backport$headPose;
    // Bone.applyModified resets its temporary matrix immediately after skinning.
    // Capture the actual frame while it is still available, not at model RETURN.
    @Inject(method="reset",at=@At("HEAD"))
    private void backport$rememberPose(CallbackInfo ci){
        if(TeraVisualRenderer.captureHead((Bone)(Object)this))backport$headPose=TeraBoneTransform.toRender(modified);
    }
    @Override public net.minecraft.util.math.vector.Matrix4f backport$teraHeadPose(){return backport$headPose;}
}
