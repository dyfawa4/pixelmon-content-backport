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
/** Actual capture/acquisition and completed native transformations count. */
public final class LegendAdvancements {
    public static final JsonObject DATA=Content.read("backport/advancements.json");
    private final List<Pending> pending=new ArrayList<>();
    private final List<com.pixelmonmod.pixelmon.api.events.legendary.ZygardeEvent.Assemble> assembled=new ArrayList<>();
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
        if(!AcquisitionAdvancements.ownedStored(player,pokemon))return;
        int dex=pokemon.getSpecies().getDex();
        JsonObject row=DATA.getAsJsonObject("captures").getAsJsonObject(Integer.toString(dex));
        if(DATA.has("capturesByForm") && DATA.getAsJsonObject("capturesByForm").has(Integer.toString(dex))){
            row=DATA.getAsJsonObject("capturesByForm").getAsJsonObject(Integer.toString(dex)).getAsJsonObject(pokemon.getForm().getName());
        }
        if(row==null)return;
        award(player,"root");award(player,row.get("category").getAsString());award(player,row.get("id").getAsString());
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void captured(CaptureEvent.SuccessfulCapture event){AcquisitionQueue.stored(event.getPokemon().getPokemon());}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void form(ItemFormChangeEvent event){
        if(!event.isCanceled() && event.player!=null && event.fusion!=null && !event.fusion.isEgg())pending.add(new Pending(event));
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public void zygardeNew(com.pixelmonmod.pixelmon.api.events.legendary.ZygardeEvent.Assemble.New.End event){assembled.add(event);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public void zygardeMerge(com.pixelmonmod.pixelmon.api.events.legendary.ZygardeEvent.Assemble.Merge.End event){assembled.add(event);}
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
        for(var completed:assembled){
            if(completed.zygarde==null)continue;
            Pokemon p=completed.zygarde;var storage=p.getStorage();
            if(completed.isCanceled() || completed.cells<=0 || completed.player==null || storage==null || p.getPosition()==null
                || storage.get(p.getPosition())!=p || !completed.player.func_110124_au().equals(p.getOwnerPlayerUUID()))continue;
            capture(completed.player,p);
        }
        assembled.clear();
    }
    public static void eternamax(ServerPlayerEntity player){
        for(Pokemon p:com.pixelmonmod.pixelmon.api.storage.StorageProxy.getParty(player).getAll())if(p!=null && p.getSpecies().getDex()==890
            && player.func_110124_au().equals(p.getOwnerPlayerUUID()) && p.getForm().isForm("eternamax") && p.getHeldItemAsItemHeld() instanceof ReferenceItems.Meteorite){
            award(player,"root");award(player,"gen8");award(player,"form_890_eternamax");
        }
    }
    public static void primal(com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper wrapper){
        if(wrapper.bc==null || wrapper.bc.simulateMode || !wrapper.getForm().isForm("primal")
            || !(wrapper.getParticipant() instanceof com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant))return;
        ServerPlayerEntity player=((com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant)wrapper.getParticipant()).player;
        if(player==null || !player.func_110124_au().equals(wrapper.pokemon.getOwnerPlayerUUID()))return;
        int dex=wrapper.getSpecies().getDex();if(dex!=382 && dex!=383)return;
        award(player,"root");award(player,"primal");award(player,"form_"+dex+"_primal");
    }
    public static void formChanged(Pokemon pokemon,String before){
        // A client copy can resolve the integrated-server player by UUID. The
        // server thread and the canonical stored object are both required.
        var server=net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();
        if(server==null || !server.func_213162_bc() || pokemon==null || pokemon.isEgg()
            || pokemon.getForm()==null || pokemon.getForm().getName().equals(before))return;
        var storage=pokemon.getStorage();
        if(storage==null || pokemon.getPosition()==null || storage.get(pokemon.getPosition())!=pokemon)return;
        ServerPlayerEntity player=pokemon.getOwnerPlayer();
        if(player==null || player.field_70170_p.field_72995_K || !player.func_110124_au().equals(pokemon.getOwnerPlayerUUID())
            || !AcquisitionAdvancements.ownedStored(player,pokemon))return;
        // The native cube changes Power Construct Zygarde directly and does
        // not emit a capture or assembly event. Award the completed base form.
        if(pokemon.getSpecies().getDex()==718){capture(player,pokemon);return;}
        for(JsonElement value:DATA.getAsJsonArray("transformations")){
            JsonObject row=value.getAsJsonObject();
            if(!row.has("source") || !row.get("source").getAsString().equals("form-change"))continue;
            if(row.get("dex").getAsInt()!=pokemon.getSpecies().getDex() || !pokemon.getForm().isForm(row.get("form").getAsString()))continue;
            award(player,"root");award(player,row.get("category").getAsString());award(player,row.get("id").getAsString());
        }
    }
    public static void fieldPrimal(Pokemon pokemon){
        var server=net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();
        if(server==null || !server.func_213162_bc() || !FieldMega.active(pokemon) || !FieldMega.primalSpecies(pokemon) || !pokemon.getForm().isForm("primal"))return;
        var storage=pokemon.getStorage();
        if(storage==null || pokemon.getPosition()==null || storage.get(pokemon.getPosition())!=pokemon)return;
        ServerPlayerEntity player=pokemon.getOwnerPlayer();
        if(player==null || !player.func_110124_au().equals(pokemon.getOwnerPlayerUUID()))return;
        award(player,"root");award(player,"primal");award(player,"form_"+pokemon.getSpecies().getDex()+"_primal");
    }
    @SubscribeEvent public void playerTick(TickEvent.PlayerTickEvent event){if(event.phase==TickEvent.Phase.END && event.player instanceof ServerPlayerEntity && event.player.field_70173_aa%20==0)eternamax((ServerPlayerEntity)event.player);}
    private static int partnerFor(int dex,String form){
        if(dex==646)return form.equals("black")?644:643;
        if(dex==800)return form.equals("dusk")?791:792;
        if(dex==898)return form.equals("icerider")?896:897;
        return -1;
    }
}
