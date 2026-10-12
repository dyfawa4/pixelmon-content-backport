package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.command.impl.CheckSpawnsCommand;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=CheckSpawnsCommand.class,remap=false)
public abstract class CheckSpawnsPermissionMixin {
    @ModifyArg(method="<init>",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/command/PixelCommand;<init>(Lcom/mojang/brigadier/CommandDispatcher;Ljava/lang/String;Ljava/lang/String;I)V"),index=3)
    private static int backport$ordinaryQuery(int level){return 0;}
    @Redirect(method="execute",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/command/PixelmonCommandUtils;hasPermission(Lnet/minecraft/command/CommandSource;Ljava/lang/String;)Z"))
    private boolean backport$spawnQuery(CommandSource source,String node){return true;}
}
