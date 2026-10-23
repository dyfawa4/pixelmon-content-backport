import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import javax.tools.ToolProvider;
import com.google.gson.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Execute native Boss identity/client eligibility and the shipped pre-selection guard. */
public final class BossCaptureVerification {
    static final String API="com.pixelmonmod.pixelmon.",PART=API+"battles.controller.participants.";
    static void add(Map<String,String>s,String n,String b){s.put(n,"package "+n.substring(0,n.lastIndexOf('.'))+";"+b);}
    static ClassNode node(byte[] b){var n=new ClassNode();new ClassReader(b).accept(n,0);return n;}
    static byte[] write(ClassNode n){var w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
    static ClassNode nativeNode(JarFile j,String n)throws Exception{try(var in=j.getInputStream(j.getJarEntry(n.replace('.','/')+".class"))){return node(in.readAllBytes());}}
    static void install(Path dir,JarFile jar,String cls,String... names)throws Exception{
        Path p=dir.resolve(cls.replace('.','/')+".class");var stub=node(Files.readAllBytes(p));int count=0;
        for(var m:nativeNode(jar,cls).methods)if(Set.of(names).contains(m.name)){stub.methods.removeIf(x->x.name.equals(m.name)&&x.desc.equals(m.desc));stub.methods.add(m);count++;}
        if(count!=names.length)throw new AssertionError("Native signature changed "+cls);Files.write(p,write(stub));
    }
    public static void main(String[] args)throws Exception{
        for(String lang:List.of("zh_cn","en_us")){
            var text=Version13StaticVerification.resource("assets/pixelmon_backport/lang/"+lang+".json");
            String field=text.get("pixelmon.backport.cap.field_capture_blocked").getAsString();
            String warning=text.get("pixelmon.backport.cap.capture_warning").getAsString();
            if(lang.equals("zh_cn")?(!field.contains("无法场外捕捉")||!warning.contains("%s")||!warning.contains("2倍")||!warning.contains("原生倍率乘0.5")||!warning.contains("只能在战斗中")):(!field.contains("only be caught in battle")||!warning.contains("%s")||!warning.contains("2x")||!warning.contains("0.5x their native")))throw new AssertionError("Cap preferred ball warning scope "+lang);
        }
        Map<String,String>s=new LinkedHashMap<>();
        add(s,API+"api.pokemon.boss.BossTier","public class BossTier {public String id;public Boolean isBoss;public BossTier(String s){id=s;}public boolean isBoss(){throw new AssertionError();}public boolean isNotBoss(){throw new AssertionError();}}");
        add(s,PART+"ParticipantType","public enum ParticipantType {WildPokemon,Trainer,Player}");
        add(s,API+"api.battles.BagSection","public enum BagSection {POKEBALLS,HEALING}");
        add(s,API+"api.battles.BattleMode","public enum BattleMode {MAIN_MENU,BAG}");
        add(s,API+"client.gui.battles.PixelmonClientData","public class PixelmonClientData {public "+API+"api.pokemon.boss.BossTier bossTier;public boolean blockCapture;}");
        add(s,API+"client.gui.battles.ClientBattleManager","public class ClientBattleManager {public "+PART+"ParticipantType[][] battleSetup;public PixelmonClientData[] displayedEnemyPokemon;public "+API+"api.battles.BagSection bagSection;public "+API+"api.battles.BattleMode mode="+API+"api.battles.BattleMode.BAG;public int messages;public boolean canCatchOpponent(){throw new AssertionError();}public void addMessage(String s){if(!s.equals(\"gui.choosebag.nopokeballs\"))throw new AssertionError();messages++;}public void setMode("+API+"api.battles.BattleMode m){mode=m;}}");
        add(s,"net.minecraft.client.resources.I18n","public class I18n {public static String func_135052_a(String k,Object... args){return k;}}");
        add(s,"net.minecraft.entity.Entity","public class Entity {}");
        add(s,"net.minecraft.entity.LivingEntity","public class LivingEntity extends Entity {}");
        add(s,"net.minecraft.entity.player.ServerPlayerEntity","public class ServerPlayerEntity extends net.minecraft.entity.LivingEntity {public int messages,packets;public void func_146105_b(net.minecraft.util.text.ITextComponent c,boolean b){if(b || !((net.minecraft.util.text.TranslationTextComponent)c).key.equals(\"gui.choosebag.nopokeballs\"))throw new AssertionError();messages++;}}");
        add(s,"net.minecraft.util.text.ITextComponent","public interface ITextComponent {}");
        add(s,"net.minecraft.util.text.TranslationTextComponent","public class TranslationTextComponent implements ITextComponent {public String key;public TranslationTextComponent(String s){key=s;}}");
        add(s,API+"entities.pixelmon.PixelmonEntity","public class PixelmonEntity {public "+API+"api.pokemon.boss.BossTier tier;public "+API+"api.pokemon.boss.BossTier getBossTier(){return tier;}}");
        add(s,PART+"PixelmonWrapper","public class PixelmonWrapper {public boolean onBattlefield=true,alive=true,wait=true;public int selectedAction=41;public java.util.UUID id=java.util.UUID.randomUUID();public "+API+"entities.pixelmon.PixelmonEntity entity=new "+API+"entities.pixelmon.PixelmonEntity();public boolean isAlive(){return alive;}public java.util.UUID getPokemonUUID(){return id;}}");
        add(s,PART+"BattleParticipant","public class BattleParticipant {public int team;public java.util.List<PixelmonWrapper> controlledPokemon=new java.util.ArrayList<>();public java.util.List<BattleParticipant> opponents=new java.util.ArrayList<>();public java.util.List<BattleParticipant> getOpponents(){return opponents;}}");
        add(s,PART+"WildPixelmonParticipant","public class WildPixelmonParticipant extends BattleParticipant {}");
        add(s,PART+"PlayerParticipant","public class PlayerParticipant extends BattleParticipant {public net.minecraft.entity.player.ServerPlayerEntity player=new net.minecraft.entity.player.ServerPlayerEntity();public "+API+"battles.controller.BattleController bc;public static boolean[] canSwitch(PixelmonWrapper p){return new boolean[]{true,true};}}");
        add(s,API+"battles.controller.BattleController","public class BattleController {public boolean simulateMode,battleEnded,raid,tera,waiting=true;public "+PART+"PlayerParticipant participant;public boolean isRaid(){return raid;}public boolean isWaiting(){return waiting;}public "+PART+"BattleParticipant getParticipantForEntity(net.minecraft.entity.LivingEntity p){return participant!=null&&participant.player==p?participant:null;}}");
        add(s,"net.minecraft.item.ItemStack","public class ItemStack {public boolean ball,empty;public int count=9;public String kind;public boolean func_190926_b(){return empty;}}");
        add(s,API+"api.pokemon.item.pokeball.PokeBall","public class PokeBall {}");
        add(s,API+"items.PokeBallPart","public interface PokeBallPart {public static java.util.Optional<"+API+"api.pokemon.item.pokeball.PokeBall> getPokeBall(net.minecraft.item.ItemStack s){return s.ball?java.util.Optional.of(new "+API+"api.pokemon.item.pokeball.PokeBall()):java.util.Optional.empty();}}");
        add(s,API+"api.events.battles.BagItemEvent","public class BagItemEvent {public static class SelectItem {public static class Pre {public boolean canceled;public "+API+"battles.controller.BattleController battle;public net.minecraft.entity.player.ServerPlayerEntity player;public net.minecraft.item.ItemStack stack;public boolean isCanceled(){return canceled;}public void setCanceled(boolean b){canceled=b;}public "+API+"battles.controller.BattleController getBattleController(){return battle;}public net.minecraft.entity.player.ServerPlayerEntity getPlayer(){return player;}public net.minecraft.item.ItemStack getFoundItem(){return stack;}}}}");
        add(s,API+"comm.data.PixelmonPacket","public interface PixelmonPacket {} ");
        add(s,API+"comm.data.AbstractPixelmonPacket","public class AbstractPixelmonPacket implements PixelmonPacket {}");
        add(s,API+"comm.packetHandlers.battles.BackToMainMenuPacket","public class BackToMainMenuPacket extends "+API+"comm.data.AbstractPixelmonPacket {public BackToMainMenuPacket(java.util.List<Boolean> switching,boolean flee,java.util.List<java.util.UUID> picking){if(picking.isEmpty()||switching.size()!=picking.size())throw new AssertionError();}}");
        add(s,API+"api.util.helpers.NetworkHelper","public class NetworkHelper {public static void sendPacket("+API+"comm.data.PixelmonPacket p,net.minecraft.entity.player.ServerPlayerEntity player){player.packets++;}}");
        add(s,"local.pixelmon.backport.TeraRaids","public class TeraRaids {public static TeraRaidParticipant get("+API+"battles.controller.BattleController b){return b.tera?new TeraRaidParticipant():null;}}");
        add(s,"local.pixelmon.backport.TeraRaidParticipant","public class TeraRaidParticipant {}");
        add(s,API+"api.pokemon.species.Species","public class Species {public int dex;public int getDex(){return dex;}}");
        add(s,API+"api.pokemon.Pokemon","public class Pokemon {public boolean shiny;public final species.Species species=new species.Species();public species.Species getSpecies(){return species;}}".replace("species.Species",API+"api.pokemon.species.Species"));
        add(s,"com.pixelmonmod.api.registry.RegistryValue","public class RegistryValue<T> {public T value;public RegistryValue(T v){value=v;}}");
        add(s,API+"api.pokemon.item.pokeball.PokeBall","public class PokeBall {public String kind;public PokeBall(String k){kind=k;}public boolean is(com.pixelmonmod.api.registry.RegistryValue<PokeBall>... balls){for(var b:balls)if(java.util.Objects.equals(kind,b.value.kind))return true;return false;}}");
        add(s,API+"api.pokemon.item.pokeball.PokeBallRegistry","public class PokeBallRegistry {public static final com.pixelmonmod.api.registry.RegistryValue<PokeBall> MASTER_BALL=new com.pixelmonmod.api.registry.RegistryValue<>(new PokeBall(\"master_ball\")),ORIGIN_BALL=new com.pixelmonmod.api.registry.RegistryValue<>(new PokeBall(\"origin_ball\")),PARK_BALL=new com.pixelmonmod.api.registry.RegistryValue<>(new PokeBall(\"park_ball\"));}");
        add(s,API+"entities.npcs.NPCTrainer","public class NPCTrainer {}");
        s.compute(API+"items.PokeBallPart",(k,v)->v.replace("new "+API+"api.pokemon.item.pokeball.PokeBall()","new "+API+"api.pokemon.item.pokeball.PokeBall(s.kind)"));
        s.compute(API+"entities.pixelmon.PixelmonEntity",(k,v)->v.replace("public class PixelmonEntity {","public class PixelmonEntity {public boolean owned;public "+API+"entities.npcs.NPCTrainer trainer;public "+API+"api.pokemon.Pokemon pokemon=new "+API+"api.pokemon.Pokemon();public boolean hasOwner(){return owned;}public "+API+"entities.npcs.NPCTrainer getTrainer(){return trainer;}public "+API+"api.pokemon.Pokemon getPokemon(){return pokemon;}"));
        s.compute(PART+"PixelmonWrapper",(k,v)->v.replace("public class PixelmonWrapper {","public class PixelmonWrapper {public "+API+"api.pokemon.Pokemon pokemon=new "+API+"api.pokemon.Pokemon();"));
        s.compute("net.minecraft.entity.player.ServerPlayerEntity",(k,v)->v.replace("public int messages,packets;","public int messages,packets;public String lastKey;").replace("if(b || !((net.minecraft.util.text.TranslationTextComponent)c).key.equals(\"gui.choosebag.nopokeballs\"))throw new AssertionError();messages++;","lastKey=((net.minecraft.util.text.TranslationTextComponent)c).key;if(b || !(lastKey.equals(\"gui.choosebag.nopokeballs\")||lastKey.equals(\"pixelmon.backport.cap.forbidden_ball\")))throw new AssertionError();messages++;"));
        s.put("fixture.BossCaptureCases",Files.readString(Path.of("tests/resources/BossCaptureCases.java.txt")));
        s.put("fixture.CapBallGuardCases",Files.readString(Path.of("tests/resources/CapBallGuardCases.java.txt")));
        s.put("fixture.ClientCaptureReadinessCases",Files.readString(Path.of("tests/resources/ClientCaptureReadinessCases.java.txt")));
        CapPreferenceSupport.prepare(s);
        s.compute("net.minecraft.entity.player.ServerPlayerEntity",(k,v)->v.replace("lastKey.equals(\"pixelmon.backport.cap.forbidden_ball\")","(lastKey.equals(\"pixelmon.backport.cap.forbidden_ball\")||lastKey.equals(\"pixelmon.backport.cap.capture_warning\"))"));
        Path dir=Files.createTempDirectory(Path.of(".cache"),"boss-capture-");List<String> options=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
        for(var row:s.entrySet()){var p=dir.resolve(row.getKey().replace('.','/')+".java");Files.createDirectories(p.getParent());Files.writeString(p,row.getValue());options.add(p.toString());}
        if(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))!=0)throw new AssertionError("Boss fixture compilation failed");
        List<String> tiers=new ArrayList<>();
        try(var jar=new JarFile(args[0])){
            install(dir,jar,API+"api.pokemon.boss.BossTier","isBoss","isNotBoss");
            install(dir,jar,API+"client.gui.battles.ClientBattleManager","canCatchOpponent");
            var entries=jar.entries();while(entries.hasMoreElements()){var e=entries.nextElement();if(e.getName().startsWith("data/pixelmon/bosstiers/")&&e.getName().endsWith(".json"))try(var in=jar.getInputStream(e)){tiers.add(new JsonParser().parse(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().get("id").getAsString());}}
            // Actual native packet cancellation must precede the action commit.
            for(var m:nativeNode(jar,API+"comm.packetHandlers.battles.BagPacket").methods)if(m.name.equals("bagPacket")){
                int post=-1,commit=-1,index=0;boolean cancelReturn=false;
                for(var i:m.instructions){if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;if(c.name.equals("post")&&post<0){post=index;var branch=i.getNext();while(branch.getOpcode()<0)branch=branch.getNext();var ret=branch.getNext();while(ret.getOpcode()<0)ret=ret.getNext();cancelReturn=branch.getOpcode()==Opcodes.IFEQ&&ret.getOpcode()==Opcodes.RETURN;}if(c.name.equals("setUseItem")&&commit<0)commit=index;}index++;}
                if(post<0||commit<=post||!cancelReturn)throw new AssertionError("Native pre-selection no longer prevents action commit");
            }
        }
        // Newly backported species use the same PixelmonEntity/BossTier API.
        // Any species/name/color allowlist would silently exclude future bosses.
        var guard=Version13StaticVerification.node("local/pixelmon/backport/BossCaptureGuard");boolean nativeBoss=false;
        for(var m:guard.methods)for(var i:m.instructions)if(i instanceof MethodInsnNode){var call=(MethodInsnNode)i;
            nativeBoss|=call.owner.equals("com/pixelmonmod/pixelmon/api/pokemon/boss/BossTier")&&call.name.equals("isBoss");
            if(Set.of("getDex","getSpecies","getColor","getID","getDisplayName").contains(call.name))throw new AssertionError("Boss guard must not filter by species/name/color: "+call.name);
        }
        if(!nativeBoss)throw new AssertionError("New species not covered by native Boss identity");
        int newSpecies=1; // Independent CapPikachu joins the same entity API.
        for(var p:Version13StaticVerification.resource("backport/content.json").getAsJsonArray("patches"))if(p.getAsJsonObject().get("mode").getAsString().equals("new"))newSpecies++;
        if(newSpecies<9)throw new AssertionError("Missing current backported species audit");
        Map<String,byte[]> production=new HashMap<>();for(String name:List.of("local.pixelmon.backport.BossCaptureGuard","local.pixelmon.backport.client.BossCaptureClient","local.pixelmon.backport.CapBallGuard"))production.put(name,write(Version13StaticVerification.node(name.replace('.','/'))));
        production.put("local.pixelmon.backport.BurstCapture",CapPreferenceSupport.burst());
        production.put("local.pixelmon.backport.CapBallPreference",CapPreferenceSupport.preference());
        for(boolean patched:new boolean[]{false,true}){
        if(patched){
            var actual=node(Files.readAllBytes(Path.of(".cache/fixes-162/patched-client-battle-manager.class")));
            Path path=dir.resolve((API+"client.gui.battles.ClientBattleManager").replace('.','/')+".class");var stub=node(Files.readAllBytes(path));int copied=0;
            for(var method:actual.methods)if(method.name.equals("canCatchOpponent")||method.name.contains("backport$captureDataReady")){
                stub.methods.removeIf(m->m.name.equals(method.name)&&m.desc.equals(method.desc));stub.methods.add(method);copied++;
            }
            if(copied!=2)throw new AssertionError("Actual capture readiness injection absent or duplicated");Files.write(path,write(stub));
        }
        try(var loader=new URLClassLoader(new URL[]{dir.toUri().toURL()},BossCaptureVerification.class.getClassLoader()){
            protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{synchronized(getClassLoadingLock(n)){Class<?> c=findLoadedClass(n);String base=n.contains("$")?n.substring(0,n.indexOf('$')):n;if(c==null&&s.containsKey(base))c=findClass(n);if(c==null&&production.containsKey(n)){byte[] b=production.get(n);c=defineClass(n,b,0,b.length);}if(c==null)c=super.loadClass(n,false);if(resolve)resolveClass(c);return c;}}
        }){loader.loadClass("fixture.BossCaptureCases").getMethod("run",String.class).invoke(null,String.join(",",tiers));loader.loadClass("fixture.CapBallGuardCases").getMethod("run").invoke(null);loader.loadClass("fixture.ClientCaptureReadinessCases").getMethod("run",boolean.class).invoke(null,patched);}
        }
        System.out.println("PASS native BossTier identity/client canCatchOpponent and shipped bag guards: all native/custom wild Boss tiers, stale category and forged selection; ordinary wild/medicine/raids/players/NPC/reserve/removed/ended/simulation unchanged, no item/action consumption. No game initialized.");
        System.out.println("PASS new-species coverage audit: "+newSpecies+" added species share native Boss identity, no dex/name/color allowlist; future species use the same guard.");
    }
}
