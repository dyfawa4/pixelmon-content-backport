import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import javax.tools.ToolProvider;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import local.pixelmon.backport.Content;

public final class UltraPlantVerification {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    public static void main(String[] args)throws Exception{
        var json=Content.read("data/pixelmon_backport/advancements/ultra_space/ultra_plant_lightning.json");
        check(json.get("parent").getAsString().equals("pixelmon:ultra_space/root"),"Wrong Ultra Space tree");
        check(json.getAsJsonObject("criteria").getAsJsonObject("earned").get("trigger").getAsString().equals("minecraft:impossible"),"Other lightning could award");
        var zh=Content.read("assets/pixelmon_backport/lang/zh_cn.json");
        check(zh.get("advancement.pixelmon_backport.ultra_plant_lightning").getAsString().equals("啊".repeat(10)),"Exact user title changed");
        check(zh.get("advancement.pixelmon_backport.ultra_plant_lightning.desc").getAsString().equals("触碰究极发电厂的信号塔，并被闪电击中"),"Exact user description changed");
        try(var jar=new JarFile(args[0])){
            ClassNode n=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry("com/pixelmonmod/pixelmon/world/listener/UltraSpaceListener.class"))).accept(n,0);
            var m=n.methods.stream().filter(x->x.name.equals("onBlockBreak")).findFirst().orElseThrow();int insertion=0,bolts=0,chance=0;boolean biome=false,lamp=false;
            for(var i:m.instructions){
                if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;if(c.name.equals("func_217376_c"))insertion++;if(c.owner.endsWith("LightningBoltEntity")&&c.name.equals("<init>"))bolts++;if(c.name.equals("getRandomChance"))chance++;}
                if(i instanceof LdcInsnNode)biome|="ultra_plant".equals(((LdcInsnNode)i).cst);
                if(i instanceof FieldInsnNode)lamp|=((FieldInsnNode)i).name.equals("field_180398_cJ");
            }
            check(insertion==1&&bolts==1&&chance==2&&biome&&lamp,"Re-audit native mining bolt path");
        }
        Path dir=Files.createTempDirectory(Path.of(".cache/fixes-161"),"ultra-plant-");
        List<String> options=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
        for(String part:Files.readString(Path.of("tests/resources/UltraPlantCases.java.txt")).split("(?m)^// FILE: ")){
            if(part.isBlank())continue;int end=part.indexOf('\n');Path p=dir.resolve(part.substring(0,end).trim());Files.createDirectories(p.getParent());Files.writeString(p,part.substring(end+1));options.add(p.toString());
        }
        check(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))==0,"Ultra Plant fixture compile");
        List<URL> urls=new ArrayList<>();urls.add(dir.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
        try(URLClassLoader loader=new URLClassLoader(urls.toArray(new URL[0]),ClassLoader.getPlatformClassLoader())){loader.loadClass("UltraPlantCases").getMethod("run").invoke(null);}
        System.out.println("PASS native Ultra Plant mining bolt provenance; actual miner hit only, no mining-only/unrelated/multiplayer/cross-world/failed insertion awards; native tree and exact text. No game launched.");
    }
}
