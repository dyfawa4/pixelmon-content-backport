import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import javax.tools.ToolProvider;

/** Native PQC/BMD/animation parsing, with only file lookup and drawing isolated. */
public final class NativeVariantModelVerification {
    static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        Path dir=Files.createTempDirectory(Path.of(".cache"),"native-variant-model-");Map<String,String> sources=new LinkedHashMap<>();
        sources.put("com.pixelmonmod.pixelmon.CommonProxy","public class CommonProxy {public static java.io.BufferedInputStream getStreamForResourceLocation(net.minecraft.util.ResourceLocation r){try{return new java.io.BufferedInputStream(java.nio.file.Files.newInputStream(java.nio.file.Path.of(\"resources/assets\",r.func_110624_b(),r.func_110623_a())));}catch(java.io.IOException e){throw new RuntimeException(e);}}}");
        sources.put("com.pixelmonmod.pixelmon.client.render.PixelmonRendering","public class PixelmonRendering {public static net.minecraft.client.renderer.RenderType getSMDRenderType(net.minecraft.util.ResourceLocation r){throw new AssertionError(\"Drawing must not run\");}}");
        List<String> command=new ArrayList<>(List.of("--release","11","-proc:none","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
        for(var entry:sources.entrySet()){Path p=dir.resolve(entry.getKey().replace('.','/')+".java");Files.createDirectories(p.getParent());Files.writeString(p,"package "+entry.getKey().substring(0,entry.getKey().lastIndexOf('.'))+";"+entry.getValue());command.add(p.toString());}
        check(ToolProvider.getSystemJavaCompiler().run(null,null,null,command.toArray(new String[0]))==0,"Native resource fixture compilation");
        try(JarFile jar=new JarFile(args[0])){for(var entries=jar.entries();entries.hasMoreElements();){var e=entries.nextElement();if(e.getName().startsWith("com/pixelmonmod/pixelmon/client/models/smd/")&&e.getName().endsWith(".class")){Path p=dir.resolve(e.getName());Files.createDirectories(p.getParent());try(var in=jar.getInputStream(e)){Files.copy(in,p);}}}}
        try(var loader=new URLClassLoader(new URL[]{dir.toUri().toURL()},NativeVariantModelVerification.class.getClassLoader()){
            protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{synchronized(getClassLoadingLock(name)){boolean math=name.equals("com.pixelmonmod.pixelmon.tools.Matrix4f")||name.equals("com.pixelmonmod.pixelmon.api.util.helpers.VectorHelper")||name.equals("net.minecraft.util.math.vector.Matrix4f")||name.equals("net.minecraft.util.math.vector.Vector4f");if(!math&&!sources.containsKey(name)&&!name.startsWith("com.pixelmonmod.pixelmon.client.models.smd."))return super.loadClass(name,resolve);Class<?> c=findLoadedClass(name);if(c==null){if(math){try(var in=getParent().getResourceAsStream(name.replace('.','/')+".class")){byte[] bytes=in.readAllBytes();if(name.equals("net.minecraft.util.math.vector.Matrix4f")){var n=new org.objectweb.asm.tree.ClassNode();new org.objectweb.asm.ClassReader(bytes).accept(n,0);for(var f:n.fields)f.access=(f.access&~6)|1;var w=new org.objectweb.asm.ClassWriter(0);n.accept(w);bytes=w.toByteArray();}c=defineClass(name,bytes,0,bytes.length);}catch(java.io.IOException e){throw new ClassNotFoundException(name,e);}}else c=findClass(name);}if(resolve)resolveClass(c);return c;}}
        }){
            var type=loader.loadClass("com.pixelmonmod.pixelmon.client.models.smd.ValveStudioModel");int loaded=0;
            List<String> descriptors=new ArrayList<>();
            descriptors.add("textures/reference/pokemon/801_magearna/all/mega/none/model.pqc");
            descriptors.add("textures/reference/pokemon/978_tatsugiri/all/mega/none/model.pqc");
            try(var paths=Files.walk(Path.of("resources/assets/pixelmon_backport/textures/smooth_variants"))){paths.filter(p->p.toString().endsWith("model.pqc")).forEach(p->descriptors.add(p.toString().replace('\\','/').substring("resources/assets/pixelmon_backport/".length())));}
            descriptors.add("textures/reference/pokemon/658_greninja/all/mega/none/model.pqc");
            for(String path:descriptors){
                Object model;try{model=type.getConstructor(net.minecraft.util.ResourceLocation.class).newInstance(new net.minecraft.util.ResourceLocation("pixelmon_backport",path));}catch(java.lang.reflect.InvocationTargetException e){throw new AssertionError("Native model parse failed "+path,e.getCause());}
                Object body=type.getField("body").get(model);List<?> bones=(List<?>)body.getClass().getField("bones").get(body);List<?> faces=(List<?>)body.getClass().getField("faces").get(body);
                check(!bones.isEmpty()&&!faces.isEmpty(),"Empty model "+path);check((Boolean)type.getMethod("hasAnimations").invoke(model),"Missing native animation sequence "+path);
                for(Object bone:bones)for(String matrix:List.of("rest","restInverted")){Object m=bone.getClass().getField(matrix).get(bone);check(m!=null,"No native bind matrix");for(var field:m.getClass().getFields())if(field.getType()==float.class)check(Float.isFinite(field.getFloat(m)),"Nonfinite native matrix "+path);}
                loaded++;
            }
            System.out.println("PASS native PQC/BMD material, skinning, bind matrices and idle/walk precalculation: "+loaded+" models; no drawing or game launch.");
        }
    }
}
