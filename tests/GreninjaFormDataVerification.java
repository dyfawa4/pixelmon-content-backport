import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import javax.tools.ToolProvider;
import com.google.gson.*;
import local.pixelmon.backport.*;
import net.minecraft.util.ResourceLocation;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

/** Native appearance inheritance and battle-start ordering, without starting a game. */
public final class GreninjaFormDataVerification {
    static void require(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    static ClassNode node(byte[] bytes){ClassNode node=new ClassNode();new ClassReader(bytes).accept(node,0);return node;}
    static int call(MethodNode method,String owner,String name){int i=0;for(var insn:method.instructions){if(insn instanceof MethodInsnNode){var m=(MethodInsnNode)insn;if(m.owner.equals(owner)&&m.name.equals(name))return i;}i++;}return -1;}
    public static void main(String[] args)throws Exception{
        JsonObject original;JsonParser parser=new JsonParser();
        try(JarFile jar=new JarFile(args[0])){
            var entry=jar.stream().filter(e->e.getName().startsWith("data/pixelmon/species/")&&e.getName().endsWith("greninja.json")).findFirst().orElseThrow();
            try(var reader=new InputStreamReader(jar.getInputStream(entry),StandardCharsets.UTF_8)){original=parser.parse(reader).getAsJsonObject();}
            String participant="com/pixelmonmod/pixelmon/battles/controller/participants/";
            ClassNode trainer=node(jar.getInputStream(jar.getJarEntry(participant+"TrainerParticipant.class")).readAllBytes());
            MethodNode start=trainer.methods.stream().filter(m->m.name.equals("startBattle")).findFirst().orElseThrow();
            require(call(start,participant+"PixelmonWrapper","setHealth")>=0,"Native trainer startup no longer normalizes health; review fixture timing");
            ClassNode controller=node(jar.getInputStream(jar.getJarEntry("com/pixelmonmod/pixelmon/battles/controller/BattleController.class")).readAllBytes());
            boolean ordered=false;
            for(MethodNode m:controller.methods)if(m.name.equals("initBattle")){
                int begin=call(m,participant+"BattleParticipant","startBattle"),snapshot=call(m,participant+"BattleParticipant","updateOtherPokemon");
                if(begin>=0&&snapshot>begin)ordered=true;
            }
            require(ordered,"Native initial opponent snapshot no longer follows participant startup");
        }
        JsonObject merged=parser.parse(original.toString()).getAsJsonObject();
        for(JsonElement row:Content.DATA.getAsJsonArray("patches")){
            JsonObject species=row.getAsJsonObject().getAsJsonObject("species");
            if(species.get("dex").getAsInt()==658)merged=Content.mergeSpecies(merged,species);
        }
        Map<ResourceLocation,JsonElement> data=new LinkedHashMap<>();ResourceLocation id=new ResourceLocation("pixelmon","greninja");data.put(id,merged);PermanentAsh.addForm(data);merged=data.get(id).getAsJsonObject();
        for(JsonElement nativeForm:original.getAsJsonArray("forms")){
            String name=nativeForm.getAsJsonObject().get("name").getAsString();
            if(!name.equals("battle_bond")&&!name.equals("ash"))continue;
            boolean same=false;for(JsonElement f:merged.getAsJsonArray("forms"))if(f.equals(nativeForm))same=true;
            require(same,"Native Greninja form overwritten: "+name);
        }
        Content.addNinjaLearning(merged);Content.applyTeraLearning(merged);
        for(var row:merged.getAsJsonArray("forms")){
            var f=row.getAsJsonObject();String name=f.get("name").getAsString();if(!name.equals("ash")&&!name.equals(PermanentAsh.FORM)&&!name.equals(PermanentAsh.MEGA))continue;
            var moves=f.getAsJsonObject("moves");require(moves.getAsJsonArray("levelUpMoves").size()>=11,"Ash lost native inherited levels");
            require(moves.getAsJsonArray("tutorMoves").contains(new JsonPrimitive("Ninja Training")),"Ash signature tutor missing");
            require(moves.getAsJsonArray("tmMoves9").contains(new JsonPrimitive("Tera Blast")),"Ash Tera Blast TM missing");
            boolean reminder=false;for(var level:moves.getAsJsonArray("levelUpMoves"))if(level.getAsJsonObject().getAsJsonArray("attacks").contains(new JsonPrimitive("Tera Blast")))reminder=true;
            require(reminder,"Ash cannot relearn Tera Blast");
        }
        // Retain the actual native appearance adapters. Remove unrelated fields
        // that require a running sound/particle/item registry, not appearance data.
        for(JsonElement f:merged.getAsJsonArray("forms")){
            JsonObject form=f.getAsJsonObject();
            for(var field:new ArrayList<>(form.entrySet()))if(!Arrays.asList("name","genderProperties","defaultBaseForm","tags").contains(field.getKey()))form.remove(field.getKey());
            if(form.has("genderProperties"))for(var gender:form.getAsJsonArray("genderProperties"))for(var palette:gender.getAsJsonObject().getAsJsonArray("palettes")){
                palette.getAsJsonObject().remove("sounds");palette.getAsJsonObject().remove("particle");
            }
        }
        Path temp=Files.createTempDirectory("backport-greninja-native-");
        Path env=temp.resolve("net/minecraftforge/fml/loading/FMLEnvironment.java");Files.createDirectories(env.getParent());
        // Only the Forge launch-side value is supplied in this isolated loader.
        // No Pokemon, form, palette or battle implementation is substituted.
        Files.writeString(env,"package net.minecraftforge.fml.loading; public final class FMLEnvironment {public static final net.minecraftforge.api.distmarker.Dist dist=net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER; public static final boolean production=true;}");
        Path probe=temp.resolve("NativeGreninjaAppearance.java");
        Files.writeString(probe,"import com.pixelmonmod.pixelmon.api.pokemon.species.*; import com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender; public final class NativeGreninjaAppearance {public static void verify(String json){Species s=Species.GSON.fromJson(json,Species.class); for(String n:new String[]{\"\",\"battle_bond\",\"ash\",\"ash_permanent\"}){Stats f=s.getForm(n);String sprite=f.getGenderProperties(Gender.MALE).getDefaultPalette().getSprite().toString();String expected=n.startsWith(\"ash\")?\"/ash/\":\"/base/\";if(!sprite.contains(expected))throw new AssertionError(n+\" appearance: \"+sprite);if(f.isTemporary()!=n.equals(\"ash\"))throw new AssertionError(\"Unexpected temporary tag: \"+n);System.out.println(\"GRENINJA_NATIVE_APPEARANCE form=\"+n+\" sprite=\"+sprite);}}}");
        String cp=System.getProperty("java.class.path");
        require(ToolProvider.getSystemJavaCompiler().run(null,null,null,"--release","11","-proc:none","-encoding","UTF-8","-cp",cp,"-d",temp.toString(),env.toString(),probe.toString())==0,"Appearance probe compilation failed");
        List<URL> urls=new ArrayList<>();urls.add(temp.toUri().toURL());for(String path:cp.split(java.io.File.pathSeparator))urls.add(Paths.get(path).toUri().toURL());
        try(URLClassLoader loader=new URLClassLoader(urls.toArray(new URL[0]),ClassLoader.getPlatformClassLoader())){Class.forName("NativeGreninjaAppearance",true,loader).getMethod("verify",String.class).invoke(null,merged.toString());}
        ClassNode fixture;
        try(InputStream in=GreninjaFormDataVerification.class.getResourceAsStream("/TestMaps$AshOpponent.class")){fixture=node(in.readAllBytes());}
        String trainer="com/pixelmonmod/pixelmon/battles/controller/participants/TrainerParticipant";
        require(fixture.superName.equals(trainer),"Ash fixture must have genuine trainer reserves");
        MethodNode begin=fixture.methods.stream().filter(m->m.name.equals("startBattle")).findFirst().orElseThrow();
        require(call(begin,trainer,"startBattle")>=0&&call(begin,"com/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper","setHealth")>call(begin,trainer,"startBattle"),"1 HP must be set after native startup healing");
        ClassNode maps;try(InputStream in=GreninjaFormDataVerification.class.getResourceAsStream("/TestMaps.class")){maps=node(in.readAllBytes());}
        require(maps.methods.stream().anyMatch(m->m.name.equals("action")&&call(m,"com/pixelmonmod/pixelmon/battles/controller/participants/PlayerParticipant","updateOpponentPokemon")>=0),"Manual fixture must refresh the actual client opponent snapshot");
        System.out.println("PASS Greninja native normal/Battle Bond/Ash/permanent appearance and tags; native trainer heal/snapshot ordering; true-reserve fixture and client refresh (offline, no gameplay acceptance)");
    }
}
