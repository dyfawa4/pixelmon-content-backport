package local.pixelmon.backport.mixin;

import com.pixelmonmod.pixelmon.api.moveskills.MoveSkill;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.moveSkills.WeatherChanger;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Commit accepted weather skills with coherent timers, as native Kyogre already does. */
@Mixin(value=WeatherChanger.class,remap=false)
public abstract class FieldWeatherTimerMixin {
    @org.spongepowered.asm.mixin.Shadow public static boolean allowMultiplayerWeather;
    @Inject(method="<clinit>",at=@At("TAIL"))
    private static void backport$multiplayerWeather(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){
        allowMultiplayerWeather=true;
    }
    @Inject(method="lambda$createBaseMoveSkill$0",at=@At("RETURN"))
    private static void backport$weatherTimers(boolean rain,MoveSkill skill,PixelmonEntity pokemon,CallbackInfoReturnable<Integer> cir){
        // Native roof, already-weather and multiplayer denials return -1.
        // Preserve them, their messages, PP handling and cooldown handling.
        if(cir.getReturnValue()==null || cir.getReturnValue()<=0 || !(pokemon.field_70170_p instanceof ServerWorld))return;
        if(rain && pokemon.getPokemon().getSpecies().is(PixelmonSpecies.KYOGRE))return;
        // Same five-minute weather interval used by the native Kyogre branch.
        ((ServerWorld)pokemon.field_70170_p).func_241113_a_(rain?0:6000,rain?6000:0,rain,false);
    }
}
