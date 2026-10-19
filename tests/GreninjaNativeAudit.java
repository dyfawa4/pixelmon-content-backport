import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.events.battles.AttackEvent;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.api.pokemon.species.stat.ImmutableBattleStats;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.entities.npcs.NPCRelearner;
import local.pixelmon.backport.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Read-only, opt-in observation of the human's battle. Never chooses a move or changes a stat. */
@Mod.EventBusSubscriber(modid="backport_greninja_test")
public final class GreninjaNativeAudit {
    static final BattleStatsType[] SIX={BattleStatsType.HP,BattleStatsType.ATTACK,BattleStatsType.DEFENSE,BattleStatsType.SPECIAL_ATTACK,BattleStatsType.SPECIAL_DEFENSE,BattleStatsType.SPEED};
    static final Map<UUID,int[]> BEFORE=new HashMap<>();
    static final Set<String> ATTACKS=new HashSet<>();
    static final JsonObject RESULT=new JsonObject();
    static final JsonArray HITS=new JsonArray();
    static boolean ready;
    static boolean enabled(){return Files.isRegularFile(Path.of("greninja-live-audit.request"));}
    static void save(){try{RESULT.add("hits",HITS);Files.writeString(Path.of("greninja-live-audit.json"),new GsonBuilder().setPrettyPrinting().create().toJson(RESULT));}catch(Exception error){error.printStackTrace();}}
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayerEntity)||!enabled())return;
        var player=(ServerPlayerEntity)e.player;if(!GreninjaTestMap.map(player)||player.field_70173_aa<60)return;
        if(!ready){
            ready=true;var party=StorageProxy.getParty(player);Pokemon ash=party.get(1),normal=party.get(2);
            var moves=NPCRelearner.getRelearnableMoves(ash);RESULT.addProperty("ashPP",ash.getMoveset().get(0).getMaxPP());RESULT.addProperty("ordinaryPP",normal.getMoveset().get(0).getMaxPP());
            RESULT.addProperty("nativeReminderCount",moves.size());RESULT.addProperty("ninjaRelearnable",moves.stream().anyMatch(a->NinjaTraining.MOVE.equals(a.getAttackName())));RESULT.addProperty("teraBlastRelearnable",moves.stream().anyMatch(a->"Tera Blast".equals(a.getAttackName())));RESULT.addProperty("awaitingHumanBattle",true);save();
        }
        var b=BattleRegistry.getBattle(player);if(b==null)return;
        for(var part:b.participants)for(var mon:part.allPokemon)if(mon.onBattlefield&&!Erosion.active(mon))BEFORE.put(mon.getPokemonUUID(),new int[]{b.battleIndex,mon.getHealth(),mon.getMaxHealth()});
    }
    @SubscribeEvent public void damage(AttackEvent.Damage e){
        if(!enabled()||e.user.bc.simulateMode||!Erosion.active(e.target)||e.getAttack()==null||!"Water Shuriken".equals(e.getAttack().getActualMove().getAttackName()))return;
        var target=e.target;var b=target.bc;JsonObject hit=new JsonObject();
        hit.addProperty("phase","before_native_damage");hit.addProperty("battle",b.battleIndex);hit.addProperty("turn",b.battleTurn);hit.addProperty("damage",e.damage);hit.addProperty("erosionTurns",Erosion.remaining(target));
        try{
            var base=target.getForm().getBattleStats();var reduced=new EnumMap<BattleStatsType,Integer>(BattleStatsType.class);for(var stat:SIX)reduced.put(stat,Math.max(1,base.getStat(stat)-20));
            final var reducedBases=new ImmutableBattleStats(reduced);Stats reference=new Stats(){@Override public ImmutableBattleStats getBattleStats(){return reducedBases;}};
            JsonArray stats=new JsonArray();
            for(var stat:SIX){
                int expected=target.getStats().calculateStat(stat,target.getNature(),reference,target.getPokemonLevelNum());
                // These passive test targets have no base-stat-changing status or held item.
                int actual=stat==BattleStatsType.HP?target.getMaxHealth():target.getBattleStats().getBaseBattleStats()[stat.getStatIndex()];
                check(actual==expected,stat+" expected "+expected+" got "+actual);
                JsonObject row=new JsonObject();row.addProperty("stat",stat.name());row.addProperty("base",base.getStat(stat));row.addProperty("reducedBase",reduced.get(stat));row.addProperty("nativeExpected",expected);row.addProperty("actual",actual);stats.add(row);
                check(target.pokemon.getStat(stat)==target.getStats().calculateStat(stat,target.getNature(),target.getForm(),target.getPokemonLevelNum()),"Stored stat was edited: "+stat);
            }
            hit.add("stats",stats);hit.addProperty("hpBeforeHit",target.getHealth());hit.addProperty("maxHP",target.getMaxHealth());
            String attack=b.battleIndex+":"+target.getPokemonUUID();
            if(ATTACKS.add(attack)){
                int[] before=BEFORE.get(target.getPokemonUUID());
                if(before!=null&&before[0]==b.battleIndex){int expectedHP=(int)((long)before[1]*target.getMaxHealth()/Math.max(1,before[2]));check(target.getHealth()==expectedHP,"HP ratio before the first native hit");hit.addProperty("proportionalHPBeforeDamage",true);}
            }
            hit.addProperty("passed",true);RESULT.addProperty("awaitingHumanBattle",false);RESULT.addProperty("observedDebuffedNativeDamage",true);
        }catch(Throwable error){hit.addProperty("passed",false);hit.addProperty("error",error.toString());error.printStackTrace();}
        if(HITS.size()<100)HITS.add(hit);save();System.out.println("GRENINJA_LIVE_AUDIT "+hit);
    }
}
