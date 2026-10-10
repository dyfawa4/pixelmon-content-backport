package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.species.moves.Moves;
import com.pixelmonmod.pixelmon.api.config.PixelmonServerConfig;
import com.pixelmonmod.pixelmon.enums.technicalmoves.ITechnicalMove;
import local.pixelmon.backport.BackportTechnicalMoves;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=Moves.class,remap=false)
public abstract class MovesMixin {
    @Inject(method="canLearn(Lcom/pixelmonmod/pixelmon/enums/technicalmoves/ITechnicalMove;)Z",at=@At("HEAD"),cancellable=true)
    private void backport$tm(ITechnicalMove move,CallbackInfoReturnable<Boolean> ci){
        if(!(move instanceof BackportTechnicalMoves))return;
        Moves self=(Moves)(Object)this;
        ci.setReturnValue(PixelmonServerConfig.superUniversalTMs?self.canLearn(move.getAttack()):self.canLearnTMTRHM(move.getAttack()));
    }
}
