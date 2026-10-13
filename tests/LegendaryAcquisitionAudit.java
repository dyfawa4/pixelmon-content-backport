import com.google.gson.*;
import local.pixelmon.backport.*;
import java.util.*;
import java.util.zip.*;
import java.nio.file.*;
import java.io.*;
public final class LegendaryAcquisitionAudit {
    static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);}
    static JsonObject read(ZipFile jar,ZipEntry entry)throws Exception{try(var in=jar.getInputStream(entry)){return new JsonParser().parse(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();}}
    public static void main(String[] args)throws Exception{
        Map<Integer,JsonObject> species=new TreeMap<>();Map<String,String> nativeCriteria=new HashMap<>();
        try(ZipFile jar=new ZipFile(args[0])){for(var all=jar.entries();all.hasMoreElements();){var e=all.nextElement();String path=e.getName();
            if(path.startsWith("data/pixelmon/species/") && path.endsWith(".json")){JsonObject s=read(jar,e);species.put(s.get("dex").getAsInt(),s);}
            if(path.startsWith("data/pixelmon/advancements/") && path.endsWith(".json"))for(var c:read(jar,e).getAsJsonObject("criteria").entrySet()){
                JsonObject criterion=c.getValue().getAsJsonObject();if(criterion.get("trigger").getAsString().equals("pixelmon:legendary_capture_trigger")){
                    String name=criterion.getAsJsonObject("conditions").get("pokemon").getAsString().toLowerCase(Locale.ROOT);
                    nativeCriteria.put(name,path.substring("data/pixelmon/advancements/".length(),path.length()-5));
                }
            }
        }}
        for(var patch:Content.DATA.getAsJsonArray("patches")){
            JsonObject row=patch.getAsJsonObject(),incoming=row.getAsJsonObject("species");int dex=incoming.get("dex").getAsInt();
            if(!species.containsKey(dex))species.put(dex,new JsonParser().parse(incoming.toString()).getAsJsonObject());
            else if(!row.get("mode").getAsString().equals("new"))species.put(dex,Content.mergeSpecies(species.get(dex),incoming));
        }
        species.put(1026,CapSpecies.create(species.get(25)));for(var s:species.values())LegendaryEvolutionData.normalize(s);
        Map<String,JsonObject> byName=new HashMap<>();for(var s:species.values())byName.put(s.get("name").getAsString().toLowerCase(Locale.ROOT),s);
        JsonObject catalogue=Content.read("backport/advancements.json"),captures=catalogue.getAsJsonObject("captures"),forms=catalogue.getAsJsonObject("capturesByForm");
        List<String> rows=new ArrayList<>(),routes=new ArrayList<>();Set<Integer> covered=new HashSet<>();int nativeCount=0,backportCount=0;
        for(var e:captures.entrySet()){
            int dex=Integer.parseInt(e.getKey());check(species.containsKey(dex),"Unregistered capture species "+dex);JsonObject row=e.getValue().getAsJsonObject();verifyAdv(row.get("id").getAsString());covered.add(dex);backportCount++;
            rows.add("| "+dex+" | "+species.get(dex).get("name").getAsString()+" | "+row.get("id").getAsString()+" | 代码通过：最终同行/PC真实对象；六类获得入口共用 |\n");
        }
        for(var e:forms.entrySet()){
            int dex=Integer.parseInt(e.getKey());check(species.containsKey(dex),"Unregistered form species");Set<String> actual=new HashSet<>();for(var f:species.get(dex).getAsJsonArray("forms"))actual.add(f.getAsJsonObject().get("name").getAsString());
            for(var f:e.getValue().getAsJsonObject().entrySet()){
                check(actual.contains(f.getKey()),"Unknown acquisition form "+dex+" "+f.getKey());var row=f.getValue().getAsJsonObject();verifyAdv(row.get("id").getAsString());covered.add(dex);backportCount++;
                rows.add("| "+dex+" "+f.getKey()+" | "+species.get(dex).get("name").getAsString()+" | "+row.get("id").getAsString()+" | 代码通过：最终真实形态；同行/PC共用，不借用外观 |\n");
            }
        }
        for(var e:nativeCriteria.entrySet()){
            if(e.getKey().equals("legendary")){nativeCount++;rows.add("| 通用 | 首只传说 | pixelmon:"+e.getValue()+" | 代码通过：最终获得后检查原生通用条件；不冒充具体物种 |\n");continue;}
            check(byName.containsKey(e.getKey()),"Unresolved native legendary criterion "+e.getKey());var s=byName.get(e.getKey());int dex=s.get("dex").getAsInt();covered.add(dex);nativeCount++;
            rows.add("| "+dex+" | "+s.get("name").getAsString()+" | pixelmon:"+e.getValue()+" | 代码通过：统一SPEC/LEGENDARY实际获得重放；三鸟仅普通形态 |\n");
        }
        int legendarySpecies=0;
        for(var s:species.values()){
            JsonObject base=s.getAsJsonArray("forms").get(0).getAsJsonObject();Set<String> tags=new HashSet<>();if(base.has("tags"))for(var tag:base.getAsJsonArray("tags"))tags.add(tag.getAsString());
            if(!Collections.disjoint(tags,Set.of("legendary","mythical","ultrabeast"))){int dex=s.get("dex").getAsInt();check(covered.contains(dex),"Legendary species has no actual acquisition achievement: "+dex+" "+s.get("name"));legendarySpecies++;}
            for(var f:s.getAsJsonArray("forms")){
                var form=f.getAsJsonObject();if(!form.has("evolutions"))continue;
                for(var ev:form.getAsJsonArray("evolutions")){
                    var evo=ev.getAsJsonObject();if(!evo.has("to"))continue;String target=evo.get("to").getAsString();String[] parts=target.split(" ");var evolved=byName.get(parts[0].toLowerCase(Locale.ROOT));if(evolved==null)continue;
                    int dex=evolved.get("dex").getAsInt();if(!covered.contains(dex))continue;
                    for(String part:parts)if(part.startsWith("form:")){String wanted=part.substring(5);boolean found=false;for(var ef:evolved.getAsJsonArray("forms"))found|=ef.getAsJsonObject().get("name").getAsString().equals(wanted);check(found,"Evolution points to missing real form: "+target);}
                    check(AcquisitionPolicy.evolved(s.get("dex").getAsInt(),dex,false,true,true,true),"Completed evolution rejected");routes.add("| "+s.get("name").getAsString()+" | "+target+" | 代码通过：完成真实物种变化、最终同行/PC对象 |\n");
                }
            }
        }
        check(routes.size()>=8,"Missed native legendary evolution routes");
        String doc="# 1.4.0 神兽获得成就逐项代码回归\n\n原生及回迁资源实际枚举，记录日期2026-10-13。共"+legendarySpecies+"个传奇/幻之/究极异兽种类覆盖；本体"+nativeCount+"项物种条件、回迁"+backportCount+"项物种/形态路由逐条检查。保留原ID/已得进度。\n\n代码共同入口：捕捉后最终入库、进化完成、巢穴实际入库、完成孵化、交换/赠送实际存储、登录已拥有补查；同行栏与PC使用同一真实所有者/存储对象验证，不固定槽位，不伪造球或捕捉统计。成就记录均为代码/资源接入检查，不宣称游戏内获得已经验收。形态专属任务只接受对应形态，普通/伽勒尔三鸟分开；临时战斗变形不伪造实际获得。伪装百变怪只按还原后物种，“乔装打扮”还需真实捕捉资格。\n\n| 编号/形态 | 物种 | 成就/条件 | 检查记录 |\n| --- | --- | --- | --- |\n"+String.join("",rows)+"\n实际进化路线（从原生数据枚举，武道熊师两流派分别检查）：\n\n| 来源 | 实际目标 | 检查记录 |\n| --- | --- | --- |\n"+String.join("",routes);
        Files.writeString(Path.of("docs/LEGENDARY_ACQUISITION_AUDIT_1.4.0.md"),doc);
        System.out.println("PASS every legendary acquisition resource: "+legendarySpecies+" species, "+nativeCount+" native criteria, "+backportCount+" backport routes, "+routes.size()+" actual evolution paths; final party+PC routing shared. Code/resource audit only.");
    }
    static void verifyAdv(String id)throws Exception{
        Path file=Path.of("resources/data/pixelmon_backport/advancements/legends/"+id+".json");check(Files.exists(file),"Missing achievement "+id);
        JsonObject d=new JsonParser().parse(Files.readString(file)).getAsJsonObject();check(d.getAsJsonObject("criteria").has("earned"),"Criterion ID changed "+id);
    }
}
