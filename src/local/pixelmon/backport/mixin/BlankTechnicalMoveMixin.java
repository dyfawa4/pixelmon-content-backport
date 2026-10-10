package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.enums.TMType;
import com.pixelmonmod.pixelmon.enums.technicalmoves.ITechnicalMove;
import com.pixelmonmod.pixelmon.items.heldItems.BlankTechnicalMachineItem;
import local.pixelmon.backport.BackportTechnicalMoves;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=BlankTechnicalMachineItem.class,remap=false)
public abstract class BlankTechnicalMoveMixin {
    @Redirect(method="onAttackUsed",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/enums/technicalmoves/ITechnicalMove;getMoveFor(Lcom/pixelmonmod/pixelmon/enums/TMType;Ljava/lang/String;)Lcom/pixelmonmod/pixelmon/enums/technicalmoves/ITechnicalMove;"))
    private ITechnicalMove backport$imprint(TMType type,String name){return BackportTechnicalMoves.resolve(type,name);}
    @Redirect(method="func_77624_a",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/enums/technicalmoves/ITechnicalMove;getMoveFor(Lcom/pixelmonmod/pixelmon/enums/TMType;I)Lcom/pixelmonmod/pixelmon/enums/technicalmoves/ITechnicalMove;"))
    private ITechnicalMove backport$tooltip(TMType type,int id){return BackportTechnicalMoves.resolve(type,id);}
}
