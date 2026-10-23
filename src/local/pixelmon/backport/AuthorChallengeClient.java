package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.client.ClientProxy;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
/** Display-only reads from the authenticated current-battle snapshot. */
public final class AuthorChallengeClient {
 private AuthorChallengeClient(){}
 public static boolean active(){var bm=ClientProxy.battleManager;var state=TeraClient.snapshot;return bm!=null&&!bm.battleEnded&&!bm.showZMoves&&state!=null&&state.battle==bm.battleControllerIndex&&state.author;}
 public static int accuracy(Attack attack,int original){if(!active()||success(attack))return original;return Math.max(0,(original<0?100:original)-20);}
 public static boolean success(Attack attack){return attack!=null&&(AuthorChallenge.protect(attack.getMove())||attack.getAttackCategory()==AttackCategory.STATUS&&AuthorChallenge.selfBuff(attack.getMove()));}
 public static String label(Attack attack,String key,Object[] args){
  if(!active()||!key.equals("gui.battle.accuracyval")||!success(attack))return net.minecraft.client.resources.I18n.func_135052_a(key,args);
  if(AuthorChallenge.protect(attack.getMove())){var bm=ClientProxy.battleManager;var current=bm.getCurrentPokemon();int chain=current==null?0:TeraClient.snapshot.protects.getOrDefault(current.pokemonUUID,0);double percent=80/Math.pow(3,chain);String number=chain==0?"80":String.format(java.util.Locale.ROOT,"%.2f",percent);return net.minecraft.client.resources.I18n.func_135052_a("pixelmon.backport.author.protect_rate",number);}
  return net.minecraft.client.resources.I18n.func_135052_a("pixelmon.backport.author.buff_rate");
 }
}
