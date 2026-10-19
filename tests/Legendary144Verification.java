import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import javax.tools.ToolProvider;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Run shipped acquisition/form logic with native PCBox insertion and owner lookup bytecode. */
public final class Legendary144Verification {
    static void add(Map<String,String> s,String n,String b){s.put(n,"package "+n.substring(0,n.lastIndexOf('.'))+";"+b);}
    static byte[] bytes(JarFile jar,String name)throws Exception{try(var in=jar.getInputStream(jar.getJarEntry(name.replace('.','/')+".class"))){return in.readAllBytes();}}
    static ClassNode node(byte[] bytes){var n=new ClassNode();new ClassReader(bytes).accept(n,0);return n;}
    static byte[] write(ClassNode n){var w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
    static void nativeMethods(Path dir,JarFile jar,String name,String... names)throws Exception{
        Path path=dir.resolve(name.replace('.','/')+".class");ClassNode stub=node(Files.readAllBytes(path)),nativeNode=node(bytes(jar,name));
        Set<String> wanted=Set.of(names);
        for(MethodNode m:nativeNode.methods)if(wanted.contains(m.name)){
            if(name.endsWith("PCBox")&&!m.desc.startsWith("(Lcom/pixelmonmod/pixelmon/api/storage/StoragePosition;"))continue;
            stub.methods.removeIf(existing->existing.name.equals(m.name)&&existing.desc.equals(m.desc));stub.methods.add(m);
        }
        Files.write(path,write(stub));
    }
    static byte[] production(String name,JarFile legacy)throws Exception{
        ClassNode n=legacy==null?Version13StaticVerification.node(name.replace('.','/')):node(bytes(legacy,name));
        if(name.endsWith("AcquisitionAdvancements"))n.methods.removeIf(m->!Set.of("ownedStored","species","evolved").contains(m.name));
        if(name.endsWith("LegendAdvancements"))n.methods.removeIf(m->!Set.of("capture","award","formChanged","fusionCompleted","partnerFor","primal","megaRayquaza","<clinit>").contains(m.name));
        return write(n);
    }
    public static void main(String[] args)throws Exception{
        Map<String,String>s=new LinkedHashMap<>();String api="com.pixelmonmod.pixelmon.api.",storage=api+"storage.",pokemon=api+"pokemon.",parts="com.pixelmonmod.pixelmon.battles.controller.participants.";
        add(s,"net.minecraft.world.World","public class World {public boolean field_72995_K;}");
        add(s,"net.minecraft.entity.player.PlayerEntity","public class PlayerEntity {public java.util.UUID id=java.util.UUID.randomUUID();public net.minecraft.world.World field_70170_p=new net.minecraft.world.World();public java.util.UUID func_110124_au(){return id;}}");
        add(s,"net.minecraft.entity.player.ServerPlayerEntity","public class ServerPlayerEntity extends PlayerEntity {public final net.minecraft.advancements.PlayerAdvancements progress=new net.minecraft.advancements.PlayerAdvancements();public int nativeLegend,nativeSpec;public net.minecraft.server.MinecraftServer func_184102_h(){return net.minecraftforge.fml.server.ServerLifecycleHooks.server;}public net.minecraft.advancements.PlayerAdvancements func_192039_O(){return progress;}}");
        add(s,"net.minecraft.advancements.Advancement","public class Advancement {public String id;public Advancement(String s){id=s;}}");
        add(s,"net.minecraft.advancements.PlayerAdvancements","public class PlayerAdvancements {public final java.util.Set<String> earned=new java.util.HashSet<>();public boolean func_192750_a(Advancement a,String c){if(!c.equals(\"earned\"))throw new AssertionError();return earned.add(a.id);}}");
        add(s,"net.minecraft.advancements.AdvancementManager","public class AdvancementManager {public Advancement func_192778_a(net.minecraft.util.ResourceLocation id){return new Advancement(id.toString());}}");
        add(s,"net.minecraft.server.management.PlayerList","public class PlayerList {public java.util.Map<java.util.UUID,net.minecraft.entity.player.ServerPlayerEntity> players=new java.util.HashMap<>();public net.minecraft.entity.player.ServerPlayerEntity func_177451_a(java.util.UUID id){return players.get(id);}}");
        add(s,"net.minecraft.server.MinecraftServer","public class MinecraftServer {public boolean main=true;public final net.minecraft.server.management.PlayerList players=new net.minecraft.server.management.PlayerList();public boolean func_213162_bc(){return main;}public net.minecraft.server.management.PlayerList func_184103_al(){return players;}public net.minecraft.advancements.AdvancementManager func_191949_aK(){return new net.minecraft.advancements.AdvancementManager();}}");
        add(s,"net.minecraftforge.fml.server.ServerLifecycleHooks","public class ServerLifecycleHooks {public static net.minecraft.server.MinecraftServer server=new net.minecraft.server.MinecraftServer();public static net.minecraft.server.MinecraftServer getCurrentServer(){return server;}}");
        add(s,"net.minecraftforge.event.TickEvent","public class TickEvent {public enum Phase {START,END}public static class ServerTickEvent {public Phase phase;public ServerTickEvent(Phase p){phase=p;}}}");
        add(s,"net.minecraftforge.event.entity.player.PlayerEvent","public class PlayerEvent {public net.minecraft.entity.player.PlayerEntity player;public net.minecraft.entity.player.PlayerEntity getPlayer(){return player;}public static class PlayerLoggedInEvent extends PlayerEvent {}}");
        add(s,"net.minecraftforge.fml.event.server.FMLServerStoppedEvent","public class FMLServerStoppedEvent {}");
        add(s,api+"events.EggHatchEvent","public class EggHatchEvent {public static class Post {public "+pokemon+"Pokemon getPokemon(){return null;}}}");
        add(s,api+"events.PokemonReceivedEvent","public class PokemonReceivedEvent {public boolean isCanceled(){return false;}public "+pokemon+"Pokemon getPokemon(){return null;}}");
        add(s,storage+"StoragePosition","public class StoragePosition {public int box,order;public StoragePosition(int b,int o){box=b;order=o;}}");
        add(s,storage+"PokemonStorage","public abstract class PokemonStorage {public java.util.UUID uuid;public abstract "+pokemon+"Pokemon get(StoragePosition p);public abstract "+pokemon+"Pokemon[] getAll();public void setNeedsSaving(){}public void notifyListeners(StoragePosition p,"+pokemon+"Pokemon v,com.pixelmonmod.pixelmon.comm.EnumUpdateType... t){}}");
        add(s,storage+"PartyStorage","public class PartyStorage extends PokemonStorage {public "+pokemon+"Pokemon[] pokemon=new "+pokemon+"Pokemon[6];public "+pokemon+"Pokemon get(StoragePosition p){return pokemon[p.order];}public "+pokemon+"Pokemon[] getAll(){return pokemon;}public void set(StoragePosition p,"+pokemon+"Pokemon v){pokemon[p.order]=v;if(v!=null)v.setStorage(this,p);}}");
        add(s,storage+"PlayerPartyStorage","public class PlayerPartyStorage extends PartyStorage {}");
        add(s,storage+"TrainerPartyStorage","public class TrainerPartyStorage extends PartyStorage {}");
        add(s,storage+"PCStorage","public class PCStorage extends PokemonStorage {public java.util.UUID playerUUID;public String playerName;public PCBox[] boxes;public PCStorage(int count){boxes=new PCBox[count];for(int i=0;i<count;i++)boxes[i]=new PCBox(this,i);}public int getBoxCount(){return boxes.length;}public PCBox[] getBoxes(){return boxes;}public PCBox getBox(int i){return boxes[i];}public "+pokemon+"Pokemon get(StoragePosition p){return boxes[p.box].get(p);}public "+pokemon+"Pokemon[] getAll(){return java.util.Arrays.stream(boxes).flatMap(b->java.util.Arrays.stream(b.getAll())).toArray("+pokemon+"Pokemon[]::new);}public void forEach(java.util.function.BiConsumer<"+pokemon+"Pokemon,StoragePosition> c){for(var b:boxes)for(int i=0;i<30;i++)if(b.pokemon[i]!=null)c.accept(b.pokemon[i],new StoragePosition(b.boxNumber,i));}}");
        add(s,storage+"PCBox","public class PCBox extends PokemonStorage {public static final int POKEMON_PER_BOX=30;public PCStorage pc;public int boxNumber;public "+pokemon+"Pokemon[] pokemon=new "+pokemon+"Pokemon[30];public PCBox(PCStorage p,int i){pc=p;boxNumber=i;}public "+pokemon+"Pokemon get(StoragePosition p){return get(p.order);}public "+pokemon+"Pokemon get(int i){return pokemon[i];}public "+pokemon+"Pokemon[] getAll(){return pokemon;}public void set(StoragePosition p,"+pokemon+"Pokemon v){throw new AssertionError(\"Native method not installed\");}}");
        add(s,storage+"StorageProxy","public class StorageProxy {public static java.util.Map<java.util.UUID,PlayerPartyStorage> parties=new java.util.HashMap<>();public static java.util.Map<java.util.UUID,PCStorage> pcs=new java.util.HashMap<>();public static PlayerPartyStorage getParty(net.minecraft.entity.player.ServerPlayerEntity p){return parties.get(p.id);}public static PCStorage getPCForPlayer(net.minecraft.entity.player.ServerPlayerEntity p){return pcs.get(p.id);}}");
        add(s,"com.pixelmonmod.pixelmon.comm.EnumUpdateType","public enum EnumUpdateType {NONE}");
        add(s,pokemon+"species.palette.PaletteProperties","public class PaletteProperties {public String name=\"none\";public String getName(){return name;}}");
        add(s,pokemon+"species.Species","public class Species {public int dex;public Species(int d){dex=d;}public int getDex(){return dex;}}");
        add(s,pokemon+"species.Stats","public class Stats {public String name;public Species parent;public Stats(Species p,String n){parent=p;name=n;}public Species getParentSpecies(){return parent;}public String getName(){return name;}public boolean isForm(String... forms){return java.util.Arrays.asList(forms).contains(name);}}");
        add(s,"net.minecraft.item.Item","public class Item {}");
        add(s,"net.minecraft.item.ItemStack","public class ItemStack {public Item item;public ItemStack(Item i){item=i;}public Item func_77973_b(){return item;}}");
        add(s,"com.pixelmonmod.pixelmon.items.HeldItem","public class HeldItem extends net.minecraft.item.Item {}");
        for(String item:List.of("RustedSwordItem","RustedShieldItem"))add(s,"com.pixelmonmod.pixelmon.items.heldItems."+item,"public class "+item+" extends com.pixelmonmod.pixelmon.items.HeldItem {}");
        add(s,api+"registries.PixelmonItems","public class PixelmonItems {public static final com.pixelmonmod.pixelmon.items.heldItems.RustedSwordItem rusted_sword=new com.pixelmonmod.pixelmon.items.heldItems.RustedSwordItem();public static final com.pixelmonmod.pixelmon.items.heldItems.RustedShieldItem rusted_shield=new com.pixelmonmod.pixelmon.items.heldItems.RustedShieldItem();}");
        add(s,pokemon+"Pokemon","public class Pokemon {public "+storage+"PokemonStorage storage;public "+storage+"StoragePosition position;public species.Species unused;public com.pixelmonmod.pixelmon.api.pokemon.species.Species species;public com.pixelmonmod.pixelmon.api.pokemon.species.Stats form;public boolean egg,shiny;public com.pixelmonmod.pixelmon.api.pokemon.species.palette.PaletteProperties palette=new com.pixelmonmod.pixelmon.api.pokemon.species.palette.PaletteProperties();public com.pixelmonmod.pixelmon.api.pokemon.species.palette.PaletteProperties getPalette(){return palette;}public boolean isShiny(){return shiny;}public net.minecraft.item.ItemStack held=new net.minecraft.item.ItemStack(new net.minecraft.item.Item());public net.minecraft.nbt.CompoundNBT persistent=new net.minecraft.nbt.CompoundNBT();public Pokemon(int dex,String name){species=new com.pixelmonmod.pixelmon.api.pokemon.species.Species(dex);form=new com.pixelmonmod.pixelmon.api.pokemon.species.Stats(species,name);}public com.pixelmonmod.pixelmon.api.pokemon.species.Species getSpecies(){return species;}public com.pixelmonmod.pixelmon.api.pokemon.species.Stats getForm(){return form;}public boolean isEgg(){return egg;}public net.minecraft.item.ItemStack getHeldItem(){return held;}public net.minecraft.nbt.CompoundNBT getPersistentData(){return persistent;}public com.pixelmonmod.pixelmon.api.pokemon.ability.Ability getAbility(){return null;}public "+storage+"PokemonStorage getStorage(){return storage;}public "+storage+"StoragePosition getPosition(){return position;}public void setStorage("+storage+"PokemonStorage s,"+storage+"StoragePosition p){storage=s;position=p;local.pixelmon.backport.AcquisitionQueue.stored(this);}public java.util.UUID getOwnerPlayerUUID(){throw new AssertionError();}public net.minecraft.entity.player.ServerPlayerEntity getOwnerPlayer(){throw new AssertionError();}public String getOriginalTrainer(){return null;}public java.util.UUID getOriginalTrainerUUID(){return null;}public void setOriginalTrainer(java.util.UUID id,String n){}}");
        // Remove the intentionally unused short type token from the fixture source.
        s.compute(pokemon+"Pokemon",(k,v)->v.replace("public species.Species unused;",""));
        add(s,api+"advancements.triggers.LegendaryCaptureTrigger","public class LegendaryCaptureTrigger {public void trigger(net.minecraft.entity.player.ServerPlayerEntity p,"+pokemon+"Pokemon v){p.nativeLegend++;}}");
        add(s,api+"advancements.triggers.SpecTrigger","public class SpecTrigger {public void trigger(net.minecraft.entity.player.ServerPlayerEntity p,"+pokemon+"Pokemon v){p.nativeSpec++;}}");
        add(s,"com.pixelmonmod.pixelmon.advancements.PixelmonAdvancements","public class PixelmonAdvancements {public static final "+api+"advancements.triggers.LegendaryCaptureTrigger LEGENDARY_CAPTURE_TRIGGER=new "+api+"advancements.triggers.LegendaryCaptureTrigger();public static final "+api+"advancements.triggers.SpecTrigger SPEC_TRIGGER=new "+api+"advancements.triggers.SpecTrigger();}");
        add(s,parts+"PixelmonWrapper","public class PixelmonWrapper {public "+pokemon+"Pokemon pokemon;public com.pixelmonmod.pixelmon.battles.controller.BattleController bc;public BattleParticipant participant;public boolean onBattlefield,alive=true;public boolean isAlive(){return alive;}public "+pokemon+"species.Species getSpecies(){return pokemon.getSpecies();}public "+pokemon+"species.Stats getForm(){return pokemon.getForm();}public BattleParticipant getParticipant(){return participant;}}");
        add(s,parts+"BattleParticipant","public class BattleParticipant {public PixelmonWrapper[] allPokemon=new PixelmonWrapper[0];public java.util.List<PixelmonWrapper> controlledPokemon=new java.util.ArrayList<>();}");
        add(s,parts+"PlayerParticipant","public class PlayerParticipant extends BattleParticipant {public net.minecraft.entity.player.ServerPlayerEntity player;}");
        add(s,"com.pixelmonmod.pixelmon.battles.controller.BattleController","public class BattleController {public boolean simulateMode,battleEnded;public java.util.List<"+parts+"BattleParticipant> participants=new java.util.ArrayList<>();}");
        add(s,"local.pixelmon.backport.FieldMega","public class FieldMega {public static final String MARKER=\"BackportFieldMega\";public static boolean active("+pokemon+"Pokemon p){return p.getPersistentData().func_74764_b(MARKER);}}");
        add(s,"local.pixelmon.backport.Content","public class Content {public static final String NS=\"pixelmon_backport\";public static com.google.gson.JsonObject read(String path){try(var in=Content.class.getClassLoader().getResourceAsStream(path)){return new com.google.gson.JsonParser().parse(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}catch(Exception ex){throw new RuntimeException(ex);}}}");
        s.put("fixture.Legendary144Cases",Files.readString(Path.of("tests/resources/Legendary144Cases.java.txt")));
        s.put("fixture.LateBloomerCases",Files.readString(Path.of("tests/resources/LateBloomerCases.java.txt")));
        Path dir=Files.createTempDirectory(Path.of(".cache"),"legendary144-fixture-");
        List<String> options=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
        for(var row:s.entrySet()){Path p=dir.resolve(row.getKey().replace('.','/')+".java");Files.createDirectories(p.getParent());Files.writeString(p,row.getValue());options.add(p.toString());}
        if(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))!=0)throw new AssertionError("Legendary fixture compilation failed");
        try(JarFile nativeJar=new JarFile(args[0])){
            nativeMethods(dir,nativeJar,storage+"PCBox","set");
            nativeMethods(dir,nativeJar,pokemon+"Pokemon","getOwnerPlayerUUID","getOwnerPlayer");
        }
        boolean baseline=Files.isRegularFile(Path.of("dist/pixelmon-content-backport-1.16.5-1.4.3.jar"));
        if(!baseline)System.out.println("SKIP optional 1.4.3 baseline replay: published JAR absent; current production checks still run.");
        for(boolean legacy:baseline?new boolean[]{true,false}:new boolean[]{false})try(JarFile old=legacy?new JarFile("dist/pixelmon-content-backport-1.16.5-1.4.3.jar"):null){
            Map<String,byte[]> code=new HashMap<>();
            for(String n:List.of("AcquisitionAdvancements","AcquisitionQueue","LegendAdvancements","CrownedAdvancements","LateBloomerAdvancements","SpecialPaletteAdvancements"))if(!legacy||(!n.equals("CrownedAdvancements")&&!n.equals("LateBloomerAdvancements")&&!n.equals("SpecialPaletteAdvancements")))code.put("local.pixelmon.backport."+n,production("local.pixelmon.backport."+n,old));
            try(URLClassLoader loader=new URLClassLoader(new URL[]{dir.toUri().toURL()},Legendary144Verification.class.getClassLoader()){
                protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{synchronized(getClassLoadingLock(n)){Class<?> c=findLoadedClass(n);String base=n.contains("$")?n.substring(0,n.indexOf('$')):n;if(c==null&&s.containsKey(base))c=findClass(n);if(c==null&&code.containsKey(n)){byte[] b=code.get(n);c=defineClass(n,b,0,b.length);}if(c==null)c=super.loadClass(n,false);if(resolve)resolveClass(c);return c;}}
            }){loader.loadClass("fixture.Legendary144Cases").getMethod(legacy?"legacy":"fixed").invoke(null);if(!legacy){loader.loadClass("fixture.LateBloomerCases").getMethod("run").invoke(null);loader.loadClass("fixture.Legendary144Cases").getMethod("palettes").invoke(null);}}
        }
        System.out.println("PASS actual 1.4.3 PCBox omission and reserve Crowned double award reproduced; fixed acquisition/form code executed with original PCBox.set and native owner lookup. Party/PC full-capacity, ownership and actor-negative matrix passed; no game world initialized.");
    }
}
