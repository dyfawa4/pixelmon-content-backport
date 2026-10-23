package local.pixelmon.backport.mixin;
import local.pixelmon.backport.*;
import net.minecraft.command.CommandSource;
import net.minecraft.command.impl.LocateCommand;
import net.minecraft.world.gen.feature.structure.Structure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=LocateCommand.class,remap=false)
public abstract class AuthorLocateMixin {
    @Inject(method="func_241053_a_",at=@At("HEAD"),cancellable=true)
    private static void backport$author(CommandSource source,Structure<?> structure,CallbackInfoReturnable<Integer> cir){
        if(structure instanceof AuthorBossLocator.RoomAlias)cir.setReturnValue(AuthorLocateCommands.begin(source,(AuthorBossLocator.RoomAlias)structure));
        else if(structure instanceof GymLocator.TypedGym)cir.setReturnValue(GymLocator.begin(source,((GymLocator.TypedGym)structure).type,32768));
        else if(structure==GymLocator.nativeGym())cir.setReturnValue(GymLocator.begin(source,"any",32768));
    }
}
