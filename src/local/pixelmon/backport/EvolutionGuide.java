package local.pixelmon.backport;

import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.api.pokemon.PokemonSpecification;
import com.pixelmonmod.api.pokemon.requirement.impl.*;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.species.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.evolution.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.evolution.types.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.evolution.conditions.*;
import net.minecraft.item.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/** Read-only snapshots of the server's effective native evolution objects. */
public final class EvolutionGuide {
    private static final Gson JSON=new Gson();
    private static final JsonObject FORM_ROUTES=Content.read("backport/evolution_form_routes.json");
    private EvolutionGuide(){}
    public static JsonObject term(String type){var v=new JsonObject();v.addProperty("evoConditionType",type);return v;}
    public static JsonObject text(String key){var v=term("text");v.addProperty("key",key);return v;}
    public static JsonObject item(String id,boolean held){var v=term(held?"heldItem":"useItem");var item=new JsonObject();item.addProperty("itemID",id);v.add("item",item);return v;}
    public static JsonObject condition(EvoCondition value){
        if(value instanceof InvertCondition){var v=term("invert");var child=((InvertCondition)value).getCondition();if(child!=null)v.add("condition",condition(child));return v;}
        if(value instanceof PartyCondition){var c=(PartyCondition)value;var v=term("party");var specs=new JsonArray();if(c.withPokemon!=null)for(var p:c.withPokemon)specs.add(target(p));v.add("withPokemon",specs);v.add("withTypes",JSON.toJsonTree(c.withTypes));v.add("withForms",JSON.toJsonTree(c.withForms));v.add("withPalettes",JSON.toJsonTree(c.withPalettes));return v;}
        return JSON.toJsonTree(value).getAsJsonObject();
    }
    public static JsonObject target(PokemonSpecification spec){
        // Specs deserialized before registry reload may retain an empty parsed requirement list.
        // Reparse only the declaration; never create a Pokemon or run the evolution.
        if(!spec.getValue(SpeciesRequirement.class).isPresent())spec=com.pixelmonmod.api.pokemon.PokemonSpecificationProxy.create(spec.toString());
        var value=spec.getValue(SpeciesRequirement.class).orElse(null);
        if(value==null)throw new IllegalStateException("Evolution target has no species: "+spec);
        Species species=value.getValueUnsafe();String form=spec.getValue(FormRequirement.class).orElse(species.getFirstForm().getName());
        var result=new JsonObject();result.addProperty("dex",species.getDex());result.addProperty("form",form);result.addProperty("spec",spec.toString());if(!spec.getValue(FormRequirement.class).isPresent()&&species.getDefaultFormNames().size()>1)result.addProperty("formUnspecified",true);return result;
    }
    static JsonObject identity(Species species,Stats form){var v=new JsonObject();v.addProperty("dex",species.getDex());v.addProperty("form",form.getName());return v;}
    static int order(JsonObject v){String type=v.get("evoConditionType").getAsString();return type.equals("heldItem")||type.equals("useItem")?0:type.equals("level")?1:2;}
    static void row(JsonArray rows,String group,JsonObject from,JsonObject to,List<JsonObject> terms){
        terms.sort(Comparator.comparingInt(EvolutionGuide::order));var r=new JsonObject();r.addProperty("group",group);r.add("from",from);r.add("to",to);var c=new JsonArray();terms.forEach(c::add);r.add("conditions",c);rows.add(r);
    }
    public static JsonObject build(Pokemon pokemon){
        var result=new JsonObject();result.addProperty("pokemon",pokemon.getUUID().toString());result.addProperty("egg",pokemon.isEgg());var rows=new JsonArray();result.add("routes",rows);
        if(pokemon.isEgg()){result.addProperty("final",false);return result;}
        var species=pokemon.getSpecies();var form=pokemon.getForm();var from=identity(species,form);int normal=0;var earlierLevels=new JsonArray();
        for(var evo:form.getEvolutions()){
            var terms=new ArrayList<JsonObject>();
            if(evo instanceof InteractEvolution){var e=(InteractEvolution)evo;if(e.item!=null){var t=term("useItem");t.add("item",JSON.toJsonTree(e.item));terms.add(t);}else if(Boolean.TRUE.equals(e.emptyHand))terms.add(text("empty_hand"));}
            if(evo instanceof LevelingEvolution){var e=(LevelingEvolution)evo;var t=term("level");t.addProperty("level",e.level==null?0:e.level);t.addProperty("upgrade",true);terms.add(t);}
            if(evo instanceof TradeEvolution){var e=(TradeEvolution)evo;var t=term("trade");if(e.with!=null)t.add("with",target(e.with));terms.add(t);}
            if(evo.conditions!=null)for(var c:evo.conditions)terms.add(condition(c));
            if(evo instanceof TickingEvolution)terms.add(text("automatic"));
            var destination=target(evo.to);
            // PixelmonEntity.testLevelEvolution returns the first matching route. Later branches
            // also require earlier branches not to have triggered (e.g. Eevee and Tyrogue).
            if(evo instanceof LevelingEvolution){
                var baseTerms=new JsonArray();terms.forEach(baseTerms::add);
                if(earlierLevels.size()>0){var priority=term("priority");priority.add("earlier",new JsonParser().parse(earlierLevels.toString()));terms.add(priority);}
                var previous=new JsonObject();previous.add("to",destination);previous.add("conditions",baseTerms);earlierLevels.add(previous);
            }
            row(rows,"evolution",from,destination,terms);normal++;
            // Native EvolutionQuery.checkShedinja is a bonus result, not a Stats evolution entry.
            if(species.getDex()==290&&destination.get("dex").getAsInt()==291){
                var extra=new ArrayList<JsonObject>(terms);extra.add(text("shedinja"));
                var shedinja=com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies.SHEDINJA.getValueUnsafe();
                row(rows,"bonus",from,identity(shedinja,shedinja.getFirstForm()),extra);
            }
        }
        result.addProperty("final",normal==0);
        if(!form.getName().startsWith("mega")){
            for(var stone:form.getMegaItems()){
                var mega=form.getMegaForm(stone);if(!mega.isPresent()||mega.get()==form)continue;
                var terms=new ArrayList<JsonObject>();
                if(stone!=Items.field_190931_a)terms.add(item(stone.getRegistryName().toString(),true));
                else if(species.getDex()==384){var t=term("move");t.addProperty("attackName","Dragon Ascent");terms.add(t);}
                terms.add(text("mega"));row(rows,mega.get().getName().startsWith("primal")?"form":"mega",from,identity(species,mega.get()),terms);
            }
            if(species.getDex()==384){var mega=species.getForms().stream().filter(f->f.getName().equals("mega")).findFirst();if(mega.isPresent()){
                var learned=term("move");learned.addProperty("attackName","Dragon Ascent");row(rows,"mega",from,identity(species,mega.get()),new ArrayList<>(Arrays.asList(learned,text("mega"))));
            }}
        }
        if(species.getDex()==658&&PermanentAsh.isAsh(form)&&!form.getName().startsWith("mega")){
            var mega=species.getForm(PermanentAsh.MEGA);if(mega!=null)row(rows,"mega",from,identity(species,mega),new ArrayList<>(Arrays.asList(item(Content.NS+":ash_greninjite",true),text("form_field"))));
        }
        if(species.getDex()==479)for(var entry:com.pixelmonmod.pixelmon.listener.RotomListener.getBlockImpactMap().entrySet()){
            var target=species.getForm(entry.getValue());if(target!=null&&target!=form)row(rows,"form",from,identity(species,target),new ArrayList<>(Arrays.asList(item(entry.getKey().getRegistryName().toString(),false),text("form_block"))));
        }
        formRoutes(species,form,rows);
        return result;
    }
    static void formRoutes(Species species,Stats current,JsonArray rows){
        var data=FORM_ROUTES;String key=String.valueOf(species.getDex());
        if(!data.has(key))return;
        for(var value:data.getAsJsonArray(key)){
            var v=value.getAsJsonObject();String destination=v.get("to").getAsString();
            if(destination.equals(current.getName()))continue;
            if(v.has("from")){boolean allowed=false;for(var origin:v.getAsJsonArray("from"))if(origin.getAsString().equals(current.getName()))allowed=true;if(!allowed)continue;}
            Stats target=species.getForms().stream().filter(f->f.getName().equals(destination)).findFirst().orElse(null);if(target==null)continue;
            var terms=new ArrayList<JsonObject>();
            if(v.has("item")){String id=v.get("item").getAsString();var item=ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));if(item==null||item==Items.field_190931_a)continue;terms.add(item(id,v.has("held")&&v.get("held").getAsBoolean()));}
            if(v.has("conditions"))for(var c:v.getAsJsonArray("conditions"))terms.add(new JsonParser().parse(c.toString()).getAsJsonObject());
            row(rows,"form",identity(species,current),identity(species,target),terms);
        }
    }
}
