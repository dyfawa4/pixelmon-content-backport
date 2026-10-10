package local.pixelmon.backport.mixin;
import local.pixelmon.backport.PermanentAsh;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.attackModifiers.MultipleHit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=MultipleHit.class,remap=false)
public abstract class AshMultipleHitMixin {
    @ModifyArg(method="applyEffectDuring",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/species/Stats;isForm([Ljava/lang/String;)Z"),index=0)
    private String[] backport$waterShurikenHits(String[] forms){
        if(forms.length==1 && "ash".equals(forms[0]))return new String[]{"ash",PermanentAsh.FORM};
        return forms;
    }
}
