package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.battles.controller.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;

/** Per-planning-scope snapshots of submitted player actions; never retained into another turn. */
public final class NpcTurnKnowledge {
    private static final Map<PixelmonWrapper,Integer> WAITING=Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Context> CURRENT=new ThreadLocal<>();
    private static final class Intent {
        final Attack attack;final UUID arrival;final Set<UUID> targets=new HashSet<>();final Map<BattleParticipant,Set<Integer>> slots=new IdentityHashMap<>();
        Intent(PixelmonWrapper p){
            arrival=p.newPokemonUUID;
            attack=p.wait||p.willUseItemInStack!=null||p.newPokemonUUID!=null||p.attack==null?null:p.attack.deepCopy();
            if(p.targets!=null)for(PixelmonWrapper target:p.targets){targets.add(target.getPokemonUUID());int i=target.getParticipant().controlledPokemon.indexOf(target);if(i>=0)slots.computeIfAbsent(target.getParticipant(),key->new HashSet<>()).add(i);}
        }
        boolean targets(PixelmonWrapper p){return targets.isEmpty()||targets.contains(p.getPokemonUUID())||slots.getOrDefault(p.getParticipant(),Collections.emptySet()).contains(p.getParticipant().controlledPokemon.indexOf(p));}
    }
    private static final class Context {
        final BattleController battle;final int turn;boolean future;final Map<UUID,Intent> intents=new HashMap<>();final Map<UUID,Double> health=new HashMap<>();final Map<UUID,PixelmonWrapper> items=new HashMap<>();final Map<UUID,Boolean> candidates=new HashMap<>();final Map<UUID,PixelmonWrapper> departures=new HashMap<>();
        Context(BattleController b){battle=b;turn=b.battleTurn;for(var p:b.participants)if(p instanceof PlayerParticipant)for(var m:p.controlledPokemon){
            intents.put(m.getPokemonUUID(),new Intent(m));
            if(m.willUseItemInStack!=null&&m.willUseItemPokemon!=null){var target=p.getPokemonFromParty(m.willUseItemPokemon);if(target!=null){var projected=NpcMedicineForecast.project(items.getOrDefault(target.getPokemonUUID(),target),m.willUseItemInStack,m.willUseItemInStackInfo);items.put(target.getPokemonUUID(),projected);health.put(target.getPokemonUUID(),(double)projected.getHealth());}}
        }}
    }
    private NpcTurnKnowledge(){}
    public static boolean enabled(PixelmonWrapper p){
        if(p==null||p.bc==null||p.bc.isRaid()||!(p.getParticipant() instanceof TrainerParticipant))return false;
        var npc=((TrainerParticipant)p.getParticipant()).trainer;
        return NpcGimmicks.enabled(npc)&&(GymCities.isLeader(npc)||AuthorBossTeams.is(npc));
    }
    public static boolean defer(PixelmonWrapper p){
        // Ninja Training repeats one actor inside the same global turn. Preserve
        // everyone else's already chosen action; do not request another NPC turn.
        if(enabled(p)&&NinjaTraining.bonus(p.bc)!=null)return true;
        if(!enabled(p)||p.bc.simulateMode||p.bc.battleEnded||CURRENT.get()!=null||!p.isAlive()||!p.onBattlefield||p.skipsTurn()
            ||p.attack!=null&&p.attack.doesPersist(p)||p.bc.participants.stream().noneMatch(t->t instanceof PlayerParticipant))return false;
        WAITING.put(p,p.bc.battleTurn);p.wait=true;return true;
    }
    public static void ready(BattleController battle){
        if(battle.simulateMode||battle.battleEnded){if(battle.battleEnded)end(battle);return;}
        if(NinjaTraining.bonus(battle)!=null)return;
        if(battle.getStage()!=BattleStage.DOACTION)return;
        for(var p:battle.participants)if(p instanceof PlayerParticipant&&p.getWait())return;
        List<PixelmonWrapper> pending=new ArrayList<>();
        synchronized(WAITING){for(var it=WAITING.entrySet().iterator();it.hasNext();){var e=it.next();var p=e.getKey();
            if(p.bc!=battle)continue;
            if(e.getValue()!=battle.battleTurn||!p.isAlive()||!p.onBattlefield){p.wait=false;it.remove();continue;}
            pending.add(p);it.remove();
        }}
        if(pending.isEmpty())return;
        CURRENT.set(new Context(battle));
        try(NpcArrivalPreview preview=NpcArrivalPreview.open(battle)){for(PixelmonWrapper p:pending){p.wait=false;p.selectAIAction();}}
        finally{CURRENT.remove();}
    }
    public static boolean active(BattleController b){Context c=CURRENT.get();return c!=null&&c.battle==b&&c.turn==b.battleTurn;}
    public static final class ActionPreview implements AutoCloseable {
        private final PixelmonWrapper mon;private final Attack attack,selected;private final Context context;private final Boolean previous;
        ActionPreview(PixelmonWrapper mon,Attack action){this.mon=mon;attack=mon.attack;selected=mon.selectedAttack;context=active(mon.bc)?CURRENT.get():null;
            previous=context==null?null:context.candidates.put(mon.getPokemonUUID(),action!=null&&action.getAttackCategory()!=com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS);
            mon.attack=action;mon.selectedAttack=action;}
        @Override public void close(){mon.attack=attack;mon.selectedAttack=selected;if(context!=null){if(previous==null)context.candidates.remove(mon.getPokemonUUID());else context.candidates.put(mon.getPokemonUUID(),previous);}}
    }
    public static ActionPreview preview(PixelmonWrapper mon,Attack action){return new ActionPreview(mon,action);}
    /** -1 unknown/future, 0 non-attacking action, 1 attacking action. */
    public static int offensive(PixelmonWrapper target){
        if(!active(target.bc)||CURRENT.get().future)return -1;Context c=CURRENT.get();
        Boolean candidate=c.candidates.get(target.getPokemonUUID());if(candidate!=null)return candidate?1:0;
        Intent intent=c.intents.get(target.getPokemonUUID());if(intent!=null)return intent.attack!=null&&intent.attack.getAttackCategory()!=com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS?1:0;
        return target.attack==null?-1:target.attack.getAttackCategory()==com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS?0:1;
    }
    /** Null means native memory; empty means a submitted non-attacking action. */
    public static List<Attack> moves(PixelmonWrapper enemy){
        Context c=CURRENT.get();if(c==null||c.battle!=enemy.bc||c.turn!=enemy.bc.battleTurn||!(enemy.getParticipant() instanceof PlayerParticipant))return null;
        if(c.future)return fullMoves(enemy);
        Intent intent=c.intents.get(enemy.getPokemonUUID());
        if(intent!=null)return intent.attack==null?Collections.emptyList():Collections.singletonList(intent.attack.deepCopy());
        return fullMoves(enemy);
    }
    private static List<Attack> fullMoves(PixelmonWrapper enemy){List<Attack> moves=new ArrayList<>();for(Attack a:enemy.getMoveset())if(a!=null&&a.pp>0&&!a.getDisabled())moves.add(a.deepCopy());return moves;}
    static PixelmonWrapper itemView(PixelmonWrapper target){Context c=CURRENT.get();return active(target.bc)?c.items.getOrDefault(target.getPokemonUUID(),target):target;}
    public static double health(PixelmonWrapper target){Context c=CURRENT.get();return active(target.bc)?c.health.getOrDefault(target.getPokemonUUID(),(double)target.getHealth()):target.getHealth();}
    public static PixelmonWrapper arrival(PixelmonWrapper enemy){Context c=CURRENT.get();if(!active(enemy.bc))return enemy;Intent intent=c.intents.get(enemy.getPokemonUUID());if(intent==null||intent.arrival==null)return enemy;var target=enemy.getParticipant().getPokemonFromParty(intent.arrival);return target!=null&&target.isAlive()?target:enemy;}
    static void bindArrival(PixelmonWrapper old,PixelmonWrapper next){Context c=CURRENT.get();if(active(old.bc)){Intent intent=c.intents.get(old.getPokemonUUID());if(intent!=null){c.intents.put(next.getPokemonUUID(),intent);if(intent.arrival!=null)c.departures.put(next.getPokemonUUID(),old);}}}
    static PixelmonWrapper departure(PixelmonWrapper incoming){return active(incoming.bc)?CURRENT.get().departures.get(incoming.getPokemonUUID()):null;}
    public static boolean targets(PixelmonWrapper enemy,PixelmonWrapper own){Context c=CURRENT.get();if(!active(enemy.bc))return true;Intent intent=c.intents.get(enemy.getPokemonUUID());return intent==null||intent.targets(own);}
    public static boolean future(BattleController b,boolean value){Context c=CURRENT.get();if(!active(b))return false;boolean previous=c.future;c.future=value;return previous;}
    public static List<MoveChoice> restrict(BattleController battle,List<MoveChoice> choices){
        if(!active(battle))return choices;Context c=CURRENT.get();if(c.future)return choices;List<MoveChoice> result=new ArrayList<>();
        for(MoveChoice choice:choices){Intent intent=c.intents.get(choice.user.getPokemonUUID());
            if(intent==null){result.add(choice);continue;}
            if(intent.attack==null||!choice.isAttack()||!choice.attack.getMove().getAttackName().equals(intent.attack.getMove().getAttackName()))continue;
            if(!intent.targets.isEmpty()&&choice.targets.stream().noneMatch(intent::targets))continue;
            result.add(choice);
        }return result;
    }
    public static void end(BattleController battle){synchronized(WAITING){WAITING.entrySet().removeIf(e->{if(e.getKey().bc!=battle)return false;e.getKey().wait=false;return true;});}if(active(battle))CURRENT.remove();}
}
