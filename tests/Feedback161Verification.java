import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarFile;
import javax.tools.ToolProvider;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import local.pixelmon.backport.*;
import local.pixelmon.backport.client.EvolutionBeamOrigin;
import net.minecraft.util.ResourceLocation;

/** Original packet control flow versus transformed packet; real production service sources. */
public final class Feedback161Verification {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    static ClassNode node(byte[] bytes){var n=new ClassNode();new ClassReader(bytes).accept(n,0);return n;}
    public static void main(String[] args)throws Exception{
        for(double h:new double[]{.2,.5,1,2,8,25})for(double width:new double[]{.2,1,5,30})for(int phase=0;phase<=180;phase++){
            float original=2+phase/10f;float length=EvolutionBeamOrigin.length(original,width,h);
            check(length>0&&length<=2.50001&&length<original,"Beam remains native 20-unit length");
            check(Math.abs(-(-1.501f+EvolutionBeamOrigin.modelY(h))-.35*h)<1e-6,"Beam center not lowered to 35% height");
            check(EvolutionBeamOrigin.width((float)width/10)<=.120001f,"Beam flare too wide");
        }
        var names=Content.read("backport/spawn-npc-names.json");
        for(var e:names.entrySet()){
            String line="\u00a76"+e.getKey()+"\u00a77: \u00a7d0.04%";
            check(SpawnNpcNames.display(line).equals("\u00a76"+e.getValue().getAsString()+"\u00a77: \u00a7d0.04%"),"NPC label untranslated: "+e.getKey());
        }
        check(SpawnNpcNames.display("\u00a76普通宝可梦\u00a77: \u00a7a23.3%").equals("\u00a76普通宝可梦\u00a77: \u00a7a23.3%"),"Pokemon/percentage changed");
        for(String file:BallItemArt.FILES){
            var mapped=BallItemArt.sprite(new ResourceLocation("pixelmon","items/pokeballs/"+file));
            check(mapped.toString().equals("pixelmon_backport:items/pokeballs/"+file),"Art still masked by original namespace");
            check(Files.isRegularFile(Path.of("resources/assets/pixelmon_backport/textures/items/pokeballs/"+file+".png")),"Owned art missing");
        }
        check(BallItemArt.FILES.size()==85,"Reference ball/lid/part coverage");
        ResourceLocation unknown=new ResourceLocation("othermod","items/pokeballs/poke_ball");check(BallItemArt.sprite(unknown)==unknown,"Foreign namespace altered");
        try(var jar=new JarFile(args[0])){
            for(String n:new String[]{"com/pixelmonmod/pixelmon/api/events/spawning/SpawnEvent","com/pixelmonmod/pixelmon/api/events/CaptureEvent$StartCapture"}){
                var cn=node(jar.getInputStream(jar.getJarEntry(n+".class")).readAllBytes());
                check(cn.visibleAnnotations!=null&&cn.visibleAnnotations.stream().anyMatch(a->a.desc.endsWith("/Cancelable;")),"Native sentinel acquisition event cannot be canceled "+n);
            }
        }
        Path dir=Files.createTempDirectory(Path.of(".cache/feedback-161"),"services-");
        List<String> options=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
        for(String part:Files.readString(Path.of("tests/resources/Feedback161Cases.java.txt")).split("(?m)^// FILE: ")){
            if(part.isBlank())continue;int end=part.indexOf('\n');Path p=dir.resolve(part.substring(0,end).trim());Files.createDirectories(p.getParent());Files.writeString(p,part.substring(end+1));options.add(p.toString());
        }
        for(String source:new String[]{"StorageSnapshot","TeraRevival","client/StoragePacketGuard"})options.add("src/local/pixelmon/backport/"+source+".java");
        check(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))==0,"Feedback fixture compile");
        String packet="com/pixelmonmod/pixelmon/comm/packetHandlers/clientStorage/newStorage/ClientSetPacket";
        Path packetFile=dir.resolve(packet+".class");ClassNode stub=node(Files.readAllBytes(packetFile));stub.methods.removeIf(m->m.name.equals("original")||m.name.equals("fixed"));
        for(String version:new String[]{"original","patched"}){
            ClassNode n=node(Files.readAllBytes(Path.of(".cache/feedback-161/"+version+"-clientset.class")));
            for(var m:n.methods)if(m.name.equals("handlePacket")||version.equals("patched")&&m.name.contains("backport$")){
                if(m.name.equals("handlePacket")){m.name=version.equals("original")?"original":"fixed";m.access=Opcodes.ACC_PUBLIC;}
                stub.methods.add(m);
            }
        }
        ClassWriter writer=new ClassWriter(0);stub.accept(writer);Files.write(packetFile,writer.toByteArray());
        String moves="com/pixelmonmod/pixelmon/api/pokemon/species/moves/Moves";
        Path movesFile=dir.resolve(moves+".class");ClassNode moveStub=node(Files.readAllBytes(movesFile));moveStub.methods.removeIf(m->m.name.equals("original")||m.name.equals("fixed"));
        for(String version:new String[]{"original","patched"}){
            ClassNode n=node(Files.readAllBytes(Path.of(".cache/feedback-161/"+version+"-moves.class")));
            for(var m:n.methods)if(m.name.equals("isMoveInSet")||version.equals("patched")&&m.name.contains("backport$validMoves")){
                if(m.name.equals("isMoveInSet")){m.name=version.equals("original")?"original":"fixed";m.access=Opcodes.ACC_PUBLIC;}
                moveStub.methods.add(m);
            }
        }
        ClassWriter mw=new ClassWriter(0);moveStub.accept(mw);Files.write(movesFile,mw.toByteArray());
        List<URL> urls=new ArrayList<>();urls.add(dir.toUri().toURL());for(String p:System.getProperty("java.class.path").split(File.pathSeparator))urls.add(Path.of(p).toUri().toURL());
        try(URLClassLoader loader=new URLClassLoader(urls.toArray(new URL[0]),ClassLoader.getPlatformClassLoader())){loader.loadClass("Feedback161Cases").getMethod("run").invoke(null);}
        System.out.println("PASS native original empty-slot delta reproduces MissingNo / wrong-UUID hybrid; patched packet retains real data and recovers only from full snapshots; owned storage authorization/rate limits; AI/player Tera revival success/canceled/missing-owner/missing-storage/failed-spawn/end/simulation/idempotence; compact beam, NPC labels and 85 owned ball art paths. No game launched.");
    }
}
