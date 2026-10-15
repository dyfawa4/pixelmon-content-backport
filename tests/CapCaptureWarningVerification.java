import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Execute the shipped event method; isolate player messages and battle services. */
public final class CapCaptureWarningVerification {
    static void add(Map<String,String> s,String n,String body){s.put(n,"package "+n.substring(0,n.lastIndexOf('.'))+";"+body);}
    public static void main(String[] args)throws Exception {
        Map<String,String> s=new LinkedHashMap<>();
        String api="com.pixelmonmod.pixelmon.api.pokemon.",parts="com.pixelmonmod.pixelmon.battles.controller.participants.";
        add(s,api+"species.Species","public class Species {public int dex;public int getDex(){return dex;}}");
        add(s,api+"Pokemon","public class Pokemon {public final "+api+"species.Species species=new "+api+"species.Species();public "+api+"species.Species getSpecies(){return species;}}");
        add(s,parts+"PixelmonWrapper","public class PixelmonWrapper {public "+api+"Pokemon pokemon=new "+api+"Pokemon();}");
        add(s,parts+"BattleParticipant","public class BattleParticipant {public int team;public PixelmonWrapper[] allPokemon=new PixelmonWrapper[0];}");
        add(s,parts+"WildPixelmonParticipant","public class WildPixelmonParticipant extends BattleParticipant {}");
        add(s,parts+"PlayerParticipant","public class PlayerParticipant extends BattleParticipant {public net.minecraft.entity.player.ServerPlayerEntity player=new net.minecraft.entity.player.ServerPlayerEntity();}");
        add(s,"com.pixelmonmod.pixelmon.battles.controller.BattleController","public class BattleController {public boolean simulateMode,battleEnded;public java.util.List<"+parts+"BattleParticipant> participants=new java.util.ArrayList<>();}");
        add(s,"com.pixelmonmod.pixelmon.api.events.battles.BattleStartedEvent","public class BattleStartedEvent {public static class Post {public com.pixelmonmod.pixelmon.battles.controller.BattleController bc;public com.pixelmonmod.pixelmon.battles.controller.BattleController getBattleController(){return bc;}}}");
        add(s,"net.minecraft.util.text.ITextComponent","public interface ITextComponent {}");
        add(s,"net.minecraft.util.text.TranslationTextComponent","public class TranslationTextComponent implements ITextComponent {public String key;public TranslationTextComponent(String k){key=k;}}");
        add(s,"net.minecraft.entity.player.ServerPlayerEntity","public class ServerPlayerEntity {public final java.util.UUID id=java.util.UUID.randomUUID();public int messages;public java.util.UUID func_110124_au(){return id;}public void func_146105_b(net.minecraft.util.text.ITextComponent message,boolean action){if(action || !\"pixelmon.backport.cap.capture_warning\".equals(((net.minecraft.util.text.TranslationTextComponent)message).key))throw new AssertionError();messages++;}}");
        add(s,"fixture.CapWarningCases",String.join("\n",
            "public class CapWarningCases {static void check(boolean b){if(!b)throw new AssertionError();}public static void run(){",
            "var hook=new local.pixelmon.backport.BurstCapture();var e=new com.pixelmonmod.pixelmon.api.events.battles.BattleStartedEvent.Post();hook.battle(e);var b=new com.pixelmonmod.pixelmon.battles.controller.BattleController();e.bc=b;var p=new "+parts+"PlayerParticipant();var wild=new "+parts+"WildPixelmonParticipant();wild.team=1;var target=new "+parts+"PixelmonWrapper();wild.allPokemon=new "+parts+"PixelmonWrapper[]{target};b.participants.add(p);b.participants.add(wild);",
            "target.pokemon.species.dex=25;hook.battle(e);check(p.player.messages==0);target.pokemon.species.dex=658;hook.battle(e);check(p.player.messages==0);target.pokemon.species.dex=1026;b.simulateMode=true;hook.battle(e);check(p.player.messages==0);b.simulateMode=false;b.battleEnded=true;hook.battle(e);check(p.player.messages==0);b.battleEnded=false;wild.team=0;hook.battle(e);check(p.player.messages==0);wild.team=1;hook.battle(e);hook.battle(e);check(p.player.messages==1);",
            "var p2=new "+parts+"PlayerParticipant();b.participants.add(p2);hook.battle(e);check(p.player.messages==1 && p2.player.messages==1);var fresh=new com.pixelmonmod.pixelmon.battles.controller.BattleController();fresh.participants.add(p);fresh.participants.add(wild);e.bc=fresh;hook.battle(e);check(p.player.messages==2);var nonwild=new "+parts+"BattleParticipant();nonwild.team=1;nonwild.allPokemon=wild.allPokemon;fresh=new com.pixelmonmod.pixelmon.battles.controller.BattleController();fresh.participants.add(p);fresh.participants.add(nonwild);e.bc=fresh;hook.battle(e);check(p.player.messages==2);",
            "}}"));
        Path dir=Files.createTempDirectory(Path.of(".cache"),"cap-warning-fixture-");
        List<String> options=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
        for(var row:s.entrySet()){Path p=dir.resolve(row.getKey().replace('.','/')+".java");Files.createDirectories(p.getParent());Files.writeString(p,row.getValue());options.add(p.toString());}
        if(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))!=0)throw new AssertionError("Cap warning fixture compilation failed");
        // Keep the production event, its lambda, static map and constructor. Other
        // capture methods are already covered separately and need no fake game API.
        ClassNode node=Version13StaticVerification.node("local/pixelmon/backport/BurstCapture");
        node.methods.removeIf(m->!m.name.equals("battle")&&!m.name.equals("<init>")&&!m.name.equals("<clinit>")&&!m.name.startsWith("lambda$battle$"));
        ClassWriter writer=new ClassWriter(0);node.accept(writer);byte[] production=writer.toByteArray();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{dir.toUri().toURL()},CapCaptureWarningVerification.class.getClassLoader()){
            protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{synchronized(getClassLoadingLock(n)){Class<?> c=findLoadedClass(n);String base=n.contains("$")?n.substring(0,n.indexOf('$')):n;if(c==null&&s.containsKey(base))c=findClass(n);if(c==null&&n.equals("local.pixelmon.backport.BurstCapture"))c=defineClass(n,production,0,production.length);if(c==null)c=super.loadClass(n,false);if(resolve)resolveClass(c);return c;}}
        }){loader.loadClass("fixture.CapWarningCases").getMethod("run").invoke(null);}
        System.out.println("PASS shipped Cap battle-entry warning: once per player/battle, two players and fresh battle; ordinary Pikachu/Greninja, friendly target, nonwild, ended and AI simulation negatives. No live game initialized.");
    }
}
