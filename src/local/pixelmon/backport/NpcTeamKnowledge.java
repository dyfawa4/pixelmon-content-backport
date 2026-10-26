package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import net.minecraft.nbt.CompoundNBT;
/** Read-only battle-lifetime scouting. The player's native complete data is copied, never edited. */
public final class NpcTeamKnowledge {
    public enum Plan {SETUP,PASS,EVASION,HAZARDS,SCREENS,ROOM,WEATHER,RECOVERY,PRIORITY,SASH,SNOWBALL}
    private static final Map<BattleController,Map<UUID,CompoundNBT>> DATA=new WeakHashMap<>();
    private static final Map<BattleController,EnumSet<Plan>> PLANS=new WeakHashMap<>();
    public static void capture(TrainerParticipant trainer){
        BattleController battle=trainer.bc;if(battle==null||battle.simulateMode||!NpcGimmicks.enabled(trainer.trainer)||!GymCities.isLeader(trainer.trainer)&&!AuthorBossTeams.is(trainer.trainer))return;
        if(DATA.containsKey(battle))return;Map<UUID,CompoundNBT> snapshots=new LinkedHashMap<>();EnumSet<Plan> plans=EnumSet.noneOf(Plan.class);
        for(var side:battle.participants)if(side instanceof PlayerParticipant&&side.team!=trainer.team)for(var wrapper:side.allPokemon){Pokemon p=wrapper.pokemon;snapshots.put(p.getUUID(),p.writeToNBT(new CompoundNBT()).func_74737_b());
            for(var move:p.getMoveset()){String name=move.getMove().getAttackName();if(NpcSetupControl.boost(move))plans.add(Plan.SETUP);if(name.equals("Baton Pass")||name.equals("Shed Tail"))plans.add(Plan.PASS);if(name.equals("Minimize")||name.equals("Double Team"))plans.add(Plan.EVASION);
                if(Set.of("Stealth Rock","Spikes","Toxic Spikes","Sticky Web","Ceaseless Edge","Stone Axe").contains(name))plans.add(Plan.HAZARDS);
                if(Set.of("Reflect","Light Screen","Aurora Veil").contains(name))plans.add(Plan.SCREENS);if(name.equals("Trick Room"))plans.add(Plan.ROOM);if(Set.of("Rain Dance","Sunny Day","Sandstorm","Hail","Snowscape").contains(name))plans.add(Plan.WEATHER);
                if(NpcMoves.recovery(move)||Set.of("Rest","Wish","Leech Seed").contains(name))plans.add(Plan.RECOVERY);if(wrapper.bc!=null&&move.getMove().getPriority(wrapper)>0&&move.getAttackCategory()!=com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS)plans.add(Plan.PRIORITY);}
            if(p.getHeldItem().func_77973_b() instanceof com.pixelmonmod.pixelmon.items.heldItems.FocusSashItem)plans.add(Plan.SASH);String ability=p.getAbility().getClass().getSimpleName();if(Set.of("Moxie","BeastBoost","GrimNeigh","ChillingNeigh","SoulHeart").contains(ability)||ability.contains("AsOne"))plans.add(Plan.SNOWBALL);
        }DATA.put(battle,snapshots);PLANS.put(battle,plans);
    }
    public static boolean has(BattleController battle){return DATA.containsKey(battle);}
    public static int size(BattleController battle){return DATA.getOrDefault(battle,Collections.emptyMap()).size();}
    public static CompoundNBT data(BattleController battle,UUID id){CompoundNBT data=DATA.getOrDefault(battle,Collections.emptyMap()).get(id);return data==null?null:data.func_74737_b();}
    public static boolean plan(BattleController battle,Plan plan){return PLANS.getOrDefault(battle,EnumSet.noneOf(Plan.class)).contains(plan);}
    public static List<PixelmonWrapper> opponents(PixelmonWrapper own){List<PixelmonWrapper> result=new ArrayList<>();for(var side:own.bc.participants)if(side.team!=own.getParticipant().team&&side instanceof PlayerParticipant)for(var p:side.allPokemon)if(p.isAlive())result.add(p);return result;}
    public static List<Attack> known(PixelmonWrapper enemy){if(!(enemy.getParticipant() instanceof PlayerParticipant)||!has(enemy.bc))return null;List<Attack> result=new ArrayList<>();for(var a:enemy.getMoveset())if(a.pp>0&&!a.getDisabled())result.add(a.deepCopy());return result;}
    public static double lead(PixelmonWrapper candidate){double result=0;if(plan(candidate.bc,Plan.PASS)||plan(candidate.bc,Plan.EVASION)||plan(candidate.bc,Plan.SETUP)){
        if(candidate.getMoveset().stream().anyMatch(a->a.getMove().getAttackName().equals("Haze")))result+=50;
        if(candidate.getBattleAbility().getClass().getSimpleName().equals("Unaware"))result+=40;
    }return result;}
    public static void configure(TrainerParticipant side){List<Pokemon> team=new ArrayList<>();for(var p:side.allPokemon)team.add(p.pokemon);
        for(var p:team)p.getPersistentData().func_74757_a("BackportScoutHazards",plan(side.bc,Plan.HAZARDS));
        if(plan(side.bc,Plan.HAZARDS))role(team,List.of("Rapid Spin","Defog","Court Change"));
        if(plan(side.bc,Plan.SCREENS))role(team,List.of("Psychic Fangs","Brick Break","Defog"));
        if(plan(side.bc,Plan.RECOVERY)||plan(side.bc,Plan.ROOM))role(team,List.of("Taunt","Encore","Knock Off"));
        if(plan(side.bc,Plan.SETUP)||plan(side.bc,Plan.PASS)||plan(side.bc,Plan.EVASION)){
            if(team.stream().noneMatch(p->p.getMoveset().stream().anyMatch(a->a.getMove().getAttackName().equals("Haze"))))role(team,List.of("Haze","Clear Smog","Roar","Whirlwind"));
            for(var p:team)for(var a:p.getForm().getAbilities().getAll())if(a!=null&&a.getClass().getSimpleName().equals("Unaware"))p.setAbility(a);
        }
    }
    private static void role(List<Pokemon> team,List<String> moves){for(String name:moves){if(team.stream().anyMatch(p->p.getMoveset().stream().anyMatch(a->a.getMove().getAttackName().equals(name))))return;List<Pokemon> options=new ArrayList<>(team);options.sort(Comparator.comparingLong(p->p.getMoveset().stream().filter(a->a.getAttackCategory()==com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS).count()));for(var p:options)if(GymCounterTeam.ensure(p,name))return;}}
    public static void end(BattleController battle){DATA.remove(battle);PLANS.remove(battle);}
}
