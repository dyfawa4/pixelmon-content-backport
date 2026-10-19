package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.client.gui.battles.ClientBattleManager;

/** Client state is scoped to the authoritative battle ID, never the generic RAID mode. */
public final class TeraRaidGimmicks {
    private TeraRaidGimmicks(){}
    public static boolean restricted(ClientBattleManager manager){return manager!=null && !manager.battleEnded &&
        ((TeraClient.snapshot!=null && TeraClient.snapshot.battle==manager.battleControllerIndex && TeraClient.snapshot.teraRaid)
        || (TeraRaidClient.status!=null && TeraRaidClient.status.battle==manager.battleControllerIndex));}
    public static void clear(ClientBattleManager manager){
        if(!restricted(manager))return;
        manager.megaEvolving=false;manager.dynamaxing=false;manager.showZMoves=false;manager.dynamaxTurnsLeft=0;
    }
}
