import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import javax.tools.ToolProvider;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Execute native grid matching and native ball construction with the production ingredients. */
public final class DetectorRecipeVerification {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    static ClassNode node(byte[] b){var n=new ClassNode();new ClassReader(b).accept(n,0);return n;}
    static byte[] write(ClassNode n){var w=new ClassWriter(0);n.accept(w);return w.toByteArray();}
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory(Path.of(".cache/fixes-161"),"detector-recipes-");
        List<String> options=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
        for(String part:Files.readString(Path.of("tests/resources/DetectorRecipeCases.java.txt")).split("(?m)^// FILE: ")){
            if(part.isBlank())continue;int end=part.indexOf('\n');Path p=dir.resolve(part.substring(0,end).trim());Files.createDirectories(p.getParent());Files.writeString(p,part.substring(end+1));options.add(p.toString());
        }
        check(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))==0,"Detector recipe fixture compile");
        try(JarFile nativeJar=new JarFile(args[0])){
            String name="com.pixelmonmod.pixelmon.items.PokeBallItem";
            ClassNode actual=node(nativeJar.getInputStream(nativeJar.getJarEntry(name.replace('.','/')+".class")).readAllBytes());
            Path p=dir.resolve(name.replace('.','/')+".class");ClassNode stub=node(Files.readAllBytes(p));
            stub.methods.removeIf(m->m.name.equals("of"));for(var m:actual.methods)if(m.name.equals("of"))stub.methods.add(m);Files.write(p,write(stub));
        }
        String name="net.minecraft.item.crafting.ShapedRecipe";
        try(InputStream in=DetectorRecipeVerification.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")){
            ClassNode actual=node(in.readAllBytes());Path p=dir.resolve(name.replace('.','/')+".class");ClassNode stub=node(Files.readAllBytes(p));
            stub.methods.removeIf(m->m.name.equals("func_77573_a")||m.name.equals("func_77569_a"));
            for(var m:actual.methods)if(m.name.equals("func_77573_a")||m.name.equals("func_77569_a")&&m.desc.contains("CraftingInventory"))stub.methods.add(m);Files.write(p,write(stub));
        }
        List<URL> urls=new ArrayList<>();urls.add(dir.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
        try(URLClassLoader loader=new URLClassLoader(urls.toArray(new URL[0]),ClassLoader.getPlatformClassLoader())){loader.loadClass("DetectorRecipeCases").getMethod("run").invoke(null);}
        System.out.println("PASS native PokeBallItem construction + native ShapedRecipe matching: both exact 3x3 recipes, real PokeBallID, wrong/missing/replaced/renamed balls, serializer roundtrip, seven ingots and correct detector tier. No game launched.");
    }
}
