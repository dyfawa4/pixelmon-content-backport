import java.io.*;
import java.lang.reflect.*;
import java.util.jar.JarFile;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

public final class MixinApplicationVerification {
    public static void main(String[] args)throws Exception {
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        MixinEnvironment.getCurrentEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        Mixins.addConfiguration("pixelmon_backport.mixins.json");
        Method phase=MixinEnvironment.class.getDeclaredMethod("gotoPhase",MixinEnvironment.Phase.class);phase.setAccessible(true);phase.invoke(null,MixinEnvironment.Phase.DEFAULT);
        Constructor<?> ctor=Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer").getDeclaredConstructor();ctor.setAccessible(true);
        IMixinTransformer transformer=(IMixinTransformer)ctor.newInstance();
        java.util.List<String> weatherTargets=new java.util.ArrayList<>();
        weatherTargets.add("com.pixelmonmod.pixelmon.api.pokemon.boss.BossTier");
        weatherTargets.add("com.pixelmonmod.pixelmon.blocks.tileentity.HealerTileEntity");
        weatherTargets.add("net.minecraft.loot.LootTableManager");
        weatherTargets.add("net.minecraft.client.resources.ClientLanguageMap");
        weatherTargets.add("net.minecraft.command.impl.LocateCommand");
        weatherTargets.add("net.minecraft.client.renderer.tileentity.SkullTileEntityRenderer");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.controller.ai.BattleAIBase");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.SuckerPunch");
        weatherTargets.add("com.pixelmonmod.pixelmon.client.models.smd.Bone");
        for(String target:new String[]{"com.pixelmonmod.pixelmon.entities.pixelmon.AbstractClientEntity","com.pixelmonmod.pixelmon.client.models.smd.DeformVertex","com.pixelmonmod.pixelmon.client.models.smd.ValveStudioModel","com.pixelmonmod.pixelmon.client.render.entity.layers.PokemonShimLayer"})weatherTargets.add(target);
        for(String ability:new String[]{"Chlorophyll","SwiftSwim","SolarPower","DrySkin","RainDish","Hydration","LeafGuard","Forecast","FlowerGift"})weatherTargets.add("com.pixelmonmod.pixelmon.api.pokemon.ability.abilities."+ability);
        for(String effect:new String[]{"WeatherBall","Synthesis","Growth","HydroSteam"})weatherTargets.add("com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic."+effect);
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.multiTurn.SolarBeam");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.attackModifiers.AlwaysHit");
        weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.ChooseAttack");
        weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.ChooseAttack$MoveButton");
        weatherTargets.add("com.pixelmonmod.pixelmon.container.PokeBagContainer");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.raids.RaidData");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.raids.RaidRandomizer");
        weatherTargets.add("com.pixelmonmod.pixelmon.entities.DenEntity");
        weatherTargets.add("com.pixelmonmod.pixelmon.entities.pixelmon.AbstractBaseEntity");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.controller.BattleController");
        weatherTargets.add("com.pixelmonmod.pixelmon.items.SpawnDenItem");
        weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.raids.RaidStartScreen");
        weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.raids.RaidCatchScreen");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.raids.RaidData$RaidPlayer");
        weatherTargets.add("com.pixelmonmod.pixelmon.api.pokemon.catching.CaptureMethod");
        weatherTargets.add("com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer");
        weatherTargets.add("com.pixelmonmod.pixelmon.entities.npcs.NPCChatting");
        weatherTargets.add("com.pixelmonmod.pixelmon.entities.pokeballs.EmptyPokeBallEntity");
        weatherTargets.add("com.pixelmonmod.pixelmon.blocks.PokeChestBlock");
        weatherTargets.add("com.pixelmonmod.pixelmon.entities.npcs.registry.DropItemRegistry");
        for(String reward:new String[]{"api.pokemon.boss.DropSet","api.pokemon.boss.DropSet$Drop","entities.pixelmon.drops.DropItemQueryList","api.drops.CustomDropScreenFactory$Builder","battles.raids.WeightedItemStacks","api.pokemon.drops.ItemWithChance"})weatherTargets.add("com.pixelmonmod.pixelmon."+reward);
            weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.ScreenHelper");
            weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.pokechecker.PokeCheckerMovesScreen");
            weatherTargets.add("com.pixelmonmod.pixelmon.battles.tasks.BattleMessagePacket");
            weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.custom.dialogue.DialogueScreen");
        for(String dialogue:new String[]{"DialogueNextActionPacket","SetDialogueDataPacket","DialogueChoiceMadePacket"})weatherTargets.add("com.pixelmonmod.pixelmon.comm.packetHandlers.dialogue."+dialogue);
        weatherTargets.add("com.pixelmonmod.pixelmon.quests.actions.actions.EndDialogueAction");
        for(String riding:new String[]{"entities.pixelmon.PixelmonEntity","entities.pixelmon.movement.PixelmonMovementGround","entities.pixelmon.movement.PixelmonMovementFlying","comm.packetHandlers.RidingMovementPacket","comm.packetHandlers.RidingVerticalMovementPacket","client.keybindings.MovementHandler","client.keybindings.OldMovementHandler","client.render.entity.renderers.AbstractPokemonRenderer"})weatherTargets.add("com.pixelmonmod.pixelmon."+riding);
        weatherTargets.add("com.pixelmonmod.pixelmon.entities.pixelmon.AbstractMovesEntity");
        weatherTargets.add("com.pixelmonmod.pixelmon.blocks.tileentity.FossilMachineTileEntity");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.status.HealBlock");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.status.ProtectVariation");
        weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.battles.ClientBattleManager");
        weatherTargets.add("com.pixelmonmod.pixelmon.comm.packetHandlers.battles.ChooseAttackPacket");
        weatherTargets.add("com.pixelmonmod.pixelmon.client.keybindings.OptionsKey");
        weatherTargets.add("com.pixelmonmod.pixelmon.api.pokemon.LearnMoveController");
        for(String residual:new String[]{"Sandstorm","Hail","GrassyTerrain","Terrain"})weatherTargets.add("com.pixelmonmod.pixelmon.battles.status."+residual);
        for(String riding:new String[]{"net.minecraft.network.play.ServerPlayNetHandler","net.minecraft.client.entity.player.ClientPlayerEntity","net.minecraft.client.renderer.entity.PlayerRenderer","net.minecraft.client.renderer.ActiveRenderInfo"})weatherTargets.add(riding);
            weatherTargets.add("com.pixelmonmod.pixelmon.entities.npcs.NPCShopkeeper");
        weatherTargets.add("com.pixelmonmod.pixelmon.comm.packetHandlers.pokedex.RequestSpawnDataPacket");
        weatherTargets.add("com.pixelmonmod.pixelmon.api.spawning.conditions.SpawnCondition");
        weatherTargets.add("com.pixelmonmod.pixelmon.api.spawning.archetypes.algorithms.checkspawns.LegendaryCheckSpawns");
        weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.machines.infuser.InfuserScreen");
        weatherTargets.add("com.pixelmonmod.pixelmon.items.PixelmonItem");
        weatherTargets.add("com.pixelmonmod.pixelmon.items.PokeBallItem");
        weatherTargets.add("com.pixelmonmod.pixelmon.items.ApricornItem");
        weatherTargets.add("com.pixelmonmod.pixelmon.items.PaintingItem");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.StatsEffectPost");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.StatsEffect");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.StatsEffectCone");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.StatsEffectTeam");
        weatherTargets.add("com.pixelmonmod.pixelmon.api.spawning.archetypes.algorithms.checkspawns.PlayerTrackingCheckSpawns");
        weatherTargets.add("com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall");
        weatherTargets.add("com.pixelmonmod.pixelmon.comm.packetHandlers.clientStorage.newStorage.ClientSetPacket");
        weatherTargets.add("com.pixelmonmod.pixelmon.comm.packetHandlers.clientStorage.newStorage.pc.ClientInitializePCPacket");
        weatherTargets.add("com.pixelmonmod.pixelmon.api.storage.PCStorage");
        weatherTargets.add("com.pixelmonmod.pixelmon.items.medicine.MedicineItem");
        weatherTargets.add("com.pixelmonmod.pixelmon.items.medicine.MedicinePotion");
        weatherTargets.add("com.pixelmonmod.pixelmon.world.listener.UltraSpaceListener");
        weatherTargets.add("vectorwing.farmersdelight.setup.Configuration");
        weatherTargets.add("net.minecraft.entity.effect.LightningBoltEntity");
        weatherTargets.add("net.minecraft.world.gen.feature.LakesFeature");
        weatherTargets.add("net.minecraft.world.gen.feature.TreeFeature");
        weatherTargets.add("net.minecraft.world.gen.feature.ChorusPlantFeature");
        weatherTargets.add("net.minecraft.world.gen.feature.AbstractBigMushroomFeature");
        weatherTargets.add("net.minecraft.world.gen.feature.RandomPatchFeature");
        weatherTargets.add("net.minecraft.world.IBlockReader");
        weatherTargets.add("net.minecraft.item.crafting.RecipeManager");
        weatherTargets.add("net.minecraft.item.MusicDiscItem");
        weatherTargets.add("com.pixelmonmod.pixelmon.client.music.BattleMusic");
        weatherTargets.add("net.minecraft.entity.Entity");
        weatherTargets.add("net.minecraft.entity.player.PlayerEntity");
        weatherTargets.add("net.minecraft.entity.LivingEntity");
        weatherTargets.add("net.minecraft.network.play.ServerPlayNetHandler");
        weatherTargets.add("net.minecraft.client.entity.player.ClientPlayerEntity");
        weatherTargets.add("com.pixelmonmod.pixelmon.api.advancements.triggers.LegendaryCaptureTrigger$Instance");
        weatherTargets.add("com.pixelmonmod.pixelmon.api.pokemon.catching.CaptureMethods$1");
        weatherTargets.add("com.pixelmonmod.pixelmon.api.pokemon.catching.CaptureMethods$6");
        weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.screens.bag.ItemMenuButton");
        weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.screens.bag.ChooseBag");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.raids.RaidGovernor");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.controller.participants.RaidPixelmonParticipant");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant");
        for(String status:new String[]{"Freeze","Sunny","Rainy","EntryHazard"})weatherTargets.add("com.pixelmonmod.pixelmon.battles.status."+status);
        try(JarFile jar=new JarFile(args[0])) {
            String tooltipBall="com.pixelmonmod.pixelmon.items.PokeBallItem",greetingTrainer="com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer";
            TooltipFeedbackVerification.verify(jar,
                transformer.transformClassBytes(tooltipBall,tooltipBall,jar.getInputStream(jar.getJarEntry(tooltipBall.replace('.','/')+".class")).readAllBytes()),
                transformer.transformClassBytes(greetingTrainer,greetingTrainer,jar.getInputStream(jar.getJarEntry(greetingTrainer.replace('.','/')+".class")).readAllBytes()));
            java.util.Map<String,byte[]> questPatched=new java.util.LinkedHashMap<>();
            for(String target:new String[]{"com.pixelmonmod.pixelmon.quests.objectives.objectives.player.ItemObjective","com.pixelmonmod.pixelmon.quests.objectives.objectives.entity.NPCObjective","com.pixelmonmod.pixelmon.quests.objectives.Objective","com.pixelmonmod.pixelmon.quests.QuestProgress","com.pixelmonmod.pixelmon.quests.listeners.ItemListeners","com.pixelmonmod.pixelmon.quests.objectives.objectives.meta.DateObjective","com.pixelmonmod.pixelmon.quests.objectives.objectives.world.WorldTimeObjective","com.pixelmonmod.pixelmon.quests.objectives.objectives.world.StructureObjective"}) {
                verify(transformer,jar,target);
                questPatched.put(target,transformer.transformClassBytes(target,target,jar.getInputStream(jar.getJarEntry(target.replace('.','/')+".class")).readAllBytes()));
            }
            NpcQuestVerification.verify(jar,questPatched);
            String[] editorTargets={EditorChineseVerification.BASE,EditorChineseVerification.BALL,"com.pixelmonmod.pixelmon.client.gui.pokemoneditor.PokemonEditorAdvancedScreen","com.pixelmonmod.pixelmon.client.gui.widgets.TabCompleteTranslateableTextField"};
            byte[][] editorCode=new byte[4][];for(int i=0;i<editorTargets.length;i++){String target=editorTargets[i];verify(transformer,jar,target);editorCode[i]=transformer.transformClassBytes(target,target,jar.getInputStream(jar.getJarEntry(target.replace('.','/')+".class")).readAllBytes());}
            EditorChineseVerification.verify(jar,editorCode[0],editorCode[1],editorCode[2],editorCode[3]);
            verify(transformer,jar,"com.pixelmonmod.pixelmon.client.ClientProxy");
            String teleportProxy="com.pixelmonmod.pixelmon.client.ClientProxy",teleportChoose="com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.ChooseAttack";
            BattleTeleportVerification.verify(jar,
                transformer.transformClassBytes(teleportProxy,teleportProxy,jar.getInputStream(jar.getJarEntry(teleportProxy.replace('.','/')+".class")).readAllBytes()),
                transformer.transformClassBytes(teleportChoose,teleportChoose,jar.getInputStream(jar.getJarEntry(teleportChoose.replace('.','/')+".class")).readAllBytes()));
            weatherTargets.add("com.pixelmonmod.pixelmon.entities.pixelmon.interactions.InteractionTechnicalMove");weatherTargets.add("com.pixelmonmod.pixelmon.battles.controller.participants.TrainerParticipant");weatherTargets.add("com.pixelmonmod.pixelmon.battles.controller.ai.TacticalAI");
            weatherTargets.add("com.pixelmonmod.pixelmon.world.structure.type.GymStructure$Start");
            weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.zygarde.ZygardeCubeScreen");
            weatherTargets.add("com.pixelmonmod.pixelmon.client.render.tileEntities.RenderTileEntityArcChalice");
            weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.raids.RaidCatchScreen");
            weatherTargets.add("com.pixelmonmod.pixelmon.blocks.tileentity.ArcChaliceTileEntity");
            weatherTargets.add("com.pixelmonmod.pixelmon.api.events.legendary.ArceusEvent");
            weatherTargets.add("com.pixelmonmod.pixelmon.world.listener.DrownedWorldListener");
            weatherTargets.add("com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.logic.DuskBall");
            weatherTargets.add("com.pixelmonmod.pixelmon.api.pokemon.Pokemon");weatherTargets.add("com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity");
            weatherTargets.add("com.pixelmonmod.pixelmon.entities.pixelmon.helpers.EvolutionQuery");
            weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.moveskills.MoveSkillSelectScreen");
            weatherTargets.add("com.pixelmonmod.pixelmon.client.DefaultServerList");
            weatherTargets.add("com.pixelmonmod.pixelmon.entities.pixelmon.interactions.InteractionRainbowWing");
            weatherTargets.add("com.pixelmonmod.pixelmon.PixelmonEventHandler");
            weatherTargets.add("com.pixelmonmod.pixelmon.entities.pixelmon.interactions.InteractionZombie");
            weatherTargets.add("com.pixelmonmod.pixelmon.command.impl.CheckSpawnsCommand");
            weatherTargets.add("com.pixelmonmod.pixelmon.command.impl.EndBattleCommand");
            weatherTargets.add("com.pixelmonmod.pixelmon.api.advancements.triggers.SpecTrigger");
            weatherTargets.add("com.pixelmonmod.pixelmon.api.advancements.triggers.SpecTrigger$Instance");
            weatherTargets.add("com.pixelmonmod.pixelmon.battles.controller.ai.AdvancedAI");
            weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.override.multiplayer.CustomServerList");
            String raidPlayer="com.pixelmonmod.pixelmon.battles.raids.RaidData$RaidPlayer";
            RaidCaptureParityVerification.verify(jar.getInputStream(jar.getJarEntry(raidPlayer.replace('.','/')+".class")).readAllBytes(),
                transformer.transformClassBytes(raidPlayer,raidPlayer,jar.getInputStream(jar.getJarEntry(raidPlayer.replace('.','/')+".class")).readAllBytes()));
            for(String name:weatherTargets)verify(transformer,jar,name);
            SelfStatAiSnapshotVerification.verify(transformer,jar);
            for(String name:new String[]{"com.pixelmonmod.api.loader.DatapackLoader$Impl","com.pixelmonmod.pixelmon.client.models.smd.SmdAnimation","com.pixelmonmod.pixelmon.api.pokemon.species.palette.locator.ModelLocator","com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Sharpness","com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Bulletproof","com.pixelmonmod.pixelmon.items.TechnicalMoveItem","com.pixelmonmod.pixelmon.api.pokemon.species.moves.Moves","com.pixelmonmod.pixelmon.items.heldItems.BlankTechnicalMachineItem","com.pixelmonmod.pixelmon.api.registries.PixelmonItems","com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.BattleBond","com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.attackModifiers.MultipleHit","com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper","com.pixelmonmod.pixelmon.battles.attacks.Attack","com.pixelmonmod.pixelmon.battles.controller.BattleController","com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStats","com.pixelmonmod.pixelmon.api.pokemon.Pokemon","com.pixelmonmod.pixelmon.api.pokemon.stats.Moveset","com.pixelmonmod.pixelmon.api.registries.PixelmonForms","com.pixelmonmod.pixelmon.client.gui.battles.BattleScreen","com.pixelmonmod.pixelmon.client.gui.battles.pokemonOverlays.OpponentElement","com.pixelmonmod.pixelmon.client.gui.battles.pokemonOverlays.AllyElement","com.pixelmonmod.pixelmon.client.gui.battles.pokemonOverlays.RaidElement"}) {
                verify(transformer,jar,name);
            }
        }
        for(int i=1;i<args.length;i++)try(JarFile optional=new JarFile(args[i])){
            if(optional.getJarEntry("net/minecraft/world/gen/feature/template/Template.class")!=null){
                verify(transformer,optional,"net.minecraft.client.Minecraft");
                verify(transformer,optional,"net.minecraft.client.util.Splashes");
                verify(transformer,optional,"net.minecraft.client.gui.screen.MainMenuScreen");
                verify(transformer,optional,"net.minecraft.client.gui.screen.ConnectingScreen");
                verify(transformer,optional,"net.minecraft.world.gen.feature.template.Template");
                verify(transformer,optional,"net.minecraft.world.server.ServerChunkProvider");
                verify(transformer,optional,"net.minecraft.world.gen.ChunkGenerator");
            }
            if(optional.getJarEntry("pokelucky/Process.class")!=null)verify(transformer,optional,"pokelucky.Process");
            if(optional.getJarEntry("noobanidus/mods/lootr/entity/EntityTicker.class")!=null)verify(transformer,optional,"noobanidus.mods.lootr.entity.EntityTicker");
            if(optional.getJarEntry("noobanidus/mods/lootr/repack/shoulders/data/ShoulderList.class")!=null){
                verify(transformer,optional,"noobanidus.mods.lootr.repack.shoulders.data.ShoulderList");
                verifyLootrOfflineStartup(transformer,optional);
            }
            if(optional.getJarEntry("dev/ftb/mods/ftbquests/client/FTBQuestsClient.class")!=null)verify(transformer,optional,"dev.ftb.mods.ftbquests.client.FTBQuestsClient");
            if(optional.getJarEntry("dev/ftb/mods/ftbquests/quest/task/StructureTask.class")!=null)verify(transformer,optional,"dev.ftb.mods.ftbquests.quest.task.StructureTask");
            if(optional.getJarEntry("com/chaosthedude/naturescompass/util/BiomeUtils.class")!=null)verify(transformer,optional,"com.chaosthedude.naturescompass.util.BiomeUtils");
            if(optional.getJarEntry("snownee/jade/addon/vanilla/HorseProvider.class")!=null){
                verify(transformer,optional,"snownee.jade.addon.vanilla.HorseProvider");
                try(JarFile pixelmon=new JarFile(args[0])){
                    String base="com.pixelmonmod.pixelmon.entities.pixelmon.AbstractBaseEntity",horse="snownee.jade.addon.vanilla.HorseProvider";
                    byte[] before=pixelmon.getInputStream(pixelmon.getJarEntry(base.replace('.','/')+".class")).readAllBytes();
                    byte[] horseBefore=optional.getInputStream(optional.getJarEntry(horse.replace('.','/')+".class")).readAllBytes();
                    StatueDisplayVerification.verify(before,transformer.transformClassBytes(base,base,before),horseBefore,transformer.transformClassBytes(horse,horse,horseBefore));
                }
            }
            if(optional.getJarEntry("com/strangeone101/pixeltweaks/integration/jei/category/InfuserRecipeCategory.class")!=null)verify(transformer,optional,"com.strangeone101.pixeltweaks.integration.jei.category.InfuserRecipeCategory");
            if(optional.getJarEntry("pixelmondetector/marker/PKMarker.class")!=null){verify(transformer,optional,"pixelmondetector.marker.PKMarker");verify(transformer,optional,"pixelmondetector.entities.EntityFinder");verify(transformer,optional,"pixelmondetector.marker.MarkerManager");verify(transformer,optional,"pixelmondetector.config.PDConfig$Common");}
            if(optional.getJarEntry("com/mrcrayfish/configured/impl/ForgeValue.class")!=null){verify(transformer,optional,"com.mrcrayfish.configured.client.screen.EditListScreen");verify(transformer,optional,"com.mrcrayfish.configured.client.screen.EditListScreen$StringEntry");verify(transformer,optional,"com.mrcrayfish.configured.impl.ForgeValue");verify(transformer,optional,"com.mrcrayfish.configured.impl.ForgeFolderEntry");verify(transformer,optional,"com.mrcrayfish.configured.client.screen.ConfigScreen");verify(transformer,optional,"com.mrcrayfish.configured.client.screen.ConfigScreen$ConfigItem");verify(transformer,optional,"com.mrcrayfish.configured.client.screen.ModConfigSelectionScreen");}
            if(optional.getJarEntry("com/chaosthedude/explorerscompass/util/StructureSearchWorker.class")!=null){verify(transformer,optional,"com.chaosthedude.explorerscompass.util.StructureSearchWorker");verify(transformer,optional,"com.chaosthedude.explorerscompass/util/StructureUtils".replace('/','.'));}
        }
        verifyFieldClientSelection();
    }
    /** Run the real original/patched method against a deterministic offline URL handler. */
    static void verifyLootrOfflineStartup(IMixinTransformer transformer,JarFile jar)throws Exception{
        String name="noobanidus.mods.lootr.repack.shoulders.data.ShoulderList";
        byte[] original=jar.getInputStream(jar.getJarEntry(name.replace('.','/')+".class")).readAllBytes();
        byte[] patched=transformer.transformClassBytes(name,name,original);
        ClassNode originalNode=new ClassNode();new ClassReader(original).accept(originalNode,0);
        MethodNode load=originalNode.methods.stream().filter(m->m.name.equals("load")).findFirst().orElseThrow();
        if(!java.util.Arrays.stream(load.instructions.toArray()).anyMatch(i->i instanceof MethodInsnNode&&((MethodInsnNode)i).owner.equals("org/apache/commons/io/IOUtils")&&((MethodInsnNode)i).name.equals("toString")))throw new AssertionError("Native synchronous supporter download changed");
        // Keep this test independent of real DNS/network availability. The original
        // load blocks inside openConnection; the injected entry must never reach it.
        java.util.concurrent.CountDownLatch entered=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicInteger requests=new java.util.concurrent.atomic.AtomicInteger();
        java.net.URL.setURLStreamHandlerFactory(protocol->protocol.equals("https")?new java.net.URLStreamHandler(){
            protected java.net.URLConnection openConnection(java.net.URL url){
                requests.incrementAndGet();entered.countDown();
                try{release.await();}catch(InterruptedException ex){Thread.currentThread().interrupt();}
                return new java.net.URLConnection(url){public void connect(){}public java.io.InputStream getInputStream(){return new java.io.ByteArrayInputStream("[]".getBytes(java.nio.charset.StandardCharsets.UTF_8));}};
            }
        }:null);
        java.util.function.Function<byte[],Class<?>> define=bytes->new ClassLoader(MixinApplicationVerification.class.getClassLoader()){
            Class<?> defineNative(){return defineClass(name,bytes,0,bytes.length);}
        }.defineNative();
        Class<?> unpatched=define.apply(original),fixed=define.apply(patched);
        var originalFailure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread blocked=new Thread(()->{try{unpatched.getMethod("load").invoke(null);}catch(Throwable ex){originalFailure.set(ex);}},"offline-lootr-original");blocked.setDaemon(true);blocked.start();
        try{
            if(!entered.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("Original native downloader did not reach the blocking fixture",originalFailure.get());
            if(!blocked.isAlive())throw new AssertionError("Original native download unexpectedly completed before network release");
            int before=requests.get();
            var done=new java.util.concurrent.atomic.AtomicReference<Throwable>();
            Thread corrected=new Thread(()->{try{fixed.getMethod("load").invoke(null);}catch(Throwable ex){done.set(ex);}},"offline-lootr-fixed");corrected.setDaemon(true);corrected.start();corrected.join(2000);
            if(corrected.isAlive()||done.get()!=null||requests.get()!=before)throw new AssertionError("Patched native startup blocked or attempted a cosmetic download",done.get());
            var map=fixed.getDeclaredField("dataMap");map.setAccessible(true);
            if(!((java.util.Map<?,?>)map.get(null)).isEmpty())throw new AssertionError("Offline supporter map must remain empty");
        }finally{release.countDown();blocked.join(2000);}
        if(originalFailure.get()!=null)throw new AssertionError("Original fixture failed unexpectedly",originalFailure.get());
        System.out.println("PASS actual Lootr native supporter load blocks on unavailable HTTPS; transformed load returns without network request; empty cosmetic map is safe. No actual network or Minecraft world initialized.");
    }
    /** The default native setter calls a method stripped from a client distribution. */
    static void verifyFieldClientSelection()throws Exception{
        ClassNode node=new ClassNode();
        try(InputStream in=MixinApplicationVerification.class.getClassLoader().getResourceAsStream("local/pixelmon/backport/FieldMegaScreen.class")){
            new ClassReader(in.readAllBytes()).accept(node,0);
        }
        boolean explicitClient=false,packet=false;
        for(MethodNode method:node.methods)for(AbstractInsnNode instruction:method.instructions.toArray()){
            if(!(instruction instanceof MethodInsnNode))continue;
            MethodInsnNode call=(MethodInsnNode)instruction;
            if(!call.owner.equals("com/pixelmonmod/pixelmon/api/storage/PlayerPartyStorage"))continue;
            if(call.name.equals("sendClientUpdateSelectedPacket") || call.name.equals("setSelectedSlot") && call.desc.equals("(I)V"))
                throw new AssertionError("Client calls server-only selection path: "+method.name);
            if(call.name.equals("setSelectedSlot") && call.desc.equals("(IZ)V")){
                AbstractInsnNode value=call.getPrevious();while(value!=null && value.getOpcode()<0)value=value.getPrevious();
                if(value==null || value.getOpcode()!=org.objectweb.asm.Opcodes.ICONST_0)throw new AssertionError("Native client selection must suppress server-only broadcast");
                explicitClient=true;
            }
            if(call.name.equals("sendClientUpdatePacket"))packet=true;
        }
        if(!explicitClient || !packet)throw new AssertionError("Client selection missing local update or serverbound synchronization");
        System.out.println("PASS static field-wheel client-side linkage and selected-slot synchronization");
    }
    static void verifyFarmerConfigDefaults(String name,byte[] original,byte[] patched)throws Exception{
        java.util.function.Function<byte[],Class<?>> load=bytes->new ClassLoader(MixinApplicationVerification.class.getClassLoader()){
            Class<?> nativeClass(){return defineClass(name,bytes,0,bytes.length);}
        }.nativeClass();
        Class<?> before=load.apply(original),after=load.apply(patched);
        java.util.Set<String> keys=new java.util.HashSet<>();int values=0;
        for(var field:before.getFields())if(net.minecraftforge.common.ForgeConfigSpec.ConfigValue.class.isAssignableFrom(field.getType())){
            var oldValue=(net.minecraftforge.common.ForgeConfigSpec.ConfigValue<?>)field.get(null);
            var newValue=(net.minecraftforge.common.ForgeConfigSpec.ConfigValue<?>)after.getField(field.getName()).get(null);
            if(!oldValue.getPath().equals(newValue.getPath()))throw new AssertionError("Farmer config path changed "+field.getName());
            String specName=oldValue.getPath().get(0).equals("client")?"CLIENT_CONFIG":"COMMON_CONFIG";
            var oldSpec=(net.minecraftforge.common.ForgeConfigSpec)before.getField(specName).get(null);
            var newSpec=(net.minecraftforge.common.ForgeConfigSpec)after.getField(specName).get(null);
            var a=(net.minecraftforge.common.ForgeConfigSpec.ValueSpec)oldSpec.getRaw(oldValue.getPath());
            var b=(net.minecraftforge.common.ForgeConfigSpec.ValueSpec)newSpec.getRaw(newValue.getPath());
            if(!java.util.Objects.equals(a.getDefault(),b.getDefault())||!java.util.Objects.equals(String.valueOf(a.getRange()),String.valueOf(b.getRange()))||a.needsWorldRestart()!=b.needsWorldRestart())throw new AssertionError("Farmer config behavior changed "+field.getName());
            String expected="farmersdelight.config."+newValue.getPath().get(newValue.getPath().size()-1);
            if(!expected.equals(b.getTranslationKey()))throw new AssertionError("Farmer config translation missing "+field.getName());keys.add(expected);values++;
        }
        if(keys.size()!=25||values!=31)throw new AssertionError("Farmer config coverage changed "+values+"/"+keys.size());
        System.out.println("PASS actual native Farmer config initialization: 31 fields / 25 distinct labels, paths/defaults/ranges/restart flags unchanged.");
    }
    static void verify(IMixinTransformer transformer,JarFile jar,String name)throws Exception{
        byte[] original;
        var entry=jar.getJarEntry(name.replace('.','/')+".class");
        try(InputStream in=entry==null?MixinApplicationVerification.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class"):jar.getInputStream(entry)){if(in==null)throw new IllegalStateException("Missing native target "+name);original=in.readAllBytes();}
        byte[] patched=transformer.transformClassBytes(name,name,original);
        ClassNode node=new ClassNode();new ClassReader(patched).accept(node,0);int calls=0;
        if(name.equals("com.pixelmonmod.pixelmon.entities.pokeballs.EmptyPokeBallEntity")){
            var stored=node.methods.stream().filter(m->m.name.equals("storeCapture")).findFirst().orElseThrow();int hooks=0,reset=-1,lastHook=-1,index=0;
            for(var instruction:stored.instructions){if(instruction instanceof MethodInsnNode){var call=(MethodInsnNode)instruction;if(call.name.equals("setFriendship"))reset=index;if(call.name.contains("backport$friendship")){hooks++;lastHook=index;}}index++;}
            if(hooks==0||reset>=0&&lastHook<reset)throw new AssertionError("Cap friendship must be checked at native storage returns, after friendship reset");
        }
        if(name.equals("com.pixelmonmod.pixelmon.api.pokemon.Pokemon")){
            var palette=node.methods.stream().filter(m->m.name.equals("setPalette")&&m.desc.equals("(Ljava/lang/String;Z)Z")).findFirst().orElseThrow();
            boolean acquired=false;for(var instruction:palette.instructions)if(instruction instanceof MethodInsnNode)acquired|=((MethodInsnNode)instruction).name.contains("backport$paletteAcquired");
            if(!acquired)throw new AssertionError("All native palette setter overloads must reach acquisition queue");
        }
        if(name.equals("net.minecraft.client.gui.screen.MainMenuScreen")){
            var render=node.methods.stream().filter(m->m.name.equals("func_230430_a_")).findFirst().orElseThrow();
            boolean splash=false;int fade=0,period=0,divisor=0;
            for(var instruction:render.instructions){
                if(instruction instanceof FieldInsnNode&&((FieldInsnNode)instruction).name.equals("field_73975_c"))splash=true;
                if(instruction instanceof LdcInsnNode&&Float.valueOf(1000F).equals(((LdcInsnNode)instruction).cst)&&!splash)fade++;
                if(instruction instanceof MethodInsnNode){String method=((MethodInsnNode)instruction).name;
                    if(method.contains("backport$pulsePeriod")){if(!splash)throw new AssertionError("Pulse hook modified menu fade");period++;}
                    if(method.contains("backport$pulseDivisor")){if(!splash)throw new AssertionError("Pulse divisor modified menu fade");divisor++;}
                }
            }
            if(fade!=1||period!=1||divisor!=1)throw new AssertionError("Vanilla fade/pulse slice changed: "+fade+"/"+period+"/"+divisor);
            for(var method:node.methods)if(method.name.contains("backport$pulse")){
                boolean slow=false;for(var i:method.instructions)if(i instanceof LdcInsnNode)slow|=Long.valueOf(2000L).equals(((LdcInsnNode)i).cst)||Float.valueOf(2000F).equals(((LdcInsnNode)i).cst);
                if(!slow)throw new AssertionError("Splash pulse is not half speed");
            }
        }
        if(name.equals("com.pixelmonmod.pixelmon.api.storage.PCStorage")){
            var resize=node.methods.stream().filter(m->m.name.equals("backport$boxes")&&m.desc.equals("([Lcom/pixelmonmod/pixelmon/api/storage/PCBox;)V")).findFirst().orElseThrow();
            boolean assigns=false;for(var i:resize.instructions)if(i instanceof FieldInsnNode){var f=(FieldInsnNode)i;assigns|=f.getOpcode()==org.objectweb.asm.Opcodes.PUTFIELD&&f.name.equals("boxes");}
            if(!assigns)throw new AssertionError("PC cache resize accessor did not assign native boxes");calls++;
        }
        if(name.equals("com.pixelmonmod.pixelmon.client.gui.battles.ClientBattleManager")){
            java.nio.file.Path capture=java.nio.file.Path.of(".cache/fixes-162");java.nio.file.Files.createDirectories(capture);
            java.nio.file.Files.write(capture.resolve("original-client-battle-manager.class"),original);
            java.nio.file.Files.write(capture.resolve("patched-client-battle-manager.class"),patched);
        }
        if(name.equals("com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.screens.bag.ChooseBag")){
            var click=node.methods.stream().filter(m->m.name.equals("click")).findFirst().orElseThrow();int guard=-1,protectedCalls=0,index=0;
            for(var instruction:click.instructions){if(instruction instanceof MethodInsnNode){var call=(MethodInsnNode)instruction;
                if(call.name.contains("backport$recheckBall"))guard=index;
                if(call.name.equals("canCatchOpponent")||call.name.equals("getBagItems")||(call.owner.equals("java/util/ArrayList")&&call.name.equals("clear"))){
                    if(guard<0||guard>=index)throw new AssertionError("Unprotected capture category/list/reuse action "+call.name);
                    protectedCalls++;guard=-1;
                }
            }index++;}
            if(protectedCalls!=4)throw new AssertionError("Native capture category/list/reuse routes changed "+protectedCalls);
        }
        if(name.equals("com.pixelmonmod.pixelmon.entities.pixelmon.drops.DropItemQueryList")){
            int entries=0;for(MethodNode method:node.methods)if(method.name.equals("register")){
                boolean shared=false;for(AbstractInsnNode instruction:method.instructions)if(instruction instanceof MethodInsnNode&&((MethodInsnNode)instruction).name.contains("backport$sharedRewards"))shared=true;
                if(!shared)throw new AssertionError("Unfiltered reward register overload "+method.desc);entries++;
            }if(entries!=2)throw new AssertionError("Native reward register overload count changed");
        }
        if(name.equals("com.pixelmonmod.pixelmon.api.pokemon.boss.DropSet")){
            int entries=0;for(MethodNode method:node.methods)if(method.name.equals("<init>")){
                boolean cleaned=false;for(AbstractInsnNode instruction:method.instructions)if(instruction instanceof MethodInsnNode&&((MethodInsnNode)instruction).name.contains("backport$loaded"))cleaned=true;
                if(!cleaned)throw new AssertionError("Unfiltered reward constructor "+method.desc);entries++;
            }if(entries!=3)throw new AssertionError("Native DropSet constructor count changed");
        }
        for(MethodNode method:node.methods){
            new Analyzer<>(new BasicVerifier()).analyze(node.name,method);
            for(AbstractInsnNode instruction:method.instructions.toArray())if(instruction instanceof MethodInsnNode){
                MethodInsnNode call=(MethodInsnNode)instruction;
                if(call.name.contains("backport$") || (name.endsWith(".CaptureMethod") && method.name.equals("calculateBallBonus") && call.owner.equals("local/pixelmon/backport/BurstCapture") && call.name.equals("bonus")))++calls;
            }
        }
        if(name.equals("net.minecraft.world.server.ServerChunkProvider")){
            boolean request=false;
            for(MethodNode m:node.methods)if(m.name.equals("backport$requestChunk"))for(AbstractInsnNode i:m.instructions)
                if(i instanceof MethodInsnNode && ((MethodInsnNode)i).name.equals("func_217233_c"))request=true;
            if(!request || !node.interfaces.contains("local/pixelmon/backport/mixin/CompassChunkRequestInvoker"))throw new AssertionError("Missing nonblocking chunk request invoker");
            ++calls;
        }
        if(name.equals("net.minecraft.world.gen.ChunkGenerator")){
            boolean ring=false,init=false;
            for(MethodNode m:node.methods)for(AbstractInsnNode i:m.instructions){
                if(m.name.equals("backport$strongholdRings")&&i instanceof FieldInsnNode&&((FieldInsnNode)i).name.equals("field_235951_f_"))ring=true;
                if(m.name.equals("backport$initializeStrongholds")&&i instanceof MethodInsnNode&&((MethodInsnNode)i).name.equals("func_235958_g_"))init=true;
            }
            if(!ring||!init)throw new AssertionError("Native stronghold ring accessor/init missing");++calls;
        }
        if(name.equals("com.pixelmonmod.pixelmon.quests.QuestProgress")){
            if(!node.interfaces.contains("local/pixelmon/backport/mixin/NpcQuestProgressAccess")
                    ||node.methods.stream().filter(m->m.name.equals("backport$remaining")||m.name.equals("backport$totals")).count()!=2)
                throw new AssertionError("Native quest counter accessors missing");
            ++calls;
        }
        if(name.equals("com.pixelmonmod.pixelmon.client.models.smd.DeformVertex")){
            boolean rest=false;
            for(MethodNode m:node.methods)if(m.name.equals("backport$restPosition"))for(AbstractInsnNode instruction:m.instructions)
                if(instruction instanceof FieldInsnNode && ((FieldInsnNode)instruction).name.equals("baseLoc"))rest=true;
            if(!rest || !node.interfaces.contains("local/pixelmon/backport/mixin/TeraVertexAccess"))throw new AssertionError("Missing immutable rest-position accessor");
            ++calls;
        }
        if(name.equals("com.pixelmonmod.pixelmon.client.models.smd.Bone")){
            boolean before=false,reset=false;
            for(MethodNode method:node.methods)if(method.name.equals("reset"))for(AbstractInsnNode instruction:method.instructions){
                if(instruction instanceof MethodInsnNode){MethodInsnNode call=(MethodInsnNode)instruction;
                    if(call.name.contains("backport$rememberPose"))before=true;
                    if(call.name.equals("setIdentity")){if(!before)throw new AssertionError("Head pose captured after native reset");reset=true;}
                }
            }
            if(!before || !reset || !node.interfaces.contains("local/pixelmon/backport/client/TeraBonePose"))throw new AssertionError("Missing actual skinning-pose capture before reset");
        }
        if(name.equals("com.pixelmonmod.pixelmon.api.pokemon.LearnMoveController")){
            boolean accessor=false;for(MethodNode m:node.methods)if(m.desc.equals("()Lcom/google/common/collect/ListMultimap;"))for(AbstractInsnNode i:m.instructions)if(i instanceof FieldInsnNode&&((FieldInsnNode)i).name.equals("userMap"))accessor=true;
            if(!accessor)throw new AssertionError("Missing native pending learn accessor: "+node.methods.stream().map(m->m.name+m.desc).collect(java.util.stream.Collectors.joining(",")));
            ++calls;
        }
        if(name.endsWith(".ClientInitializePCPacket")){
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(".cache/fixes-170"));
            java.nio.file.Files.write(java.nio.file.Path.of(".cache/fixes-170/original-pcinit.class"),original);
            java.nio.file.Files.write(java.nio.file.Path.of(".cache/fixes-170/patched-pcinit.class"),patched);
        }
        if(name.equals("com.pixelmonmod.pixelmon.comm.packetHandlers.clientStorage.newStorage.ClientSetPacket")){
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(".cache/feedback-161"));
            java.nio.file.Files.write(java.nio.file.Path.of(".cache/feedback-161/original-clientset.class"),original);
            java.nio.file.Files.write(java.nio.file.Path.of(".cache/feedback-161/patched-clientset.class"),patched);
        }
        if(name.equals("com.pixelmonmod.pixelmon.api.pokemon.species.moves.Moves")){
            java.nio.file.Files.write(java.nio.file.Path.of(".cache/feedback-161/original-moves.class"),original);
            java.nio.file.Files.write(java.nio.file.Path.of(".cache/feedback-161/patched-moves.class"),patched);
        }
        if(name.equals("vectorwing.farmersdelight.setup.Configuration"))verifyFarmerConfigDefaults(name,original,patched);
        if(name.equals("com.pixelmonmod.pixelmon.world.listener.UltraSpaceListener")){
            var m=node.methods.stream().filter(x->x.name.equals("onBlockBreak")).findFirst().orElseThrow();
            long hits=java.util.Arrays.stream(m.instructions.toArray()).filter(i->i instanceof MethodInsnNode&&((MethodInsnNode)i).name.contains("backport$markMiningBolt")).count();
            if(hits!=1)throw new AssertionError("Native mining bolt must be tagged exactly once");
        }
        if(name.equals("net.minecraft.entity.effect.LightningBoltEntity")){
            var m=node.methods.stream().filter(x->x.name.equals("func_70071_h_")).findFirst().orElseThrow();
            int forge=-1,hook=-1,pos=0;for(var i:m.instructions){if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;if(c.name.equals("onEntityStruckByLightning"))forge=pos;if(c.name.contains("backport$afterActualStrike"))hook=pos;}pos++;}
            if(forge<0||hook<=forge)throw new AssertionError("Canceled Forge strike could award advancement");
            var wrapper=node.methods.stream().filter(x->x.name.contains("backport$afterActualStrike")).findFirst().orElseThrow();
            int nativeHit=-1,award=-1;pos=0;for(var i:wrapper.instructions){if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;if(c.name.equals("func_241841_a"))nativeHit=pos;if(c.owner.equals("local/pixelmon/backport/UltraPlantAchievement")&&c.name.equals("struck"))award=pos;}pos++;}
            if(nativeHit<0||award<=nativeHit)throw new AssertionError("Award precedes actual strike callback");
        }
        if(name.equals("com.pixelmonmod.pixelmon.battles.controller.ai.BattleAIBase")){
            boolean bridge=false;for(MethodNode m:node.methods)if(m.name.equals("backport$damage"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode&&((MethodInsnNode)i).name.equals("weightOffensiveMove")&&((MethodInsnNode)i).desc.equals("(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;Lcom/pixelmonmod/pixelmon/battles/controller/ai/MoveChoice;)V"))bridge=true;
            if(!bridge)throw new AssertionError("Exact submitted move must reach native damage weighting");
        }
        if(name.equals("com.pixelmonmod.pixelmon.items.medicine.MedicineItem")||name.equals("com.pixelmonmod.pixelmon.items.medicine.MedicinePotion")){
            String method=name.endsWith("MedicineItem")?"backport$methods":"backport$healing",field=name.endsWith("MedicineItem")?"healMethods":"healHP";
            boolean found=false;for(MethodNode m:node.methods)if(m.name.equals(method))for(AbstractInsnNode i:m.instructions)if(i instanceof FieldInsnNode&&i.getOpcode()==org.objectweb.asm.Opcodes.GETFIELD&&((FieldInsnNode)i).name.equals(field))found=true;
            if(!found)throw new AssertionError("Native read-only healing accessor missing "+name);++calls;
        }
        if(calls<1)throw new AssertionError("No injected hook "+name);
        if(name.equals("net.minecraft.world.gen.feature.template.Template")){
            boolean placed=false,converted=false;
            for(MethodNode m:node.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;
                if(!m.name.equals("func_237145_a_")&&c.name.equals("processBlockInfos"))placed=true;
                if(m.name.equals("processBlockInfos")&&c.name.contains("backport$naturalStops"))converted=true;
            }
            if(!placed||!converted)throw new AssertionError("Supply conversion does not hook the actual Forge placement route");
        }
        if(name.equals("noobanidus.mods.lootr.entity.EntityTicker")){
            boolean cleanup=false,now=false,add=false,oldRead=false;
            for(MethodNode m:node.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;
                cleanup|=c.name.equals("discardRetired");now|=c.name.equals("func_225313_a");add|=c.name.equals("func_217376_c");oldRead|=c.name.equals("func_212849_a_");
            }
            if(!cleanup||!now||!add||oldRead)throw new AssertionError("Lootr must retire worlds and read loaded chunks, keeping insertion without blocking");
        }
        if(name.equals("com.pixelmonmod.pixelmon.battles.attacks.Attack")){
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(".cache/battle-audit-151"));
            java.nio.file.Files.write(java.nio.file.Path.of(".cache/battle-audit-151/transformed-attack.class"),patched);
            boolean rounding=false,weather=false;
            for(MethodNode m:node.methods)for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;
                rounding|=m.name.equals("doDamageCalc")&&c.name.contains("backport$roundedDamage");
                weather|=m.name.equals("modifyMoveStats")&&c.name.contains("backport$deferWeather");}
            if(!rounding||!weather)throw new AssertionError("Damage rounding and weather stage adapter not injected");
            for(MethodNode m:node.methods)if(m.name.equals("modifyMoveStats")){
                int nativeAbility=-1,boost=-1,pos=0;
                for(AbstractInsnNode i:m.instructions){if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;
                    if((c.name.equals("modifyPowerAndAccuracyUser")&&c.owner.endsWith("/Ability"))||c.name.contains("backport$ashBasePower"))nativeAbility=pos;
                    if(c.name.contains("backport$power"))boost=pos;}pos++;}
                if(nativeAbility<0||boost<=nativeAbility)throw new AssertionError("Water boost applied before native Battle Bond power adjustment");
            }
            boolean retainedNativeAbility=false;
            for(MethodNode m:node.methods)if(m.name.contains("backport$ashBasePower"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;retainedNativeAbility|=c.name.equals("modifyPowerAndAccuracyUser")&&c.owner.endsWith("/Ability");}
            if(!retainedNativeAbility)throw new AssertionError("Ash Mega base power adapter lost native ability callback");
        }
        if(name.equals("com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper")){
            boolean sourceHook=false;
            for(MethodNode m:node.methods)if(m.name.equals("addStatus"))for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode)sourceHook|=((MethodInsnNode)i).name.contains("backport$statusSource");
            if(!sourceHook)throw new AssertionError("Successful status source hook not injected");
            for(MethodNode m:node.methods)if(m.name.equals("useAttack")&&m.desc.equals("(Z)V")){
                int boost=-1,abilityStart=-1,retarget=-1,erosion=-1,attackUse=-1,pos=0;
                for(AbstractInsnNode i:m.instructions){if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;
                    if(c.name.contains("backport$ashWaterBeforeAttack"))boost=pos;
                    if(c.name.equals("startMove")&&c.owner.endsWith("/Ability"))abilityStart=pos;}pos++;}
                if(boost<0||abilityStart<=boost)throw new AssertionError("Water boost starts after the attack begins");
                pos=0;for(AbstractInsnNode i:m.instructions){if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;if(c.name.equals("manageTargetingFaintedPokemon"))retarget=pos;if(c.name.contains("backport$erosionTarget"))erosion=pos;if(c.name.equals("use")&&c.owner.endsWith("/Attack"))attackUse=pos;}pos++;}
                if(retarget<0||erosion<=retarget||attackUse<=erosion)throw new AssertionError("Erosion must recompute the actual target before damage");
            }
            for(MethodNode m:node.methods)if(m.name.contains("backport$erosionTarget")){int apply=-1,message=-1,pos=0;for(AbstractInsnNode i:m.instructions){if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;if(c.owner.endsWith("/Erosion")&&c.name.equals("start"))apply=pos;if(c.name.equals("sendToAll"))message=pos;if(c.name.equals("turnTick")||c.name.equals("endTurn"))throw new AssertionError("Erosion refresh advances a turn");}pos++;}if(apply<0||message<=apply)throw new AssertionError("Erosion announcement must precede the used-move message");}
        }
        if(name.equals("com.pixelmonmod.pixelmon.client.DefaultServerList"))MultiplayerRegressionVerification.executePatched(patched);
        System.out.println("PASS transformed and verified "+name);
    }
}
