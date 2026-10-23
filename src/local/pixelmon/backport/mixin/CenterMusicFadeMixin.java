package local.pixelmon.backport.mixin;
import local.pixelmon.backport.CenterMusic;
import net.minecraft.client.audio.SimpleSound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Keep the native restart/battle/cleanup logic, but use a half-second center fade. */
@Mixin(targets="com.pixelmonmod.pixelmon.blocks.tileentity.MusicBlockTileEntity$BlockSoundRecord",remap=false)
public abstract class CenterMusicFadeMixin {
 @ModifyConstant(method="func_73660_a",constant=@Constant(floatValue=100f))
 private float backport$centerFade(float original){return CenterMusic.centerTrack(((SimpleSound)(Object)this).func_147650_b())?10f:original;}
 @ModifyConstant(method="func_147667_k",constant=@Constant(intValue=100))
 private int backport$centerStop(int original){return CenterMusic.centerTrack(((SimpleSound)(Object)this).func_147650_b())?10:original;}
}
