package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.ability.Ability;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.Transformed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=Transformed.class,remap=false)
public abstract class AbilityShieldTransformMixin {
    @Redirect(method="<init>(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;)V",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;setTempAbility(Lcom/pixelmonmod/pixelmon/api/pokemon/ability/Ability;)V"),require=1)
    private void backport$transform(PixelmonWrapper mon,Ability next){mon.setTempAbility(next,true);}
}
