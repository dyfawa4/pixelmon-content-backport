package local.pixelmon.backport.mixin;
import java.util.UUID;
import com.pixelmonmod.pixelmon.api.pokemon.LearnMoveController;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.IEventBus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=LearnMoveController.class,remap=false)
public abstract class TeraMachineCheckMixin {
 @Redirect(method="canLearnMove",at=@At(value="INVOKE",target="Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z"))
 private static boolean backport$machineCheck(IEventBus bus,Event event,ServerPlayerEntity player,UUID id,ImmutableAttack move){return local.pixelmon.backport.TeraMachineLearning.postCheck(bus,event,player,id,move);}
}
