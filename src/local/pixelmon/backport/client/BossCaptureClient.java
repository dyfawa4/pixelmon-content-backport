package local.pixelmon.backport.client;

import com.pixelmonmod.pixelmon.api.battles.BagSection;
import com.pixelmonmod.pixelmon.api.battles.BattleMode;
import com.pixelmonmod.pixelmon.client.gui.battles.ClientBattleManager;
import com.pixelmonmod.pixelmon.battles.controller.participants.ParticipantType;
import net.minecraft.client.resources.I18n;

/** Preserve the native catchability decision when returning to an already-open ball list. */
public final class BossCaptureClient {
    private BossCaptureClient(){}
    /** Client setup and display packets arrive separately; never cache readiness. */
    public static boolean ready(ClientBattleManager battle){
        if(battle==null || battle.battleSetup==null || battle.battleSetup.length<=1 || battle.battleSetup[1]==null)return false;
        var opponents=battle.battleSetup[1];
        for(int i=0;i<opponents.length;i++){
            if(opponents[i]==null)return false;
            // Native canCatchOpponent only dereferences display slots for wild participants.
            if(opponents[i]==ParticipantType.WildPokemon && (battle.displayedEnemyPokemon==null
                || i>=battle.displayedEnemyPokemon.length || battle.displayedEnemyPokemon[i]==null
                || battle.displayedEnemyPokemon[i].bossTier==null))return false;
        }
        return true;
    }
    public static boolean blocked(ClientBattleManager battle){
        return battle!=null && battle.bagSection==BagSection.POKEBALLS && ready(battle) && !battle.canCatchOpponent();
    }
    public static void prepare(ClientBattleManager battle){
        if(battle!=null && battle.bagSection==BagSection.POKEBALLS && (!ready(battle)||blocked(battle)))battle.bagSection=null;
    }
    public static boolean click(ClientBattleManager battle){
        if(battle!=null && battle.bagSection==BagSection.POKEBALLS && !ready(battle)){
            battle.bagSection=null;return true; // Retry from bag categories after the display packet arrives.
        }
        if(!blocked(battle))return false;
        battle.bagSection=null;
        battle.addMessage(I18n.func_135052_a("gui.choosebag.nopokeballs"));
        battle.setMode(BattleMode.MAIN_MENU);return true;
    }
}
