package qa.clone110mixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.blocks.tileentity.CloningMachineTileEntity;
import com.pixelmonmod.pixelmon.client.render.tileEntities.RenderTileEntityCloningMachine;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=RenderTileEntityCloningMachine.class,remap=false,priority=800)
public abstract class Clone110DepthProbe {
 @Inject(method="render(Lcom/pixelmonmod/pixelmon/blocks/tileentity/CloningMachineTileEntity;Lnet/minecraft/block/BlockState;DDDFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;II)V",at=@At("RETURN"))
 private void sample(CloningMachineTileEntity t,BlockState s,double x,double y,double z,float p,MatrixStack m,IRenderTypeBuffer b,int l,int o,CallbackInfo ci){qa.Clone110Probe.depth(org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK));}
}
