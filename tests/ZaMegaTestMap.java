import java.util.*;
import com.google.gson.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.boss.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.pokemon.ability.AbilityRegistry;
import com.pixelmonmod.pixelmon.items.heldItems.MegaStoneItem;
import com.pixelmonmod.pixelmon.client.ServerStorageDisplay;
import com.pixelmonmod.pixelmon.client.gui.starter.ChooseStarterScreen;
import com.pixelmonmod.pixelmon.spawning.PixelmonSpawning;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.util.FakePlayerFactory;
import com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraft.world.server.ServerWorld;

/** Disposable Mega gallery; native registry/spawn/boss checks run once in this world. */
@Mod("backport_mega_test")
@Mod.EventBusSubscriber(modid="backport_mega_test")
public final class ZaMegaTestMap {
    static final String WORLD="轻快之旅 1.6.0 Mega补全测试";
    static final Set<UUID> READY=new HashSet<>();
    static boolean testWorld(ServerWorld world){return WORLD.equals(world.func_73046_m().func_240793_aU_().func_76065_j());}
    static void protectGallery(PixelmonEntity entity){
        entity.canDespawn=false;
        entity.func_110163_bv();entity.func_94061_f(true);
        // The disposable Boss tier is not a global registry entry. Restore it
        // from the saved exhibit tag when a chunk/save loads the entity.
        if(entity.getPersistentData().func_74767_n("ZaMegaBoss"))entity.setBossTier(new BossTier("qa-mega","Mega",false,1,java.awt.Color.WHITE,1,true,1,1,"",1,0));
    }
    @SubscribeEvent public static void joined(EntityJoinWorldEvent event){
        if(event.getWorld() instanceof ServerWorld&&event.getEntity() instanceof PixelmonEntity&&testWorld((ServerWorld)event.getWorld())&&event.getEntity().getPersistentData().func_74767_n("ZaMegaTest"))protectGallery((PixelmonEntity)event.getEntity());
    }
    static void verifyReload(PixelmonEntity source,ServerWorld world){
        var saved=new net.minecraft.nbt.CompoundNBT();check(source.func_70039_c(saved),"Exhibit serialization rejected");
        var loaded=net.minecraft.entity.EntityType.func_220335_a(saved,world,e->e);check(loaded instanceof PixelmonEntity,"Exhibit entity did not reload");
        var restored=(PixelmonEntity)loaded;joined(new EntityJoinWorldEvent(restored,world));
        check(restored.getPersistentData().func_74767_n("ZaMegaTest")&&!restored.canDespawn,"Despawn protection lost across save/chunk reload");
        check(restored.getPokemon().getForm()==source.getPokemon().getForm(),"Exhibit form lost across reload");
        check(restored.getBossTier().isBoss()==source.getBossTier().isBoss(),"Exhibit Boss / capture identity lost across reload");
        // The round-trip probe is never inserted into the world.
        restored.func_70106_y();
    }
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void prepare(ServerPlayerEntity player){
        var world=player.func_71121_q();var party=StorageProxy.getParty(player);party.starterPicked=true;
        for(int x=-15;x<=155;x++)for(int z=-15;z<=155;z++)world.func_180501_a(new BlockPos(x,64,z),net.minecraft.block.Blocks.field_150348_b.func_176223_P(),2);
        List<PixelmonEntity> oldGallery=new ArrayList<>();for(var entity:world.func_241136_z_())if(entity instanceof PixelmonEntity&&entity.getPersistentData().func_74767_n("ZaMegaTest"))oldGallery.add((PixelmonEntity)entity);
        for(var entity:oldGallery)entity.func_70106_y();
        int count=0;
        for(JsonElement row:Content.read("backport/za-mega-import.json").getAsJsonArray("forms")){
            JsonObject r=row.getAsJsonObject();int dex=r.get("dex").getAsInt();String form=r.get("form").getAsString(),stone=r.get("stone").getAsString();
            var species=PixelmonSpecies.fromNationalDex(dex);var stats=species.getForm(form);check(stats!=null,"Missing registered Mega #"+dex);
            var item=Backport.item(stone);check(item instanceof MegaStoneItem,"Unregistered stone "+stone);
            check(stats.getMegaItems().size()==1&&stats.getMegaItems().contains(item),"Native Boss drops sibling stone #"+dex);
            check(stats.getMegaForm(item).orElseThrow()==stats,"Native Boss reselects a sibling Mega #"+dex);
            for(var ability:stats.getAbilities().getAll())check(ability!=null,"Unresolved Mega ability #"+dex);
            String spawn="Backport Mega "+species.getName()+(form.equals("megaz")?" Z":"");
            check(PixelmonSpawning.megas.stream().anyMatch(s->spawn.equals(s.id)&&!s.spawnInfos.isEmpty()),"Unloaded Mega Boss spawn "+spawn);
            Pokemon p=PokemonFactory.create(species);p.setForm(stats);p.setLevelNum(70);p.initialize();p.resetAbility();p.heal();
            PixelmonEntity entity=p.getOrCreatePixelmon(world,(count%4)*15,65,(count/4)*15);
            BossTier tier=new BossTier("qa-mega","Mega",false,1,java.awt.Color.WHITE,1,true,1,1,"",1,0);entity.setBossTier(tier);
            check(entity.getBossTier().isBoss()&&p.getForm()==stats,"Mega Boss identity/form lost #"+dex);
            boolean dropped=false;
            for(int attempt=0;attempt<128&&!dropped;attempt++)for(DropSet.Drop drop:tier.getDrops(FakePlayerFactory.getMinecraft(world),entity)){
                var found=drop.getDisplayItem().func_77973_b();if(found instanceof MegaStoneItem){check(found==item,"Native Boss dropped wrong stone #"+dex);dropped=true;}
            }
            check(dropped,"Native Boss failed to drop matching stone #"+dex);
            // The sixteen imported groups are checked here; the complete
            // catalogue below owns spawning, so models never overlap.
            entity.func_70106_y();count++;
        }
        int gallery=0,reloadChecks=0;Set<net.minecraft.item.Item> stones=new HashSet<>();
        for(JsonElement row:Content.read("mega-gallery.json").getAsJsonArray("entries")){
            JsonObject r=row.getAsJsonObject();int dex=r.get("dex").getAsInt();String form=r.get("form").getAsString();
            var species=PixelmonSpecies.fromNationalDex(dex);var stats=species.getForm(form);check(stats!=null&&stats.isForm(form),"Gallery form missing #"+dex+"/"+form);
            Pokemon p=PokemonFactory.create(species);p.setForm(stats);p.setLevelNum(70);p.initialize();
            if(r.has("gender"))p.setGender(Gender.valueOf(r.get("gender").getAsString()));
            check(p.setPalette(r.get("palette").getAsString()),"Gallery palette missing #"+dex+"/"+form);
            p.setNickname(r.get("label").getAsString());p.resetAbility();p.heal();
            PixelmonEntity entity=p.getOrCreatePixelmon(world,(gallery%8)*20,65,(gallery/8)*20);
            if(r.get("boss").getAsBoolean()){
                entity.setBossTier(new BossTier("qa-mega","Mega",false,1,java.awt.Color.WHITE,1,true,1,1,"",1,0));
                check(entity.getBossTier().isBoss()&&p.getForm()==stats,"Gallery Boss form lost #"+dex+"/"+form);
                for(var item:stats.getMegaItems())if(stones.add(item))player.field_71071_by.func_70441_a(new net.minecraft.item.ItemStack(item));
            }
            entity.getPersistentData().func_74757_a("ZaMegaTest",true);entity.getPersistentData().func_74757_a("ZaMegaBoss",r.get("boss").getAsBoolean());
            protectGallery(entity);world.func_217376_c(entity);check(!entity.canDespawn,"Gallery native despawn protection lost");
            if(gallery==0||(dex==801&&form.equals("original_color"))){verifyReload(entity,world);reloadChecks++;}gallery++;
        }
        for(String name:new String[]{"MegaSol","Dragonize","AuraFortify","PiercingDrill","Updraft","FlamingMane","SpicySpray"})check(AbilityRegistry.getAbility(name).isPresent(),"Unregistered official ability "+name);
        Pokemon ash=PokemonFactory.create(PixelmonSpecies.fromNationalDex(658));ash.setForm(PermanentAsh.MEGA);ash.setLevelNum(100);ash.initialize();ash.resetAbility();ash.getMoveset().clear();ash.getMoveset().add(new Attack("Water Shuriken"));ash.getMoveset().add(new Attack(NinjaTraining.MOVE));ash.heal();
        check(NinjaTraining.eligible(ash)&&ash.getMoveset().get(0).getMaxPP()==5,"Mega Ash lost signature/PP");
        check("Protean".equals(ash.getAbilityName()),"Mega Ash retained obsolete ability");if(!player.getPersistentData().func_74767_n("za_mega_prepared"))party.set(0,ash);
        var source=player.func_195051_bN().func_197033_a(4).func_197031_a();var commands=player.func_184102_h().func_195571_aL();
        for(String command:new String[]{"gamemode creative","time set day","gamerule doDaylightCycle false","gamerule doMobSpawning false","tp @s 22 66 -10 0 12"})commands.func_197059_a(source,command);
        player.func_145747_a(new StringTextComponent("Mega测试场已更新："+gallery+"个独立展示位，含原始颜色玛机雅娜（未进化）、三种米立龙及闪光对照；P打开中文配置。"),new UUID(0,0));
        System.out.println("ZA_MEGA_GALLERY_PASS entries="+gallery+"; complete ZA catalogue plus five base forms, Ash Mega and shiny comparisons.");
        check(reloadChecks==2,"Boss and ordinary reload probes incomplete");
        System.out.println("ZA_MEGA_PERSISTENCE_PASS entries="+gallery+" native canDespawn=false; native NBT round-trip and join protection, Boss and non-Mega original-color form retained.");
        System.out.println("ZA_MEGA_NATIVE_PASS forms="+count+" registered abilities=7; native Boss forms, spawn pools and exclusive drops; Mega Ash Protean, signature and PP=5.");
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayerEntity)||e.player.field_70173_aa<60)return;
        var p=(ServerPlayerEntity)e.player;if(!WORLD.equals(p.func_184102_h().func_240793_aU_().func_76065_j())||!READY.add(p.func_110124_au()))return;
        try{
            if(!p.getPersistentData().func_74767_n("za_mega_gallery_v3")){prepare(p);p.getPersistentData().func_74757_a("za_mega_prepared",true);p.getPersistentData().func_74757_a("za_mega_gallery_v3",true);}
            DenPlacementVerification.run(p);
            p.field_71071_by.func_70441_a(new net.minecraft.item.ItemStack(Backport.item("tera_den_spawner")));
            p.field_71071_by.func_70441_a(new net.minecraft.item.ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation("pixelmon","den_spawner"))));
        }catch(Throwable error){System.out.println("ZA_MEGA_NATIVE_FAIL "+error);error.printStackTrace();}
    }
    @Mod.EventBusSubscriber(modid="backport_mega_test",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class Client {
        static boolean opened;
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=net.minecraft.client.Minecraft.func_71410_x();if(!opened&&"backport-mega-160".equals(System.getProperty("backport.manual.world"))&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_("backport-mega-160");}if(mc.field_71439_g!=null&&READY.contains(mc.field_71439_g.func_110124_au())){ServerStorageDisplay.showStarterScreen=false;if(mc.field_71462_r instanceof ChooseStarterScreen)mc.func_147108_a(null);}}
    }
}
