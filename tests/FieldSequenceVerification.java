import local.pixelmon.backport.FieldSequencePolicy;
import com.google.gson.*;
import java.nio.file.*;
public final class FieldSequenceVerification {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    public static void main(String[] args)throws Exception{
        for(String form:new String[]{"ten_percent","fifty_percent"}){
            check(FieldSequencePolicy.completeStep(718,true,form),"Construct base must complete before Mega");
            check(!FieldSequencePolicy.completeStep(718,false,form),"Aura Break must not gain complete form");
            check(!FieldSequencePolicy.completeStep(25,true,form),"Other species must not enter core sequence");
        }
        check(!FieldSequencePolicy.completeStep(718,true,"complete") && !FieldSequencePolicy.completeStep(718,true,"mega"),"Already transformed must not repeat core step");
        check(FieldSequencePolicy.chain(true,true,"complete","complete",true),"Second J should chain Mega using original snapshot");
        check(!FieldSequencePolicy.chain(true,true,"complete","complete",false),"No crystal means revert rather than free Mega");
        check(!FieldSequencePolicy.chain(true,true,"mega","mega",true),"Third J should restore original base");
        check(!FieldSequencePolicy.chain(true,false,"complete","complete",true),"Nonconstruct should not acquire core chain");
        JsonObject content=new JsonParser().parse(Files.readString(Path.of("resources/backport/content.json"))).getAsJsonObject();boolean nativeStone=false;
        for(JsonElement patch:content.getAsJsonArray("patches")){JsonObject species=patch.getAsJsonObject().getAsJsonObject("species");if(species.get("dex").getAsInt()!=718)continue;
            for(JsonElement value:species.getAsJsonArray("forms")){JsonObject f=value.getAsJsonObject();if(f.get("name").getAsString().equals("complete"))nativeStone=f.getAsJsonArray("megaItems").toString().contains("zygardite") && f.getAsJsonArray("megas").toString().contains("mega");}}
        check(nativeStone,"Complete form must retain its real matching Mega crystal");
        String field=Files.readString(Path.of("src/local/pixelmon/backport/FieldMega.java"));
        check(field.contains("chaining?p.getPersistentData().func_74775_l(MARKER).func_74737_b()") && field.contains("FieldSequencePolicy.chain"),"Chain loses original restoration snapshot");
        System.out.println("PASS Power Construct ten/fifty -> complete -> matching Mega sequence, Aura Break/no-stone/revert boundaries and original snapshot preservation; no game initialized.");
    }
}
