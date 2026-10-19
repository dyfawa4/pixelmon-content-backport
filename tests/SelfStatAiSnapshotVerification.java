import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import javax.tools.ToolProvider;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

/** Reproduce the submitted StatsEffectPost:143 with the actual native method. */
public final class SelfStatAiSnapshotVerification {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static ClassNode node(byte[] b){ClassNode n=new ClassNode();new ClassReader(b).accept(n,0);return n;}
    static byte[] bytes(ClassNode n){ClassWriter w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
    public static void verify(IMixinTransformer transformer,JarFile jar)throws Exception{
        Set<String> allocationMethods=new TreeSet<>();int battleClasses=0;
        for(var entries=jar.entries();entries.hasMoreElements();){
            var entry=entries.nextElement();if(!entry.getName().startsWith("com/pixelmonmod/pixelmon/battles/")||!entry.getName().endsWith(".class"))continue;
            battleClasses++;ClassNode cls=node(jar.getInputStream(entry).readAllBytes());
            for(var method:cls.methods)for(var instruction:method.instructions)if(instruction.getOpcode()==Opcodes.ANEWARRAY&&((TypeInsnNode)instruction).desc.equals("com/pixelmonmod/pixelmon/api/pokemon/stats/BattleStats"))allocationMethods.add(cls.name.substring(cls.name.lastIndexOf('/')+1)+"."+method.name);
        }
        check(allocationMethods.equals(Set.of("StatsEffect.weightStatsEffect","StatsEffectPost.weightEffect","StatsEffectCone.weightEffect","StatsEffectTeam.weightEffect","Swap.weightEffect")),"Unreviewed battle stat snapshot allocation: "+allocationMethods);
        System.out.println("PASS native battle class inventory: "+battleClasses+" classes scanned; all BattleStats[] allocations in "+allocationMethods+" reviewed (Swap initializes both slots).");
        for(String suffix:new String[]{"StatsEffectPost","StatsEffect","StatsEffectCone","StatsEffectTeam"})verifyType(transformer,jar,suffix);
        System.out.println("PASS actual native self-stat AI methods: submitted StatsEffectPost:143 reproduced; post-move and ordinary self effects fixed for 0/1/2/4 targets, all target effects preserved. No game launched.");
    }
    static void verifyType(IMixinTransformer transformer,JarFile jar,String suffix)throws Exception{
        String name="com.pixelmonmod.pixelmon.battles.attacks.specialAttacks."+suffix;
        byte[] original=jar.getInputStream(jar.getJarEntry(name.replace('.','/')+".class")).readAllBytes();
        byte[] patched=transformer.transformClassBytes(name,name,original);
        ClassNode nativeNode=node(original),fixedNode=node(patched);
        MethodNode nativeMethod=nativeNode.methods.stream().filter(m->m.name.equals(suffix.equals("StatsEffect")?"weightStatsEffect":"weightEffect")).findFirst().orElseThrow();
        int arrays=0,lists=0;
        for(var i:nativeMethod.instructions){
            if(i.getOpcode()==Opcodes.ANEWARRAY&&((TypeInsnNode)i).desc.endsWith("/BattleStats"))arrays++;
            if(i instanceof MethodInsnNode&&((MethodInsnNode)i).owner.equals("java/util/List")&&((MethodInsnNode)i).name.equals("size"))lists++;
        }
        check(arrays==6&&lists==9,"Native self-stat snapshot sites changed");
        Path root=Files.createTempDirectory(Path.of(".cache/fixes-161"),"self-stat-ai-");
        List<String> options=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",root.toString()));
        for(String part:Files.readString(Path.of("tests/resources/SelfStatAiCases.java.txt")).split("(?m)^// FILE: ")){
            if(part.isBlank())continue;int e=part.indexOf('\n');Path p=root.resolve(part.substring(0,e).trim());
            Files.createDirectories(p.getParent());Files.writeString(p,part.substring(e+1));options.add(p.toString());
        }
        check(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))==0,"Self-stat AI fixture compilation");
        Path target=root.resolve(name.replace('.','/')+".class");byte[] stub=Files.readAllBytes(target);
        for(boolean fixed:new boolean[]{false,true}){
            ClassNode n=node(stub);n.methods.removeIf(m->m.name.equals("weightEffect")||m.name.equals("weightStatsEffect")||m.name.equals("getAffectedOpponents"));
            for(MethodNode m:(fixed?fixedNode:nativeNode).methods)if(m.name.equals("weightEffect")||m.name.equals("weightStatsEffect")||m.name.equals("getAffectedOpponents")||m.name.contains("backport$"))n.methods.add(m);
            Files.write(target,bytes(n));
            try(URLClassLoader loader=new URLClassLoader(new URL[]{root.toUri().toURL()},ClassLoader.getPlatformClassLoader())){
                loader.loadClass("SelfStatAiCases").getMethod("run",boolean.class,String.class).invoke(null,fixed,suffix);
            }
        }
    }
}
