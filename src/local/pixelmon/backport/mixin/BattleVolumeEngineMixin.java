package local.pixelmon.backport.mixin;
import java.util.Map;
import java.util.function.Consumer;
import local.pixelmon.backport.*;
import net.minecraft.client.audio.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=SoundEngine.class,remap=false)
public abstract class BattleVolumeEngineMixin implements BattleMusicVolume.Refresh {
    @Shadow private Map<ISound,ChannelManager.Entry> field_217942_m;
    @Inject(method="<init>",at=@At("RETURN"))
    private void backport$bind(CallbackInfo ci){BattleMusicVolume.engine(this);}
    @Override public void backport$refreshBattleVolume(){
        // Also refresh while a single-player pause menu has stopped sound ticks.
        field_217942_m.forEach((sound,channel)->{if(PokemonMusic.isMusic(sound)){
            channel.func_217888_a(source->((PokemonMusic.Gain)source).backport$refreshMusic());
        }});
    }
    @Redirect(method="func_148611_c",at=@At(value="INVOKE",target="Lnet/minecraft/client/audio/ChannelManager$Entry;func_217888_a(Ljava/util/function/Consumer;)V"))
    private void backport$bindMusic(ChannelManager.Entry channel,Consumer<SoundSource> setup,ISound sound){
        // Every source is reset on reuse, including non-music. Native fades still
        // set raw gain; the source applies our slider after each fade adjustment.
        channel.func_217888_a(source->{((PokemonMusic.Gain)source).backport$music(PokemonMusic.isMusic(sound));setup.accept(source);});
    }
}
