import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;

/** Execute the shipped sidebar power helper without booting a game. */
public final class AshBattleDisplayVerification {
    static final String API="com.pixelmonmod.pixelmon.";
    static void add(Map<String,String> sources,String name,String body){sources.put(name,"package "+name.substring(0,name.lastIndexOf('.'))+";"+body);}
    public static void main(String[] args)throws Exception{
        Map<String,String> sources=new LinkedHashMap<>();
        add(sources,API+"api.pokemon.Element","public enum Element { NORMAL,WATER,MYSTERY;public static float getTotalEffectiveness(java.util.List<Element> l,Element e,boolean b){return 1;} }");
        add(sources,API+"api.battles.AttackCategory","public enum AttackCategory { STATUS,SPECIAL,PHYSICAL }");
        add(sources,API+"api.pokemon.species.Species","public class Species {public int dex=658;public int getDex(){return dex;} }");
        add(sources,API+"api.pokemon.species.Stats","public class Stats {public java.util.List<"+API+"api.pokemon.Element> getTypes(){return java.util.List.of("+API+"api.pokemon.Element.WATER);} }");
        add(sources,API+"battles.attacks.ImmutableAttack","public class ImmutableAttack {public String name;public String getAttackName(){return name;} }");
        add(sources,API+"battles.attacks.Attack","public class Attack {public ImmutableAttack move=new ImmutableAttack();public ImmutableAttack getMove(){return move;}public "+API+"api.pokemon.Element getType(){return "+API+"api.pokemon.Element.WATER;}public "+API+"api.battles.AttackCategory getAttackCategory(){return "+API+"api.battles.AttackCategory.SPECIAL;} }");
        add(sources,API+"client.gui.battles.PixelmonClientData","public class PixelmonClientData {public java.util.UUID pokemonUUID=java.util.UUID.randomUUID();public "+API+"api.pokemon.species.Species species=new "+API+"api.pokemon.species.Species();public String form;public int dynamax;public "+API+"api.pokemon.species.Stats getBaseStats(){return new "+API+"api.pokemon.species.Stats();} }");
        add(sources,API+"client.gui.battles.ClientBattleManager","public class ClientBattleManager {public boolean showZMoves,dynamaxing,megaEvolving;public int dynamaxTurnsLeft,battleControllerIndex;} ");
        add(sources,API+"client.ClientProxy","public class ClientProxy {public static "+API+"client.gui.battles.ClientBattleManager battleManager;} ");
        add(sources,"local.pixelmon.backport.Tera","public class Tera {public static final "+API+"api.pokemon.Element[] TYPES="+API+"api.pokemon.Element.values();} ");
        add(sources,"local.pixelmon.backport.TeraNetwork","public class TeraNetwork {public static class Snapshot {public int battle;public java.util.Map<java.util.UUID,java.util.List<"+API+"api.pokemon.Element>> defense=new java.util.HashMap<>();public java.util.Map<java.util.UUID,Integer> active=new java.util.HashMap<>(),attackStats=new java.util.HashMap<>();}public static class Reply {public int battle,type;public java.util.UUID pokemon;public boolean selected,active;} }");
        add(sources,"local.pixelmon.backport.TeraClient","public class TeraClient {public static TeraNetwork.Snapshot snapshot;public static TeraNetwork.Reply battle;} ");
        add(sources,"fixture.AshDisplayCases",String.join("\n",
            "import "+API+"battles.attacks.*;import "+API+"client.*;import "+API+"client.gui.battles.*;import local.pixelmon.backport.*;",
            "public class AshDisplayCases {static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}public static void run(){",
            "var bm=new ClientBattleManager();ClientProxy.battleManager=bm;var user=new PixelmonClientData();var attack=new Attack();attack.move.name=\"Water Shuriken\";",
            "for(String form:new String[]{\"base\",\"battle_bond\",\"mega\",null}){user.form=form;check(BattleInfoCompat.movePower(attack,user,15)==15,\"Ordinary form changed\");}",
            "for(String form:new String[]{\"ash\",\"ash_permanent\"}){user.form=form;check(BattleInfoCompat.movePower(attack,user,15)==20,\"Ash sidebar still shows 15\");}",
            "user.species.dex=25;check(BattleInfoCompat.movePower(attack,user,15)==15,\"Other species changed\");user.species.dex=658;",
            "attack.move.name=\"Surf\";check(BattleInfoCompat.movePower(attack,user,90)==90,\"Other move changed\");attack.move.name=\"Water Shuriken\";",
            "bm.showZMoves=true;check(BattleInfoCompat.movePower(attack,user,100)==100,\"Z power replaced\");bm.showZMoves=false;bm.dynamaxing=true;check(BattleInfoCompat.movePower(attack,user,100)==100,\"Max power replaced\");bm.dynamaxing=false;bm.dynamaxTurnsLeft=2;check(BattleInfoCompat.movePower(attack,user,100)==100,\"Active Max power replaced\");bm.dynamaxTurnsLeft=0;user.dynamax=1;check(BattleInfoCompat.movePower(attack,user,100)==100,\"Dynamaxed actor changed\");user.dynamax=0;",
            "check(BattleInfoCompat.movePower(attack,null,15)==15,\"Missing user changed\");check(BattleInfoCompat.movePower(null,user,15)==15,\"Missing move changed\");",
            "attack.move.name=\"Tera Blast\";var s=new TeraNetwork.Snapshot();TeraClient.snapshot=s;s.active.put(user.pokemonUUID,2);check(BattleInfoCompat.movePower(attack,user,80)==100,\"Stellar Tera Blast regressed\");",
            "}}"));
        Path dir=Files.createTempDirectory(Paths.get(".cache"),"ash-battle-display-");
        List<String> options=new ArrayList<>(List.of("--release","11","-encoding","UTF-8","-proc:none","-d",dir.toString()));
        for(var entry:sources.entrySet()){Path file=dir.resolve(entry.getKey().replace('.','/')+".java");Files.createDirectories(file.getParent());Files.writeString(file,entry.getValue());options.add(file.toString());}
        if(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))!=0)throw new AssertionError("Display fixture compilation failed");
        try(URLClassLoader loader=new URLClassLoader(new URL[]{dir.toUri().toURL()},AshBattleDisplayVerification.class.getClassLoader()){
            protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{synchronized(getClassLoadingLock(name)){
                Class<?> type=findLoadedClass(name);if(type==null&&(sources.containsKey(name)||name.startsWith("local.pixelmon.backport.TeraNetwork$")))type=findClass(name);
                if(type==null&&name.equals("local.pixelmon.backport.BattleInfoCompat"))try(var in=getParent().getResourceAsStream(name.replace('.','/')+".class")){byte[] bytes=in.readAllBytes();type=defineClass(name,bytes,0,bytes.length);}catch(Exception e){throw new ClassNotFoundException(name,e);}
                if(type==null)type=super.loadClass(name,false);if(resolve)resolveClass(type);return type;
            }}
        }){loader.loadClass("fixture.AshDisplayCases").getMethod("run").invoke(null);}
        System.out.println("PASS battle sidebar per-hit Ash power 20; ordinary 15; other species/moves, Z/Max exclusions, null inputs and Stellar Tera Blast preserved.");
    }
}
