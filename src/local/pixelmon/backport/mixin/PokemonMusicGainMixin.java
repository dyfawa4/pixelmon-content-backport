package local.pixelmon.backport.mixin;
import net.minecraft.client.audio.SoundSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=SoundSource.class,remap=false)
public abstract class PokemonMusicGainMixin implements local.pixelmon.backport.PokemonMusic.Gain {
    @Unique private boolean backport$music;
    @Unique private float backport$rawGain;
    @ModifyVariable(method="func_216430_b",at=@At("HEAD"),argsOnly=true)
    private float backport$musicGain(float gain){
        backport$rawGain=gain;
        return backport$music?gain*local.pixelmon.backport.BattleMusicVolume.get():gain;
    }
    @Override public void backport$music(boolean music){backport$music=music;backport$rawGain=0;}
    @Override public void backport$refreshMusic(){((SoundSource)(Object)this).func_216430_b(backport$rawGain);}
}
