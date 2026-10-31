package local.pixelmon.backport.client;
import java.util.*;import java.util.stream.Collectors;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.pokemon.species.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import net.minecraft.item.*;import net.minecraft.util.text.*;
/** Localized presentation of server snapshots. No eligibility checks or changes to live Pokemon. */
public final class EvolutionGuideText {
    public static String tr(String key,Object... args){return new TranslationTextComponent("pixelmon_backport.evolution."+key,args).getString();}
    public static String string(JsonObject o,String key,String fallback){return o.has(key)&&!o.get(key).isJsonNull()?o.get(key).getAsString():fallback;}
    static int number(JsonObject o,String key,int fallback){try{return o.get(key).getAsInt();}catch(Exception e){return fallback;}}
    static List<String> strings(JsonObject o,String key){var out=new ArrayList<String>();if(o.has(key)&&o.get(key).isJsonArray())for(var v:o.getAsJsonArray(key))out.add(v.getAsString());return out;}
    public static Species species(JsonObject id){return PixelmonSpecies.fromNationalDex(id.get("dex").getAsInt());}
    public static Stats form(JsonObject id){var s=species(id);String name=string(id,"form",s.getFirstForm().getName());return s.getForms().stream().filter(f->f.getName().equals(name)).findFirst().orElse(s.getFirstForm());}
    public static String name(JsonObject id){var s=species(id);var f=form(id);if(id.has("formUnspecified")&&id.get("formUnspecified").getAsBoolean())return s.getLocalizedName();if(s.getDex()==892){boolean single=f.getName().equals("singlestrike")||f.getName().equals("gmaxss");String style=tr(single?"single_strike":"rapid_strike");return s.getLocalizedName()+"（"+style+(f.getName().startsWith("gmax")?"，"+f.getLocalizedName():"")+"）";}return s.getDefaultFormNames().size()==1&&s.isDefaultForm(f)?s.getLocalizedName():s.getLocalizedName()+"（"+f.getLocalizedName()+"）";}
    public static ItemStack item(JsonObject c){try{return new Gson().fromJson(c.get("item"),com.pixelmonmod.pixelmon.api.item.JsonItemStack.class).getItemStack().func_77946_l();}catch(Exception e){return ItemStack.field_190927_a;}}
    static String translated(String prefix,String key){String k=prefix+key.toLowerCase(Locale.ROOT);String v=new TranslationTextComponent(k).getString();return v.equals(k)?key:v;}
    static String enumName(String type,String key){return tr(type+"."+key.toLowerCase(Locale.ROOT));}
    static String move(String name){return com.pixelmonmod.pixelmon.api.battles.attack.AttackRegistry.getAttackBase(name).map(a->a.getLocalizedName()).orElse(name);}
    public static String condition(JsonObject c){
        String type=string(c,"evoConditionType","");
        switch(type){
            case "useItem":return tr("use",itemName(c));
            case "heldItem":return tr("held",itemName(c));
            case "level":{int n=number(c,"level",0);return c.has("upgrade")?n>0?tr("level_up",n):tr("level_any"):tr("level",n);}
            case "trade":return c.has("with")?tr("trade_with",name(c.getAsJsonObject("with"))):tr("trade");
            case "priority":{var earlier=new ArrayList<String>();for(var v:c.getAsJsonArray("earlier"))earlier.add(name(v.getAsJsonObject().getAsJsonObject("to")));return tr("priority",String.join("、",earlier));}
            case "text":return tr(string(c,"key","extra"));
            case "friendship":return tr("friendship",number(c,"friendship",0));
            case "weather":return enumName("weather",string(c,"weather","RAIN"));
            case "time":{var values=strings(c,"times");if(values.isEmpty())values.add(string(c,"time","DAY"));return values.stream().map(v->enumName("time",v)).collect(Collectors.joining(tr("or")));}
            case "move":return tr("move",move(string(c,"attackName","")));
            case "moveType":return tr("move_type",translated("type.",string(c,"type","")));
            case "gender":return tr("gender",strings(c,"genders").stream().map(v->enumName("gender",v)).collect(Collectors.joining(tr("or"))));
            case "nature":return tr("nature",strings(c,"natures").stream().map(v->translated("enum.nature.",v)).collect(Collectors.joining("、")));
            case "shiny":return tr(c.has("shiny")&&c.get("shiny").getAsBoolean()?"shiny":"not_shiny");
            case "chance":return tr("chance",String.format(Locale.ROOT,"%.2f",c.get("chance").getAsDouble()*100).replaceAll("\\.?0+$",""));
            case "statRatio":return tr("ratio",enumName("stat",string(c,"stat1","")),enumName("stat",string(c,"stat2","")),string(c,"ratio","1"));
            case "invert":return c.has("condition")?tr("not",condition(c.getAsJsonObject("condition"))):tr("extra");
            case "blocksWalkedOutsideBall":return tr("walk",number(c,"blocksToWalk",0));
            case "moveUses":return tr("move_uses",move(string(c,"move","")),number(c,"uses",0));
            case "critical":return tr("critical",number(c,"critical",0));
            case "recoil":return tr("recoil",number(c,"recoil",0));
            case "healthAbsence":return tr("health",number(c,"health",0));
            case "highAltitude":return tr("altitude",string(c,"minAltitude","0"));
            case "insideBattle":return tr("battle");
            case "isBurning":return tr("burning");
            case "status":return tr("status",enumName("status",string(c,"type","")));
            case "nuggets":return tr("nuggets",number(c,"nuggets",0));
            case "structure":case "withinStructure":return tr("structure",string(c,"structure",""));
            case "biome":return tr("biome",strings(c,"biomes").stream().map(EvolutionGuideText::biome).collect(Collectors.joining("、")));
            case "evolutionRock":return tr("rock",enumName("rock",string(c,"evolutionRock","")),(int)Math.sqrt(number(c,"maxRangeSquared",0)));
            case "evolutionScroll":return tr("scroll",enumName("scroll",string(c,"evolutionScroll","")),(int)Math.sqrt(number(c,"maxRangeSquared",0)));
            case "moonPhase":return tr("moon",number(c,"moonPhase",0));
            case "potionEffect":case "potions":return tr("potions",String.join("、",strings(c,"potions")));
            case "party":{var terms=new ArrayList<String>();if(c.has("withPokemon")&&c.get("withPokemon").isJsonArray())for(var v:c.getAsJsonArray("withPokemon"))terms.add(name(v.getAsJsonObject()));for(var v:strings(c,"withTypes"))terms.add(translated("type.",v));for(var v:strings(c,"withForms"))terms.add(translated("pixelmon.generic.form.",v)+tr("form_pokemon"));for(var v:strings(c,"withPalettes"))terms.add(v);return tr("party",String.join("、",terms));}
            default:return tr("extra");
        }
    }
    static String itemName(JsonObject c){var stack=item(c);String name=stack.func_200301_q().getString();return stack.func_190916_E()>1?name+"×"+stack.func_190916_E():name;}
    static String biome(String id){String k="biome."+id.replace(':','.');String v=new TranslationTextComponent(k).getString();return v.equals(k)?id:v;}
    public static String formula(JsonObject route){var parts=new ArrayList<String>();parts.add(name(route.getAsJsonObject("from")));for(var c:route.getAsJsonArray("conditions"))parts.add(condition(c.getAsJsonObject()));return String.join(" ＋ ",parts)+" → "+name(route.getAsJsonObject("to"));}
}
