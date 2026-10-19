package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.stats.Moveset;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;

/** Native per-move PP override, never a change to the shared Water Shuriken definition. */
public final class AshShurikenPP {
    private AshShurikenPP(){}
    public static void apply(Moveset moves){if(moves!=null&&moves.pokemon!=null)apply(moves,moves.pokemon);}
    public static void apply(Moveset moves,Pokemon owner){
        if(moves==null||owner==null)return;
        boolean ash=NinjaTraining.eligible(owner);
        for(Attack a:moves.attacks){
            if(a==null||a.getActualMove()==null||!"Water Shuriken".equals(a.getActualMove().getAttackName()))continue;
            AshShurikenState state=(AshShurikenState)(Object)a;
            if(ash){
                int maximum=5+Math.max(0,Math.min(3,a.ppLevel));
                if(state.backport$previousPP()==null){
                    Integer previous=a.getOverriddenPPMax();
                    state.backport$previousPP(previous==null||previous==maximum?-1:previous);
                }
                a.overridePPMax(maximum);a.pp=Math.min(a.pp,maximum);
            }else if(state.backport$previousPP()!=null){a.overridePPMax(state.backport$previousPP());state.backport$previousPP(null);}
        }
    }
}
