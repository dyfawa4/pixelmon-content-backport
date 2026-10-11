package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.HeldItemChangedEvent;
import com.pixelmonmod.pixelmon.api.events.battles.AttackEvent;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.battles.attacks.Effectiveness;
import com.pixelmonmod.pixelmon.enums.heldItems.EnumHeldItems;
import com.pixelmonmod.pixelmon.items.HeldItem;
import com.pixelmonmod.pixelmon.items.heldItems.MegaStoneItem;
import net.minecraft.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import java.util.*;
import com.google.gson.*;

@Mod(Content.NS)
public final class Backport {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Content.NS);
    public static final DeferredRegister<net.minecraft.entity.EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITIES,Content.NS);
    public static final RegistryObject<net.minecraft.entity.EntityType<com.pixelmonmod.pixelmon.entities.DenEntity>> TERA_DEN=ENTITIES.register("tera_den",()->net.minecraft.entity.EntityType.Builder.<com.pixelmonmod.pixelmon.entities.DenEntity>func_220322_a(TeraDenEntity::new,net.minecraft.entity.EntityClassification.MISC).func_220321_a(1.4f,2.1f).setTrackingRange(12).setUpdateInterval(1).func_206830_a(Content.NS+":tera_den"));
    public static final Map<String,RegistryObject<Item>> REGISTERED = new LinkedHashMap<>();
    public static final net.minecraft.item.ItemGroup CREATIVE_TAB=new BackportCreativeTab();
    static {
        for (String name : new String[]{"dragoninite","starminite","chandelurite","darkranite","raichunite_x","raichunite_y"}) {
            String form=name.equals("raichunite_x")?"megax":name.equals("raichunite_y")?"megay":"mega";
            REGISTERED.put(name,ITEMS.register(name,()->new MegaStoneItem(form)));
        }
        for (String name : new String[]{"metal_alloy","unremarkable_teacup","masterpiece_teacup"}) REGISTERED.put(name,ITEMS.register(name,()->new com.pixelmonmod.pixelmon.items.PixelmonItem(new Item.Properties())));
        for (String form : new String[]{"teal","wellspring","hearthflame","cornerstone"}) REGISTERED.put(form+"_mask",ITEMS.register(form+"_mask",()->new Mask(form)));
        for(JsonElement value:Content.DATA.getAsJsonArray("referenceItems")){
            JsonObject entry=value.getAsJsonObject();String name=entry.get("name").getAsString();
            if(REGISTERED.containsKey(name))continue;
            String kind=entry.get("kind").getAsString(),form=entry.get("form").isJsonNull()?"":entry.get("form").getAsString();
            REGISTERED.put(name,ITEMS.register(name,()->ReferenceItems.create(name,kind,form)));
        }
        for(int i=0;i<TeraItems.NAMES.length;i++){final int index=i;String name=TeraItems.NAMES[i]+"_tera_shard";REGISTERED.put(name,ITEMS.register(name,()->new TeraItems.Shard(index)));}
        REGISTERED.put("tera_den_spawner",ITEMS.register("tera_den_spawner",TeraDenItem::new));
        REGISTERED.put("tera_blast_disc_single_use",ITEMS.register("tera_blast_disc_single_use",DisposableTeraDisc::new));
    }
    public Backport() {
        net.minecraftforge.common.crafting.CraftingHelper.register(MasterBallIngredient.ID,MasterBallIngredient.SERIALIZER);
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ENTITIES.register(FMLJavaModLoadingContext.get().getModEventBus());
        FMLJavaModLoadingContext.get().getModEventBus().addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent event)->event.put(TERA_DEN.get(),net.minecraft.entity.LivingEntity.func_233639_cI_().func_233813_a_()));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new TeraRaids.Events());
        Pixelmon.EVENT_BUS.register(new TeraRaids.Events());
        BackportEffects.register();
        TeraNetwork.register();
        Pixelmon.EVENT_BUS.register(new TeraItems.Charging());
        FMLJavaModLoadingContext.get().getModEventBus().addListener((net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent event)->event.enqueueWork(()->{
            for(RegistryObject<Item> entry:REGISTERED.values())if(entry.get() instanceof MegaStoneItem && !com.pixelmonmod.pixelmon.api.registries.PixelmonItems.megaStones.contains(entry.get()))com.pixelmonmod.pixelmon.api.registries.PixelmonItems.megaStones.add(entry.get());
        }));
        Pixelmon.EVENT_BUS.register(this);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void heldItem(HeldItemChangedEvent event) {
        if(!event.isCanceled() && event.getSpecies().getDex()==890){
            event.pokemon.setForm(event.newHeldItem.func_77973_b() instanceof ReferenceItems.Meteorite?"eternamax":"ordinary");
            return;
        }
        if(event.isCanceled() || event.getSpecies().getDex()!=1017 || Tera.active(event.pokemon))return;
        Item item=event.newHeldItem.func_77973_b();
        event.pokemon.setForm(item instanceof Mask ? ((Mask)item).form : "teal");
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void typeEffectiveness(AttackEvent.TypeEffectiveness event) {
        if(event.target.getBattleAbility() instanceof BackportAbilities.TeraShell
            && event.getAttack().getAttackCategory()!=AttackCategory.STATUS
            && !event.getAttack().getActualMove().getAttackName().equals("Struggle")) {
            BackportAbilities.TeraShell shell=(BackportAbilities.TeraShell)event.target.getBattleAbility();
            if(shell.resists(event.user,event.target) && event.getMultiplier()>.5) event.setEffectiveness(Effectiveness.Not);
        }
    }
    public static class Mask extends HeldItem {
        public final String form;
        public Mask(String form){super(EnumHeldItems.other,new Item.Properties().func_200917_a(1));this.form=form;}
        @Override public double preProcessDamagingAttackUser(com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper user,
            com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper target,com.pixelmonmod.pixelmon.battles.attacks.Attack attack,double damage) {
            // The three removable masks boost attack damage for Ogerpon.
            return user.getSpecies().getDex()==1017 && !"teal".equals(form) ? damage*1.2 : damage;
        }
    }
}
