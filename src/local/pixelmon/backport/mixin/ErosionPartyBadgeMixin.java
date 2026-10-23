package local.pixelmon.backport.mixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import local.pixelmon.backport.ErosionClient;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
/** Attach to each native sprite, so every native party layout keeps its own positioning. */
@Mixin(targets={"com.pixelmonmod.pixelmon.api.screens.OverlayScreenState$1","com.pixelmonmod.pixelmon.api.screens.OverlayScreenState$2","com.pixelmonmod.pixelmon.api.screens.OverlayScreenState$3","com.pixelmonmod.pixelmon.api.screens.OverlayScreenState$4","com.pixelmonmod.pixelmon.api.screens.OverlayScreenState$5","com.pixelmonmod.pixelmon.api.screens.OverlayScreenState$6"},remap=false)
public abstract class ErosionPartyBadgeMixin {
 @Unique private ResourceLocation backport$sprite;@Unique private boolean backport$eroded;
 @Redirect(method="renderParty",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/Pokemon;getSprite()Lnet/minecraft/util/ResourceLocation;"))
 private ResourceLocation backport$sprite(Pokemon p){backport$eroded=ErosionClient.has(p);return backport$sprite=p.getSprite();}
 @Redirect(method="renderParty",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/client/gui/ScreenHelper;drawImageQuad(Lnet/minecraft/util/ResourceLocation;Lcom/mojang/blaze3d/matrix/MatrixStack;FFFFFFFFFFFFF)V"))
 private void backport$portrait(ResourceLocation image,MatrixStack m,float x,float y,float width,float height,float u,float v,float u2,float v2,float r,float g,float b,float a,float z){ScreenHelper.drawImageQuad(image,m,x,y,width,height,u,v,u2,v2,r,g,b,a,z);if(image.equals(backport$sprite)){if(backport$eroded)ErosionClient.icon(m,x+width-12,y+height-12,z+1,12);backport$sprite=null;}}
}
