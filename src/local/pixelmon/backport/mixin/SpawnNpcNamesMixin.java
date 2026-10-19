package local.pixelmon.backport.mixin;
import local.pixelmon.backport.SpawnNpcNames;
import net.minecraft.util.text.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(targets="com.pixelmonmod.pixelmon.api.spawning.archetypes.algorithms.checkspawns.PlayerTrackingCheckSpawns",remap=false)
public abstract class SpawnNpcNamesMixin {
    @ModifyArg(method="*",at=@At(value="INVOKE",target="Lnet/minecraft/command/CommandSource;func_197030_a(Lnet/minecraft/util/text/ITextComponent;Z)V"),index=0)
    private ITextComponent backport$npcNames(ITextComponent original){
        String text=original.getString(),translated=SpawnNpcNames.display(text);
        return text.equals(translated)?original:new StringTextComponent(translated);
    }
}
