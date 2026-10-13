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
     * @reason Mixin 0.8.4 forbids injectors in interfaces. Keep the native ball
     * logic call exactly, then add the current battle's bonus to its final value.
     */
    @Overwrite
    default double calculateBallBonus(PokeBall ball,PokeBallEntity entity,ServerPlayerEntity player,Pokemon pokemon,PokeBallMode mode){
        double nativeBonus=ball.getLogic().getCatchBonus(ball,entity,player,pokemon,mode);
        return BurstCapture.bonus(player,pokemon,ball,nativeBonus);
    }
}
