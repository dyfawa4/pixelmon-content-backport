package local.pixelmon.backport;

import com.google.gson.*;
import java.util.Map;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import com.pixelmonmod.pixelmon.api.events.HeldItemChangedEvent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;

/** Cosmetic cap, tied to the real native held crystal, with unchanged base stats. */
public final class CapPikachu {
    public static final String FORM="ash_cap";
    public static void addForm(Map<ResourceLocation,JsonElement> data){
        for(Map.Entry<ResourceLocation,JsonElement> entry:data.entrySet()){
            JsonObject species=entry.getValue().getAsJsonObject();
            if(!species.has("dex") || species.get("dex").getAsInt()!=25)continue;
            JsonArray forms=species.getAsJsonArray("forms");
            for(JsonElement f:forms)if(FORM.equals(f.getAsJsonObject().get("name").getAsString()))return;
            // Clone the actual base data to retain other packs' compatible stat/move changes.
            JsonObject cap=new JsonParser().parse(forms.get(0).toString()).getAsJsonObject();cap.addProperty("name",FORM);cap.addProperty("defaultBaseForm","");
            cap.add("genderProperties",new JsonParser().parse(Content.read("backport/ash_cap.json").get("genderProperties").toString()));
            forms.add(cap);return;
        }
    }
    public static void sync(Pokemon pokemon){sync(pokemon,pokemon.getHeldItem());}
    public static void sync(Pokemon pokemon,ItemStack held){
        if(pokemon.getSpecies().getDex()!=25 || pokemon.isEgg())return;
        String form=pokemon.getForm().getName();
        if(!form.isEmpty() && !"base".equals(form) && !FORM.equals(form))return;
        boolean wear=held.func_77973_b()==PixelmonItems.pikashunium_z;
        String palette=pokemon.getPalette().getName();
        if(wear && !palette.equals("none") && !palette.equals("shiny"))return;
        if(wear && !FORM.equals(form))pokemon.setForm(FORM);
        else if(!wear && FORM.equals(form))pokemon.setForm("");
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void held(HeldItemChangedEvent event){if(!event.isCanceled())sync(event.pokemon,event.newHeldItem);}
}
