package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.client.gui.battles.ClientBattleManager;

/** Client state is scoped to the authoritative battle ID, never the generic RAID mode. */
public final class TeraRaidGimmicks {
    private TeraRaidGimmicks(){}
    public static boolean restricted(ClientBattleManager manager){return manager!=null && !manager.battleEnded &&
        ((TeraClient.snapshot!=null && TeraClient.snapshot.battle==manager.battleControllerIndex && TeraClient.snapshot.teraRaid)
        || (TeraRaidClient.status!=null && TeraRaidClient.status.battle==manager.battleControllerIndex));}
    public static void clear(ClientBattleManager manager){
        if(!blocked(manager,manager==null?null:manager.getCurrentPokemon()))return;
        manager.megaEvolving=false;manager.dynamaxing=false;manager.showZMoves=false;manager.dynamaxTurnsLeft=0;
    }
    public static boolean blocked(ClientBattleManager manager,com.pixelmonmod.pixelmon.client.gui.battles.PixelmonClientData pokemon){
        if(restricted(manager))return true;
        if(manager==null||manager.battleEnded||pokemon==null)return false;
        var snapshot=TeraClient.snapshot;
        if(snapshot!=null&&snapshot.battle==manager.battleControllerIndex&&snapshot.active.containsKey(pokemon.pokemonUUID))return true;
        var reply=TeraClient.battle;
        return reply!=null&&reply.battle==manager.battleControllerIndex&&reply.pokemon.equals(pokemon.pokemonUUID)&&reply.active;
    }
}
