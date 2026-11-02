package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.blocks.tileentity.OrbTileEntity;
import com.pixelmonmod.pixelmon.blocks.OrbBlock;
import com.pixelmonmod.pixelmon.enums.items.EnumOrbShard;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.Model;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Native JADE OBJ files/texture were never shipped. Use complete native-code models. */
@Mixin(targets="com.pixelmonmod.pixelmon.client.render.tileEntities.RenderTileEntityOrb",remap=false)
public abstract class DeltaOrbRenderMixin {
 @Inject(method="render(Lcom/pixelmonmod/pixelmon/blocks/tileentity/OrbTileEntity;Lnet/minecraft/block/BlockState;DDDFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;II)V",at=@At("HEAD"),cancellable=true)
 private void backport$delta(OrbTileEntity tile,BlockState state,double x,double y,double z,float partial,MatrixStack matrices,IRenderTypeBuffer buffer,int light,int overlay,CallbackInfo ci){
  if(!(state.func_177230_c() instanceof OrbBlock)||((OrbBlock)state.func_177230_c()).shardType!=EnumOrbShard.JADE)return;ci.cancel();
  matrices.func_227860_a_();try{
  matrices.func_227863_a_(com.pixelmonmod.pixelmon.api.util.helpers.QuaternionHelper.fromEuler(180,1,0,0));
  matrices.func_227862_a_(.06f,.06f,.06f);
  int pieces=Math.max(1,Math.min(10,tile.getPieces()));
  var model=com.pixelmonmod.pixelmon.client.render.tileEntities.RenderTileEntityOrb.EnumOrbModels.getModel(EnumOrbShard.BLUE,pieces);
  ((com.pixelmonmod.pixelmon.client.render.tileEntities.TileEntityRenderer<?>)(Object)this).renderOBJ(matrices,buffer,model,new ResourceLocation("pixelmon_backport","textures/block/delta_orb.png"),light,overlay);
  }finally{matrices.func_227865_b_();}
 }
}
