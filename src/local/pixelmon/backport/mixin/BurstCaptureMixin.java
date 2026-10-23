package local.pixelmon.backport.mixin;

import local.pixelmon.backport.BurstCapture;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.catching.CaptureMethod;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import com.pixelmonmod.pixelmon.entities.pokeballs.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.spongepowered.asm.mixin.*;

@Mixin(value=CaptureMethod.class,remap=false)
public interface BurstCaptureMixin {
    /**
     * @author Content Backport
     * @reason A cap's preferred ball has a fixed multiplier; Heavy Ball's
     * additive catch-rate adjustment must not bypass that common rule.
     */
    @Overwrite
    default int calculateCatchRate(PokeBall ball,PokeBallEntity entity,ServerPlayerEntity player,Pokemon pokemon,PokeBallMode mode){
        int rate=pokemon.getForm().getCatchRate();
        return local.pixelmon.backport.CapBallPreference.preferred(ball,pokemon)?rate:ball.getLogic().modifyCaptureRate(ball,entity,pokemon,rate);
    }
    /**
     * @author Content Backport
     * @reason Mixin 0.8.4 forbids injectors in interfaces. Keep the native ball
     * logic call exactly, then add the current battle's bonus to its final value.
     */
    @Overwrite
    default double calculateBallBonus(PokeBall ball,PokeBallEntity entity,ServerPlayerEntity player,Pokemon pokemon,PokeBallMode mode){
        double nativeBonus=local.pixelmon.backport.CapBallPreference.preferred(ball,pokemon)||BurstCapture.forbidden(ball,pokemon)?0:ball.getLogic().getCatchBonus(ball,entity,player,pokemon,mode);
        return BurstCapture.bonus(player,pokemon,ball,nativeBonus);
    }
}
