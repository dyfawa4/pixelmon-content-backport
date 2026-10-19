import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import com.google.gson.*;

/** Executes production move/slot policies with isolated native services, no game. */
public final class MegaFollowupVerification {
    static final String P="com.pixelmonmod.pixelmon.",W=P+"battles.controller.participants.PixelmonWrapper";
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void add(Map<String,String> sources,String name,String body){sources.put(name,"package "+name.substring(0,name.lastIndexOf('.'))+";"+body);}
    public static void main(String[] args)throws Exception{
        JsonObject move=null;for(JsonElement row:Version13StaticVerification.resource("backport/content.json").getAsJsonArray("moves"))if(row.getAsJsonObject().get("attackName").getAsString().equals("Nihil Light"))move=row.getAsJsonObject();
        check(move!=null&&move.get("attackIndex").getAsInt()==920&&move.get("basePower").getAsInt()==200,"ZA power / unique native move id");
        check(move.get("ppBase").getAsInt()==10&&!move.get("makesContact").getAsBoolean(),"PP / non-contact beam");
        check(move.get("effects").toString().contains("IgnoreDefense")&&!move.get("effects").toString().contains("AlwaysHit"),"Native target stages, not unconditional accuracy bypass");
        check(move.getAsJsonObject("targetingInfo").get("hitsAll").getAsBoolean()&&!move.getAsJsonObject("targetingInfo").get("hitsAdjacentAlly").getAsBoolean(),"Opponent-only spread target");
        Map<String,String> s=new LinkedHashMap<>();
        add(s,P+"api.pokemon.Pokemon","public class Pokemon {java.util.UUID id;public Pokemon(java.util.UUID id){this.id=id;}public java.util.UUID getUUID(){return id;}}");
        add(s,P+"api.pokemon.species.Species","public class Species {public int dex=718;public int getDex(){return dex;}}");
        add(s,P+"api.pokemon.species.Stats","public class Stats {public String name=\"mega\";public boolean isForm(String... names){return java.util.Arrays.asList(names).contains(name);}}");
        add(s,P+"api.pokemon.stats.Moveset","public class Moveset extends java.util.ArrayList<"+P+"battles.attacks.Attack>{public "+P+"battles.attacks.Attack get(int i){return super.get(i);}public boolean add("+P+"battles.attacks.Attack a){return super.add(a);}public "+P+"battles.attacks.Attack set(int i,"+P+"battles.attacks.Attack a){return super.set(i,a);}}");
        add(s,P+"battles.attacks.ImmutableAttack","public class ImmutableAttack {String name;public ImmutableAttack(String n){name=n;}public String getAttackName(){return name;}}");
        add(s,P+"battles.attacks.Attack","public class Attack {public int pp=10,ppLevel;Integer max;public "+P+"api.pokemon.Element type="+P+"api.pokemon.Element.DRAGON;ImmutableAttack move;public Attack(String n){move=new ImmutableAttack(n);}public ImmutableAttack getMove(){return move;}public "+P+"api.pokemon.Element getType(){return type;}public void overridePPMax(int n){max=n;}public int getMaxPP(){return max==null?10+ppLevel*2:max;}}");
        add(s,P+"battles.controller.BattleController","public class BattleController {public boolean simulateMode;}");
        add(s,W,"public class PixelmonWrapper {public "+P+"battles.controller.BattleController bc=new "+P+"battles.controller.BattleController();public "+P+"api.pokemon.species.Species species=new "+P+"api.pokemon.species.Species();public "+P+"api.pokemon.species.Stats form=new "+P+"api.pokemon.species.Stats();public "+P+"api.pokemon.stats.Moveset temporaryMoveset,permanent=new "+P+"api.pokemon.stats.Moveset();public "+P+"battles.attacks.Attack attack;public "+P+"api.pokemon.species.Species getSpecies(){return species;}public "+P+"api.pokemon.species.Stats getForm(){return form;}public "+P+"api.pokemon.stats.Moveset getMoveset(){return temporaryMoveset==null?permanent:temporaryMoveset;}public void setTemporaryMoveset("+P+"api.pokemon.stats.Moveset m){temporaryMoveset=m;}}");
        Path dir=Files.createTempDirectory(Path.of(".cache"),"mega-followup-");List<String> files=new ArrayList<>();
        for(var row:s.entrySet()){Path p=dir.resolve(row.getKey().replace('.','/')+".java");Files.createDirectories(p.getParent());Files.writeString(p,row.getValue());files.add(p.toString());}
        Path cases=dir.resolve("fixture/MegaCases.java");Files.createDirectories(cases.getParent());Files.copy(Path.of("tests/resources/MegaFollowupCases.java.txt"),cases);files.add(cases.toString());
        List<String> command=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));command.addAll(files);
        check(ToolProvider.getSystemJavaCompiler().run(null,null,null,command.toArray(new String[0]))==0,"Fixture compilation");
        Set<String> isolated=new HashSet<>(s.keySet());isolated.addAll(List.of("local.pixelmon.backport.ZaMoves","local.pixelmon.backport.ZaMoves$Change","local.pixelmon.backport.client.EditorImport"));
        for(String name:isolated)if(name.startsWith("local.")){Path p=dir.resolve(name.replace('.','/')+".class");Files.createDirectories(p.getParent());try(var in=MegaFollowupVerification.class.getResourceAsStream('/'+name.replace('.','/')+".class")){Files.copy(in,p);}}
        try(var loader=new URLClassLoader(new URL[]{dir.toUri().toURL()},MegaFollowupVerification.class.getClassLoader()){
            protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{synchronized(getClassLoadingLock(name)){if(!isolated.contains(name)&&!name.startsWith("fixture."))return super.loadClass(name,resolve);Class<?> c=findLoadedClass(name);if(c==null)c=findClass(name);if(resolve)resolveClass(c);return c;}}
        }){try{loader.loadClass("fixture.MegaCases").getMethod("run").invoke(null);}catch(java.lang.reflect.InvocationTargetException e){throw new AssertionError("Mega followup policies failed",e.getCause());}}
        System.out.println("PASS Nihil Light native type chart, dual/inverse matchups, temporary move and PP restoration; detached editor identity/slot boundaries. No game launched.");
    }
}
