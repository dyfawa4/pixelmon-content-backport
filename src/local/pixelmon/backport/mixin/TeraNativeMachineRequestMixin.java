package local.pixelmon.backport.mixin;
import java.util.UUID;
import java.util.function.Predicate;
import com.pixelmonmod.pixelmon.api.pokemon.LearnMoveController;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.interactions.InteractionTechnicalMove;
import net.minecraft.entity.player.*;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value=InteractionTechnicalMove.class,remap=false)
public abstract class TeraNativeMachineRequestMixin {
 @Redirect(method="processInteract",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/LearnMoveController;sendLearnMove(Lnet/minecraft/entity/player/ServerPlayerEntity;Ljava/util/UUID;Lcom/pixelmonmod/pixelmon/battles/attacks/ImmutableAttack;Ljava/util/function/Predicate;)V"))
 private void backport$paid(ServerPlayerEntity p,UUID id,ImmutableAttack move,Predicate<ServerPlayerEntity> cost,PixelmonEntity entity,PlayerEntity actor,Hand hand,ItemStack stack){local.pixelmon.backport.TeraMachineLearning.sendMachine(p,id,move,cost,stack);}
 @Redirect(method="processInteract",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/LearnMoveController;sendLearnMove(Lnet/minecraft/entity/player/ServerPlayerEntity;Ljava/util/UUID;Lcom/pixelmonmod/pixelmon/battles/attacks/ImmutableAttack;)V"))
 private void backport$reusable(ServerPlayerEntity p,UUID id,ImmutableAttack move,PixelmonEntity entity,PlayerEntity actor,Hand hand,ItemStack stack){local.pixelmon.backport.TeraMachineLearning.sendMachine(p,id,move,null,stack);}
}
