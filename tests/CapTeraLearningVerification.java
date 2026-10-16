import com.google.gson.*;
import local.pixelmon.backport.*;
import java.util.*;
import java.util.zip.ZipFile;
import org.objectweb.asm.tree.*;

/** Run the shipped post-creation eligibility pass against actual native Pikachu JSON. */
public final class CapTeraLearningVerification {
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    static boolean has(JsonObject moves,String pool,String attack){return moves.has(pool)&&moves.getAsJsonArray(pool).contains(new JsonPrimitive(attack));}
    public static void main(String[] args)throws Exception{
        JsonObject pikachu;
        try(var jar=new ZipFile(args[0]);var in=jar.getInputStream(jar.getEntry("data/pixelmon/species/025_pikachu.json"))){pikachu=new JsonParser().parse(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}
        String nativeBefore=pikachu.toString();JsonObject cap=CapSpecies.create(pikachu),before=new JsonParser().parse(cap.toString()).getAsJsonObject();
        JsonObject oldMoves=before.getAsJsonArray("forms").get(0).getAsJsonObject().getAsJsonObject("moves");
        check(!has(oldMoves,"tmMoves9","Tera Blast"),"Native fixture no longer reproduces reported omission");
        Content.applyTeraLearning(cap);
        JsonObject base=cap.getAsJsonArray("forms").get(0).getAsJsonObject(),moves=base.getAsJsonObject("moves");
        check(has(moves,"tmMoves9","Tera Blast"),"Cap still cannot learn TM9-171");
        boolean level50=false;Set<Integer> levels=new HashSet<>();
        for(var row:moves.getAsJsonArray("levelUpMoves")){var r=row.getAsJsonObject();int level=r.get("level").getAsInt();check(levels.add(level),"Duplicate level replaced native moves");if(level==50&&r.getAsJsonArray("attacks").contains(new JsonPrimitive("Tera Blast")))level50=true;}
        check(level50,"Shared level-50/relearner acquisition missing");
        for(var entry:oldMoves.entrySet()){
            if(entry.getKey().equals("levelUpMoves")){
                for(var row:entry.getValue().getAsJsonArray()){var r=row.getAsJsonObject();JsonArray retained=null;for(var next:moves.getAsJsonArray("levelUpMoves"))if(next.getAsJsonObject().get("level").equals(r.get("level")))retained=next.getAsJsonObject().getAsJsonArray("attacks");for(var attack:r.getAsJsonArray("attacks"))check(retained!=null&&retained.contains(attack),"Lost native level move");}
            }else for(var attack:entry.getValue().getAsJsonArray())check(moves.getAsJsonArray(entry.getKey()).contains(attack),"Lost native/signature compatibility: "+attack);
        }
        JsonObject metadata=new JsonParser().parse(cap.toString()).getAsJsonObject(),oldMetadata=new JsonParser().parse(before.toString()).getAsJsonObject();metadata.getAsJsonArray("forms").get(0).getAsJsonObject().remove("moves");oldMetadata.getAsJsonArray("forms").get(0).getAsJsonObject().remove("moves");check(metadata.equals(oldMetadata),"Stats, traits, forms or signature rules changed");
        String once=cap.toString();Content.applyTeraLearning(cap);check(once.equals(cap.toString()),"Reload duplicates eligibility");check(nativeBefore.equals(pikachu.toString()),"Creating/updating Cap mutated ordinary Pikachu");
        Content.applyTeraLearning(pikachu);check(nativeBefore.equals(pikachu.toString()),"Existing ordinary Pikachu eligibility changed");
        int created=-1,learning=-1,index=0;for(var method:Version13StaticVerification.node("local/pixelmon/backport/Content").methods)if(method.name.equals("apply"))for(var instruction:method.instructions){if(instruction instanceof MethodInsnNode){var call=(MethodInsnNode)instruction;if(call.owner.endsWith("/CapSpecies")&&call.name.equals("add"))created=index;if(call.name.equals("applyTeraLearning"))learning=index;}index++;}check(created>=0&&learning>created,"Shared eligibility must run after independent Cap creation");
        System.out.println("PASS reported Cap omission reproduced; production post-creation pass adds TM9 Tera Blast and level-50 recall once, retains all native/extra/signature moves and stats, preserves ordinary Pikachu and loader order. No game initialized.");
    }
}
