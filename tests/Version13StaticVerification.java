import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Read class bytes and resource data only. Never initialize a native game class. */
public final class Version13StaticVerification {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static ClassNode node(String name)throws Exception{
        try(InputStream in=Version13StaticVerification.class.getClassLoader().getResourceAsStream(name+".class")){
            check(in!=null,"Missing class "+name);ClassNode n=new ClassNode();new ClassReader(in.readAllBytes()).accept(n,0);return n;
        }
    }
    static AbstractInsnNode previous(AbstractInsnNode n){do{n=n.getPrevious();}while(n!=null && n.getOpcode()<0);return n;}
    static int integer(AbstractInsnNode n){
        if(n instanceof IntInsnNode)return ((IntInsnNode)n).operand;
        if(n instanceof LdcInsnNode)return ((Number)((LdcInsnNode)n).cst).intValue();
        if(n.getOpcode()>=Opcodes.ICONST_M1 && n.getOpcode()<=Opcodes.ICONST_5)return n.getOpcode()-Opcodes.ICONST_0;
        throw new AssertionError("Unexpected enum id instruction");
    }
    static JsonObject resource(String name)throws Exception{
        try(InputStream in=Version13StaticVerification.class.getClassLoader().getResourceAsStream(name)){
            check(in!=null,"Missing resource "+name);return new JsonParser().parse(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    static void capResources()throws Exception{
        ClassLoader loader=Version13StaticVerification.class.getClassLoader();
        for(JsonElement gender:resource("backport/ash_cap.json").getAsJsonArray("genderProperties")){
            for(JsonElement value:gender.getAsJsonObject().getAsJsonArray("palettes")){
                JsonObject palette=value.getAsJsonObject();
                String sprite=palette.get("sprite").getAsString();
                check(sprite.startsWith("pixelmon_backport:"),"Cap must have its own sprite");
                String[] icon=sprite.split(":",2);
                var image=javax.imageio.ImageIO.read(loader.getResource("assets/"+icon[0]+"/textures/"+icon[1]));
                check(image!=null && image.getColorModel().hasAlpha(),"Missing transparent cap sprite "+sprite);
                check(image.getWidth()==32 && image.getHeight()==32,"Cap minimap icon must match native 32px sprites, not the old 128px render");
                check((image.getRGB(0,0)>>>24)==0,"Cap sprite must retain a transparent margin");
                for(JsonElement entry:palette.getAsJsonObject("modelLocator").getAsJsonArray("pqc")){
                    String[] id=entry.getAsString().split(":",2);
                    try(InputStream in=loader.getResourceAsStream("assets/"+id[0]+"/textures/"+id[1])){
                        check(in!=null,"Missing cap PQC");
                        for(String line:new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).split("\\R")){
                            String[] fields=line.trim().split("\\s+");
                            if(!fields[0].equals("$body") && !fields[0].equals("$anim"))continue;
                            String file=fields[fields.length-1];
                            // Native ValveStudioModel.getResource returns explicit IDs verbatim,
                            // while relative IDs default to pixelmon rather than the PQC namespace.
                            check(file.contains(":"),"Custom cap PQC must avoid native relative-path fallback: "+file);
                            String[] asset=file.split(":",2);
                            check(loader.getResource("assets/"+asset[0]+"/"+asset[1])!=null,"Native loader cannot resolve "+file);
                        }
                    }
                }
            }
        }
        System.out.println("PASS cap model/idle/walk paths resolved as native loader; separate normal/shiny transparent sprites");
    }
    static AbstractInsnNode nextReal(AbstractInsnNode n){do{n=n.getNext();}while(n!=null && n.getOpcode()<0);return n;}
    static void bagSelection()throws Exception{
        ClassNode pre=node("com/pixelmonmod/pixelmon/api/events/battles/BagItemEvent$SelectItem$Pre");
        check(pre.visibleAnnotations!=null && pre.visibleAnnotations.stream().anyMatch(a->a.desc.equals("Lnet/minecraftforge/eventbus/api/Cancelable;")),"Native bag-selection pre-event must be cancelable");
        boolean before=false,cancelReturns=false,selection=false;
        for(MethodNode m:node("com/pixelmonmod/pixelmon/comm/packetHandlers/battles/BagPacket").methods)if(m.name.equals("bagPacket")){
            for(AbstractInsnNode i:m.instructions){
                if(i instanceof TypeInsnNode && ((TypeInsnNode)i).desc.endsWith("BagItemEvent$SelectItem$Pre"))before=true;
                if(i instanceof MethodInsnNode){
                    MethodInsnNode call=(MethodInsnNode)i;
                    if(before && call.name.equals("post") && !selection){
                        AbstractInsnNode branch=nextReal(i);cancelReturns|=branch.getOpcode()==Opcodes.IFEQ && nextReal(branch).getOpcode()==Opcodes.RETURN;
                    }
                    if(call.name.equals("setUseItem")){check(before && cancelReturns,"Native selection must return on cancellation before committing action");selection=true;}
                }
            }
        }
        check(selection && cancelReturns,"No native pre-selection cancellation boundary");
        boolean menu=false;
        for(MethodNode m:node("local/pixelmon/backport/GymChallenge").methods)if(m.name.equals("choose"))for(AbstractInsnNode i:m.instructions){
            if(i instanceof FieldInsnNode && i.getOpcode()==Opcodes.PUTFIELD){
                FieldInsnNode f=(FieldInsnNode)i;
                check(!Set.of("wait","battleTurn","willUseItemInStack","attack","takenTurn").contains(f.name),"Rejected bag choice mutates turn state: "+f.name);
            }
            if(i instanceof MethodInsnNode){
                MethodInsnNode call=(MethodInsnNode)i;
                check(!Set.of("setUseItem","useItem","useFromBag","consumeItem","getMove","selectAction").contains(call.name),"Rejected choice may commit/consume/reset a turn");
                menu|=call.owner.endsWith("BackToMainMenuPacket") && call.name.equals("<init>");
            }
        }
        check(menu,"Rejected choice must restore the native picking menu");
        System.out.println("PASS native cancellable bag-selection returns before action commit; gym rejection leaves turn/item state untouched and restores picking menu");
    }
    public static void main(String[] args)throws Exception{
        capResources();
        bagSelection();
        JsonObject catalogue=new JsonParser().parse(Files.readString(Path.of("docs/NATIVE_LANGUAGE_CATALOGUE_1.3.0.json"))).getAsJsonObject();
        JsonObject labels=resource("assets/pixelmon/lang/zh_cn.json");Map<String,String> declared=new HashMap<>();
        for(int generation=1;generation<=9;generation++)for(boolean record:new boolean[]{false,true}){
            if(record && generation!=8)continue;
            String type="Gen"+generation+"Technical"+(record?"Records":"Machines"),owner="com/pixelmonmod/pixelmon/enums/technicalmoves/"+type;
            for(MethodNode m:node(owner).methods)if(m.name.equals("<clinit>"))for(AbstractInsnNode i:m.instructions){
                if(!(i instanceof MethodInsnNode))continue;MethodInsnNode call=(MethodInsnNode)i;
                if(!call.owner.equals(owner) || !call.name.equals("<init>"))continue;
                AbstractInsnNode attack=previous(i),id=previous(attack);
                check(attack instanceof LdcInsnNode && ((LdcInsnNode)attack).cst instanceof String,"Native move name missing");
                String key=(record?"tr":"tm")+"_gen"+generation+":"+integer(id);
                check(declared.put(key,(String)((LdcInsnNode)attack).cst)==null,"Duplicate native TM id "+key);
            }
        }
        check(declared.size()==catalogue.getAsJsonArray("machines").size(),"Catalogue must cover every native generation's TM/TR");
        for(JsonElement value:catalogue.getAsJsonArray("machines")){
            JsonObject row=value.getAsJsonObject();String prefix=row.get("prefix").getAsString(),id=row.get("id").getAsString();
            check(row.get("attack").getAsString().equals(declared.get(prefix+":"+id)),"Incorrect native move mapping "+row);
            String key="item.pixelmon."+prefix+"."+id;
            check(labels.has(key) && labels.get(key).getAsString().contains((prefix.startsWith("tr")?"TR":"TM")),"Missing technical move translation "+key);
        }
        for(JsonElement id:catalogue.getAsJsonArray("structures"))check(labels.has("structure.pixelmon."+id.getAsString()),"Untranslated structure "+id);
        JsonObject help=resource("backport/item_help.json"),gyms=resource("backport/gym_cities.json");
        for(var entry:gyms.getAsJsonObject("nativeBadges").entrySet())check(help.getAsJsonArray("pixelmon:"+entry.getValue().getAsString()).size()>=3,"Native badge source missing");
        for(String type:gyms.getAsJsonObject("newThemes").keySet())check(help.getAsJsonArray("pixelmon_backport:"+type+"_gym_badge").size()>=3,"New badge biome missing");
        ClassNode capture=node("local/pixelmon/backport/mixin/DrownedCaptureMixin");
        for(MethodNode m:capture.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode && ((MethodInsnNode)i).name.equals("setCanceled"))check(previous(i).getOpcode()==Opcodes.ICONST_1,"Dusk exception must not undo another event cancellation");
        boolean nativeNight=false;
        for(MethodNode m:node("com/pixelmonmod/pixelmon/api/pokemon/item/pokeball/logic/DuskBall").methods)if(m.name.equals("getCatchBonus"))for(AbstractInsnNode i:m.instructions)if(i instanceof LdcInsnNode)nativeNight|=Double.valueOf(3).equals(((LdcInsnNode)i).cst);
        check(nativeNight,"Supported native dusk night bonus changed; review interception");
        ClassNode item=node("local/pixelmon/backport/mixin/TechnicalMovesMixin");boolean resolved=false,attackKey=false;
        for(MethodNode m:item.methods)if(m.name.equals("func_200295_i"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){String n=((MethodInsnNode)i).name;resolved|=n.equals("getMove");attackKey|=n.equals("getTranslationKey");check(!((MethodInsnNode)i).owner.startsWith("net/minecraft/client/"),"TM label must be server-safe");}
        check(resolved && attackKey,"TM name must follow actual NBT-resolved attack");
        JsonObject achievements=resource("backport/advancements.json");
        check(achievements.getAsJsonArray("transformations").size()>=3,"Primal forms absent");
        for(String id:new String[]{"form_382_primal","form_383_primal"}){
            JsonObject a=resource("data/pixelmon_backport/advancements/legends/"+id+".json");
            check(a.getAsJsonObject("display").get("frame").getAsString().equals("task"),"Form achievement must be normal");
            check(a.getAsJsonObject("display").getAsJsonObject("icon").get("nbt").getAsString().contains("primal"),"Primal avatar absent");
        }
        Set<String> requestedForms=Set.of("form_492_sky","form_483_origin","form_484_origin","form_487_origin","form_641_therian","form_642_therian","form_645_therian","form_905_therian","form_720_unbound","form_888_crowned","form_889_crowned");
        Set<String> foundForms=new HashSet<>();
        for(JsonElement value:achievements.getAsJsonArray("transformations")){
            JsonObject row=value.getAsJsonObject();String id=row.get("id").getAsString();
            if(!requestedForms.contains(id))continue;
            check(foundForms.add(id),"Duplicate form advancement "+id);
            check(row.get("source").getAsString().equals("form-change"),"Form must observe actual completion");
            JsonObject a=resource("data/pixelmon_backport/advancements/legends/"+id+".json");
            check(a.getAsJsonObject("display").get("frame").getAsString().equals("task"),"Special form must be a normal achievement");
            check(a.getAsJsonObject("display").getAsJsonObject("icon").get("nbt").getAsString().contains("form:\""+row.get("form").getAsString()+"\""),"Incorrect special form avatar "+id);
        }
        check(foundForms.equals(requestedForms),"Requested Sky/Origin/Therian forms absent");
        Set<String> guards=new HashSet<>();
        for(MethodNode m:node("local/pixelmon/backport/LegendAdvancements").methods)if(m.name.equals("formChanged"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode)guards.add(((MethodInsnNode)i).name);
        check(guards.containsAll(Set.of("func_213162_bc","getStorage","getPosition","getOwnerPlayerUUID")),"Form awards must use server thread and canonical owned storage");
        JsonObject tweaks=resource("assets/pixeltweaks/lang/zh_cn.json");check(tweaks.size()==302,"Incomplete PixelTweaks language coverage");
        for(var entry:tweaks.entrySet())check(entry.getValue().getAsString().codePoints().anyMatch(c->c>=0x4e00 && c<=0x9fff),"Untranslated PixelTweaks label "+entry.getKey());
        check(resource("data/pixelmon_backport/advancements/legends/capture_494.json").getAsJsonObject("display").getAsJsonObject("title").get("text").getAsString().equals("满天星斗"),"Victini title not updated");
        JsonObject ownLabels=resource("assets/pixelmon_backport/lang/zh_cn.json");
        check(ownLabels.get("pixelmon_backport.field_mega.no_stone").getAsString().equals("未携带 Mega 石"),"Missing-stone feedback must be explicit");
        JsonObject byForm=achievements.getAsJsonObject("capturesByForm");
        for(String[] pair:new String[][]{{"718","ten_percent"},{"718","fifty_percent"},{"892","singlestrike"},{"892","rapidstrike"}}){
            JsonObject row=byForm.getAsJsonObject(pair[0]).getAsJsonObject(pair[1]);
            JsonObject a=resource("data/pixelmon_backport/advancements/legends/"+row.get("id").getAsString()+".json");
            check(a.getAsJsonObject("display").getAsJsonObject("icon").get("nbt").getAsString().contains("form:\""+pair[1]+"\""),"Wrong acquisition form icon "+Arrays.toString(pair));
        }
        for(String dex:new String[]{"718","892"})check(byForm.getAsJsonObject(dex).size()==2,"Acquisition forms must be recorded separately");
        for(String dex:new String[]{"144","145","146"}){
            JsonObject bird=byForm.getAsJsonObject(dex);
            check(bird.size()==1 && bird.has("galarian") && !achievements.getAsJsonObject("captures").has(dex),"Galarian birds must exclude native regular forms");
            JsonObject a=resource("data/pixelmon_backport/advancements/legends/"+bird.getAsJsonObject("galarian").get("id").getAsString()+".json");
            check(a.getAsJsonObject("display").getAsJsonObject("icon").get("nbt").getAsString().contains("form:\"galarian\""),"Galarian bird icon missing");
        }
        int hidden=0;
        try(var paths=Files.list(Path.of("resources/data/pixelmon_backport/advancements/legends"))){
            for(Path path:(Iterable<Path>)paths.filter(p->p.toString().endsWith(".json"))::iterator){
                JsonObject display=resource("data/pixelmon_backport/advancements/legends/"+path.getFileName()).getAsJsonObject("display");
                check(display.get("hidden").getAsBoolean(),"Unfinished legend would be exposed: "+path);
                check(display.get("show_toast").getAsBoolean() && display.get("announce_to_chat").getAsBoolean(),"Completion feedback disabled: "+path);
                hidden++;
            }
        }
        check(hidden>=141,"Legend coverage incomplete");
        JsonObject names=resource("backport/species_names_zh.json");
        for(String name:new String[]{"pixelmon.charizard","pixelmon.zygarde","pixelmon.urshifu"})check(names.has(name),"Partial language overlay must not hide native map labels "+name);
        System.out.println("PASS 1.3 static checks: "+declared.size()+" native TM/TR declarations, "+catalogue.getAsJsonArray("structures").size()+" translated structures, 18 biome badge descriptions, dusk restriction, server-safe names and primal resources");
    }
}
