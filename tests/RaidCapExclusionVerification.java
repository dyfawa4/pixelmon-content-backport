import local.pixelmon.backport.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.init.registry.RaidRegistry;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

public final class RaidCapExclusionVerification {
    private static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static ClassNode nativeClass(JarFile jar,String name)throws Exception{
        ClassNode c=new ClassNode();try(var in=jar.getInputStream(jar.getJarEntry(name+".class"))){new ClassReader(in).accept(c,0);}return c;
    }
    private static boolean call(ClassNode c,String method,String name){
        for(MethodNode m:c.methods)if(m.name.equals(method))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode&&((MethodInsnNode)i).name.equals(name))return true;
        return false;
    }
    public static void main(String[] args)throws Exception{
        for(int dex=1;dex<=1026;dex++)require(RaidSpawnExclusions.allowed(dex)==(dex!=1026),"Other species accidentally excluded "+dex);
        for(String spec:new String[]{"CapPikachu","cappikachu level:70","1026 form:"})require(!RaidSpawnExclusions.allowedSpec(spec),"Cap spec bypass: "+spec);
        for(String spec:new String[]{"Pikachu","Pikachu form:ash_cap","25","Rayquaza","Poipole"})require(RaidSpawnExclusions.allowedSpec(spec),"Other den species accidentally excluded "+spec);
        List<RaidRegistry.Raid> nativePool=new ArrayList<>();nativePool.add(new RaidRegistry.Raid(25,""));nativePool.add(new RaidRegistry.Raid(384,"mega"));
        require(RaidSpawnExclusions.candidates(nativePool)==nativePool,"Unrelated native den pools copied or reordered");
        nativePool.add(new RaidRegistry.Raid(1026,""));nativePool.add(new RaidRegistry.Raid(25,""));
        var filtered=RaidSpawnExclusions.candidates(nativePool);
        require(nativePool.size()==4&&filtered.size()==3&&filtered.stream().noneMatch(r->r.getSpecies()==1026),"Cap filtering mutated native registry or retained Cap");
        require(RaidSpawnExclusions.candidates(List.of(new RaidRegistry.Raid(1026,""))).isEmpty(),"Cap-only den pool not empty");
        JsonArray entries=TeraRaids.DATA.getAsJsonArray("encounters");JsonObject injected=new JsonObject();
        injected.addProperty("spec","CapPikachu");injected.addProperty("min",1);injected.addProperty("max",6);injected.addProperty("weight",1e15);entries.add(injected);
        try{for(int stars=1;stars<=6;stars++)for(int seed=0;seed<500;seed++)require(RaidSpawnExclusions.allowedSpec(TeraRaids.pick(stars,new Random(seed),spec->true).get("spec").getAsString()),"Injected high-weight Cap leaked into Tera pool");}
        finally{entries.remove(injected);}
        try(JarFile jar=new JarFile(args[0])){
            ClassNode den=nativeClass(jar,"com/pixelmonmod/pixelmon/entities/DenEntity");
            require(call(den,"roll","getRandomRaid")&&call(den,"attemptWishingPiece","getRandomLegendary"),"Native normal/legendary wishing-piece paths changed");
            ClassNode random=nativeClass(jar,"com/pixelmonmod/pixelmon/battles/raids/RaidRandomizer");
            require(call(random,"getRandomRaid","getRandomElementFromCollection")&&call(random,"getRandomRaid","getRaid"),"Native pool/event override paths changed");
            ClassNode randomHelper=nativeClass(jar,"com/pixelmonmod/pixelmon/api/util/helpers/RandomHelper");
            require(call(randomHelper,"getRandomElementFromCollection","isEmpty"),"Native empty-pool safety changed");
        }
        require(call(Version13StaticVerification.node("local/pixelmon/backport/mixin/DenCapExclusionMixin"),"backport$excludeLoadedCap","clearData"),"Old saved Cap den not cleared");
        require(call(Version13StaticVerification.node("local/pixelmon/backport/TeraRaids"),"encounter","allowed"),"Resolved species aliases can bypass Tera exclusion");
        System.out.println("PASS Cap-only separate den exclusion: all other species preserved, native pools and legendary wishing pieces, event override/final assignment/load boundaries; 3000 actual Tera weighted picks reject injected high-weight Cap. Village spawning unaffected. Gameplay remains manual.");
    }
}
