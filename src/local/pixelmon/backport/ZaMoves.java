package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.Moveset;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;

/** Reference ZA Mega Zygarde's move change, scoped to this battle wrapper.
 * Never writes a temporary special move into permanent party/save moves. */
public final class ZaMoves {
    private ZaMoves(){}
    static final Map<PixelmonWrapper,Change> CHANGES=new WeakHashMap<>();
    static class Change {Moveset previous;Attack original,replacement;int initialPP;}
    public static void mega(PixelmonWrapper p){
        if(p.bc.simulateMode || p.getSpecies().getDex()!=718 || !p.getForm().isForm("mega") || CHANGES.containsKey(p))return;
        Moveset moves=p.getMoveset();
        for(int i=0;i<moves.size();i++)if(moves.get(i).getMove().getAttackName().equals("Core Enforcer")){
            Change c=new Change();c.previous=p.temporaryMoveset;c.original=moves.get(i);c.replacement=new Attack("Nihil Light");c.replacement.ppLevel=c.original.ppLevel;c.replacement.pp=c.original.pp;c.initialPP=c.replacement.pp;
            Moveset temporary=new Moveset();temporary.addAll(moves);temporary.set(i,c.replacement);CHANGES.put(p,c);p.setTemporaryMoveset(temporary);
            if(p.attack==c.original)p.attack=c.replacement;
            return;
        }
    }
    public static void end(PixelmonWrapper p){Change c=CHANGES.remove(p);if(c==null)return;
        c.original.pp=Math.max(0,c.original.pp-Math.max(0,c.initialPP-c.replacement.pp));p.setTemporaryMoveset(c.previous);
    }
}
