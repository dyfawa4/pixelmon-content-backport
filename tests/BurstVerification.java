import local.pixelmon.backport.BurstPolicy;
import local.pixelmon.backport.CapSpecies;
import local.pixelmon.backport.BurstDisplayPolicy;
import com.google.gson.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Pure JSON/policy/bytecode checks; no world, combat or graphical client starts. */
public final class BurstVerification {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static JsonObject resource(String path)throws Exception{return Version13StaticVerification.resource(path);}
    static void close(double a,double b){check(Math.abs(a-b)<1e-8,"Expected "+b+", got "+a);}
    public static void main(String[] args)throws Exception{
        // All ally species can inherit native terrain. Flying Electric allies can
        // still receive the extra side-specific boost; unrelated attacks cannot.
        for(int flags:new int[]{16|8,16|1,16|8|1}){
            check(BurstDisplayPolicy.move(flags,"Thunderbolt",true,true),"Eligible ally electric attack lacks border");
            check(!BurstDisplayPolicy.move(flags,"Thunder Wave",true,false),"Status electric move falsely boosted");
            check(!BurstDisplayPolicy.move(flags,"Surf",false,true),"Unrelated skill highlighted");
        }
        check(!BurstDisplayPolicy.move(16,"Thunderbolt",true,true),"Airborne non-electric ally falsely boosted");
        check(BurstDisplayPolicy.move(24,"Terrain Pulse",false,true) && !BurstDisplayPolicy.move(16,"Terrain Pulse",false,true),"Terrain Pulse grounding");
        for(String move:new String[]{"Nature Power","Secret Power","Camouflage","Rising Voltage"}){
            check(BurstDisplayPolicy.move(16,move,false,false),"Native terrain-dependent move missing "+move);
            check(!BurstDisplayPolicy.move(0,move,true,true),"Border survived field removal "+move);
        }
        check(BurstDisplayPolicy.ball(1,3,false,false),"Extreme-low catch rate must not hide legitimate ball bonus");
        for(double bonus:new double[]{0,-1,Double.NaN,Double.POSITIVE_INFINITY})check(!BurstDisplayPolicy.ball(bonus,3,false,false),"Invalid bonus falsely highlighted");
        check(!BurstDisplayPolicy.ball(1,-17,false,false) && !BurstDisplayPolicy.ball(1,0,false,false),"Heavy Ball's nonpositive rate falsely highlighted");
        check(!BurstDisplayPolicy.ball(1,3,true,false) && !BurstDisplayPolicy.ball(1,3,false,true),"Forbidden/guaranteed ball falsely highlighted");
        for(double p:new double[]{0,.2,.4,.5,.7,1}){close(BurstPolicy.spawnChance(p),Math.max(0,p-.4));close(p*BurstPolicy.spawnRetention(p),Math.max(0,p-.4));}
        for(double m:new double[]{0,.25,.5,1,2,4}){
            close(BurstPolicy.effectiveness(m,true,true,true,false,false),m==0||m==1?m:m*1.25);
            close(BurstPolicy.effectiveness(m,false,false,false,true,true),m==0||m==1?m:m*.75);
            close(BurstPolicy.effectiveness(m,true,false,true,false,false),m);
            close(BurstPolicy.effectiveness(m,true,true,false,false,false),m);
            close(BurstPolicy.effectiveness(m,false,true,true,false,true),m);
            close(BurstPolicy.effectiveness(m,true,true,true,true,true),m==0||m==1?m:m*1.25);
        }
        close(BurstPolicy.catchBonus(2,true,false),3);close(BurstPolicy.catchBonus(0,true,false),0);
        close(BurstPolicy.catchBonus(2,false,false),2);close(BurstPolicy.catchBonus(999,true,true),0);
        JsonObject nativePikachu;int metagrossRate;Set<String> nativeAttacks=new HashSet<>();
        try(ZipFile jar=new ZipFile(args[0])){
            try(InputStream in=jar.getInputStream(jar.getEntry("data/pixelmon/species/025_pikachu.json"))){nativePikachu=new JsonParser().parse(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
            try(InputStream in=jar.getInputStream(jar.getEntry("data/pixelmon/species/376_metagross.json"))){metagrossRate=new JsonParser().parse(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("forms").get(0).getAsJsonObject().get("catchRate").getAsInt();}
            for(var entries=jar.entries();entries.hasMoreElements();){var entry=entries.nextElement();if(!entry.getName().startsWith("data/pixelmon/moves/") || !entry.getName().endsWith(".json"))continue;
                try(InputStream in=jar.getInputStream(entry)){JsonElement raw=new JsonParser().parse(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8));if(raw.isJsonObject() && raw.getAsJsonObject().has("attackName"))nativeAttacks.add(raw.getAsJsonObject().get("attackName").getAsString());}
            }
        }
        String preserved=nativePikachu.toString();JsonObject cap=CapSpecies.create(nativePikachu),base=cap.getAsJsonArray("forms").get(0).getAsJsonObject();
        check(base.get("malePercentage").getAsInt()==100 && base.getAsJsonArray("possibleGenders").toString().equals("[\"MALE\"]"),"Cap must be male-only without altering native Pikachu");
        check(cap.get("dex").getAsInt()==1026 && cap.get("name").getAsString().equals("CapPikachu"),"Independent identity missing");
        check(preserved.equals(nativePikachu.toString()),"Ordinary Pikachu mutated");
        check(base.get("catchRate").getAsInt()==3 && base.get("catchRate").getAsInt()==metagrossRate && base.getAsJsonArray("tags").contains(new JsonPrimitive("legendary")),"Catch parameter must be the native Metagross extreme-low value, not a flat percentage");
        int total=0;for(var s:base.getAsJsonObject("battleStats").entrySet())total+=s.getValue().getAsInt();check(total==720,"720 base stat total");
        check(base.getAsJsonObject("abilities").getAsJsonArray("abilities").get(0).getAsString().equals("ElectricSurge"),"Native Electric Surge missing");
        check(base.getAsJsonArray("evolutions").size()==0 && base.getAsJsonArray("eggGroups").get(0).getAsString().equals("UNDISCOVERED"),"Legendary must not breed/evolve into Raichu");
        JsonObject oldMoves=nativePikachu.getAsJsonArray("forms").get(0).getAsJsonObject().getAsJsonObject("moves"),moves=base.getAsJsonObject("moves");
        Set<Integer> levels=new HashSet<>();for(JsonElement row:moves.getAsJsonArray("levelUpMoves"))check(levels.add(row.getAsJsonObject().get("level").getAsInt()),"Duplicate level overwrites native learnset");
        for(JsonElement row:oldMoves.getAsJsonArray("levelUpMoves")){int level=row.getAsJsonObject().get("level").getAsInt();JsonArray retained=null;for(JsonElement r:moves.getAsJsonArray("levelUpMoves"))if(r.getAsJsonObject().get("level").getAsInt()==level)retained=r.getAsJsonObject().getAsJsonArray("attacks");for(JsonElement move:row.getAsJsonObject().getAsJsonArray("attacks"))check(retained!=null && retained.contains(move),"Missing native level move "+move);}
        for(String pool:new String[]{"tutorMoves","eggMoves","tmMoves1","tmMoves2","tmMoves3","tmMoves4","tmMoves5","tmMoves6","tmMoves7","tmMoves8","tmMoves9","trMoves8"})if(oldMoves.has(pool))for(JsonElement move:oldMoves.getAsJsonArray(pool))check(moves.getAsJsonArray(pool).contains(move),"Lost native move "+move);
        for(var entry:oldMoves.entrySet())if(entry.getValue().isJsonArray() && !entry.getKey().equals("levelUpMoves"))for(JsonElement move:entry.getValue().getAsJsonArray())check(moves.getAsJsonArray(entry.getKey()).contains(move),"Lost native move pool "+entry.getKey());
        for(JsonElement move:CapSpecies.DATA.getAsJsonArray("extraMoves")){
            check(moves.getAsJsonArray("tmMoves9").contains(move) && moves.getAsJsonArray("tutorMoves").contains(move),"Missing extra learnable move");
            check(nativeAttacks.contains(move.getAsString()),"Additional move cannot resolve in actual native registry: "+move);
        }
        JsonObject main=CapSpecies.DATA.getAsJsonArray("moves").get(0).getAsJsonObject(),z=main.getAsJsonArray("z").get(0).getAsJsonObject();
        check(main.get("basePower").getAsInt()==120 && main.get("accuracy").getAsInt()==100,"Signature stats");
        check(z.get("basePower").getAsInt()==185 && z.get("attackName").getAsString().equals("Origin of Thunder") && z.getAsJsonArray("allowedPokemon").get(0).getAsString().equals("CapPikachu"),"Z stats/name/eligibility");
        JsonObject spawn=resource("data/pixelmon/spawning/legendaries/cappikachu.set.json").getAsJsonArray("spawnInfos").get(0).getAsJsonObject();
        check(spawn.get("interval").getAsString().equals("legendary") && spawn.getAsJsonObject("condition").getAsJsonArray("stringBiomes").toString().equals("[\"minecraft:plains\"]") && spawn.getAsJsonObject("condition").getAsJsonArray("structures").toString().equals("[\"village\"]"),"Spawn location/clock broadened or unsupported native village key");
        JsonObject recipe=resource("data/pixelmon/recipes/pikashunium_z.json");check(recipe.getAsJsonArray("ingredients").size()==4 && recipe.getAsJsonObject("result").get("count").getAsInt()==1,"Four-to-one recipe");
        check(resource("data/pixelmon_backport/advancements/legends/capture_1026.json").getAsJsonObject("display").get("frame").getAsString().equals("challenge"),"Partner challenge missing");
        for(String lang:new String[]{"en_us","zh_cn"}){JsonObject locale=resource("assets/pixelmon_backport/lang/"+lang+".json");for(String key:new String[]{"pixelmon.cappikachu","attack.ten_million_volts","attack.origin_of_thunder","pixelmon.backport.burst.start"})check(locale.has(key),"Missing localized identity "+key);}
        ClassNode terrain=Version13StaticVerification.node("local/pixelmon/backport/BurstTerrain");check(terrain.superName.endsWith("/ElectricTerrain"),"Native terrain effects replaced");
        for(MethodNode method:terrain.methods)if(method.name.equals("applyRepeatedEffect"))for(AbstractInsnNode i:method.instructions)if(i instanceof FieldInsnNode)check(!((FieldInsnNode)i).name.equals("turnsToGo"),"Burst terrain decrements its duration");
        for(String ui:new String[]{"TeraMoveDisplayMixin","BurstBagDisplayMixin","BurstRaidDisplayMixin"}){
            boolean wrapped=false;for(MethodNode method:Version13StaticVerification.node("local/pixelmon/backport/mixin/"+ui).methods)for(AbstractInsnNode i:method.instructions)if(i instanceof MethodInsnNode){var call=(MethodInsnNode)i;if(call.owner.equals("local/pixelmon/backport/BurstClient") && (call.name.equals("outline") || call.name.equals("moveOutline")))wrapped=true;}
            check(wrapped,"Full button/item frame is not connected to native render: "+ui);
        }
        ClassNode outline=Version13StaticVerification.node("local/pixelmon/backport/BurstClient");boolean width=false,height=false;
        for(MethodNode method:outline.methods)if(method.name.equals("outline"))for(AbstractInsnNode i:method.instructions)if(i instanceof MethodInsnNode){width|=((MethodInsnNode)i).name.equals("func_230998_h_");height|=((MethodInsnNode)i).name.equals("func_238483_d_");}
        check(width && height,"Border uses sprite-only dimensions rather than the complete native button");
        ClassNode capRead=Version13StaticVerification.node("local/pixelmon/backport/CapPikachu");for(MethodNode method:capRead.methods)if(method.name.equals("sync"))for(AbstractInsnNode i:method.instructions)if(i instanceof FieldInsnNode)check(!((FieldInsnNode)i).name.equals("pikashunium_z"),"Ordinary Pikachu still responds to crystal");
        boolean guard=false;for(MethodNode method:Version13StaticVerification.node("local/pixelmon/backport/mixin/CapGuaranteedCaptureMixin").methods)for(AbstractInsnNode i:method.instructions)if(i instanceof MethodInsnNode && ((MethodInsnNode)i).name.equals("forbidden"))guard=true;check(guard,"ALWAYS_CAPTURE bypass is unguarded");
        System.out.println("PASS independent native species/720 stats/full learnset, plains-village legendary clock and 70%-40pp=30%, native Z signature, inherited indefinite terrain, both sides/neutral/immunity/zero-bonus boundaries, all ally/terrain-linked move borders, full native move/bag/raid item frames and guaranteed-capture guard; no game initialized.");
    }
}
