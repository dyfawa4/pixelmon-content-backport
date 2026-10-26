package qa;
import java.util.*;
import com.pixelmonmod.pixelmon.entities.npcs.*;
import local.pixelmon.backport.*;
import local.pixelmon.backport.client.NpcNameplates;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.item.*;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.registries.ForgeRegistries;
/** Reported nurse/chatting, original art, real editor and newly requested reward regressions. */
public final class Update180RepairCases {
    static volatile int authorId;
    static final List<NPCEntity> fixtures=new ArrayList<>();
    static com.pixelmonmod.pixelmon.battles.controller.BattleController shown;
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void server(ServerPlayerEntity player){
        var available=AuthorBossRewards.available();
        megaDrops(player);
        Update180RewardCases.run(player);
        for(String id:List.of("pixelmon:red_chain","sophisticatedbackpacks:netherite_backpack","pixelmon:soul_dew")){
            var matches=available.stream().filter(e->e.has("item")&&id.equals(e.get("item").getAsString())).toArray();
            check(matches.length==1,"One available reward entry: "+id);
            check(AuthorBossRewards.stack((com.google.gson.JsonObject)matches[0]).func_190916_E()==1,"Reward count: "+id);
        }
        check(AuthorBossRewards.DATA.get("extra_draws").getAsInt()==7&&!AuthorBossRewards.DATA.get("replacement").getAsBoolean(),"Seven nonrepeating author draws");
        var world=player.func_71121_q();
        for(int x=29;x<=47;x++)for(int z=30;z<=48;z++)world.func_180501_a(new BlockPos(x,73,z),Blocks.field_180398_cJ.func_176223_P(),2);
        var nurse=new NPCNurseJoy(world,1);nurse.init("");nurse.func_70012_b(35,74,39,180,0);nurse.setAIMode(com.pixelmonmod.pixelmon.enums.EnumTrainerAI.StandStill);nurse.func_110163_bv();check(world.func_217376_c(nurse),"Nurse joins world");
        var chat=new NPCChatting(world);chat.init(com.pixelmonmod.pixelmon.entities.npcs.registry.ServerNPCRegistry.villagers.getRandom());chat.init("普通村民测试");chat.setCustomSteveTexture("npcchat1.png");chat.func_70012_b(38,74,39,180,0);chat.setAIMode(com.pixelmonmod.pixelmon.enums.EnumTrainerAI.StandStill);chat.func_110163_bv();check(world.func_217376_c(chat),"Villager joins world");
        var custom=new NPCNurseJoy(world,0);custom.init("");custom.func_200203_b(new StringTextComponent("自定义医生"));custom.func_70012_b(41,74,39,180,0);custom.setAIMode(com.pixelmonmod.pixelmon.enums.EnumTrainerAI.StandStill);custom.func_110163_bv();check(world.func_217376_c(custom),"Custom doctor joins world");
        var author=AuthorBossTeams.create(world,new BlockPos(38,73,34),"desert");world.func_217376_c(author);authorId=author.getNPCID();
        for(String id:List.of("pixelmon:max_mushroom","pixelmon:max_mushroom_block")){
            var block=ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));if(block!=null&&block!=Blocks.field_150350_a){world.func_180501_a(new BlockPos(43,74,40),block.func_176223_P(),2);System.out.println("MUSHROOM_WORLD_BLOCK "+id);break;}
        }
        fixtures.add(nurse);fixtures.add(chat);fixtures.add(custom);fixtures.add(author);
        player.func_200619_a(world,38,74,46,180,6);player.field_71075_bZ.field_75100_b=true;player.field_71075_bZ.field_75101_c=true;
        System.out.println("REPAIR180_SERVER_PASS three registered prizes, seven distinct author draws; nurse, villager, custom name and author fixtures");
    }
    public static void client(Minecraft mc)throws Exception{
        int nurses=0,chats=0,stones=0;
        for(var entity:mc.field_71441_e.func_217416_b())if(entity instanceof NPCEntity){
            var npc=(NPCEntity)entity;String name=NpcNameplates.name(npc),role=NpcNameplates.role(npc,npc.getDisplayText(),npc.getSubTitleText());
            if(npc instanceof NPCNurseJoy&&npc.getTextureIndex()==1){check(name.equals("乔伊")&&role.equals("护士"),"Nurse Joy fallback identity");nurses++;}
            if(npc instanceof NPCChatting){check(!name.isBlank()&&!role.isBlank(),"Chatting NPC role/name");chats++;}
            if(npc.func_200201_e()!=null&&npc.func_200201_e().getString().equals("自定义医生"))check(name.equals("自定义医生"),"Custom name wins");
            System.out.println("REPAIR_NPC "+npc.getClass().getSimpleName()+" name="+name+" role="+role+" nativeDisplay="+npc.getDisplayText());
        }
        for(var npc:fixtures)System.out.println("REPAIR_SERVER_STATE "+npc.getNPCID()+" "+npc.getClass().getSimpleName()+" hp="+npc.func_110143_aJ()+" alive="+npc.func_70089_S()+" removed="+npc.field_70128_L+" pos="+npc.func_233580_cy_()+" client="+mc.field_71441_e.func_73045_a(npc.getNPCID()));
        check(nurses>0&&chats>0,"Nurse and villager synchronized to rendering client");
        for(var value:Content.read("backport/upstream_art_180.json").getAsJsonArray("items")){
            var row=value.getAsJsonObject();String id=row.get("id").getAsString();var stack=Update180Cases.stack(id);
            var model=mc.func_175599_af().func_184393_a(stack,mc.field_71441_e,mc.field_71439_g);
            String sprite=model.func_177554_e().func_195668_m().toString();
            check(sprite.contains("upstream942"),"Actual selected original sprite: "+id+" -> "+sprite);
            System.out.println("REPAIR_MODEL "+id+" -> "+sprite);
        }
        var block=mc.field_71441_e.func_180495_p(new BlockPos(43,74,40));
        String blockSprite=mc.func_175602_ab().func_184389_a(block).func_177554_e().func_195668_m().toString();
        check(blockSprite.contains("upstream942/max_mushroom"),"Placed mushroom original model");
        check(!new net.minecraft.util.text.TranslationTextComponent("block.pixelmon.max_mushroom").getString().startsWith("block."),"Top block tooltip translated");
        for(var item:ForgeRegistries.ITEMS)if(item instanceof com.pixelmonmod.pixelmon.items.heldItems.MegaStoneItem){
            var stack=new ItemStack(item);check(stack.func_77962_s()&&stack.func_77953_t()==Rarity.EPIC,"Actual stack glint/purple "+item.getRegistryName());check(stack.func_77978_p()==null,"No enchantment NBT");stones++;
        }
        check(stones>50,"All Mega stones covered");
        System.out.println("REPAIR180_CLIENT_PASS actual baked sprites, placed mushroom, localized tooltip, "+stones+" Mega stones, nurse/chat names");
    }
    public static void openEditor(Minecraft mc){
        var author=(NPCTrainer)mc.field_71441_e.func_73045_a(authorId);check(author!=null&&author.getTextureIndex()==-1,"Real author custom sentinel");
        com.pixelmonmod.pixelmon.client.gui.npcEditor.TrainerEditorScreen.trainerData=new com.pixelmonmod.pixelmon.comm.SetTrainerData(author,"zh_cn");
        mc.func_147108_a(new com.pixelmonmod.pixelmon.client.gui.npcEditor.TrainerEditorScreen(authorId));
    }
    public static void editorSave(Minecraft mc)throws Exception{
        check(mc.field_71462_r instanceof com.pixelmonmod.pixelmon.client.gui.npcEditor.TrainerEditorScreen,"Native editor remains open after rendered frames");
        var field=mc.field_71462_r.getClass().getDeclaredField("textureEditor");field.setAccessible(true);var editor=field.get(mc.field_71462_r);
        var custom=editor.getClass().getDeclaredMethod("isCustomTexture");custom.setAccessible(true);check((boolean)custom.invoke(editor),"-1 detected as custom texture");
        var boxField=editor.getClass().getDeclaredField("customTextureBox");boxField.setAccessible(true);var box=(net.minecraft.client.gui.widget.TextFieldWidget)boxField.get(editor);box.func_146180_a("nursejoy");
        var save=editor.getClass().getDeclaredMethod("saveCustomTexture");save.setAccessible(true);save.invoke(editor);
    }
    public static void saved(Minecraft mc){
        mc.func_71401_C().func_213167_f(()->{try{var world=mc.func_71401_C().func_184103_al().func_181057_v().get(0).func_71121_q();var npc=(NPCTrainer)world.func_73045_a(authorId);check(npc.getTextureIndex()==-1&&npc.getCustomSteveTexture().equals("nursejoy"),"Native custom texture save reaches server, sentinel preserved");npc.setCustomSteveTexture("mystery_task_man");System.out.println("REPAIR180_EDITOR_SAVE_PASS custom author skin edited and restored through native server packet");}catch(Throwable error){error.printStackTrace();Update180Probe.report("UPDATE180_FAIL editor save "+error);}});
        var npc=(NPCTrainer)mc.field_71441_e.func_73045_a(authorId);npc.setTextureIndex(0);
        mc.func_147108_a(new com.pixelmonmod.pixelmon.client.gui.npcEditor.TrainerEditorScreen(authorId));
    }
    static void megaDrops(ServerPlayerEntity real){
        var world=real.func_71121_q();var player=net.minecraftforge.common.util.FakePlayerFactory.get(world,new com.mojang.authlib.GameProfile(UUID.fromString("ddf338cb-1111-4000-9000-000000001830"),"MegaDropQA"));
        var storage=com.pixelmonmod.pixelmon.api.storage.StorageProxy.getParty(player);
        Map<?,?> obtained=BossLootCatalog.field(storage,"megasObtained");
        int tiers=0,spawns=0,added=0;Set<String> stones=new HashSet<>();
        for(var tier:com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry.getBossTiers())if(tier.isMega()){
            double first=BossLootCatalog.field(tier,"megaStoneObtainChance"),repeat=BossLootCatalog.field(tier,"megaStoneRepeatChance");
            check(first==1,"Native first stone guaranteed: "+tier.getID());
            double expected=tier.getID().equals("epic")?.025:tier.getID().equals("legendary")?.05:tier.getID().equals("ultimate")?.1:repeat;
            check(repeat==expected,"Native repeat chance unchanged: "+tier.getID());tiers++;
        }
        var tier=com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry.getBossTierOrNotBoss("epic");
        for(var set:com.pixelmonmod.pixelmon.spawning.PixelmonSpawning.megas)for(var raw:set.spawnInfos)if(raw instanceof com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnInfoPokemon){
            var info=(com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnInfoPokemon)raw;var pokemon=info.getPokemonSpec().create();
            if(!pokemon.getForm().hasMegaForm()||pokemon.getForm().getMegaItems().stream().noneMatch(item->item instanceof com.pixelmonmod.pixelmon.items.heldItems.MegaStoneItem))continue;
            obtained.clear();pokemon.setLevelNum(70);pokemon.initialize();var entity=new com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity(world,pokemon);
            var drops=tier.getDrops(player,entity);var mega=drops.stream().filter(drop->drop.getDisplayItem().func_77973_b() instanceof com.pixelmonmod.pixelmon.items.heldItems.MegaStoneItem).findFirst().orElseThrow(()->new AssertionError("First Mega boss stone missing: "+set.id));
            var item=mega.getDisplayItem().func_77973_b();check(pokemon.getForm().getMegaItems().contains(item),"Matching stone for native Mega Boss: "+set.id);
            check(storage.isMegaItemObtained(pokemon.getSpecies(),pokemon.getForm()),"Native first discovery ledger updates: "+set.id);
            stones.add(item.getRegistryName().toString());spawns++;if(item.getRegistryName().toString().startsWith("pixelmon_backport:"))added++;
            var saved=storage.writeToNBT(new net.minecraft.nbt.CompoundNBT());check(saved.func_74764_b("MegaStonesObtained"),"Mega discovery persistence NBT");
            var restored=new com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage(player.func_110124_au(),false);restored.readFromNBT(saved);check(restored.isMegaItemObtained(pokemon.getSpecies(),pokemon.getForm()),"Native saved Mega discovery restores: "+set.id);
        }
        check(tiers>=3&&spawns>30&&added>10,"Native and newly added Mega Boss coverage");
        var greninja=Update180Cases.mon(658,"Tackle");var entity=new com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity(world,greninja);
        for(int trial=0;trial<64;trial++){obtained.clear();var drops=tier.getDrops(player,entity);check(drops.stream().anyMatch(drop->drop.getDisplayItem().func_77973_b()==Backport.item("greninjite")),"Greninja first reward never swallowed by author-only stone filter");check(drops.stream().noneMatch(drop->DrownedBosses.exclusive(drop.getDisplayItem().func_77973_b())),"Author-only Ash stone unavailable from ordinary Boss");}
        obtained.clear();System.out.println("REPAIR180_MEGADROP_PASS native100% first, epic2.5% legendary5% ultimate10% unchanged; "+spawns+" native spawn definitions / "+added+" added stone results / "+stones.size()+" sampled stones; discovery ledger and NBT");
    }
    public static void accuracyBattle(Minecraft mc){mc.func_71401_C().func_213167_f(()->{try{
        var player=mc.func_71401_C().func_184103_al().func_181057_v().get(0);var party=com.pixelmonmod.pixelmon.api.storage.StorageProxy.getParty(player);
        player.func_200619_a(player.func_71121_q(),38,74,37,180,0);
        for(int i=0;i<6;i++)party.set(i,null);var own=Update180Cases.mon(143,"Tackle","Thunder","Swift","Swords Dance");party.set(0,own);own.getOrSpawnPixelmon(player);
        var npc=(NPCTrainer)player.func_71121_q().func_73045_a(authorId);npc.healAllPokemon();
        shown=com.pixelmonmod.pixelmon.battles.BattleRegistry.startBattle(new com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant(player,own),new com.pixelmonmod.pixelmon.battles.controller.participants.TrainerParticipant(npc,player,1));
        check(shown!=null,"Author display battle starts");shown.update();TeraNetwork.sync(shown,false);System.out.println("REPAIR_ACCURACY_SERVER battle="+shown.battleIndex+" ended="+shown.battleEnded+" author="+GymChallenge.author(shown));
    }catch(Throwable error){error.printStackTrace();Update180Probe.report("UPDATE180_FAIL accuracy battle "+error);}});}
    public static void accuracyClient()throws Exception{
        var manager=com.pixelmonmod.pixelmon.client.ClientProxy.battleManager;System.out.println("REPAIR_ACCURACY_CLIENT manager="+(manager==null?"null":manager.battleControllerIndex+" ended="+manager.battleEnded+" z="+manager.showZMoves)+" snapshot="+(TeraClient.snapshot==null?"null":TeraClient.snapshot.battle+" author="+TeraClient.snapshot.author));check(AuthorChallengeClient.active(),"Actual active author battle UI snapshot");
        var button=new com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.ChooseAttack.MoveButton(null,0);var field=button.getClass().getDeclaredField("attack");field.setAccessible(true);
        var hook=Arrays.stream(button.getClass().getDeclaredMethods()).filter(m->m.getName().contains("backport$accuracy")).findFirst().orElseThrow();hook.setAccessible(true);
        var label=Arrays.stream(button.getClass().getDeclaredMethods()).filter(m->m.getName().contains("backport$successLabel")).findFirst().orElseThrow();label.setAccessible(true);
        for(String move:List.of("Tackle","Swift","Thunder","Sheer Cold")){var attack=new com.pixelmonmod.pixelmon.battles.attacks.Attack(move);field.set(button,attack);int original=attack.getMove().getAccuracy();check((Integer)hook.invoke(button,attack.getMove())==Math.max(0,(original<0?100:original)-30),"Actual transformed native move button -30: "+move);}
        field.set(button,new com.pixelmonmod.pixelmon.battles.attacks.Attack("Protect"));check(((String)label.invoke(button,"gui.battle.accuracyval",new Object[]{100})).contains("70"),"Native Protect label70");
        var current=manager.getCurrentPokemon();TeraClient.snapshot.protects.put(current.pokemonUUID,1);check(((String)label.invoke(button,"gui.battle.accuracyval",new Object[]{100})).contains("23.33"),"Consecutive protect display70/3");TeraClient.snapshot.protects.put(current.pokemonUUID,0);
        field.set(button,new com.pixelmonmod.pixelmon.battles.attacks.Attack("Swords Dance"));check(((String)label.invoke(button,"gui.battle.accuracyval",new Object[]{100})).contains("70%"),"Self-buff probability display70");
        var attack=new com.pixelmonmod.pixelmon.battles.attacks.Attack("Tackle");field.set(button,attack);manager.showZMoves=true;check((Integer)hook.invoke(button,attack.getMove())==100,"Z display exempt");manager.showZMoves=false;TeraClient.snapshot.author=false;check((Integer)hook.invoke(button,attack.getMove())==100,"Ordinary battles display unchanged");TeraClient.snapshot.author=true;
        System.out.println("REPAIR180_ACCURACY_UI_PASS actual author battle snapshot and transformed native MoveButton:100->70,70->40,always-hit->70,clamp0; Protect70/23.33, buff70; Z/ordinary exemptions");
    }
}
