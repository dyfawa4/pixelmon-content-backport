package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.client.ClientProxy;
import com.pixelmonmod.pixelmon.client.gui.battles.*;

/** Display calculation only: never edits shared species or server damage. */
public final class BattleInfoCompat {
    private BattleInfoCompat(){}
    private static TeraNetwork.Snapshot state(){ClientBattleManager b=ClientProxy.battleManager;TeraNetwork.Snapshot s=TeraClient.snapshot;return b!=null && s!=null && b.battleControllerIndex==s.battle?s:null;}
    public static List<Element> defense(PixelmonClientData p){
        if(p==null)return Collections.emptyList();TeraNetwork.Snapshot s=state();
        return s!=null && s.defense.containsKey(p.pokemonUUID)?s.defense.get(p.pokemonUUID):p.getBaseStats().getTypes();
    }
    private static Integer teraType(PixelmonClientData user){
        ClientBattleManager bm=ClientProxy.battleManager;
        if(bm==null || bm.showZMoves || bm.dynamaxing || bm.dynamaxTurnsLeft>0 || bm.megaEvolving)return null;
        TeraNetwork.Snapshot s=state();Integer type=user!=null && s!=null?s.active.get(user.pokemonUUID):null;
        TeraNetwork.Reply r=TeraClient.battle;
        if(type==null && user!=null && r!=null && r.battle==bm.battleControllerIndex && r.pokemon.equals(user.pokemonUUID) && (r.selected || r.active))type=r.type;
        return type!=null && type>=0 && type<Tera.TYPES.length?type:null;
    }
    private static boolean teraMove(Attack attack,PixelmonClientData user){
        if(attack==null || user==null || teraType(user)==null)return false;
        String name=attack.getMove().getAttackName();
        return name.equals("Tera Blast") || name.equals("Tera Starstorm") && user.species.getDex()==1024 && Tera.TYPES[teraType(user)]==Element.MYSTERY;
    }
    public static Element moveType(Attack attack,PixelmonClientData user){
        if(attack==null)return Element.NORMAL;
        String name=attack.getMove().getAttackName();
        if(teraMove(attack,user))return Tera.TYPES[teraType(user)];
        if(name.equals("Ivy Cudgel") && user!=null && user.species.getDex()==1017){String f=user.form;return f.contains("wellspring")?Element.WATER:f.contains("hearthflame")?Element.FIRE:f.contains("cornerstone")?Element.ROCK:Element.GRASS;}
        return attack.getType();
    }
    public static int movePower(Attack attack,PixelmonClientData user,int nativePower){
        // The battle sidebar reads ImmutableAttack's shared base power (15),
        // unlike Battle Bond's server calculation. Show per-hit form power;
        // the temporary +50% boost remains a separate battle message.
        ClientBattleManager bm=ClientProxy.battleManager;
        if(attack!=null && user!=null && user.species!=null && user.species.getDex()==658
            && ("ash".equals(user.form)||"ash_permanent".equals(user.form))
            && "Water Shuriken".equals(attack.getMove().getAttackName())
            && user.dynamax==0 && (bm==null||!bm.showZMoves&&!bm.dynamaxing&&bm.dynamaxTurnsLeft<=0))return 20;
        return teraMove(attack,user) && attack.getMove().getAttackName().equals("Tera Blast") && Tera.TYPES[teraType(user)]==Element.MYSTERY?100:nativePower;
    }
    public static AttackCategory moveCategory(Attack attack,PixelmonClientData user){
        if(!teraMove(attack,user))return attack.getAttackCategory();
        TeraNetwork.Snapshot s=state();Integer flags=s!=null?s.attackStats.get(user.pokemonUUID):null;
        if(flags==null)return attack.getAttackCategory();
        int bit=attack.getMove().getAttackName().equals("Tera Starstorm")?2:1;
        return (flags&bit)!=0?AttackCategory.PHYSICAL:AttackCategory.SPECIAL;
    }
    public static double multiplier(Attack attack,PixelmonClientData user,PixelmonClientData target,boolean inverse){
        if(target==null || attack==null || attack.getAttackCategory()==AttackCategory.STATUS)return 1;
        Element type=moveType(attack,user);TeraNetwork.Snapshot s=state();
        if(type==Element.MYSTERY)return s!=null && s.active.containsKey(target.pokemonUUID)?2:1;
        return Element.getTotalEffectiveness(defense(target),type,inverse);
    }
}
