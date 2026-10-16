package local.pixelmon.backport.client;

import com.pixelmonmod.pixelmon.api.battles.BagSection;
import com.pixelmonmod.pixelmon.api.battles.BattleMode;
import com.pixelmonmod.pixelmon.client.gui.battles.ClientBattleManager;
import net.minecraft.client.resources.I18n;

/** Preserve the native catchability decision when returning to an already-open ball list. */
public final class BossCaptureClient {
    private BossCaptureClient(){}
    public static boolean blocked(ClientBattleManager battle){
        return battle!=null && battle.bagSection==BagSection.POKEBALLS && !battle.canCatchOpponent();
    }
    public static void prepare(ClientBattleManager battle){if(blocked(battle))battle.bagSection=null;}
    public static boolean click(ClientBattleManager battle){
        if(!blocked(battle))return false;
        battle.bagSection=null;
        battle.addMessage(I18n.func_135052_a("gui.choosebag.nopokeballs"));
        battle.setMode(BattleMode.MAIN_MENU);return true;
    }
}
