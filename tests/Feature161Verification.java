import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.ToolProvider;
import com.google.gson.*;
import local.pixelmon.backport.Content;
import local.pixelmon.backport.client.EvolutionBeamOrigin;

/** Execute production 1.6.1 services with isolated world/network/storage boundaries. */
public final class Feature161Verification {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        DetectorItemModelVerification.verify();
        JsonObject cap=Content.read("data/pixelmon_backport/advancements/legends/capture_719.json");
        check(cap.getAsJsonObject("display").getAsJsonObject("title").get("text").getAsString().equals("窈窕淑女"),"Diancie title");
        check(Content.read("data/pixelmon_backport/advancements/legends/palette_249_drowned.json").getAsJsonObject("display").get("frame").getAsString().equals("challenge"),"Drowned challenge");
        check(Content.read("data/pixelmon_backport/advancements/legends/capture_801.json").getAsJsonObject("display").getAsJsonObject("title").get("text").getAsString().equals("沉鱼落雁"),"Ordinary Magearna renamed");
        var language=Content.read("assets/pixelmon_backport/lang/zh_cn.json");
        check(language.get("attack.ninja_training").getAsString().equals("孤忍自修"),"Signature name");
        check(language.get("pixelmon.generic.form.megaz").getAsString().equals("超级进化（Z）"),"Mega Z untranslated");
        try(var nativeJar=new java.util.jar.JarFile(args[0]);var in=nativeJar.getInputStream(nativeJar.getJarEntry("assets/pixelmon/lang/zh_cn.json"))){
            JsonObject nativeNames=new JsonParser().parse(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for(var e:language.entrySet())nativeNames.add(e.getKey(),e.getValue());
            JsonObject catalogue=new JsonParser().parse(Files.readString(Path.of("docs/ZA_MEGA_COVERAGE_1.6.0.json"))).getAsJsonObject();
            for(var row:catalogue.getAsJsonArray("forms")){
                var f=row.getAsJsonObject();String name=f.get("dex").getAsInt()==678?"mega":f.has("runtime_form")?f.get("runtime_form").getAsString():f.get("form").getAsString();
                check(nativeNames.has("pixelmon.generic.form."+name),"Untranslated Mega form "+name);
            }
        }
        var frame=Content.read("assets/pixelmon/models/item/item_frame.json");
        check(frame.get("parent").getAsString().equals("minecraft:item/generated")&&!frame.has("display"),"Frame must inherit native modern transforms");
        check(frame.getAsJsonObject("textures").get("layer0").getAsString().equals("minecraft:item/item_frame"),"Painting icon retained");
        // Validate the real Minecraft resource, not just a plausible texture ID.
        // 1.16.5 clocks/compasses have numbered frames; plain clock.png/compass.png do not exist.
        for(String item:new String[]{"legendary_spawn_checker","normal_spawn_checker","eject_pack"}){
            JsonObject model=Content.read("assets/pixelmon_backport/models/item/"+item+".json");
            for(var layer:model.getAsJsonObject("textures").entrySet()){
                String[] texture=layer.getValue().getAsString().split(":",2);
                String resource="assets/"+texture[0]+"/textures/"+texture[1]+".png";
                try(InputStream in=Feature161Verification.class.getClassLoader().getResourceAsStream(resource)){
                    check(in!=null,"Missing new item texture: "+item+" -> "+resource);
                    byte[] signature=in.readNBytes(8);
                    check(Arrays.equals(signature,new byte[]{(byte)137,80,78,71,13,10,26,10}),"Invalid PNG: "+resource);
                }
            }
        }
        for(double height:new double[]{.25,1,2.5,10})for(double scale:new double[]{.05,1,1.7,3}){
            double y=-scale*(-1.501f+EvolutionBeamOrigin.modelY(height));
            check(Math.abs(y-scale*height*.35)<1e-6,"Beam did not stay at scaled body center");
        }
        Path dir=Files.createTempDirectory(Path.of(".cache/fixes-161"),"services-");
        String text=Files.readString(Path.of("tests/resources/Feature161Cases.java.txt"));
        List<String> compile=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
        for(String section:text.split("(?m)^// FILE: ")){
            if(section.isBlank())continue;int end=section.indexOf('\n');String name=section.substring(0,end).trim();
            Path p=dir.resolve(name);Files.createDirectories(p.getParent());Files.writeString(p,section.substring(end+1));compile.add(p.toString());
        }
        check(ToolProvider.getSystemJavaCompiler().run(null,null,null,compile.toArray(new String[0]))==0,"1.6.1 service fixture compile");
        List<URL> urls=new ArrayList<>();urls.add(dir.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
        try(URLClassLoader loader=new URLClassLoader(urls.toArray(new URL[0]),ClassLoader.getPlatformClassLoader())){
            loader.loadClass("Feature161Cases").getMethod("run").invoke(null);
        }
        System.out.println("PASS 1.6.1: beam center across scales, frame model, advancement identity; production query scopes/cooldowns, eject-pack boundaries and native config live getters/cache/rollback. No game launched.");
    }
}
