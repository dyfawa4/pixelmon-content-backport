package local.pixelmon.backport.mixin;
import net.minecraft.inventory.container.RepairContainer;
import local.pixelmon.backport.BackpackAnvilIsolation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(RepairContainer.class)
public abstract class BackpackAnvilIsolationMixin {
 @Inject(method="func_82848_d",at=@At("RETURN"))
 private void backport$renameOnly(CallbackInfo ci){var c=(RepairContainer)(Object)this;var input=c.func_75139_a(0).func_75211_c();var output=c.func_75139_a(2).func_75211_c();if(BackpackAnvilIsolation.backpack(input)&&!output.func_190926_b()&&input.func_77973_b()==output.func_77973_b()&&c.func_75139_a(1).func_75211_c().func_190926_b())c.func_75139_a(2).func_75215_d(BackpackAnvilIsolation.renamed(input,output));}
}
