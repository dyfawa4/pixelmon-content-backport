import java.io.*;
import java.lang.reflect.Type;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import com.google.gson.*;
import local.pixelmon.backport.Content;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraftforge.client.model.SeparatePerspectiveModel;

/** Parse real native cuboids and Forge perspectives; resolve every texture to an actual PNG. */
public final class DetectorItemModelVerification {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void texture(String id)throws Exception{
        String[] key=id.split(":",2);String path="assets/"+key[0]+"/textures/"+key[1]+".png";
        try(InputStream in=DetectorItemModelVerification.class.getClassLoader().getResourceAsStream(path)){
            check(in!=null,"Missing detector texture: "+path);check(ImageIO.read(in)!=null,"Invalid detector PNG: "+path);
        }
    }
    public static void verify()throws Exception{
        List<URL> urls=new ArrayList<>();for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
        // A normal offline JVM has not applied Forge's declared constructor ATs.
        // Apply the native model-deserializer AT entries in this isolated test loader.
        try(URLClassLoader loader=new URLClassLoader(urls.toArray(new URL[0]),ClassLoader.getPlatformClassLoader()){
            protected Class<?> findClass(String name)throws ClassNotFoundException{
                if(Set.of("net.minecraft.client.renderer.model.BlockPart$Deserializer","net.minecraft.client.renderer.model.BlockPartFace$Deserializer","net.minecraft.client.renderer.model.BlockFaceUV$Deserializer","net.minecraft.client.renderer.model.ItemCameraTransforms$Deserializer","net.minecraft.client.renderer.model.ItemOverride$Deserializer","net.minecraft.client.renderer.model.ItemTransformVec3f$Deserializer").contains(name)){
                    try(InputStream in=getResourceAsStream(name.replace('.','/')+".class")){
                        var node=new org.objectweb.asm.tree.ClassNode();new org.objectweb.asm.ClassReader(in.readAllBytes()).accept(node,0);
                        for(var method:node.methods)if(method.name.equals("<init>")&&method.desc.equals("()V"))method.access=(method.access&~(org.objectweb.asm.Opcodes.ACC_PRIVATE|org.objectweb.asm.Opcodes.ACC_PROTECTED))|org.objectweb.asm.Opcodes.ACC_PUBLIC;
                        var writer=new org.objectweb.asm.ClassWriter(0);node.accept(writer);byte[] bytes=writer.toByteArray();return defineClass(name,bytes,0,bytes.length);
                    }catch(IOException ex){throw new ClassNotFoundException(name,ex);}
                }
                return super.findClass(name);
            }
        }){loader.loadClass("DetectorItemModelVerification").getMethod("verifyNative").invoke(null);}
    }
    public static void verifyNative()throws Exception{
        for(String name:new String[]{"normal_spawn_checker","legendary_spawn_checker"}){
            String asset="assets/pixelmon_backport/models/item/"+name;
            JsonObject root=Content.read(asset+".json"),body=Content.read(asset+"_handheld.json"),icon=Content.read(asset+"_icon.json");
            check(root.get("loader").getAsString().equals("forge:separate-perspective"),"Detector requires separate GUI/held model");
            check(root.getAsJsonObject("base").get("parent").getAsString().equals("pixelmon_backport:item/"+name+"_handheld"),"Handheld routing");
            check(root.getAsJsonObject("perspectives").getAsJsonObject("gui").get("parent").getAsString().equals("pixelmon_backport:item/"+name+"_icon"),"GUI routing");
            BlockModel nativeBody=BlockModel.func_178294_a(body.toString());
            check(nativeBody.func_178298_a().size()==body.getAsJsonArray("elements").size(),"Native model lost exposed surface patches");
            Set<String> components=new HashSet<>();for(var part:body.getAsJsonArray("elements"))components.add(part.getAsJsonObject().get("name").getAsString().split("/")[0]);
            check(components.size()==18,"Native model lost physical components");
            new SeparatePerspectiveModel.Loader().read(new JsonDeserializationContext(){
                @SuppressWarnings("unchecked") public <T>T deserialize(JsonElement json,Type type){return (T)BlockModel.func_178294_a(json.toString());}
            },root);
            check(icon.get("parent").getAsString().equals("minecraft:item/generated"),"GUI should retain the generated sprite");
            for(JsonObject model:List.of(root,body,icon))for(var entry:model.getAsJsonObject("textures").entrySet())texture(entry.getValue().getAsString());
            for(var value:body.getAsJsonArray("elements")){
                JsonObject part=value.getAsJsonObject();JsonArray low=part.getAsJsonArray("from"),high=part.getAsJsonArray("to");
                for(int i=0;i<3;i++)check(low.get(i).getAsDouble()<high.get(i).getAsDouble(),"Flat/invalid detector component");
                check(part.getAsJsonObject("faces").size()==1,"Each exposed surface must be emitted exactly once");
                for(var face:part.getAsJsonObject("faces").entrySet())check(body.getAsJsonObject("textures").has(face.getValue().getAsJsonObject().get("texture").getAsString().substring(1)),"Unresolved component texture");
            }
            for(String pose:new String[]{"firstperson_righthand","firstperson_lefthand","thirdperson_righthand","thirdperson_lefthand","ground","fixed"}){
                JsonObject transform=body.getAsJsonObject("display").getAsJsonObject(pose);check(transform!=null,"Missing held pose "+pose);
                for(var s:transform.getAsJsonArray("scale"))check(s.getAsDouble()>0&&s.getAsDouble()<=1,"Oversized handheld detector");
            }
            try(InputStream in=DetectorItemModelVerification.class.getClassLoader().getResourceAsStream("assets/pixelmon_backport/textures/item/"+name+".png")){
                var png=ImageIO.read(in);check(png.getWidth()==32&&png.getHeight()==32&&png.getColorModel().hasAlpha(),"Detector icon must be transparent 32x32");
                check((png.getRGB(0,0)>>>24)==0,"Opaque icon background");
            }
        }
        System.out.println("PASS actual native cuboid/Forge perspective parsing for two 18-part 3D detectors, both hands, GUI routing, real texture resources and transparent 32x32 icons. No game launched.");
    }
}
