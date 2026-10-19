package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.battles.attack.AttackRegistry;
import com.pixelmonmod.pixelmon.battles.attacks.*;
import com.pixelmonmod.pixelmon.battles.controller.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.api.util.helpers.NetworkHelper;
import com.pixelmonmod.pixelmon.comm.packetHandlers.battles.BackToMainMenuPacket;

/** One native action slot, inserted before the controller advances to its next actor. */
public final class NinjaTraining {
    public static final String MOVE="Ninja Training";
    private static final Set<PixelmonWrapper> GRANTED=Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<BattleController,PixelmonWrapper> BONUS=new WeakHashMap<>();
    private static final Set<BattleController> PAUSE=Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<PixelmonWrapper,List<Attack>> BLOCKED=new WeakHashMap<>();
    private static PixelmonWrapper ticking;
    private NinjaTraining(){}
    public static boolean eligible(Pokemon p){return p!=null&&p.getSpecies().getDex()==658&&PermanentAsh.isAsh(p.getForm());}
    public static boolean move(Attack a){return a!=null&&MOVE.equals(a.getMove().getAttackName());}
    public static boolean bonus(PixelmonWrapper p){return p!=null&&BONUS.get(p.bc)==p;}
    public static boolean allowed(PixelmonWrapper p){return p!=null&&eligible(p.pokemon)&&!bonus(p);}
    public static void grant(PixelmonWrapper p){if(!p.bc.simulateMode&&!Erosion.isCopy(p)&&allowed(p)&&p.isAlive())GRANTED.add(p);}
    public static PixelmonWrapper bonus(BattleController b){return BONUS.get(b);}
    public static boolean personalTick(PixelmonWrapper p){return ticking==p;}
    public static PixelmonWrapper personalActor(BattleController b){return ticking!=null&&ticking.bc==b?ticking:null;}
    public static boolean evolve(BattleController b){PixelmonWrapper p=BONUS.get(b);if(p==null||p.wait||!p.willEvolve||b.isWaiting())return false;p.willEvolve=false;return p.megaEvolve();}
    public static boolean consumePause(BattleController b){return PAUSE.remove(b);}
    public static void chooseAgain(PixelmonWrapper p){
        p.wait=true;
        if(p.getParticipant() instanceof PlayerParticipant)NetworkHelper.sendPacket(new BackToMainMenuPacket(Collections.singletonList(false),false,Collections.singletonList(p.getPokemonUUID())),((PlayerParticipant)p.getParticipant()).player);
    }
    /** Called immediately after native takeTurn, before ++turn or repeated effects. */
    public static boolean afterAction(BattleController b,PixelmonWrapper p){
        if(b.simulateMode)return false;
        if(BONUS.get(b)==p){
            BONUS.remove(b);GRANTED.remove(p);
            unblock(p);if(!b.battleEnded)ErosionNetwork.sync(b,null);
            return false;
        }
        if(!GRANTED.remove(p)||!p.isAlive()||!p.onBattlefield||b.battleEnded)return false;
        // Finish this actor's first turn before accepting the second action.
        // New effects/Dynamax activated in the extra action then lose only one
        // turn at the ordinary global boundary, rather than being charged twice.
        ticking=p;
        try{NinjaResiduals.apply(p);p.turnTick();p.endTurn();}finally{ticking=null;}
        AshWaterBoost.extraTurn(p);Erosion.extraTurn(p);
        if(p.isDynamax>0&&--p.dynamaxTurns<=0)p.dynamax(true,p.getHealthPercent());
        if(!p.isAlive()||b.battleEnded)return false;
        BONUS.put(b,p);p.takenTurn=false;p.willEvolve=false;p.usingZ=false;
        List<Attack> blocked=new ArrayList<>();for(Attack a:p.getMoveset())if(move(a)&&!a.getDisabled()){blocked.add(a);a.setDisabled(true,p);}BLOCKED.put(p,blocked);
        PAUSE.add(b);
        p.willUseItemInStack=null;p.willTryFlee=false;
        b.sendToAll("pixelmon_backport.battle.ninja_bonus",p.getNickname());
        ErosionNetwork.sync(b,null);
        if(p.getParticipant() instanceof PlayerParticipant)chooseAgain(p);
        else {
            // Never ask the AI to select the forbidden setup move recursively.
            Attack choice=null;for(Attack a:p.getMoveset())if(a!=null&&a.pp>0&&!a.getDisabled()&&!move(a)&&
                (choice==null||a.getMove().getBasePower()>choice.getMove().getBasePower()))choice=a;
            if(choice==null)choice=new Attack(AttackRegistry.STRUGGLE);
            p.setAttack(choice,p.getTargets(choice),false);
        }
        return true;
    }
    private static void unblock(PixelmonWrapper p){List<Attack> rows=BLOCKED.remove(p);if(rows!=null)for(Attack a:rows)a.setDisabled(false,p);}
    public static void end(BattleController b){BONUS.remove(b);PAUSE.remove(b);GRANTED.removeIf(p->p.bc==b);for(PixelmonWrapper p:new ArrayList<>(BLOCKED.keySet()))if(p.bc==b)unblock(p);}
}
