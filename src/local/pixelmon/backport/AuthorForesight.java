package local.pixelmon.backport;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;

/** A reserved roll is shared by planning and execution, while live conditions still set the threshold. */
public final class AuthorForesight {
    private static final Map<BattleController,Frame> FRAMES=new WeakHashMap<>();
    private static final class Frame {
        final int turn;final Map<String,Integer> rolls=new HashMap<>();final Map<UUID,Integer> moves=new HashMap<>();final Set<UUID> executing=new HashSet<>();
        Frame(int turn){this.turn=turn;}
    }
    private AuthorForesight(){}
    public static boolean author(BattleController b){return b!=null&&b.participants.stream().anyMatch(p->p instanceof TrainerParticipant&&AuthorBossTeams.is(((TrainerParticipant)p).trainer));}
    public static boolean player(PixelmonWrapper p){return p!=null&&p.bc!=null&&p.getParticipant() instanceof PlayerParticipant&&author(p.bc);}
    public static boolean forecast(PixelmonWrapper p){
        if(p==null||p.bc==null||p.bc.isRaid())return false;
        if(p.getParticipant() instanceof TrainerParticipant)return NpcTurnKnowledge.enabled(p);
        return p.getParticipant() instanceof PlayerParticipant&&p.bc.participants.stream().anyMatch(side->side instanceof TrainerParticipant&&side.allPokemon.length>0&&NpcTurnKnowledge.enabled(side.allPokemon[0]));
    }
    public static boolean visible(PixelmonWrapper p){return forecast(p)&&(!p.bc.simulateMode||NpcTurnKnowledge.present(p.bc));}
    private static Frame frame(PixelmonWrapper p){
        Frame f=FRAMES.get(p.bc);if(f==null||f.turn!=p.bc.battleTurn){f=new Frame(p.bc.battleTurn);FRAMES.put(p.bc,f);}
        return f;
    }
    public static void begin(PixelmonWrapper p){if(!forecast(p)||p.bc.simulateMode)return;Frame f=frame(p);UUID id=p.getPokemonUUID();f.moves.merge(id,1,Integer::sum);f.executing.add(id);}
    public static void finished(PixelmonWrapper p){if(!forecast(p)||p.bc.simulateMode)return;frame(p).executing.remove(p.getPokemonUUID());}
    public static int roll(PixelmonWrapper p,String trigger){
        Frame f=frame(p);UUID id=p.getPokemonUUID();
        if(!trigger.startsWith("held:"))trigger+=(":"+(f.moves.getOrDefault(id,0)+(f.executing.contains(id)?0:1)));
        return f.rolls.computeIfAbsent(id+":"+trigger,k->ThreadLocalRandom.current().nextInt(1000000));
    }
    public static boolean chance(PixelmonWrapper p,String trigger,double percent){return roll(p,trigger)<Math.max(0,Math.min(100,percent))*10000;}
    public static String target(PixelmonWrapper p){
        var side=p.getParticipant();int slot=side.controlledPokemon.indexOf(p);
        if(slot<0)for(int i=0;i<side.controlledPokemon.size();i++)if(side.controlledPokemon.get(i).getPokemonUUID().equals(p.getPokemonUUID())){slot=i;break;}
        if(slot<0&&side.controlledPokemon.size()==1)slot=0;return side.team+":"+slot;
    }
    public static boolean accuracy(PixelmonWrapper user,PixelmonWrapper target,int percent){return chance(user,"accuracy:"+target(target),percent);}
    public static boolean buff(PixelmonWrapper p){return true;}
    public static boolean protect(PixelmonWrapper p){return chance(p,"protect",100/Math.pow(3,p.protectsInARow));}
    public static boolean succeeds(PixelmonWrapper p,PixelmonWrapper target,com.pixelmonmod.pixelmon.battles.attacks.Attack attack){
        if(!visible(p)||p.usingZ||attack==null)return true;
        if(AuthorChallenge.protect(attack.getMove()))return protect(p);
        if(attack.getAttackCategory()==com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS&&AuthorChallenge.selfEffect(attack.getMove()))return buff(p);
        var copy=attack.deepCopy();var access=(local.pixelmon.backport.mixin.AuthorAccuracyAccess)(Object)copy;
        try(var preview=NpcTurnKnowledge.preview(p,copy)){
            copy.moveAccuracy=access.backport$modifiedStats(p,target,p.getBattleAbility(),target.getBattleAbility(),copy.getMove().getBasePower())[1];
            double probability=copy.moveAccuracy<0||copy.cantMiss(p)?100:access.backport$liveAccuracy(p,target,p.getBattleAbility());
            return accuracy(p,target,(int)Math.max(0,probability));
        }
    }
    public static void end(BattleController b){FRAMES.remove(b);}
}
