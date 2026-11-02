package local.pixelmon.backport.mixin;
import org.spongepowered.asm.mixin.*;
/** Method monitor flags are applied by BackportMixinPlugin without replacing the optional API. */
@Pseudo @Mixin(targets="me.shedaniel.architectury.event.EventFactory$EventImpl",remap=false)
public abstract class ArchitecturyEventRegistrationMixin {}
