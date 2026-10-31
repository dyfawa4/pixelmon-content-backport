package local.pixelmon.backport.mixin;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.blocks.tileentity.CloningMachineTileEntity;
import com.pixelmonmod.pixelmon.client.render.tileEntities.RenderTileEntityCloningMachine;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** The native glass pass leaves depth writes disabled for unrelated later renderers. */
@Mixin(value=RenderTileEntityCloningMachine.class,remap=false)
public abstract class CloningMachineRenderStateMixin {
 @Inject(method="render(Lcom/pixelmonmod/pixelmon/blocks/tileentity/CloningMachineTileEntity;Lnet/minecraft/block/BlockState;DDDFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;II)V",at=@At("RETURN"))
 private void backport$restoreDepth(CloningMachineTileEntity tile,BlockState state,double x,double y,double z,float partial,MatrixStack matrices,IRenderTypeBuffer buffers,int light,int overlay,CallbackInfo ci){RenderSystem.depthMask(true);}
}
