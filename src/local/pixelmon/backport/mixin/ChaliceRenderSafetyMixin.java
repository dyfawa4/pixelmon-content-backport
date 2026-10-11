package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.blocks.tileentity.ArcChaliceTileEntity;
import com.pixelmonmod.pixelmon.client.render.tileEntities.RenderTileEntityArcChalice;
import com.pixelmonmod.pixelmon.enums.EnumPlate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=RenderTileEntityArcChalice.class,remap=false)
public abstract class ChaliceRenderSafetyMixin {
    // The completed flute enables a native glow pass which casts its buffer to
    // Impl. Compatible renderers can supply a wrapper; use the native buffered
    // source for this pass so completion cannot fail at that unchecked cast.
    @ModifyVariable(method="render(Lcom/pixelmonmod/pixelmon/blocks/tileentity/ArcChaliceTileEntity;Lnet/minecraft/block/BlockState;DDDFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;II)V",
        at=@At("HEAD"),argsOnly=true)
    private net.minecraft.client.renderer.IRenderTypeBuffer backport$nativeGlowBuffer(net.minecraft.client.renderer.IRenderTypeBuffer buffer){
        return buffer instanceof net.minecraft.client.renderer.IRenderTypeBuffer.Impl?buffer:net.minecraft.client.Minecraft.func_71410_x().func_228019_au_().func_228487_b_();
    }
    @Redirect(method="render(Lcom/pixelmonmod/pixelmon/blocks/tileentity/ArcChaliceTileEntity;Lnet/minecraft/block/BlockState;DDDFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;II)V",
        at=@At(value="FIELD",target="Lcom/pixelmonmod/pixelmon/blocks/tileentity/ArcChaliceTileEntity;animating:Z",opcode=180))
    private boolean backport$validPlate(ArcChaliceTileEntity tile){return tile.animating && tile.plateAdded>=0 && tile.plateAdded<EnumPlate.values().length;}
}
