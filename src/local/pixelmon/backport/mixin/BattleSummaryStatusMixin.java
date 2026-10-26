package local.pixelmon.backport.mixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.gui.pokechecker.PokeCheckerScreen;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import local.pixelmon.backport.BattleStatusIcons;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=PokeCheckerScreen.class,remap=false)
public abstract class BattleSummaryStatusMixin extends Screen {
 protected BattleSummaryStatusMixin(ITextComponent title){super(title);}
 @Shadow protected Pokemon pokemon;@Shadow protected int xSize;@Shadow protected int ySize;
 @Unique private boolean backport$primary(){return pokemon.getStatus().type!=com.pixelmonmod.pixelmon.battles.status.StatusType.None;}
 @Unique private float backport$size(){int count=BattleStatusIcons.count(pokemon);return count==0?24:Math.min(24,(84-(backport$primary()?26:0)-2*(count-1))/(float)count);}
 @Unique private float backport$left(){int count=BattleStatusIcons.count(pokemon);float width=(backport$primary()?26:0)+count*backport$size()+Math.max(0,count-1)*2;return (field_230708_k_-xSize)/2f-36+(84-width)/2;}
 @Redirect(method="drawGuiContainerBackgroundLayer",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/client/gui/ScreenHelper;simpleDrawImageQuad(Lcom/mojang/blaze3d/matrix/MatrixStack;FFFFFFFFF)V"),require=2,allow=2)
 private void backport$nativeStatus(MatrixStack m,float x,float y,float width,float height,float u,float v,float u2,float v2,float z){
  if(BattleStatusIcons.has(pokemon)){if(!backport$primary())return;x=backport$left();}
  com.pixelmonmod.pixelmon.client.gui.ScreenHelper.simpleDrawImageQuad(m,x,y,width,height,u,v,u2,v2,z);
 }
 @Inject(method="drawGuiContainerBackgroundLayer",at=@At("RETURN"))
 private void backport$summary(MatrixStack m,float partial,int mx,int my,CallbackInfo ci){if(BattleStatusIcons.has(pokemon)){float size=backport$size();BattleStatusIcons.row(m,pokemon,backport$left()+(backport$primary()?26:0),(field_230709_l_-ySize)/2f+130+(24-size)/2,1,size);}}
}
