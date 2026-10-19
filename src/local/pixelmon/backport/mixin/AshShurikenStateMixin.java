package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import local.pixelmon.backport.AshShurikenState;
import org.spongepowered.asm.mixin.*;
@Mixin(value=Attack.class,remap=false)
public abstract class AshShurikenStateMixin implements AshShurikenState {
    @Unique private Integer backport$previousPP;
    public Integer backport$previousPP(){return backport$previousPP;}
    public void backport$previousPP(Integer value){backport$previousPP=value;}
}
