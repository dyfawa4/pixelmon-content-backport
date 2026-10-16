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
        if(dex==658 && pokemon.getAbility() instanceof com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.BattleBond){
            award(player,"root");award(player,"gen6");award(player,"capture_658_bond");
        }
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
        CrownedAdvancements.tick();
        for(Pending p:pending){
            fusionCompleted(p.player,p.pokemon,p.before,p.partner,p.event.isCanceled());
        }
        pending.clear();
        for(var completed:assembled){
            if(completed.zygarde==null)continue;
            Pokemon p=completed.zygarde;
            if(completed.isCanceled() || completed.cells<=0 || !AcquisitionAdvancements.ownedStored(completed.player,p))continue;
            capture(completed.player,p);
        }
        assembled.clear();
    }
    public static void fusionCompleted(ServerPlayerEntity player,Pokemon pokemon,String before,int partner,boolean canceled){
        if(canceled || !AcquisitionAdvancements.ownedStored(player,pokemon) || pokemon.getForm().getName().equals(before)
            || pokemon.getPersistentData().func_74775_l("FusedPokemon").isEmpty())return;
        String after=pokemon.getForm().getName();
        for(JsonElement value:DATA.getAsJsonArray("fusions")){
            JsonObject row=value.getAsJsonObject();
            if(row.get("dex").getAsInt()==pokemon.getSpecies().getDex() && row.get("form").getAsString().equals(after)
                && partnerFor(pokemon.getSpecies().getDex(),after)==partner){
                award(player,"root");award(player,"fusion");award(player,row.get("id").getAsString());
            }
        }
    }
    @SubscribeEvent public void stopped(net.minecraftforge.fml.event.server.FMLServerStoppedEvent event){pending.clear();assembled.clear();CrownedAdvancements.clear();}
    public static void eternamax(ServerPlayerEntity player){
        for(Pokemon p:com.pixelmonmod.pixelmon.api.storage.StorageProxy.getParty(player).getAll())if(p!=null && p.getSpecies().getDex()==890
            && player.func_110124_au().equals(p.getOwnerPlayerUUID()) && p.getForm().isForm("eternamax") && p.getHeldItemAsItemHeld() instanceof ReferenceItems.Meteorite){
            award(player,"root");award(player,"gen8");award(player,"form_890_eternamax");
        }
    }
    public static void primal(com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper wrapper){
        if(wrapper.bc==null || wrapper.bc.simulateMode || wrapper.bc.battleEnded || !wrapper.getForm().isForm("primal")
            || !(wrapper.getParticipant() instanceof com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant))return;
        ServerPlayerEntity player=((com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant)wrapper.getParticipant()).player;
        if(!wrapper.onBattlefield || !wrapper.isAlive() || !wrapper.getParticipant().controlledPokemon.contains(wrapper)
            || !wrapper.bc.participants.contains(wrapper.getParticipant()) || !AcquisitionAdvancements.ownedStored(player,wrapper.pokemon))return;
        int dex=wrapper.getSpecies().getDex();if(dex!=382 && dex!=383)return;
        award(player,"root");award(player,"primal");award(player,"form_"+dex+"_primal");
    }
    /** Called only after native battle Mega evolution returns success. */
    public static void megaRayquaza(com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper wrapper){
        if(wrapper.bc==null || wrapper.bc.simulateMode || wrapper.bc.battleEnded
            || wrapper.getSpecies().getDex()!=384 || !wrapper.getForm().isForm("mega")
            || !(wrapper.getParticipant() instanceof com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant))return;
        ServerPlayerEntity player=((com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant)wrapper.getParticipant()).player;
        if(!wrapper.onBattlefield || !wrapper.isAlive() || !wrapper.getParticipant().controlledPokemon.contains(wrapper)
            || !wrapper.bc.participants.contains(wrapper.getParticipant()) || !AcquisitionAdvancements.ownedStored(player,wrapper.pokemon))return;
        award(player,"root");award(player,"forms");award(player,"form_384_mega");
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
        if(pokemon.getSpecies().getDex()==888 || pokemon.getSpecies().getDex()==889){CrownedAdvancements.field(pokemon);return;}
        // The field marker is installed after permission checks and event
        // cancellation, before the native animation actually changes the form.
        // Bare setForm, creation, loading and a received Mega do not count.
        if(pokemon.getSpecies().getDex()==384 && FieldMega.active(pokemon) && pokemon.getForm().isForm("mega")
            && pokemon.getPersistentData().func_74775_l(FieldMega.MARKER).func_74779_i("Target").equals("mega")){
            award(player,"root");award(player,"forms");award(player,"form_384_mega");
        }
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
        if(!AcquisitionAdvancements.ownedStored(player,pokemon))return;
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
