package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.client.ClientProxy;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
/** Display-only reads from the authenticated current-battle snapshot. */
public final class AuthorChallengeClient {
 private AuthorChallengeClient(){}
 public static boolean active(){var bm=ClientProxy.battleManager;var state=TeraClient.snapshot;return bm!=null&&!bm.battleEnded&&!bm.showZMoves&&state!=null&&state.battle==bm.battleControllerIndex&&state.author;}
 public static int accuracy(Attack attack,int original){return original;}
 public static boolean success(Attack attack){return false;}
 public static String label(Attack attack,String key,Object[] args){return net.minecraft.client.resources.I18n.func_135052_a(key,args);}
}
