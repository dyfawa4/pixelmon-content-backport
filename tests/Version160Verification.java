import java.nio.file.*;
import java.net.*;
import java.util.*;
import javax.tools.ToolProvider;
import com.google.gson.*;
import local.pixelmon.backport.SpecialPaletteAdvancements;

/** Targeted 1.6.0 regressions; no client/world startup. */
public final class Version160Verification {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        var rows=SpecialPaletteAdvancements.DATA;check(rows.size()==12,"Only the 12 quest-backed legendary palettes");
        for(var value:rows){var row=value.getAsJsonObject();int dex=row.get("dex").getAsInt();String palette=row.get("palette").getAsString(),form=row.get("form").getAsString();
            check(SpecialPaletteAdvancements.matches(row,dex,palette,form,false,false),"Real appearance rejected");
            for(int wrong=1;wrong<=1026;wrong++)if(wrong!=dex)check(!SpecialPaletteAdvancements.matches(row,wrong,palette,form,false,false),"Wrong species awarded");
            check(!SpecialPaletteAdvancements.matches(row,dex,"none",form,false,false),"Ordinary palette awarded");
            check(!SpecialPaletteAdvancements.matches(row,dex,palette,form,true,false),"Egg awarded");
            check(!SpecialPaletteAdvancements.matches(row,dex,palette,form,false,true),"Shiny excluded by quest");
            check(!SpecialPaletteAdvancements.matches(row,dex,palette,"invalid",false,false),"Other form awarded");
            var resource=new JsonParser().parse(Files.readString(Path.of("resources/data/pixelmon_backport/advancements/legends/"+row.get("id").getAsString()+".json"))).getAsJsonObject();
            check(resource.getAsJsonObject("display").getAsJsonObject("title").get("text").equals(row.get("title")),"Quest title not retained");
        }
        check(rows.toString().contains("天打雷劈")&&rows.toString().contains("美若天仙"),"Requested quest names missing");
        String p="local.pixelmon.backport.",c="com.pixelmonmod.pixelmon.client.gui.battles.ClientBattleManager";
        Map<String,String> sources=new LinkedHashMap<>();
        sources.put(c,"package com.pixelmonmod.pixelmon.client.gui.battles;public class ClientBattleManager {public boolean battleEnded,megaEvolving,dynamaxing,showZMoves;public int battleControllerIndex,dynamaxTurnsLeft;}");
        sources.put(p+"TeraNetwork","package local.pixelmon.backport;public class TeraNetwork {public static class Snapshot {public int battle;public boolean teraRaid;}public static class RaidStatus {public int battle;}}");
        sources.put(p+"TeraClient","package local.pixelmon.backport;public class TeraClient {public static TeraNetwork.Snapshot snapshot;}");
        sources.put(p+"TeraRaidClient","package local.pixelmon.backport;public class TeraRaidClient {public static TeraNetwork.RaidStatus status;}");
        sources.put("fixture.RaidUI","package fixture;import local.pixelmon.backport.*;import "+c+";public class RaidUI {static void ck(boolean b){if(!b)throw new AssertionError(\"Raid UI scope\");}public static void run(){var b=new ClientBattleManager();b.battleControllerIndex=10;b.dynamaxing=true;b.megaEvolving=true;b.showZMoves=true;b.dynamaxTurnsLeft=3;var s=new TeraNetwork.Snapshot();s.battle=10;s.teraRaid=true;TeraClient.snapshot=s;ck(TeraRaidGimmicks.restricted(b));TeraRaidGimmicks.clear(b);ck(!b.dynamaxing&&!b.megaEvolving&&!b.showZMoves&&b.dynamaxTurnsLeft==0);s.teraRaid=false;b.dynamaxing=true;TeraRaidGimmicks.clear(b);ck(b.dynamaxing&&!TeraRaidGimmicks.restricted(b));s.teraRaid=true;s.battle=9;ck(!TeraRaidGimmicks.restricted(b));TeraRaidClient.status=new TeraNetwork.RaidStatus();TeraRaidClient.status.battle=10;ck(TeraRaidGimmicks.restricted(b));b.battleEnded=true;ck(!TeraRaidGimmicks.restricted(b));}}");
        Path dir=Files.createTempDirectory(Path.of(".cache/160-native"),"ui-");List<String> options=new ArrayList<>(List.of("--release","11","-proc:none","-encoding","UTF-8","-cp",System.getProperty("java.class.path"),"-d",dir.toString()));
        for(var row:sources.entrySet()){Path file=dir.resolve(row.getKey().replace('.','/')+".java");Files.createDirectories(file.getParent());Files.writeString(file,row.getValue());options.add(file.toString());}
        check(ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(new String[0]))==0,"UI fixture compile");
        try(var loader=new URLClassLoader(new URL[]{dir.toUri().toURL()},Version160Verification.class.getClassLoader()){
            protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
                if(!name.startsWith("local.pixelmon.backport.")&&!name.equals(c)&&!name.startsWith("fixture."))return super.loadClass(name,resolve);
                Class<?> found=findLoadedClass(name);if(found==null)try{found=findClass(name);}catch(ClassNotFoundException miss){try(var in=getParent().getResourceAsStream(name.replace('.','/')+".class")){byte[] bytes=in.readAllBytes();found=defineClass(name,bytes,0,bytes.length);}catch(Exception fail){throw new ClassNotFoundException(name,fail);}}if(resolve)resolveClass(found);return found;
            }
        }){loader.loadClass("fixture.RaidUI").getMethod("run").invoke(null);}
        System.out.println("PASS 1.6.0: 12 exact quest-named palettes, species/form/shiny/egg negatives; Tera-only UI flag cleanup, ordinary Max raids retained, stale/end battle isolation.");
    }
}
