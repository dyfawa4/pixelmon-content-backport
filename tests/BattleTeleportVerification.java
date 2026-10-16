import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import javax.tools.ToolProvider;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Run the actual original/patched dimension listener and native move selector. */
public final class BattleTeleportVerification {
    static final String API="com.pixelmonmod.pixelmon.";
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    static ClassNode node(byte[] b){var n=new ClassNode();new ClassReader(b).accept(n,0);return n;}
    static byte[] write(ClassNode n){var w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
    static byte[] read(JarFile jar,String name)throws Exception{try(var in=jar.getInputStream(jar.getJarEntry(name.replace('.','/')+".class"))){return in.readAllBytes();}}
    static void add(Map<String,String> s,String name,String body){s.put(name,"package "+name.substring(0,name.lastIndexOf('.'))+";"+body);}
    static void replace(Path path,ClassNode source,java.util.function.Predicate<MethodNode> include)throws Exception{
        var target=node(Files.readAllBytes(path));
        for(var m:source.methods)if(include.test(m)){target.methods.removeIf(n->n.name.equals(m.name)&&n.desc.equals(m.desc));target.methods.add(m);}
        Files.write(path,write(target));
    }
    public static void verify(JarFile jar,byte[] patchedProxy,byte[] patchedChoose)throws Exception{
        var config=Version13StaticVerification.resource("pixelmon_backport.mixins.json");
        for(String name:new String[]{"BattleDimensionClientMixin","BattleSelectionSafetyMixin"}){
            check(config.getAsJsonArray("client").toString().contains(name),"Missing client fix");
            check(!config.getAsJsonArray("mixins").toString().contains(name),"Client fix on dedicated server");
        }
        // Verify the real transformed render/click guard runs before native work.
        var choose=node(patchedChoose);
        for(String name:new String[]{"render","click"}){
            var method=choose.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();
            var calls=Arrays.stream(method.instructions.toArray()).filter(i->i instanceof MethodInsnNode).map(i->(MethodInsnNode)i).toArray(MethodInsnNode[]::new);
            check(Arrays.stream(calls).anyMatch(i->i.name.contains(name.equals("render")?"backport$renderLiveBattle":"backport$clickLiveBattle")),"Native selection entry unguarded");
            int guard=-1,nativeCall=-1;for(int i=0;i<calls.length;i++){if(calls[i].name.contains("backport$"))guard=i;if(calls[i].name.equals("getCurrentPokemon")||calls[i].name.equals("handleButtonClick")){nativeCall=i;break;}}
            check(guard>=0&&nativeCall>guard,"Guard follows unsafe native work");
        }
        Map<String,String>s=new LinkedHashMap<>();
        add(s,"net.minecraft.entity.player.PlayerEntity","public class PlayerEntity {public java.util.UUID id;public PlayerEntity(java.util.UUID id){this.id=id;}public java.util.UUID func_110124_au(){return id;}}");
        add(s,"net.minecraft.client.entity.player.ClientPlayerEntity","public class ClientPlayerEntity extends net.minecraft.entity.player.PlayerEntity {public ClientPlayerEntity(java.util.UUID id){super(id);}}");
        add(s,"net.minecraft.client.Minecraft","public class Minecraft {public static final Minecraft INSTANCE=new Minecraft();public net.minecraft.client.entity.player.ClientPlayerEntity field_71439_g;public static Minecraft func_71410_x(){return INSTANCE;}}");
        add(s,"net.minecraftforge.event.entity.player.PlayerEvent","public class PlayerEvent {public static class PlayerChangedDimensionEvent {private net.minecraft.entity.player.PlayerEntity player;public PlayerChangedDimensionEvent(net.minecraft.entity.player.PlayerEntity p){player=p;}public net.minecraft.entity.player.PlayerEntity getPlayer(){return player;}}}");
        add(s,API+"api.battles.BattleMode","public enum BattleMode {MAIN_MENU,CHOOSE_ATTACK,WAITING}");
        add(s,API+"client.gui.battles.PixelmonClientData","public class PixelmonClientData {public java.util.UUID pokemonUUID=java.util.UUID.randomUUID();}");
        add(s,API+"client.gui.battles.ClientBattleManager","public class ClientBattleManager {public boolean battleEnded,afkActive,megaEvolving,dynamaxing,showZMoves,canSwitch,isSpectating;public int battleControllerIndex=12,currentPokemon,finished;public java.util.UUID[] teamPokemon={java.util.UUID.randomUUID()};public java.util.ArrayList<Object> selectedActions=new java.util.ArrayList<>();public java.util.List<PixelmonClientData> fullOurPokemon=new java.util.ArrayList<>();public java.util.ArrayList<java.util.UUID> pokemonToChoose=new java.util.ArrayList<>();private java.util.List<Boolean> canSwitchList=new java.util.ArrayList<>();public void selectedMove(){throw new AssertionError();}public void selectedMove(boolean b){throw new AssertionError();}private boolean hasTurn(java.util.UUID id){return pokemonToChoose.contains(id);}public void finishSelection(){finished++;}public void setMode("+API+"api.battles.BattleMode mode){} }");
        add(s,API+"client.ClientProxy","public class ClientProxy {public static "+API+"client.gui.battles.ClientBattleManager battleManager;public void onDimensionChange(net.minecraftforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event){throw new AssertionError();}}");
        add(s,API+"client.gui.battles.BattleScreen","public class BattleScreen {}");
        add(s,API+"client.gui.battles.battleScreens.BattleBaseScreen","public class BattleBaseScreen {protected "+API+"client.gui.battles.ClientBattleManager bm;public BattleBaseScreen("+API+"client.gui.battles.BattleScreen parent,"+API+"api.battles.BattleMode mode){}}");
        add(s,"fixture.Selection","public class Selection extends local.pixelmon.backport.mixin.BattleSelectionSafetyMixin {}");
        s.put("fixture.BattleTeleportCases",Files.readString(Path.of("tests/resources/BattleTeleportCases.java.txt")));
        Path dir=Files.createTempDirectory(Path.of(".cache"),"battle-teleport-");
        var options=new ArrayList<String>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
        for(var e:s.entrySet()){var p=dir.resolve(e.getKey().replace('.','/')+".java");Files.createDirectories(p.getParent());Files.writeString(p,e.getValue());options.add(p.toString());}
        check(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))==0,"Teleport fixture compilation failed");
        var manager=node(read(jar,API+"client.gui.battles.ClientBattleManager"));
        replace(dir.resolve(API.replace('.','/')+"client/gui/battles/ClientBattleManager.class"),manager,m->m.name.equals("selectedMove")||m.name.startsWith("lambda$selectedMove$"));
        var proxyPath=dir.resolve(API.replace('.','/')+"client/ClientProxy.class");byte[] template=Files.readAllBytes(proxyPath);
        for(boolean patched:new boolean[]{false,true}){
            Files.write(proxyPath,template);
            replace(proxyPath,node(patched?patchedProxy:read(jar,API+"client.ClientProxy")),m->m.name.equals("onDimensionChange")||m.name.contains("backport$localDimension"));
            try(var loader=new URLClassLoader(new URL[]{dir.toUri().toURL()},BattleTeleportVerification.class.getClassLoader()){
                protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{synchronized(getClassLoadingLock(name)){
                    Class<?> c=findLoadedClass(name);String base=name.contains("$")?name.substring(0,name.indexOf('$')):name;
                    if(c==null&&s.containsKey(base))c=findClass(name);
                    if(c==null&&name.equals("local.pixelmon.backport.mixin.BattleSelectionSafetyMixin"))try(var in=getParent().getResourceAsStream(name.replace('.','/')+".class")){byte[] b=in.readAllBytes();c=defineClass(name,b,0,b.length);}catch(java.io.IOException ex){throw new ClassNotFoundException(name,ex);}
                    if(c==null)c=super.loadClass(name,false);if(resolve)resolveClass(c);return c;
                }}
            }){loader.loadClass("fixture.BattleTeleportCases").getMethod("run",boolean.class).invoke(null,patched);}
        }
        System.out.println("PASS native remote dimension battle termination and selectedMove null-team crash reproduced; patched listener preserves host battle, local UUID handling, 1000 remote events, null-player and detached render/click guards; live single/double selection preserved.");
    }
}
