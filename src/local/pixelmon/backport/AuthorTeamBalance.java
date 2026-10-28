package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;

/** Draft the entire author team for safe switch-ins without sacrificing offensive roles. */
public final class AuthorTeamBalance {
    private AuthorTeamBalance(){}
    public static boolean disruption(Stats form){
        return form.getMoves().getAllMoves().stream().anyMatch(a->Set.of("Taunt","Encore").contains(a.getAttackName()));
    }
    public static boolean disruption(Pokemon pokemon){
        // Greninja can spend both utility slots on the team's Haze/Snatch role.
        if(pokemon.getSpecies().getDex()==658)return false;
        if(pokemon.getMoveset().stream().anyMatch(a->Set.of("Taunt","Encore").contains(a.getMove().getAttackName())))return true;
        long locked=pokemon.getMoveset().stream().filter(a->Set.of("Haze","Snatch").contains(a.getMove().getAttackName())).count();
        return locked<2&&disruption(pokemon.getForm());
    }
    private static double effectiveness(Stats form,String ability,Element attack){
        if(attack==Element.GROUND&&ability.equals("Levitate")
            ||attack==Element.ELECTRIC&&Set.of("VoltAbsorb","MotorDrive","LightningRod").contains(ability)
            ||attack==Element.WATER&&Set.of("WaterAbsorb","StormDrain","DrySkin").contains(ability)
            ||attack==Element.FIRE&&ability.equals("FlashFire")
            ||attack==Element.GRASS&&ability.equals("SapSipper"))return 0;
        // Primal weather disappears when its owner leaves; it is not a permanent switch-in immunity.
        return Element.getTotalEffectiveness(form.getTypes(),attack);
    }
    public static double[] defences(Pokemon pokemon){
        return defences(pokemon.getForm(),pokemon.getAbility()==null?"":pokemon.getAbility().getClass().getSimpleName());
    }
    private static double[] defences(Stats form,String ability){
        double[] values=new double[Tera.TYPES.length];
        for(int i=0;i<values.length;i++)values[i]=effectiveness(form,ability,Tera.TYPES[i]);
        return values;
    }
    static double[] defences(Stats form){
        // Do not assume a selectable immunity ability which training might not choose.
        var abilities=Arrays.stream(form.getAbilities().getAll()).filter(Objects::nonNull).map(a->a.getClass().getSimpleName()).distinct().toArray(String[]::new);
        return defences(form,abilities.length==1?abilities[0]:"");
    }
    static double coverage(List<double[]> members){
        double result=0;
        for(int i=0;i<Tera.TYPES.length;i++){
            if(Tera.TYPES[i]==Element.MYSTERY)continue;
            int weak=0,resist=0;double best=4;
            for(double[] member:members){best=Math.min(best,member[i]);if(member[i]>1)weak++;if(member[i]<1)resist++;}
            result+=best==0?22:best<1?16:best==1?0:-45;
            result+=Math.min(2,resist)*6;
            result-=weak*(resist==0?14:resist==1?4:1);
            if(weak>=2&&resist==0)result-=30;
        }
        return result;
    }
    public static double score(List<Pokemon> team,Stats candidate){
        List<double[]> members=new ArrayList<>();for(var p:team)members.add(defences(p));
        members.add(defences(candidate));return coverage(members);
    }
    public static double score(List<Pokemon> team,Pokemon candidate){
        List<double[]> members=new ArrayList<>();for(var p:team)members.add(defences(p));
        members.add(defences(candidate));return coverage(members);
    }
    public static double role(List<Pokemon> team,Stats candidate){
        boolean physical=false,special=false,recovery=false;
        for(var p:team){
            var stats=p.getForm().getBattleStats();
            physical|=stats.getStat(BattleStatsType.ATTACK)>stats.getStat(BattleStatsType.SPECIAL_ATTACK);
            special|=stats.getStat(BattleStatsType.SPECIAL_ATTACK)>=stats.getStat(BattleStatsType.ATTACK);
            recovery|=p.getMoveset().stream().anyMatch(a->NpcMoves.recovery(a));
        }
        var stats=candidate.getBattleStats();boolean attack=stats.getStat(BattleStatsType.ATTACK)>stats.getStat(BattleStatsType.SPECIAL_ATTACK);
        double result=(!physical&&attack||!special&&!attack)?25:0;
        boolean canRecover=candidate.getMoves().getAllMoves().stream().anyMatch(a->Set.of("Recover","Roost","Morning Sun","Moonlight","Oblivion Wing").contains(a.getAttackName()));
        if(!recovery&&canRecover)result+=18;
        return result;
    }
}
