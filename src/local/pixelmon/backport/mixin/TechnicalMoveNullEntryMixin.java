package local.pixelmon.backport.mixin;
import java.util.Set;
import com.pixelmonmod.pixelmon.enums.technicalmoves.ITechnicalMove;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="com.pixelmonmod.pixelmon.api.pokemon.species.moves.Moves",remap=false)
public abstract class TechnicalMoveNullEntryMixin {
    @Inject(method="isMoveInSet",at=@At("HEAD"),cancellable=true)
    private void backport$validMoves(Set<? extends ITechnicalMove> moves,String attack,CallbackInfoReturnable<Boolean> cir){
        if(moves!=null && attack!=null)for(var move:moves)if(move!=null && attack.equalsIgnoreCase(move.getAttackName())){cir.setReturnValue(true);return;}
        cir.setReturnValue(false);
    }
}
