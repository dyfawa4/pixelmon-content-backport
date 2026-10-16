import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import local.pixelmon.backport.JavaRuntimePolicy;

/** Execute shipped gates with isolated UI services; never open a world or network connection. */
public final class JavaRuntimeGateVerification {
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    static void add(Map<String,String> s,String name,String body){s.put(name,"package "+name.substring(0,name.lastIndexOf('.'))+";"+body);}
    static byte[] write(ClassNode node){var w=new ClassWriter(0);node.accept(w);return w.toByteArray();}
    public static void main(String[] args)throws Exception{
        for(int feature=0;feature<=50;feature++)check(JavaRuntimePolicy.allowed(feature)==(feature==11),"Version escaped gate: "+feature);
        check(JavaRuntimePolicy.allowed()==(Runtime.version().feature()==11),"Actual JVM detection wrong");
        for(var method:Version13StaticVerification.node("local/pixelmon/backport/JavaRuntimePolicy").methods)for(var i:method.instructions)if(i instanceof MethodInsnNode)check(!((MethodInsnNode)i).name.equals("getProperty"),"JVM version can be spoofed by a system property");
        var config=Version13StaticVerification.resource("pixelmon_backport.mixins.json");for(String name:new String[]{"JavaRuntimeMinecraftMixin","JavaRuntimeConnectMixin"}){check(config.getAsJsonArray("client").toString().contains(name),"Missing client-only gate");check(!config.getAsJsonArray("mixins").toString().contains(name),"Gate loads client UI on dedicated server");}
        var screen=Version13StaticVerification.node("local/pixelmon/backport/client/JavaRuntimeScreen");boolean noEscape=false,noClose=false,quit=false,wrapped=false;
        for(var method:screen.methods){
            if(method.name.equals("func_231178_ax__"))noEscape=Arrays.stream(method.instructions.toArray()).anyMatch(i->i.getOpcode()==Opcodes.ICONST_0);
            if(method.name.equals("func_231175_as__"))noClose=Arrays.stream(method.instructions.toArray()).filter(i->i.getOpcode()>=0).count()==1;
            for(var i:method.instructions)if(i instanceof MethodInsnNode){quit|=((MethodInsnNode)i).name.equals("func_71400_g");wrapped|=((MethodInsnNode)i).name.equals("func_238425_b_");}
        }
        check(noEscape&&noClose&&quit&&wrapped,"Gate allows back/escape, lacks quit or lacks wrapping");
        var chinese=Version13StaticVerification.resource("assets/pixelmon_backport/lang/zh_cn.json");
        check(chinese.get("pixelmon.backport.java11.notice").getAsString().replace("%s","Java11").equals("检测到您未使用Java11启动游戏，请选择Java11启动游戏！！！"),"Requested novice warning changed");
        check(chinese.get("pixelmon.backport.java11.bundled").getAsString().replace("%s","Java11").equals("您下载的文件里面有Java11，解压即可导入使用！"),"Requested bundled-Java instruction changed");
        boolean yellow=false,white=false;for(var method:screen.methods)for(var i:method.instructions)if(i instanceof FieldInsnNode){var f=(FieldInsnNode)i;if(f.owner.equals("net/minecraft/util/text/TextFormatting")){yellow|=method.name.equals("java11")&&f.name.equals("YELLOW");white|=f.name.equals("WHITE");}}
        check(yellow&&white,"Java11 token must be yellow and remaining text white");
        Map<String,String>s=new LinkedHashMap<>();String client="net.minecraft.client.",screens=client+"gui.screen.";
        add(s,"local.pixelmon.backport.JavaRuntimePolicy","public class JavaRuntimePolicy {public static int feature=11;public static boolean allowed(int n){throw new AssertionError();}public static boolean allowed(){return allowed(feature);}}");
        add(s,screens+"Screen","public class Screen {}");
        for(String name:new String[]{"MainMenuScreen","WorldSelectionScreen","CreateWorldScreen","MultiplayerScreen","ConnectingScreen"})add(s,screens+name,"public class "+name+" extends Screen {}");
        add(s,"local.pixelmon.backport.client.JavaRuntimeScreen","public class JavaRuntimeScreen extends "+screens+"Screen {}");
        add(s,client+"Minecraft","public class Minecraft {public static final Minecraft INSTANCE=new Minecraft();public "+screens+"Screen screen;public int opens;public static Minecraft func_71410_x(){return INSTANCE;}public void func_147108_a("+screens+"Screen s){screen=local.pixelmon.backport.client.JavaRuntimeGate.filter(s);opens++;}}");
        add(s,"fixture.Gates","public class Gates {public static class Game extends local.pixelmon.backport.mixin.JavaRuntimeMinecraftMixin {}public static class Connect extends local.pixelmon.backport.mixin.JavaRuntimeConnectMixin {}}");
        try(var in=JavaRuntimeGateVerification.class.getClassLoader().getResourceAsStream("JavaRuntimeGateCases.java.txt")){s.put("fixture.JavaRuntimeGateCases",new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}
        Path dir=Files.createTempDirectory("java11-gate-");List<String> compiler=new ArrayList<>(List.of("--release","11","-proc:none","-classpath",System.getProperty("java.class.path"),"-d",dir.toString()));
        for(var source:s.entrySet()){Path path=dir.resolve(source.getKey().replace('.','/')+".java");Files.createDirectories(path.getParent());Files.writeString(path,source.getValue());compiler.add(path.toString());}
        check(ToolProvider.getSystemJavaCompiler().run(null,null,null,compiler.toArray(new String[0]))==0,"Gate fixture compilation failed");
        Path path=dir.resolve("local/pixelmon/backport/JavaRuntimePolicy.class");var stub=new ClassNode();new ClassReader(Files.readAllBytes(path)).accept(stub,0);stub.methods.removeIf(m->m.name.equals("allowed")&&m.desc.equals("(I)Z"));for(var m:Version13StaticVerification.node("local/pixelmon/backport/JavaRuntimePolicy").methods)if(m.name.equals("allowed")&&m.desc.equals("(I)Z"))stub.methods.add(m);Files.write(path,write(stub));
        Set<String> production=Set.of("local.pixelmon.backport.client.JavaRuntimeGate","local.pixelmon.backport.mixin.JavaRuntimeMinecraftMixin","local.pixelmon.backport.mixin.JavaRuntimeConnectMixin");
        try(var loader=new URLClassLoader(new URL[]{dir.toUri().toURL()},JavaRuntimeGateVerification.class.getClassLoader()){
            protected Class<?> loadClass(String n,boolean resolve)throws ClassNotFoundException{synchronized(getClassLoadingLock(n)){Class<?> c=findLoadedClass(n);String base=n.contains("$")?n.substring(0,n.indexOf('$')):n;if(c==null&&s.containsKey(base))c=findClass(n);if(c==null&&production.contains(n)){try(var in=getParent().getResourceAsStream(n.replace('.','/')+".class")){byte[] b=in.readAllBytes();c=defineClass(n,b,0,b.length);}catch(java.io.IOException ex){throw new ClassNotFoundException(n,ex);}}if(c==null)c=super.loadClass(n,false);if(resolve)resolveClass(c);return c;}}
        }){loader.loadClass("fixture.JavaRuntimeGateCases").getMethod("run").invoke(null);}
        System.out.println("PASS strict Java 11 policy, real runtime API, client-only mixins, shipped menu/world/connect callbacks, null/back/other-menu bypass prevention and Java 11 identity pass-through. No game/network initialized.");
    }
}
