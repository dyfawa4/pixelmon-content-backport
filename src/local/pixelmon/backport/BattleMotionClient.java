package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.client.ClientProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
public final class BattleMotionClient {
    private BattleMotionClient(){}
    public static boolean frozen(PlayerEntity player){
        var battle=ClientProxy.battleManager;
        return player==Minecraft.func_71410_x().field_71439_g && battle!=null && BattleMotion.eligible(true,true,battle.battleEnded,false,battle.isSpectating);
    }
}
