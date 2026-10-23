import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import com.google.gson.JsonElement;

/** Real debuff service under a controlled battle clock and native-shaped stat/health boundaries. */
public final class ErosionVerification {
    static final String P="com.pixelmonmod.pixelmon.",W=P+"battles.controller.participants.PixelmonWrapper";
    static void add(Map<String,String> sources,String name,String code){sources.put(name,"package "+name.substring(0,name.lastIndexOf('.'))+";"+code);}
    public static void main(String[] args)throws Exception {
        var moveData=new java.util.LinkedHashMap<net.minecraft.util.ResourceLocation,JsonElement>();
        local.pixelmon.backport.Content.apply("pixelmon","moves",moveData);
        long registered=moveData.values().stream().filter(v->v.getAsJsonObject().get("attackName").getAsString().equals("Ninja Training")).count();
        if(registered!=1)throw new AssertionError("Ninja Training missing from native move loader");
        if(!moveData.values().stream().filter(v->v.getAsJsonObject().get("attackName").getAsString().equals("Ninja Training")).findFirst().get().getAsJsonObject().get("attackType").getAsString().equals("NORMAL"))throw new AssertionError("Ninja Training must be Normal type");
        local.pixelmon.backport.Content.apply("pixelmon","moves",moveData);
        if(moveData.values().stream().filter(v->v.getAsJsonObject().get("attackName").getAsString().equals("Ninja Training")).count()!=1)throw new AssertionError("Ninja Training duplicated on reload");
        Map<String,String> sources=new LinkedHashMap<>();
        add(sources,P+"api.pokemon.stats.BattleStatsType","public enum BattleStatsType {HP,ATTACK,DEFENSE,SPECIAL_ATTACK,SPECIAL_DEFENSE,SPEED}");
        add(sources,P+"api.pokemon.Nature","public class Nature {public "+P+"api.pokemon.stats.BattleStatsType getIncreasedStat(){return null;}public "+P+"api.pokemon.stats.BattleStatsType getDecreasedStat(){return null;}}");
        add(sources,P+"api.pokemon.stats.IVStore","public class IVStore {public int getStat(BattleStatsType t){return 31;}public boolean isHyperTrained(BattleStatsType t){return false;}}");
        add(sources,P+"api.pokemon.stats.EVStore","public class EVStore {public int getStat(BattleStatsType t){return 252;}}");
        add(sources,P+"api.pokemon.stats.PermanentStats","public class PermanentStats {public IVStore getIVs(){return new IVStore();}public EVStore getEVs(){return new EVStore();}}");
        add(sources,P+"api.pokemon.species.stat.ImmutableBattleStats","public class ImmutableBattleStats {public int getStat("+P+"api.pokemon.stats.BattleStatsType t){return 100;}}");
        add(sources,P+"api.pokemon.species.Species","public class Species {public int dex=658;public int getDex(){return dex;}}");
        add(sources,P+"api.pokemon.species.Stats","public class Stats {public boolean ash=true;public boolean isForm(String... f){return ash;}public "+P+"api.pokemon.species.stat.ImmutableBattleStats getBattleStats(){return new "+P+"api.pokemon.species.stat.ImmutableBattleStats();}}");
        add(sources,P+"api.pokemon.ability.Ability","public interface Ability {}");
        add(sources,P+"api.pokemon.ability.abilities.BattleBond","public class BattleBond implements "+P+"api.pokemon.ability.Ability {}");
        add(sources,P+"api.pokemon.Pokemon","public class Pokemon {public "+P+"api.pokemon.species.Stats form=new "+P+"api.pokemon.species.Stats();public "+P+"api.pokemon.species.Species getSpecies(){return new "+P+"api.pokemon.species.Species();}public "+P+"api.pokemon.species.Stats getForm(){return form;}public "+P+"api.pokemon.ability.Ability getAbility(){return new "+P+"api.pokemon.ability.abilities.BattleBond();}}");
        add(sources,P+"api.pokemon.Element","public enum Element {WATER,NORMAL}");
        add(sources,P+"api.battles.AttackCategory","public enum AttackCategory {PHYSICAL,SPECIAL,STATUS}");
        add(sources,P+"battles.attacks.ImmutableAttack","public class ImmutableAttack {public String name=\"Water Shuriken\";public String getAttackName(){return name;}public int getBasePower(){return 20;}}");
        add(sources,P+"battles.attacks.Attack","public class Attack {public int pp=20;public boolean disabled;public ImmutableAttack base=new ImmutableAttack();public Attack(){}public Attack(java.util.Optional<ImmutableAttack> a){base=a.get();}public void setDisabled(boolean d,"+W+" p){disabled=d;}public boolean getDisabled(){return disabled;}public ImmutableAttack getMove(){return base;}public "+P+"api.pokemon.Element getType(){return "+P+"api.pokemon.Element.WATER;}public "+P+"api.battles.AttackCategory getAttackCategory(){return "+P+"api.battles.AttackCategory.SPECIAL;}}");
        add(sources,P+"api.pokemon.stats.Moveset","public class Moveset extends java.util.ArrayList<"+P+"battles.attacks.Attack> {}");
        add(sources,P+"api.battles.attack.AttackRegistry","public class AttackRegistry {public static java.util.Optional<"+P+"battles.attacks.ImmutableAttack> STRUGGLE=java.util.Optional.of(new "+P+"battles.attacks.ImmutableAttack());}");
        add(sources,P+"battles.controller.participants.BattleParticipant","public class BattleParticipant {}");
        add(sources,P+"battles.controller.participants.PlayerParticipant","public class PlayerParticipant extends BattleParticipant {public net.minecraft.entity.player.ServerPlayerEntity player;}");
        add(sources,P+"battles.controller.BattleController","public class BattleController {public boolean simulateMode,battleEnded;public boolean isWaiting(){return false;}public int battleTurn=10,statRefresh,turn;public java.util.List<"+W+"> turnList=new java.util.ArrayList<>();public void modifyStats(){statRefresh++;}public void modifyStatsCancellable("+W+" p){statRefresh++;}public void modifyStats("+W+" p){statRefresh++;}public void sendToAll(String key,Object... a){}}");
        sources.put(W,Files.readString(Path.of("tests/resources/TurnClockWrapper.java.txt")));
        add(sources,"local.pixelmon.backport.ErosionNetwork","public class ErosionNetwork {public static int packets;public static void sync("+P+"battles.controller.BattleController b,"+W+" p){packets++;}}");
        add(sources,"local.pixelmon.backport.NinjaResiduals","public class NinjaResiduals {public static void apply("+W+" p){}}");
        add(sources,"net.minecraftforge.fml.loading.FMLPaths","public enum FMLPaths {CONFIGDIR;public java.nio.file.Path get(){return java.nio.file.Path.of(System.getProperty(\"fixture.configDir\"));}}");
        add(sources,"local.pixelmon.backport.GreninjaEnhancement","public class GreninjaEnhancement {public static boolean enabled(){return true;}} ");
        sources.put("fixture.ErosionCases",Files.readString(Path.of("tests/resources/ErosionCases.java.txt")));
        Path root=Files.createTempDirectory(Path.of(".cache/erosion-160"),"fixture-");List<String> compile=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",root.toString()));
        System.setProperty("fixture.configDir",root.resolve("config").toString());
        for(var row:sources.entrySet()){Path f=root.resolve(row.getKey().replace('.','/')+".java");Files.createDirectories(f.getParent());Files.writeString(f,row.getValue());compile.add(f.toString());}
        if(ToolProvider.getSystemJavaCompiler().run(null,null,null,compile.toArray(new String[0]))!=0)throw new AssertionError("Erosion fixtures did not compile");
        try(var loader=new URLClassLoader(new URL[]{root.toUri().toURL()},ErosionVerification.class.getClassLoader()){
            protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException {synchronized(getClassLoadingLock(n)){
                if(!n.startsWith("com.pixelmonmod.")&&!n.startsWith("local.pixelmon.backport.")&&!n.startsWith("fixture.")&&!n.equals("net.minecraftforge.fml.loading.FMLPaths"))return super.loadClass(n,resolve);
                Class<?> c=findLoadedClass(n);if(c==null)try{c=findClass(n);}catch(ClassNotFoundException e){try(var in=getParent().getResourceAsStream(n.replace('.','/')+".class")){if(in==null)throw e;byte[] bytes=in.readAllBytes();c=defineClass(n,bytes,0,bytes.length);}catch(java.io.IOException ex){throw new ClassNotFoundException(n,ex);}}
                if(resolve)resolveClass(c);return c;
            }}
        }){try{loader.loadClass("fixture.ErosionCases").getMethod("run").invoke(null);}catch(java.lang.reflect.InvocationTargetException e){throw new AssertionError("Erosion lifecycle",e.getCause());}}
        System.out.println("PASS erosion: six bases -20, five rounds/bench/refresh/HP/faint/cleanup/AI; ninja bonus advances only its own timers in singles/doubles/teams; config ranges.");
    }
}
