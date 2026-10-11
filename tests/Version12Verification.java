import java.util.*;
import com.mojang.authlib.GameProfile;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.*;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.api.events.CaptureEvent;
import com.pixelmonmod.pixelmon.api.events.pokemon.ItemFormChangeEvent;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.interactions.formChanges.*;
import com.pixelmonmod.pixelmon.blocks.tileentity.ArcChaliceTileEntity;
import com.pixelmonmod.pixelmon.api.events.legendary.ArceusEvent;
import com.pixelmonmod.pixelmon.enums.EnumPlate;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import com.pixelmonmod.pixelmon.entities.pixelmon.helpers.EvolutionQuery;
import com.pixelmonmod.pixelmon.entities.pixelmon.helpers.EvolutionQueryList;
import com.pixelmonmod.pixelmon.enums.EnumMegaItem;
import com.pixelmonmod.pixelmon.api.events.MegaEvolutionEvent;
public final class Version12Verification {
    /** Forge explicitly refuses advancement grants to FakePlayer. Use a real
     * server player with a packet sink, without opening a client or socket. */
    public static void runReal(net.minecraft.server.MinecraftServer server)throws Exception {
        var world=server.func_71218_a(net.minecraft.world.World.field_234918_g_);
        UUID uuid=UUID.fromString("324ceb20-c8b7-4b77-8f42-37d3b7bc0182");
        ServerPlayerEntity player=new ServerPlayerEntity(server,world,new GameProfile(uuid,"BackportV12"),new net.minecraft.server.management.PlayerInteractionManager(world));
        var network=new net.minecraft.network.NetworkManager(net.minecraft.network.PacketDirection.SERVERBOUND){@Override public void func_179290_a(net.minecraft.network.IPacket<?> packet){}};
        player.field_71135_a=new net.minecraft.network.play.ServerPlayNetHandler(server,network,player){@Override public void func_147359_a(net.minecraft.network.IPacket<?> packet){}};
        Map<UUID,ServerPlayerEntity> players=net.minecraftforge.fml.common.ObfuscationReflectionHelper.getPrivateValue(net.minecraft.server.management.PlayerList.class,server.func_184103_al(),"field_177454_f");
        require(!players.containsKey(uuid),"Version 12 fixture UUID in use");players.put(uuid,player);world.func_217369_A().add(player);
        try{run(player);}finally{players.remove(uuid);world.func_217369_A().remove(player);}
    }
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void reset(ServerPlayerEntity p,String name){var a=p.func_184102_h().func_191949_aK().func_192778_a(new ResourceLocation(Content.NS,"legends/"+name));require(a!=null,"Missing achievement "+name);p.func_192039_O().func_192744_b(a,"earned");}
    static boolean achieved(ServerPlayerEntity p,String name){var a=p.func_184102_h().func_191949_aK().func_192778_a(new ResourceLocation(Content.NS,"legends/"+name));require(a!=null,"Missing achievement "+name);return p.func_192039_O().func_192747_a(a).func_192105_a();}
    public static void run(ServerPlayerEntity player){
        var party=StorageProxy.getParty(player);Pokemon[] prior=party.getAll().clone();ItemStack hand=player.func_184614_ca();
        try{
            ItemStack orb=new ItemStack(Backport.item("tera_orb"));TeraItems.Orb item=(TeraItems.Orb)orb.func_77973_b();
            require(item.func_77636_d(orb),"Charged orb lacks vanilla glint");orb.func_196082_o().func_74757_a("BackportTeraSpent",true);require(!item.func_77636_d(orb),"Spent orb still glows");
            List<ITextComponent> lines=new ArrayList<>();item.func_77624_a(orb,player.field_70170_p,lines,null);require(lines.size()==3,"Orb instructions not three lines");
            for(ITextComponent line:lines)require(line.func_150256_b().func_240711_a_().equals(Color.func_240744_a_(TextFormatting.GRAY)),"Orb description not gray");
            Item stone=Backport.item("dragoninite");
            var creative=net.minecraft.util.NonNullList.<ItemStack>func_191196_a();((BackportCreativeTab)Backport.CREATIVE_TAB).func_78018_a(creative);
            require(creative.stream().filter(s->s.func_77973_b()==stone).count()==1,"Duplicate creative stone");
            require(Backport.item("dragonitite")==stone && creative.stream().filter(s->Backport.isDragonStone(s.func_77973_b().getRegistryName().func_110623_a())).count()==1,"Two backport Dragonite aliases still visible");
            Pokemon dragon=FeatureTestMaps.mon(149);require(PixelmonWrapper.canMegaEvolve(new ItemStack(stone),dragon.getSpecies(),dragon.getForm().getName()),"Canonical stone lost new Mega form");
            require(!Backport.REGISTERED.containsKey("dragonitite"),"Duplicate Dragonite registration retained");
            var itemRegistry=net.minecraftforge.registries.ForgeRegistries.ITEMS;
            var mapping=new net.minecraftforge.event.RegistryEvent.MissingMappings.Mapping<Item>(itemRegistry,itemRegistry,new net.minecraft.util.ResourceLocation("pixelmon_backport:dragonitite"),3117);
            var mappingEvent=new net.minecraftforge.event.RegistryEvent.MissingMappings<Item>(new net.minecraft.util.ResourceLocation("minecraft:item"),itemRegistry,Collections.singletonList(mapping));
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(mappingEvent);
            require(mapping.getAction()==net.minecraftforge.event.RegistryEvent.MissingMappings.Action.REMAP && mapping.getTarget()==stone,"Old world registry ID did not remap on the actual Forge bus");
            System.out.println("BACKPORT_DRAGON_MIGRATION_PASS unique registered/creative stone, actual Forge missing mapping remap");
            CompoundNBT oldStone=new ItemStack(stone,3).func_77955_b(new CompoundNBT());oldStone.func_74778_a("id","pixelmon_backport:dragonitite");CompoundNBT custom=new CompoundNBT();custom.func_74778_a("Test","preserved");oldStone.func_218657_a("tag",custom);ItemStack migrated=ItemStack.func_199557_a(oldStone);
            require(migrated.func_77973_b()==stone && migrated.func_190916_E()==3 && migrated.func_196082_o().func_74779_i("Test").equals("preserved"),"Removed stone NBT migration lost item/count/tag");
            player.func_184611_a(Hand.MAIN_HAND,ItemStack.field_190927_a);
            for(int dex:new int[]{1020,1021,1022,1023,1024,1025}){
                player.func_184210_p();Pokemon pokemon=FeatureTestMaps.mon(dex);party.set(0,pokemon);PixelmonEntity entity=pokemon.getOrSpawnPixelmon(player);
                require(pokemon.getForm().getMovement().isRideable()==(dex<=1023),"Wrong ride policy "+dex);
                entity.func_230254_b_(player,Hand.MAIN_HAND);require((player.func_184187_bx()==entity)==(dex<=1023),"Native mount interaction failed "+dex);
                player.func_184210_p();entity.retrieve("RideFixture");
            }
            int count=0;
            for(var entry:LegendAdvancements.DATA.getAsJsonObject("captures").entrySet()){
                Pokemon pokemon=FeatureTestMaps.mon(Integer.parseInt(entry.getKey()));
                String captureId=entry.getValue().getAsJsonObject().get("id").getAsString();
                var definition=player.func_184102_h().func_191949_aK().func_192778_a(new ResourceLocation(Content.NS,"legends/"+captureId));
                require(definition.func_192068_c().func_192291_d()==(Integer.parseInt(entry.getKey())==493?net.minecraft.advancements.FrameType.CHALLENGE:net.minecraft.advancements.FrameType.TASK),"Wrong capture frame "+captureId);
                reset(player,captureId);party.set(0,pokemon);require(!achieved(player,captureId),"Gift awarded capture "+captureId);
                Pixelmon.EVENT_BUS.post(new CaptureEvent.SuccessfulRaidCapture(player,pokemon,null));
                require(achieved(player,captureId),"Raid capture event failed "+captureId);
                reset(player,captureId);Pokemon wild=FeatureTestMaps.mon(Integer.parseInt(entry.getKey()));PixelmonEntity specimen=wild.getOrCreatePixelmon(player.field_70170_p,4,65,0);
                require(!achieved(player,captureId),"Spawn awarded capture "+captureId);
                Pixelmon.EVENT_BUS.post(new CaptureEvent.SuccessfulCapture(player,specimen,null));require(achieved(player,captureId),"Wild capture event failed "+captureId);++count;
            }
            int[][] pairs={{646,644},{646,643},{800,791},{800,792},{898,896},{898,897}};
            Item[] items={PixelmonItems.dna_splicers,PixelmonItems.dna_splicers,PixelmonItems.n_solarizer,PixelmonItems.n_lunarizer,PixelmonItems.reins_of_unity,PixelmonItems.reins_of_unity};
            for(int i=0;i<6;i++){
                String id=LegendAdvancements.DATA.getAsJsonArray("fusions").get(i).getAsJsonObject().get("id").getAsString();reset(player,id);
                for(int slot=0;slot<6;slot++)party.set(slot,null);
                Pokemon base=FeatureTestMaps.mon(pairs[i][0]),partner=FeatureTestMaps.mon(pairs[i][1]);party.set(0,base);party.set(1,partner);PixelmonEntity entity=base.getOrSpawnPixelmon(player);
                ItemStack tool=new ItemStack(items[i]);player.func_184611_a(Hand.MAIN_HAND,tool);
                IFormChange change=i<2?new KyuremFormChange():i<4?new NecrozmaFormChange():new CalyrexFormChange();
                java.util.function.Consumer<ItemFormChangeEvent> cancel=event->event.setCanceled(true);
                Pixelmon.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST,false,ItemFormChangeEvent.class,cancel);
                try{
                    require(!change.execute(entity,tool,player),"Canceled fusion proceeded "+i);
                    MinecraftForge.EVENT_BUS.post(new TickEvent.ServerTickEvent(TickEvent.Phase.END));require(!achieved(player,id),"Canceled fusion awarded "+id);
                }finally{Pixelmon.EVENT_BUS.unregister(cancel);}
                require(change.execute(entity,tool,player),"Native fusion refused "+i);
                MinecraftForge.EVENT_BUS.post(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
                require(achieved(player,id),"Native fusion achievement missing "+id+" "+base.getForm().getName());
                Pokemon saved=PokemonFactory.create(base.writeToNBT(new CompoundNBT()));require(saved.getForm().getName().equals(base.getForm().getName()) && !saved.getPersistentData().func_74775_l("FusedPokemon").isEmpty(),"Fusion save lost partner "+i);
                require(change.execute(entity,tool,player),"Native unfusion refused "+i);MinecraftForge.EVENT_BUS.post(new TickEvent.ServerTickEvent(TickEvent.Phase.END));
                require(base.getForm().getName().equals(base.getSpecies().getDefaultForm().getName()) && achieved(player,id),"Unfusion lost achievement or original form "+i);
                final int partnerDex=pairs[i][1];require(Arrays.stream(party.getAll()).anyMatch(p->p!=null && p.getSpecies().getDex()==partnerDex),"Unfusion lost partner "+i);
                entity.retrieve("FusionFixture");
            }
            var world=player.func_71121_q();BlockPos pos=new BlockPos(28,65,0);var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pixelmon:arc_chalice"));world.func_180501_a(pos,block.func_176223_P(),3);
            ArcChaliceTileEntity chalice=(ArcChaliceTileEntity)world.func_175625_s(pos);
            java.util.concurrent.atomic.AtomicReference<ArceusEvent.CreateFlute> created=new java.util.concurrent.atomic.AtomicReference<>();
            java.util.function.Consumer<ArceusEvent.CreateFlute> observer=created::set;Pixelmon.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.NORMAL,false,ArceusEvent.CreateFlute.class,observer);
            try{
                for(var plate:EnumPlate.values()){
                    ItemStack material=new ItemStack(plate.getItem(),2);player.func_184611_a(Hand.MAIN_HAND,material);
                    block.func_225533_a_(block.func_176223_P(),world,pos,player,Hand.MAIN_HAND,new net.minecraft.util.math.BlockRayTraceResult(new net.minecraft.util.math.vector.Vector3d(28.5,66,.5),Direction.UP,pos,false));
                    require(material.func_190916_E()==1,"Real plate interaction failed "+plate);
                    for(int tick=0;tick<(plate.ordinal()==17?501:101);tick++)chalice.func_73660_a();
                }
                require(chalice.itemThere && !chalice.animating && created.get()!=null,"Real 18-plate synthesis did not finish");
                require(created.get().getPlayer()==player && created.get().getAzureFlute().func_92059_d().func_77973_b()==PixelmonItems.azure_flute,"Flute synthesis owner/item");
                created.get().getAzureFlute().func_70100_b_(player);require(!created.get().getAzureFlute().func_70089_S(),"Synthesized flute could not be collected");
            }finally{Pixelmon.EVENT_BUS.unregister(observer);}
            CompoundNBT nbt=chalice.func_189515_b(new CompoundNBT());nbt.func_186854_a("BackportLastUsedPlayer",player.func_110124_au());nbt.func_74757_a("Animating",true);nbt.func_74777_a("AnimationTimer",(short)501);nbt.func_74774_a("CurrentPlate",(byte)17);int[] plates=new int[17];for(int i=0;i<17;i++)plates[i]=i;nbt.func_74783_a("Plates",plates);
            chalice.func_230337_a_(block.func_176223_P(),nbt);require(chalice.func_189515_b(new CompoundNBT()).func_186857_a("BackportLastUsedPlayer").equals(player.func_110124_au()),"Flute owner lost across reload");
            chalice.func_73660_a();require(chalice.itemThere && !chalice.animating,"Reloaded chalice did not produce flute");
            var event=new ArceusEvent.CreateFlute(null,chalice,null);require(event.getPlayer()==null,"Null legacy flute owner crashed");
            reset(player,"form_890_eternamax");Pokemon eternatus=FeatureTestMaps.mon(890);party.set(0,eternatus);LegendAdvancements.eternamax(player);require(!achieved(player,"form_890_eternamax"),"Ordinary Eternatus awarded Eternamax");
            eternatus.setHeldItem(new ItemStack(Backport.item("eternatus_meteorite")));LegendAdvancements.eternamax(player);require(eternatus.getForm().isForm("eternamax") && achieved(player,"form_890_eternamax"),"Native meteorite form lacked achievement");
            eternatus.setHeldItem(ItemStack.field_190927_a);require(eternatus.getForm().isForm("ordinary") && achieved(player,"form_890_eternamax"),"Removing meteorite revoked achievement");
            fieldMega(player);
            FeatureStations.setup(player);require(FeatureStations.resolve("493")==493 && FeatureStations.resolve("Arceus")==493,"Named species selection");
            FeatureStations.zygarde(player);require(party.get(0).getSpecies().getDex()==718 && party.get(0).getOrSpawnPixelmon(player).getOwner()==player,"Zygarde owned entity fixture");
            System.out.println("BACKPORT_VERSION12_PASS orb gray/glint, canonical stone/Mega, native riding=4 exclusions=2, capture events="+count+", real native fusions=6, flute reload");
        }finally{player.func_184210_p();for(int i=0;i<6;i++)party.set(i,prior[i]);player.func_184611_a(Hand.MAIN_HAND,hand);}
    }
    static void fieldMega(ServerPlayerEntity player){
        var party=StorageProxy.getParty(player);var accessory=party.getMegaItem();var unlock=party.getMegaItemsUnlocked();
        try{
            party.unlockMega(false);party.setMegaItem(EnumMegaItem.BraceletORAS,false);
            Item[] stones={PixelmonItems.charizardite_x,PixelmonItems.charizardite_y,Backport.item("dragoninite")};
            var tick=EvolutionQuery.class.getDeclaredMethod("tick",net.minecraft.world.World.class);tick.setAccessible(true);
            for(int i=0;i<stones.length;i++){
                Pokemon p=FeatureTestMaps.mon(i==2?149:6);party.set(0,p);p.setHeldItem(new ItemStack(stones[i]));
                if(i==0){p.setAbility(p.getForm().getAbilities().getRandomHiddenAbility());p.overrideAbilitySlot();require(p.hasHiddenAbility(),"Hidden ability fixture not initialized");}
                int slot=p.getAbilitySlot();boolean hidden=p.hasHiddenAbility();
                String base=p.getForm().getName(),ability=p.getAbilityName();int hp=p.getHealth()-10;p.setHealth(hp);String moves=moveSnapshot(p);
                require(FieldMega.target(p)!=null,"Field stone not compatible "+i);
                require(FieldMega.start(player,p.getUUID()).equals("sendout"),"Field Mega spawned a recalled Pokemon");
                PixelmonEntity entity=p.getOrSpawnPixelmon(player);
                require(FieldMega.start(player,UUID.randomUUID()).equals("owned"),"Field Mega accepted foreign UUID");
                party.setMegaItem(EnumMegaItem.None,false);require(FieldMega.start(player,p.getUUID()).equals("bracelet"),"Field Mega bypassed accessory");party.setMegaItem(EnumMegaItem.BraceletORAS,false);
                java.util.function.Consumer<MegaEvolutionEvent.MoveSkill> cancel=e->e.setCanceled(true);
                Pixelmon.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.NORMAL,false,MegaEvolutionEvent.MoveSkill.class,cancel);
                try{require(FieldMega.start(player,p.getUUID()).equals("canceled") && !FieldMega.active(p),"Field Mega ignored canceled native event");}finally{Pixelmon.EVENT_BUS.unregister(cancel);}
                require(FieldMega.start(player,p.getUUID()).equals("started"),"Field Mega did not start "+i);
                EvolutionQuery query=EvolutionQueryList.queryList.stream().filter(q->q.pokemonUUID.equals(p.getUUID())).findFirst().orElseThrow();
                CompoundNBT pre=p.writeToNBT(new CompoundNBT());Pokemon clone=PokemonFactory.create(pre);
                require(!FieldMega.active(clone) && !clone.getForm().getName().startsWith("mega") && EvolutionQueryList.queryList.contains(query),"Reload copy canceled live field animation");
                for(int n=0;n<600 && EvolutionQueryList.queryList.contains(query);n++)tick.invoke(query,player.field_70170_p);
                require(p.getForm().getName().startsWith("mega"),"Native field animation did not transform "+i);
                require(Arrays.asList(p.getForm().getAbilities().getAbilities()).contains(p.getAbility()),"Field Mega inherited an invalid base ability "+i);
                require(p.getHealth()==hp && moves.equals(moveSnapshot(p)),"Field transformation changed health/moves: hp "+hp+" -> "+p.getHealth()+"; "+moves+" -> "+moveSnapshot(p));
                CompoundNBT saved=p.writeToNBT(new CompoundNBT());require(saved.func_74779_i("Variant").equals(base) && p.getForm().getName().startsWith("mega"),"Save mutated live form or stored Mega");
                clone=PokemonFactory.create(saved);require(!clone.getForm().getName().startsWith("mega") && clone.getAbilityName().equals(ability) && clone.getHealth()==hp && clone.getAbilitySlot()==slot && clone.hasHiddenAbility()==hidden,"Field reload changed permanent data");
                if(i==0){new PlayerParticipant(player,p);require(!p.getForm().getName().startsWith("mega") && !FieldMega.active(p) && p.getAbilityName().equals(ability),"Battle wrapper cached field form");}
                else if(i==1){entity.retrieve("FieldMegaFixture");require(!p.getForm().getName().startsWith("mega") && !FieldMega.active(p),"Recall kept field Mega");}
                else{require(FieldMega.start(player,p.getUUID()).equals("reverted"),"Field toggle did not revert");}
                FieldMega.restore(p);require(p.getAbilitySlot()==slot && p.hasHiddenAbility()==hidden,"Field restore changed ability inheritance "+i);entity.retrieve("FieldMegaFixture");
            }
            Pokemon wrong=FeatureTestMaps.mon(6);wrong.setHeldItem(new ItemStack(PixelmonItems.blastoisinite));require(FieldMega.target(wrong)==null,"Wrong-species Mega stone accepted");
            Pokemon first=FeatureTestMaps.mon(6),second=FeatureTestMaps.mon(149);party.set(0,first);party.set(1,second);
            first.setHeldItem(new ItemStack(PixelmonItems.charizardite_x));second.setHeldItem(new ItemStack(Backport.item("dragoninite")));
            first.getOrSpawnPixelmon(player);second.getOrSpawnPixelmon(player);
            require(FieldMega.start(player,second.getUUID()).equals("started") && !FieldMega.active(first) && FieldMega.active(second),"Field request evolved a different released Pokemon");
            require(FieldMega.start(player,first.getUUID()).equals("started") && FieldMega.active(first) && FieldMega.active(second),"Multiple field targets mixed identities");
            require(FieldMega.start(player,second.getUUID()).equals("reverted") && FieldMega.active(first) && !FieldMega.active(second),"Reverting one target affected another");
            first.getPixelmonEntity().get().retrieve("FieldMultiFixture");second.getPixelmonEntity().get().retrieve("FieldMultiFixture");
            System.out.println("BACKPORT_FIELD_MEGA_PASS native X/Y + ZA animation, multiple released UUID isolation, ownership, accessory, cancellation, save/reload, recall, battle entry, HP/moves/ability");
        }catch(Exception e){throw new RuntimeException("Field Mega lifecycle",e);}finally{for(Pokemon p:party.getAll())if(p!=null)FieldMega.restore(p);party.setMegaItemsUnlocked(unlock);party.setMegaItem(accessory,false);}
    }
    static String moveSnapshot(Pokemon p){StringBuilder result=new StringBuilder();for(var a:p.getMoveset())result.append(a.getMove().getAttackName()).append(':').append(a.pp).append(':').append(a.ppLevel).append(';');return result.toString();}
}

