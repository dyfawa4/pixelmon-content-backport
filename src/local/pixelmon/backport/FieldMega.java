package local.pixelmon.backport;

import java.util.*;
import java.util.function.Supplier;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy;
import com.pixelmonmod.pixelmon.api.events.MegaEvolutionEvent;
import com.pixelmonmod.pixelmon.api.events.pokemon.ItemFormChangeEvent;
import com.pixelmonmod.pixelmon.api.registries.PixelmonItems;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.helpers.EvolutionQuery;
import com.pixelmonmod.pixelmon.entities.pixelmon.helpers.EvolutionQueryList;
import com.pixelmonmod.pixelmon.items.heldItems.MegaStoneItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.*;
import net.minecraftforge.fml.network.simple.SimpleChannel;

/** Temporary field transformation using the native animation and species rules. */
public final class FieldMega {
    public static final String MARKER="BackportFieldMega";
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(Content.NS,"field_mega"),()->"1","1"::equals,"1"::equals);
    private static final Set<Pokemon> ACTIVE=Collections.newSetFromMap(new IdentityHashMap<>());
    private FieldMega(){}
    public static void register(){
        CHANNEL.registerMessage(0,Request.class,Request::encode,Request::decode,Request::handle,Optional.of(NetworkDirection.PLAY_TO_SERVER));
        Events events=new Events();net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(events);Pixelmon.EVENT_BUS.register(events);
    }
    public static Stats target(Pokemon p){
        return p==null?null:target(p,p.getForm());
    }
    public static boolean primalSpecies(Pokemon p){return p!=null && (p.getSpecies().getDex()==382 || p.getSpecies().getDex()==383);}
    public static boolean crownedSpecies(Pokemon p){return p!=null && (p.getSpecies().getDex()==888 || p.getSpecies().getDex()==889);}
    private static boolean specialSpecies(Pokemon p){return primalSpecies(p)||crownedSpecies(p);}
    public static boolean transformed(Pokemon p){return p!=null && (p.getForm().getName().startsWith("mega") || primalSpecies(p) && p.getForm().isForm("primal") || crownedSpecies(p) && p.getForm().isForm("crowned"));}
    private static Stats target(Pokemon p,Stats base){
        if(p.isEgg() || base==null)return null;
        if(primalSpecies(p)){
            if(base.isForm("primal"))return null;
            return p.getHeldItem().func_77973_b()==(p.getSpecies().getDex()==382?PixelmonItems.blue_orb:PixelmonItems.red_orb)?p.getSpecies().getForm("primal"):null;
        }
        if(crownedSpecies(p)){
            if(base.isForm("crowned"))return null;
            return p.getHeldItem().func_77973_b()==(p.getSpecies().getDex()==888?PixelmonItems.rusted_sword:PixelmonItems.rusted_shield)?p.getSpecies().getForm("crowned"):null;
        }
        if(base.getName().startsWith("mega") || !(p.getHeldItemAsItemHeld() instanceof MegaStoneItem))return null;
        // A form name alone is insufficient: each base form declares its compatible stones.
        return base.getMegaForm(p.getHeldItem().func_77973_b()).filter(f->f!=null).orElse(null);
    }
    public static boolean active(Pokemon p){return p.getPersistentData().func_74764_b(MARKER);}
    private static boolean querying(Pokemon p){synchronized(EvolutionQueryList.queryList){return EvolutionQueryList.queryList.stream().anyMatch(q->q.pokemonUUID.equals(p.getUUID()));}}
    public static String start(ServerPlayerEntity player,UUID id){
        if(BattleRegistry.getBattle(player)!=null)return "battle";
        if(!PixelmonConfigProxy.getExternalMoves().isAllowExternalMoves())return "disabled";
        var party=StorageProxy.getParty(player);
        Pokemon p=Arrays.stream(party.getAll()).filter(x->x!=null && x.getUUID().equals(id)).findFirst().orElse(null);
        if(p==null || !player.func_110124_au().equals(p.getOwnerPlayerUUID()))return "owned";
        if(p.isEgg() || p.getHealth()<=0 || p.hasFlag("unmegaevo"))return "unavailable";
        PixelmonEntity entity=p.getPixelmonEntity().orElse(null);
        if(entity==null || !entity.func_70089_S() || entity.field_70170_p!=player.field_70170_p || entity.getOwner()!=player || entity.func_70068_e(player)>1024)return "sendout";
        if(active(p)){restore(p);return "reverted";}
        if(entity.battleController!=null || querying(p))return "busy";
        if(transformed(p))return "already";
        Stats form=target(p);
        if(form==null){
            if(primalSpecies(p))return p.getSpecies().getDex()==382?"blue_orb":"red_orb";
            if(crownedSpecies(p))return p.getSpecies().getDex()==888?"rusted_sword":"rusted_shield";
            return p.getHeldItemAsItemHeld() instanceof MegaStoneItem?"stone":"no_stone";
        }
        if(!specialSpecies(p) && !party.getMegaItem().canMega())return "bracelet";
        if(specialSpecies(p)?Pixelmon.EVENT_BUS.post(new ItemFormChangeEvent(player,p.getHeldItem(),entity,null)):Pixelmon.EVENT_BUS.post(new MegaEvolutionEvent.MoveSkill(player,p,p.getHeldItem())))return "canceled";
        CompoundNBT state=new CompoundNBT();state.func_74778_a("Form",p.getForm().getName());state.func_74778_a("Target",form.getName());state.func_74768_a("Slot",p.getAbilitySlot());state.func_74757_a("Hidden",p.hasHiddenAbility());state.func_74778_a("Ability",p.getAbilityName());
        p.getPersistentData().func_218657_a(MARKER,state);ACTIVE.add(p);
        new EvolutionQuery(entity,form);
        return primalSpecies(p)?"primal_started":crownedSpecies(p)?"crowned_started":"started";
    }
    public static void restore(Pokemon p){
        if(!active(p))return;
        CompoundNBT state=p.getPersistentData().func_74775_l(MARKER).func_74737_b();
        p.getPersistentData().func_82580_o(MARKER);boolean tracked=ACTIVE.remove(p);
        // A deserialized copy can have the same UUID; it must not cancel the live animation.
        if(tracked)synchronized(EvolutionQueryList.queryList){
            for(Iterator<EvolutionQuery> i=EvolutionQueryList.queryList.iterator();i.hasNext();){EvolutionQuery q=i.next();if(q.pokemonUUID.equals(p.getUUID())){q.abort();i.remove();}}
        }
        int health=p.getHealth();Stats original=p.getSpecies().getForm(state.func_74779_i("Form"));
        if(original==null)original=p.getSpecies().getDefaultForm();
        p.setForm(original);p.setAbilitySlot(state.func_74762_e("Slot"));
        com.pixelmonmod.pixelmon.api.pokemon.ability.AbilityRegistry.getAbility(state.func_74779_i("Ability")).ifPresent(p::setAbility);
        p.setHA(state.func_74767_n("Hidden"));
        p.setHealth(Math.min(health,p.getMaxHealth()));p.getPixelmonEntity().ifPresent(PixelmonEntity::resetAI);
    }
    /** Normalize saved data without changing the live form during autosave. */
    public static void saved(Pokemon p,CompoundNBT nbt){
        if(!active(p))return;
        CompoundNBT state=p.getPersistentData().func_74775_l(MARKER);
        nbt.func_74778_a("Variant",state.func_74779_i("Form"));
        nbt.func_74778_a("Ability",state.func_74779_i("Ability"));
        nbt.func_82580_o("AbilitySlot");nbt.func_74768_a("PAbilitySlot",state.func_74762_e("Slot"));nbt.func_74757_a("PHiddenAbility",state.func_74767_n("Hidden"));
        // PersistentData is shared by native writeToNBT; copy before editing the output.
        CompoundNBT persistent=nbt.func_74775_l("PersistentData").func_74737_b();
        nbt.func_218657_a("PersistentData",persistent);
    }
    public static final class Request {
        final UUID pokemon;public Request(UUID id){pokemon=id;}
        static void encode(Request r,PacketBuffer b){b.func_179252_a(r.pokemon);}
        static Request decode(PacketBuffer b){return new Request(b.func_179253_g());}
        static void handle(Request r,Supplier<NetworkEvent.Context> supplied){NetworkEvent.Context c=supplied.get();c.enqueueWork(()->{
            ServerPlayerEntity p=c.getSender();if(p!=null)p.func_146105_b(new TranslationTextComponent("pixelmon_backport.field_mega."+start(p,r.pokemon)),true);
        });c.setPacketHandled(true);}
    }
    public static final class Events {
        @SubscribeEvent public void evolved(MegaEvolutionEvent.Post event){
            Pokemon p=event.getPokemon();
            if(!ACTIVE.contains(p) || !active(p) || !p.getForm().getName().equals(p.getPersistentData().func_74775_l(MARKER).func_74779_i("Target")))return;
            // A base hidden/second slot may not exist in the Mega form. Select its
            // actual ability; the base slot and hidden flag remain in our marker.
            for(var ability:p.getForm().getAbilities().getAbilities())if(ability!=null){p.setAbilitySlot(-1);p.setHA(false);p.setAbility(ability);break;}
            if(primalSpecies(p))LegendAdvancements.fieldPrimal(p);
        }
        @SubscribeEvent public void tick(TickEvent.ServerTickEvent event){if(event.phase!=TickEvent.Phase.END)return;
            for(Pokemon p:new ArrayList<>(ACTIVE)){
                PixelmonEntity e=p.getPixelmonEntity().orElse(null);ServerPlayerEntity owner=p.getOwnerPlayer();
                if(!active(p)){ACTIVE.remove(p);continue;}
                CompoundNBT state=p.getPersistentData().func_74775_l(MARKER);Stats base=p.getSpecies().getForm(state.func_74779_i("Form"));
                Stats compatible=target(p,base);boolean stone=compatible!=null && compatible.getName().equals(state.func_74779_i("Target"));
                if(e==null || !e.func_70089_S() || owner==null || e.getOwner()!=owner || e.field_70170_p!=owner.field_70170_p || p.getHealth()<=0 || !stone || !specialSpecies(p) && !StorageProxy.getParty(owner).getMegaItem().canMega() || (!p.getForm().getName().equals(state.func_74779_i("Target")) && !querying(p)))restore(p);
            }
        }
        @SubscribeEvent public void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getPlayer() instanceof ServerPlayerEntity)for(Pokemon p:StorageProxy.getParty((ServerPlayerEntity)event.getPlayer()).getAll())if(p!=null)restore(p);}
        @SubscribeEvent public void dimension(PlayerEvent.PlayerChangedDimensionEvent event){logoutParty(event);}
        private void logoutParty(PlayerEvent event){if(event.getPlayer() instanceof ServerPlayerEntity)for(Pokemon p:StorageProxy.getParty((ServerPlayerEntity)event.getPlayer()).getAll())if(p!=null)restore(p);}
        @SubscribeEvent public void stopped(net.minecraftforge.fml.event.server.FMLServerStoppedEvent event){ACTIVE.clear();}
    }
}
