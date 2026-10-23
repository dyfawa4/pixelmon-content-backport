import java.nio.file.*;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.net.*;
import java.lang.reflect.*;
import java.util.*;
import javax.tools.ToolProvider;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Execute the shipped helper against a controlled battle clock and actors. */
public final class AshWaterBoostVerification {
    static final String API="com.pixelmonmod.pixelmon.";
    static final Map<String,String> FIXTURES=new LinkedHashMap<>();
    static void fixture(String suffix,String body){String name=API+suffix;int dot=name.lastIndexOf('.');FIXTURES.put(name,"package "+name.substring(0,dot)+";"+body);}
    static void check(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    static JsonElement json(String path)throws Exception {try(var in=AshWaterBoostVerification.class.getClassLoader().getResourceAsStream(path)){return new JsonParser().parse(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}}
    static ClassLoader loader;
    static Class<?> wrapper,battle,attack,helper;
    static Object actor(Object bc,String form,boolean bond)throws Exception{
        return wrapper.getConstructor(battle,String.class,boolean.class).newInstance(bc,form,bond);
    }
    static Object move(String name,String type,boolean status)throws Exception{return attack.getConstructor(String.class,String.class,boolean.class).newInstance(name,type,status);}
    static void field(Object target,String name,Object value)throws Exception{target.getClass().getField(name).set(target,value);}
    static int power(Object p,Object a,int n)throws Exception{return (Integer)helper.getMethod("power",wrapper,attack,int.class).invoke(null,p,a,n);}
    static void call(String name,Object p)throws Exception{helper.getMethod(name,wrapper).invoke(null,p);}
    static void clock(String name,Object bc)throws Exception{helper.getMethod(name,battle).invoke(null,bc);}
    static List<?> messages(Object bc)throws Exception{return (List<?>)battle.getField("messages").get(bc);}
    public static void main(String[] args)throws Exception{
        fixture("api.pokemon.Element","public enum Element { WATER,FIRE }");
        fixture("api.battles.AttackCategory","public enum AttackCategory { STATUS,SPECIAL }");
        fixture("api.pokemon.ability.Ability","public interface Ability {}");
        fixture("api.pokemon.ability.abilities.BattleBond","public class BattleBond implements "+API+"api.pokemon.ability.Ability {}");
        fixture("api.pokemon.species.Stats","public class Stats {public String name;public Stats(String n){name=n;}public boolean isForm(String... names){for(String n:names)if(n.equals(name))return true;return false;}}");
        fixture("api.pokemon.species.Species","public class Species {public int dex=658;public int getDex(){return dex;}}");
        fixture("api.pokemon.Pokemon","public class Pokemon {public "+API+"api.pokemon.ability.Ability ability;public "+API+"api.pokemon.ability.Ability getAbility(){return ability;}}");
        fixture("battles.attacks.ImmutableAttack","public class ImmutableAttack {public String name;public String getAttackName(){return name;}}");
        fixture("battles.attacks.Attack","public class Attack {public ImmutableAttack move=new ImmutableAttack();public "+API+"api.pokemon.Element type;public "+API+"api.battles.AttackCategory category;public Attack(String n,String t,boolean s){move.name=n;type="+API+"api.pokemon.Element.valueOf(t);category=s?"+API+"api.battles.AttackCategory.STATUS:"+API+"api.battles.AttackCategory.SPECIAL;}public ImmutableAttack getMove(){return move;}public "+API+"api.pokemon.Element getType(){return type;}public "+API+"api.battles.AttackCategory getAttackCategory(){return category;}}");
        fixture("battles.controller.BattleController","public class BattleController {public int battleTurn;public boolean battleEnded,simulateMode;public java.util.List<String> messages=new java.util.ArrayList<>();public void sendToAll(String key,Object... values){messages.add(key);}}");
        fixture("battles.controller.participants.PixelmonWrapper","public class PixelmonWrapper {public "+API+"battles.controller.BattleController bc;public "+API+"api.pokemon.Pokemon pokemon=new "+API+"api.pokemon.Pokemon();public "+API+"battles.attacks.Attack attack;public boolean usingZ,alive=true;public "+API+"api.pokemon.species.Species species=new "+API+"api.pokemon.species.Species();public "+API+"api.pokemon.species.Stats form;public PixelmonWrapper("+API+"battles.controller.BattleController b,String f,boolean bond){bc=b;form=new "+API+"api.pokemon.species.Stats(f);pokemon.ability=bond?new "+API+"api.pokemon.ability.abilities.BattleBond():new "+API+"api.pokemon.ability.Ability(){};}public boolean isAlive(){return alive;}public "+API+"api.pokemon.species.Species getSpecies(){return species;}public "+API+"api.pokemon.species.Stats getForm(){return form;}public String getNickname(){return "+'"'+"actor"+'"'+";}}");
        FIXTURES.put("local.pixelmon.backport.GreninjaEnhancement","package local.pixelmon.backport; public class GreninjaEnhancement {public static boolean enabled;public static boolean enabled(){return enabled;}} ");
        Path dir=Files.createTempDirectory(Paths.get(".cache"),"ash-water-fixture-");
        List<String> compile=new ArrayList<>(List.of("--release","11","-proc:none","-d",dir.toString()));
        for(var row:FIXTURES.entrySet()){Path p=dir.resolve(row.getKey().replace('.','/')+".java");Files.createDirectories(p.getParent());Files.writeString(p,row.getValue());compile.add(p.toString());}
        check(ToolProvider.getSystemJavaCompiler().run(null,null,null,compile.toArray(new String[0]))==0,"Fixture compilation failed");
        loader=new URLClassLoader(new URL[]{dir.toUri().toURL()},AshWaterBoostVerification.class.getClassLoader()){
            protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
                synchronized(getClassLoadingLock(name)){
                    Class<?> c=findLoadedClass(name);
                    if(c==null&&FIXTURES.containsKey(name))c=findClass(name);
                    if(c==null&&(name.equals("local.pixelmon.backport.AshWaterBoost")||name.equals("local.pixelmon.backport.PermanentAsh")))
                        try(var in=getParent().getResourceAsStream(name.replace('.','/')+".class")){byte[] bytes=in.readAllBytes();c=defineClass(name,bytes,0,bytes.length);}catch(Exception e){throw new ClassNotFoundException(name,e);}
                    if(c==null)c=super.loadClass(name,false);if(resolve)resolveClass(c);return c;
                }
            }
        };
        wrapper=loader.loadClass(API+"battles.controller.participants.PixelmonWrapper");battle=loader.loadClass(API+"battles.controller.BattleController");attack=loader.loadClass(API+"battles.attacks.Attack");helper=loader.loadClass("local.pixelmon.backport.AshWaterBoost");
        Object bc=battle.getConstructor().newInstance(),shuriken=move("Water Shuriken","WATER",false),surf=move("Surf","WATER",false),fire=move("Flamethrower","FIRE",false),status=move("Aqua Ring","WATER",true);
        field(bc,"battleTurn",7);Object ash=actor(bc,"ash",true);field(ash,"attack",shuriken);
        check(power(ash,shuriken,20)==20,"Preview started a boost");call("start",ash);check(power(ash,shuriken,20)==20,"Base core must not grant Water boost");loader.loadClass("local.pixelmon.backport.GreninjaEnhancement").getField("enabled").setBoolean(null,true);call("start",ash);
        check(power(ash,shuriken,20)==30&&power(ash,surf,90)==135,"Current attack or other own Water attack lacks boost");
        check(power(ash,fire,90)==90&&power(ash,status,20)==20,"Non-Water/status boosted");
        Object teammate=actor(bc,"ash",true);check(power(teammate,shuriken,20)==20,"Boost leaked to teammate/opponent");
        for(String form:new String[]{"base","mega"}){Object normal=actor(bc,form,true);field(normal,"attack",shuriken);call("start",normal);check(power(normal,shuriken,20)==20,"Ordinary/Mega form boosted");}
        for(String form:new String[]{"ash","ash_permanent","mega_ash"}){Object changed=actor(battle.getConstructor().newInstance(),form,false);field(changed,"attack",shuriken);call("start",changed);check(power(changed,shuriken,20)==30,"Ash origin lost boost after ability change: "+form);call("clear",changed);}
        field(bc,"battleTurn",8);clock("expire",bc);check(power(ash,shuriken,20)==30,"Expired after one turn");
        field(bc,"battleTurn",9);clock("expire",bc);check(power(ash,shuriken,20)==30,"Expired after two turns");
        field(bc,"battleTurn",10);clock("expire",bc);check(power(ash,shuriken,20)==30,"Expired after three turns");
        field(bc,"battleTurn",11);clock("expire",bc);check(power(ash,shuriken,20)==30,"Expired after four turns");
        field(bc,"battleTurn",12);clock("expire",bc);check(power(ash,shuriken,20)==20,"Survived fifth completed turn");
        check(Collections.frequency(messages(bc),"pixelmon_backport.battle.ash_water_boost_end")==1,"Expiry message missing/duplicated");clock("expire",bc);
        check(Collections.frequency(messages(bc),"pixelmon_backport.battle.ash_water_boost_end")==1,"Repeated expiry message");
        call("start",ash);field(bc,"battleTurn",13);call("start",ash);check(power(ash,shuriken,20)==30,"Repeated use stacks");
        field(bc,"battleTurn",17);clock("expire",bc);check(power(ash,shuriken,20)==30,"Refresh failed to extend");
        field(bc,"battleTurn",18);clock("expire",bc);check(power(ash,shuriken,20)==20,"Refreshed effect never expires");
        Object permanent=actor(bc,"ash_permanent",true);field(permanent,"attack",shuriken);call("start",permanent);check(power(permanent,shuriken,20)==30,"Permanent Ash form omitted");
        call("clear",permanent);check(power(permanent,shuriken,20)==20,"Switch retains boost");
        field(permanent,"usingZ",true);call("start",permanent);check(power(permanent,shuriken,20)==20,"Z move starts regular move buff");field(permanent,"usingZ",false);
        call("start",ash);Object sim=battle.getConstructor().newInstance();field(sim,"simulateMode",true);field(sim,"battleTurn",14);Object clone=actor(sim,"ash",true);
        helper.getMethod("copy",wrapper,wrapper).invoke(null,ash,clone);check(power(clone,shuriken,20)==30,"AI clone lost active state");call("clear",clone);
        check(power(ash,shuriken,20)==30&&messages(sim).isEmpty(),"AI modified live boost or broadcast");
        // Native AI wrappers retain the SAME controller. simulateMode is reset
        // before the live endTurn; it cannot identify a retained AI copy then.
        int endsBefore=Collections.frequency(messages(bc),"pixelmon_backport.battle.ash_water_boost_end");
        Object copy1=actor(bc,"ash",true),copy2=actor(bc,"ash",true);
        helper.getMethod("copy",wrapper,wrapper).invoke(null,ash,copy1);
        helper.getMethod("copy",wrapper,wrapper).invoke(null,ash,copy2);
        field(bc,"simulateMode",true);check(power(copy1,shuriken,20)==30,"Shared-controller AI copy lost boost");
        field(bc,"simulateMode",false);field(bc,"battleTurn",23);clock("expire",bc);
        check(Collections.frequency(messages(bc),"pixelmon_backport.battle.ash_water_boost_end")==endsBefore+1,"AI copies duplicate the live expiry announcement");
        Object freshCopy=actor(bc,"ash",true);helper.getMethod("copy",wrapper,wrapper).invoke(null,ash,freshCopy);
        field(freshCopy,"attack",shuriken);field(bc,"simulateMode",true);call("start",freshCopy);
        field(bc,"simulateMode",false);int messagesBefore=messages(bc).size();call("clear",freshCopy);
        check(messages(bc).size()==messagesBefore,"AI-started boost broadcasts after simulation flag resets");
        call("start",ash);field(bc,"simulateMode",true);field(bc,"battleTurn",20);clock("expire",bc);
        field(bc,"battleTurn",17);field(bc,"simulateMode",false);
        check(power(ash,shuriken,20)==30,"Simulated endTurn consumed the live actor's boost");
        call("start",ash);field(ash,"alive",false);clock("expire",bc);check(power(ash,shuriken,20)==20,"Fainted actor retains boost");field(ash,"alive",true);
        call("start",ash);clock("end",bc);check(power(ash,shuriken,20)==20,"Battle end retains boost");
        // Verify hooks relative to the real native attack/turn timeline, not just fixture behavior.
        boolean increment=false;for(MethodNode m:Version13StaticVerification.node(API.replace('.','/')+"battles/controller/BattleController").methods)if(m.name.equals("endTurn"))for(AbstractInsnNode i:m.instructions)if(i instanceof FieldInsnNode&&i.getOpcode()==Opcodes.PUTFIELD&&((FieldInsnNode)i).name.equals("battleTurn"))increment=true;
        check(increment,"Native endTurn clock contract changed");
        boolean sharedController=false;
        for(MethodNode m:Version13StaticVerification.node(API.replace('.','/')+"battles/controller/participants/PixelmonWrapper").methods)
            if(m.name.equals("<init>")&&m.desc.equals("(L"+API.replace('.','/')+"battles/controller/participants/PixelmonWrapper;)V"))
                for(AbstractInsnNode i:m.instructions)if(i instanceof FieldInsnNode&&i.getOpcode()==Opcodes.PUTFIELD&&((FieldInsnNode)i).name.equals("bc"))sharedController=true;
        check(sharedController,"Native AI copy controller contract changed");
        for(String key:new String[]{"pixelmon_backport.battle.ash_water_boost","pixelmon_backport.battle.ash_water_boost_end"})for(String lang:new String[]{"en_us","zh_cn"})check(json("assets/pixelmon_backport/lang/"+lang+".json").getAsJsonObject().has(key),"Missing battle message "+key);
        System.out.println("PASS production Ash Water boost: current attack, own actor isolation, ordinary/form/ability/Z exclusions, five-turn expiry, refresh without stacking, switch/faint/end cleanup, AI clone isolation and bilingual end messages.");
    }
}
