import java.util.*;
import java.util.zip.*;
import com.google.gson.*;
import org.objectweb.asm.tree.*;
import local.pixelmon.backport.*;

public final class LateBloomerVerification {
    static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
    public static void main(String[] args)throws Exception{
        String[] ids={"dragonite","tyranitar","salamence","metagross","garchomp","hydreigon","goodra","kommo_o","dragapult","hisuian_goodra","baxcalibur"};
        int[] dex={149,248,373,376,445,635,706,784,887,706,998};
        String[] titles={"快龙","班基拉斯","暴飞龙","巨金怪","烈咬陆鲨","三首恶龙","黏美龙","杖尾鳞甲龙","多龙巴鲁托","洗翠黏美龙","戟脊龙"};
        String base="data/pixelmon_backport/advancements/late_bloomers/";
        var root=Version13StaticVerification.resource(base+"root.json");
        var zh=Version13StaticVerification.resource("assets/pixelmon_backport/lang/zh_cn.json");var en=Version13StaticVerification.resource("assets/pixelmon_backport/lang/en_us.json");
        check(root.getAsJsonObject("criteria").size()==1&&root.getAsJsonObject("criteria").getAsJsonObject("earned").get("trigger").getAsString().equals("minecraft:impossible")&&!root.has("requirements"),"Opening must unlock on first earned species, not all eleven");
        for(int i=0;i<ids.length;i++){
            check(ids[i].equals(LateBloomerAdvancements.target(dex[i],i==9?"hisuian":"")),"Wrong target/form mapping");
            var row=Version13StaticVerification.resource(base+ids[i]+".json");var d=row.getAsJsonObject("display");
            check(row.get("parent").getAsString().equals("pixelmon_backport:late_bloomers/root")&&d.get("frame").getAsString().equals("task"),"Individual parent/frame");
            check(zh.get(d.getAsJsonObject("title").get("translate").getAsString()).getAsString().equals(titles[i]),"Not the species name "+ids[i]);
            check(zh.get(d.getAsJsonObject("description").get("translate").getAsString()).getAsString().startsWith("获得"),"Capture-only wording");
            for(String key:new String[]{"title","description"})check(en.has(d.getAsJsonObject(key).get("translate").getAsString()),"English key missing");
            check(!row.has("rewards")&&row.getAsJsonObject("criteria").size()==1&&row.getAsJsonObject("criteria").getAsJsonObject("earned").get("trigger").getAsString().equals("minecraft:impossible"),"Unrequested reward or raw-capture trigger");
            check(d.getAsJsonObject("icon").get("nbt").getAsString().contains("ndex:"+dex[i]+"s"),"Wrong sprite species");
        }
        check(zh.get(root.getAsJsonObject("display").getAsJsonObject("title").get("translate").getAsString()).getAsString().equals("大器晚成"),"Root name");
        check(LateBloomerAdvancements.target(706,"unknown")==null&&LateBloomerAdvancements.target(25,"")==null,"Unknown species/form accepted");
        try(var z=new ZipFile(args[0])){
            var e=z.getEntry("data/pixelmon/species/706_goodra.json");try(var in=z.getInputStream(e)){var goodra=new JsonParser().parse(new java.io.InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();Set<String> actual=new HashSet<>();for(var f:goodra.getAsJsonArray("forms"))actual.add(f.getAsJsonObject().get("name").getAsString());check(actual.contains("")&&actual.contains("hisuian"),"Goodra actual native form names changed");}
        }
        for(String method:new String[]{"species","raidStored"}){
            boolean canonical=false,awarded=false;for(var m:Version13StaticVerification.node("local/pixelmon/backport/AcquisitionAdvancements").methods)if(m.name.equals(method))for(var i:m.instructions)if(i instanceof MethodInsnNode){var call=(MethodInsnNode)i;if(call.name.equals("ownedStored"))canonical=true;if(call.owner.endsWith("/LateBloomerAdvancements")&&call.name.equals("acquired")){check(canonical,"Award before canonical storage check");awarded=true;}}
            check(awarded,"Acquisition route missing "+method);
        }
        System.out.println("PASS 11 species-name task resources, native Goodra form identifiers, first-obtained opening, canonical general/raid acquisition wiring and Chinese/English coverage. Gameplay acceptance remains manual.");
    }
}
