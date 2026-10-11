package local.pixelmon.backport;
import com.google.gson.*;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.events.CaptureEvent;
import com.pixelmonmod.pixelmon.api.events.pokemon.ItemFormChangeEvent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.event.TickEvent;
/** Only successful capture events and completed native item-fusion operations count. */
public final class LegendAdvancements {
    public static final JsonObject DATA=Content.read("backport/advancements.json");
    private final List<Pending> pending=new ArrayList<>();
    private static final class Pending {
        final ServerPlayerEntity player;final Pokemon pokemon;final String before;final int partner;final ItemFormChangeEvent event;
        Pending(ItemFormChangeEvent event){this.event=event;player=event.player;pokemon=event.pokemon.getPokemon();before=pokemon.getForm().getName();partner=event.fusion.getSpecies().getDex();}
    }
    public static void award(ServerPlayerEntity player,String name){
        if(player==null || player.field_70170_p.field_72995_K)return;
        var advancement=player.func_184102_h().func_191949_aK().func_192778_a(new ResourceLocation(Content.NS,"legends/"+name));
        if(advancement!=null)player.func_192039_O().func_192750_a(advancement,"earned");
    }
    public static void capture(ServerPlayerEntity player,Pokemon pokemon){
        if(pokemon==null || pokemon.isEgg())return;
        JsonObject row=DATA.getAsJsonObject("captures").getAsJsonObject(Integer.toString(pokemon.getSpecies().getDex()));
        if(row==null)return;
        award(player,"root");award(player,row.get("category").getAsString());award(player,row.get("id").getAsString());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void captured(CaptureEvent.SuccessfulCapture event){capture(event.getPlayer(),event.getPokemon().getPokemon());}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void raid(CaptureEvent.SuccessfulRaidCapture event){capture(event.getPlayer(),event.getRaidPokemon());}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void form(ItemFormChangeEvent event){
        if(!event.isCanceled() && event.player!=null && event.fusion!=null && !event.fusion.isEgg())pending.add(new Pending(event));
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;
        for(Pending p:pending){
            String after=p.pokemon.getForm().getName();
            if(p.event.isCanceled() || after.equals(p.before) || !p.player.func_110124_au().equals(p.pokemon.getOwnerPlayerUUID())
                || p.pokemon.getPersistentData().func_74775_l("FusedPokemon").isEmpty())continue;
            for(JsonElement value:DATA.getAsJsonArray("fusions")){
                JsonObject row=value.getAsJsonObject();
                if(row.get("dex").getAsInt()==p.pokemon.getSpecies().getDex() && row.get("form").getAsString().equals(after)
                    && partnerFor(p.pokemon.getSpecies().getDex(),after)==p.partner){
                    award(p.player,"root");award(p.player,"fusion");award(p.player,row.get("id").getAsString());
                }
            }
        }
        pending.clear();
    }
    public static void eternamax(ServerPlayerEntity player){
        for(Pokemon p:com.pixelmonmod.pixelmon.api.storage.StorageProxy.getParty(player).getAll())if(p!=null && p.getSpecies().getDex()==890
            && player.func_110124_au().equals(p.getOwnerPlayerUUID()) && p.getForm().isForm("eternamax") && p.getHeldItemAsItemHeld() instanceof ReferenceItems.Meteorite){
            award(player,"root");award(player,"gen8");award(player,"form_890_eternamax");
        }
    }
    @SubscribeEvent public void playerTick(TickEvent.PlayerTickEvent event){if(event.phase==TickEvent.Phase.END && event.player instanceof ServerPlayerEntity && event.player.field_70173_aa%20==0)eternamax((ServerPlayerEntity)event.player);}
    private static int partnerFor(int dex,String form){
        if(dex==646)return form.equals("black")?644:643;
        if(dex==800)return form.equals("dusk")?791:792;
        if(dex==898)return form.equals("icerider")?896:897;
        return -1;
    }
}
