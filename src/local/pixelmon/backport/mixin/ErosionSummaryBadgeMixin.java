package local.pixelmon.backport.mixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.gui.pokechecker.PokeCheckerScreen;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.ITextComponent;
import local.pixelmon.backport.ErosionClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=PokeCheckerScreen.class,remap=false)
public abstract class ErosionSummaryBadgeMixin extends Screen {
 protected ErosionSummaryBadgeMixin(ITextComponent title){super(title);}
 @Shadow protected Pokemon pokemon;@Shadow protected int xSize;@Shadow protected int ySize;
 @ModifyArg(method="drawGuiContainerBackgroundLayer",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/client/gui/ScreenHelper;simpleDrawImageQuad(Lcom/mojang/blaze3d/matrix/MatrixStack;FFFFFFFFF)V"),index=1,require=2,allow=2)
 private float backport$centerPair(float x){return ErosionClient.has(pokemon)&&pokemon.getStatus().type!=com.pixelmonmod.pixelmon.battles.status.StatusType.None?x-14:x;}
 @Inject(method="drawGuiContainerBackgroundLayer",at=@At("RETURN"))
 private void backport$summary(MatrixStack m,float partial,int mx,int my,CallbackInfo ci){if(ErosionClient.has(pokemon)){boolean other=pokemon.getStatus().type!=com.pixelmonmod.pixelmon.battles.status.StatusType.None;ErosionClient.icon(m,(field_230708_k_-xSize)/2f+(other?10:-6),(field_230709_l_-ySize)/2f+130,1,24);}}
}
