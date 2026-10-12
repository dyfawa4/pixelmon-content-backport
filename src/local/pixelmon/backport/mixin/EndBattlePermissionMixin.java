package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.command.impl.EndBattleCommand;
import local.pixelmon.backport.UtilityCommandPolicy;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=EndBattleCommand.class,remap=false)
public abstract class EndBattlePermissionMixin {
    @ModifyArg(method="<init>",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/command/PixelCommand;<init>(Lcom/mojang/brigadier/CommandDispatcher;Ljava/lang/String;Ljava/lang/String;I)V"),index=3)
    private static int backport$ordinarySelfHelp(int level){return 0;}
    @Inject(method="execute",at=@At("HEAD"),cancellable=true)
    private void backport$ownTarget(CommandSource source,String[] args,CallbackInfo ci){
        if(source.func_197034_c(2))return;
        String name=source.func_197022_f() instanceof ServerPlayerEntity?source.func_197037_c():null;
        if(!UtilityCommandPolicy.ownBattle(name,args)){
            source.func_197021_a(new TranslationTextComponent("pixelmon.backport.command.endbattle.self"));ci.cancel();
        }
    }
}
