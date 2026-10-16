import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import javax.tools.ToolProvider;
import com.google.gson.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Execute the shipped border path and original native Attack category fallback. */
public final class BurstZCrashVerification {
    static final String API="com.pixelmonmod.pixelmon.";
    static void add(Map<String,String> s,String n,String b){s.put(n,"package "+n.substring(0,n.lastIndexOf('.'))+";"+b);}
    static byte[] read(JarFile j,String n)throws Exception{try(var in=j.getInputStream(j.getJarEntry(n.replace('.','/')+".class"))){return in.readAllBytes();}}
    static ClassNode node(byte[] b){var n=new ClassNode();new ClassReader(b).accept(n,0);return n;}
    static byte[] write(ClassNode n){var w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
    public static void main(String[] args)throws Exception{
        Map<String,String> s=new LinkedHashMap<>();
        add(s,API+"api.pokemon.Element","public enum Element {NORMAL,FIRE,WATER,GRASS,ELECTRIC,ICE,FIGHTING,POISON,GROUND,FLYING,PSYCHIC,BUG,ROCK,GHOST,DRAGON,DARK,STEEL,FAIRY,MYSTERY}");
        add(s,API+"api.battles.AttackCategory","public enum AttackCategory {PHYSICAL,SPECIAL,STATUS}");
        add(s,API+"api.pokemon.species.Species","public class Species {public int dex=25;public int getDex(){return dex;}}");
        add(s,API+"api.pokemon.species.Stats","public class Stats {}");
        add(s,API+"battles.attacks.ZMove","public class ZMove {public String attackName;public ZMove(String n){attackName=n;}}");
        add(s,API+"battles.attacks.ImmutableAttack","public class ImmutableAttack {public String name;public "+API+"api.pokemon.Element type;public "+API+"api.battles.AttackCategory category;public String getAttackName(){return name;}public "+API+"api.pokemon.Element getAttackType(){return type;}public "+API+"api.battles.AttackCategory getAttackCategory(){return category;}public ZMove getZMove("+API+"client.gui.battles.PixelmonClientData p,boolean b){p.previewCalls++;return p.z;}}");
        add(s,API+"battles.attacks.Attack","public class Attack {public static java.util.Map<String,ImmutableAttack> registry=new java.util.HashMap<>();public ImmutableAttack baseAttack,overrideAttack;public "+API+"api.battles.AttackCategory overrideAttackCategory;public "+API+"api.pokemon.Element overrideType;public Attack originalMove;public int pp=17;public Attack(String n){baseAttack=registry.get(n);if(baseAttack==null)throw new AssertionError(n);}public static boolean hasAttack(String n){return registry.containsKey(n);}public ImmutableAttack getMove(){throw new AssertionError();}public ImmutableAttack getActualMove(){throw new AssertionError();}public "+API+"api.battles.AttackCategory getAttackCategory(){throw new AssertionError();}public "+API+"api.pokemon.Element getType(){throw new AssertionError();}public "+API+"api.pokemon.Element getActualType(){throw new AssertionError();}public Attack copy(){var p=new Attack(baseAttack.name);p.pp=pp;return p;}}");
        add(s,API+"battles.controller.participants.PixelmonWrapper","public class PixelmonWrapper {}");
        add(s,API+"battles.attacks.MaxMoveConverter","public class MaxMoveConverter {public static Attack getMaxMoveFromAttack(Attack a,"+API+"battles.controller.participants.PixelmonWrapper p){var m=new Attack(a.getAttackCategory()=="+API+"api.battles.AttackCategory.STATUS?\"Max Guard\":a.getType()=="+API+"api.pokemon.Element.ELECTRIC?\"Max Lightning\":\"Max Strike\");m.originalMove=a;m.overrideAttackCategory=a.getAttackCategory();return m;}public static Attack getGMaxMoveFromAttack(Attack a,"+API+"battles.controller.participants.PixelmonWrapper p,"+API+"api.pokemon.species.Species s,"+API+"api.pokemon.species.Stats f){return getMaxMoveFromAttack(a,p);}}");
        add(s,API+"client.gui.battles.PixelmonClientData","public class PixelmonClientData {public java.util.UUID pokemonUUID=java.util.UUID.randomUUID();public boolean gmaxFactor;public int previewCalls;public "+API+"api.pokemon.species.Species species=new "+API+"api.pokemon.species.Species();public "+API+"battles.attacks.ZMove z;public "+API+"api.pokemon.species.Stats getBaseStats(){return new "+API+"api.pokemon.species.Stats();}}");
        add(s,API+"client.gui.battles.ClientBattleManager","public class ClientBattleManager {public int battleControllerIndex=144;public boolean showZMoves,dynamaxing;public java.util.UUID dynamax;public PixelmonClientData current=new PixelmonClientData();public PixelmonClientData getCurrentPokemon(){return current;}}");
        add(s,API+"client.ClientProxy","public class ClientProxy {public static "+API+"client.gui.battles.ClientBattleManager battleManager;}");
        add(s,"local.pixelmon.backport.TeraNetwork","public class TeraNetwork {public static class Snapshot {public int battle=144;public java.util.Map<java.util.UUID,Integer> burst=new java.util.HashMap<>();public java.util.Map<java.util.UUID,java.util.List<String>> burstElectricMoves=new java.util.HashMap<>();}}");
        add(s,"local.pixelmon.backport.TeraClient","public class TeraClient {public static TeraNetwork.Snapshot snapshot;}");
        add(s,"local.pixelmon.backport.BattleInfoCompat","public class BattleInfoCompat {public static "+API+"api.pokemon.Element moveType("+API+"battles.attacks.Attack a,"+API+"client.gui.battles.PixelmonClientData p){return a.getType();}public static java.util.List<"+API+"api.pokemon.Element> defense("+API+"client.gui.battles.PixelmonClientData p){return java.util.List.of("+API+"api.pokemon.Element.NORMAL);}}");
        s.put("fixture.BurstZCases",Files.readString(Path.of("tests/resources/BurstZCases.java.txt")));
        Path dir=Files.createTempDirectory(Path.of(".cache"),"burst-z-crash-");
        List<String> options=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
        for(var row:s.entrySet()){var p=dir.resolve(row.getKey().replace('.','/')+".java");Files.createDirectories(p.getParent());Files.writeString(p,row.getValue());options.add(p.toString());}
        if(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))!=0)throw new AssertionError("Z crash fixture compilation failed");
        JsonArray moves=new JsonArray();
        try(JarFile nativeJar=new JarFile(args[0])){
            Path p=dir.resolve(API.replace('.','/')+"battles/attacks/Attack.class");var stub=node(Files.readAllBytes(p));var actual=node(read(nativeJar,API+"battles.attacks.Attack"));
            Set<String> names=Set.of("getMove","getActualMove","getAttackCategory","getType","getActualType");int installed=0;
            for(var m:actual.methods)if(names.contains(m.name)){stub.methods.removeIf(v->v.name.equals(m.name)&&v.desc.equals(m.desc));stub.methods.add(m);installed++;}
            if(installed!=5)throw new AssertionError("Native Attack getters changed");Files.write(p,write(stub));
            var entries=nativeJar.entries();while(entries.hasMoreElements()){
                var e=entries.nextElement();if(!e.getName().startsWith("data/pixelmon/moves/")||!e.getName().endsWith(".json"))continue;
                try(var in=nativeJar.getInputStream(e)){JsonObject json=new JsonParser().parse(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();moves.add(json);}
            }
            // Confirm native Max conversion supplies both category and original move.
            var converter=node(read(nativeJar,API+"battles.attacks.MaxMoveConverter"));boolean category=false,original=false;
            for(var m:converter.methods)if(m.name.equals("getMaxMoveFromAttack"))for(var i:m.instructions){if(i instanceof MethodInsnNode)category|=((MethodInsnNode)i).name.equals("overrideAttackCategory");if(i instanceof FieldInsnNode)original|=((FieldInsnNode)i).name.equals("originalMove")&&i.getOpcode()==Opcodes.PUTFIELD;}
            if(!category||!original)throw new AssertionError("Max fixture no longer reflects native category/original link");
        }
        for(var move:Version13StaticVerification.resource("backport/cap_pikachu.json").getAsJsonArray("moves"))moves.add(move);
        boolean baseline=Files.isRegularFile(Path.of("dist/pixelmon-content-backport-1.16.5-1.4.3.jar"));
        for(boolean old:baseline?new boolean[]{true,false}:new boolean[]{false}){
            ClassNode border;
            if(old)try(var jar=new JarFile("dist/pixelmon-content-backport-1.16.5-1.4.3.jar")){border=node(read(jar,"local.pixelmon.backport.BurstClient"));}
            else border=Version13StaticVerification.node("local/pixelmon/backport/BurstClient");
            border.methods.removeIf(m->!Set.of("move","flags").contains(m.name));byte[] code=write(border);
            try(var loader=new URLClassLoader(new URL[]{dir.toUri().toURL()},BurstZCrashVerification.class.getClassLoader()){
                protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{synchronized(getClassLoadingLock(n)){Class<?> c=findLoadedClass(n);String base=n.contains("$")?n.substring(0,n.indexOf('$')):n;if(c==null&&s.containsKey(base))c=findClass(n);if(c==null&&n.equals("local.pixelmon.backport.BurstClient"))c=defineClass(n,code,0,code.length);if(c==null)c=super.loadClass(n,false);if(resolve)resolveClass(c);return c;}}
            }){loader.loadClass("fixture.BurstZCases").getMethod("run",String.class,boolean.class).invoke(null,moves.toString(),old);}
        }
        System.out.println("PASS shipped BurstClient border with native Attack getMove/category/type fallback and real move JSON; generic Z crash, physical/special inheritance, statuses, absent/stale fields and Max/GMax preview contracts. No game initialized.");
    }
}
