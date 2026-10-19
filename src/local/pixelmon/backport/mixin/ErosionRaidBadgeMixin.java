package local.pixelmon.backport.mixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.gui.battles.PixelmonClientData;
import com.pixelmonmod.pixelmon.client.gui.battles.pokemonOverlays.OpponentElement;
import com.pixelmonmod.pixelmon.client.gui.battles.pokemonOverlays.RaidElement;
import com.pixelmonmod.pixelmon.client.gui.ScreenParticleEngine;
import net.minecraft.client.gui.screen.Screen;
import local.pixelmon.backport.ErosionClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=RaidElement.class,remap=false)
public abstract class ErosionRaidBadgeMixin extends OpponentElement {
    private ErosionRaidBadgeMixin(PixelmonClientData p,Screen s,ScreenParticleEngine e){super(p,s,e);}
    @Inject(method="drawElement",at=@At("RETURN"))
    private void backport$erosionRaidBadge(MatrixStack m,float scale,CallbackInfo ci){ErosionClient.opponent(m,getEnemy(),x,y,zLevel,true);}
}
