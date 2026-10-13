package local.pixelmon.backport.mixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.battles.raids.RaidData;
import com.pixelmonmod.pixelmon.items.*;
import java.util.List;
import local.pixelmon.backport.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="com.pixelmonmod.pixelmon.client.gui.raids.RaidCatchScreen",remap=false)
public abstract class BurstRaidDisplayMixin {
    @Shadow private RaidData raid;
    @Shadow private List<ItemData> balls;
    @Shadow private int ballIndex;
    @Shadow private boolean canCatch;
    @Shadow private boolean inCatchAnimation;
    @Shadow private boolean closing;
    @Inject(method="func_230430_a_",at=@At("RETURN"))
    private void backport$ballFrame(MatrixStack matrix,int mouseX,int mouseY,float partial,CallbackInfo ci){
        var state=TeraClient.raidBurst;
        if(state==null || raid==null || !state.raid.equals(raid.uuid) || !canCatch || inCatchAnimation || closing || balls==null || ballIndex<0 || ballIndex>=balls.size())return;
        var ball=PokeBallPart.getPokeBall(balls.get(ballIndex).getItemStack());if(!ball.isPresent() || !state.balls.contains(ball.get().getName()))return;
        var screen=(net.minecraft.client.gui.screen.Screen)(Object)this;
        // Native screen translates to (width*.525,height*.7675), then draws
        // a 16px ball at scale width/320. Enclose that entire item, at any GUI scale.
        int x=(int)Math.floor(screen.field_230708_k_*.525)-3,y=(int)Math.floor(screen.field_230709_l_*.7675)-3;
        int size=(int)Math.ceil(screen.field_230708_k_*.05)+6;BurstClient.outline(matrix,x,y,size,size);
    }
    @Inject(method="func_231164_f_",at=@At("RETURN"))
    private void backport$closed(CallbackInfo ci){if(TeraClient.raidBurst!=null && raid!=null && TeraClient.raidBurst.raid.equals(raid.uuid))TeraClient.raidBurst=null;}
}
