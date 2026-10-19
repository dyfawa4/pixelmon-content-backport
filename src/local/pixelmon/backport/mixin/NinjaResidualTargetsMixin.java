package local.pixelmon.backport.mixin;
import local.pixelmon.backport.NinjaTraining;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(value={Sandstorm.class,Hail.class,GrassyTerrain.class},remap=false)
public abstract class NinjaResidualTargetsMixin {
    @Redirect(method="applyRepeatedEffect",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/controller/BattleController;getDefaultTurnOrder()Ljava/util/List;"))
    private java.util.List<PixelmonWrapper> backport$onlyActingNinja(BattleController b){PixelmonWrapper p=NinjaTraining.personalActor(b);return p==null?b.getDefaultTurnOrder():java.util.Collections.singletonList(p);}
}
