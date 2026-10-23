package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.PowerConstruct;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=PowerConstruct.class,remap=false)
public abstract class PowerConstructAdvancementMixin {
    @Redirect(method="applyRepeatedEffectAfterStatus",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;setForm(Ljava/lang/String;)V"))
    private void backport$actualConstruct(PixelmonWrapper pokemon,String form){
        boolean before=pokemon.getForm().isForm("complete");pokemon.setForm(form);
        if(!before&&pokemon.getForm().isForm("complete"))local.pixelmon.backport.ZygardeAdvancements.powerConstruct(pokemon);
    }
}
