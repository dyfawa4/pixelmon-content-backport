package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.blocks.tileentity.PokeDisplayTileEntity;
import local.pixelmon.backport.DisplayProtection;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import com.mojang.blaze3d.matrix.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="com.pixelmonmod.pixelmon.client.render.tileEntities.RenderTileEntityPokeDisplay",remap=false)
public abstract class EmptyPokeDisplayRenderMixin {
 @Inject(method="render(Lcom/pixelmonmod/pixelmon/blocks/tileentity/PokeDisplayTileEntity;Lnet/minecraft/block/BlockState;DDDFLcom/mojang/blaze3d/matrix/MatrixStack;Lnet/minecraft/client/renderer/IRenderTypeBuffer;II)V",at=@At("HEAD"),cancellable=true)
 private void backport$empty(PokeDisplayTileEntity tile,BlockState state,double x,double y,double z,float partial,MatrixStack matrix,IRenderTypeBuffer buffer,int light,int overlay,CallbackInfo ci){
  var stored=tile.getStoredPoke();if(stored.isPresent()&&DisplayProtection.valid(stored.get()))return;
  ci.cancel();var mc=Minecraft.func_71410_x();var ball=new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("pixelmon:poke_ball")));ball.func_196082_o().func_74778_a("PokeBallID","poke_ball");
  matrix.func_227860_a_();try{matrix.func_227861_a_(.5,.35,.5);mc.func_175599_af().func_229110_a_(ball,ItemCameraTransforms.TransformType.GROUND,light,overlay,matrix,buffer);}finally{matrix.func_227865_b_();}
  matrix.func_227860_a_();try{matrix.func_227861_a_(.5,.8,.5);matrix.func_227863_a_(mc.func_175598_ae().func_229098_b_());matrix.func_227862_a_(-.018f,-.018f,.018f);var text=new TranslationTextComponent(stored.isEmpty()?"backport.display.empty":"backport.display.invalid");mc.field_71466_p.func_243247_a(text,-mc.field_71466_p.func_238414_a_(text)/2f,0,0xffffff,false,matrix.func_227866_c_().func_227870_a_(),buffer,false,0x60000000,light);}finally{matrix.func_227865_b_();}
 }
}
