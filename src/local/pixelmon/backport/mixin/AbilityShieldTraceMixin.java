package local.pixelmon.backport.mixin;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Trace;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.AbilityShield;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Trace blocked at entry must not retry just because the shield is removed later. */
@Mixin(value=Trace.class,remap=false)
public abstract class AbilityShieldTraceMixin {
    @Unique private final Set<PixelmonWrapper> backport$blocked=Collections.newSetFromMap(new WeakHashMap<>());
    @Inject(method="applySwitchInEffect",at=@At("HEAD"))
    private void backport$newEntry(PixelmonWrapper mon,CallbackInfo ci){backport$blocked.remove(mon);}
    @Inject(method="traceAbility",at=@At("HEAD"),cancellable=true)
    private void backport$trace(PixelmonWrapper mon,CallbackInfo ci){if(AbilityShield.active(mon)){backport$blocked.add(mon);AbilityShield.blocked(mon);}if(backport$blocked.contains(mon))ci.cancel();}
}
