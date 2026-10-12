package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import local.pixelmon.backport.LegendAdvancements;
import java.util.ArrayDeque;
import java.util.Deque;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe completed native changes; construction, client copies and battle simulations do not count. */
@Mixin(value=Pokemon.class,remap=false)
public abstract class FormAdvancementMixin {
    @Unique private Deque<String> backport$formHistory;
    @Inject(method="setForm(Lcom/pixelmonmod/pixelmon/api/pokemon/species/Stats;)V",at=@At("HEAD"))
    private void backport$beforeForm(Stats form,CallbackInfo ci){
        Pokemon pokemon=(Pokemon)(Object)this;
        if(backport$formHistory==null)backport$formHistory=new ArrayDeque<>();
        backport$formHistory.push(pokemon.getForm()==null?"":pokemon.getForm().getName());
    }
    @Inject(method="setForm(Lcom/pixelmonmod/pixelmon/api/pokemon/species/Stats;)V",at=@At("RETURN"))
    private void backport$afterForm(Stats form,CallbackInfo ci){
        LegendAdvancements.formChanged((Pokemon)(Object)this,backport$formHistory.pop());
    }
}
