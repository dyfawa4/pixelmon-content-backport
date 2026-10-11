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
        for(String ability:new String[]{"Chlorophyll","SwiftSwim","SolarPower","DrySkin","RainDish","Hydration","LeafGuard","Forecast","FlowerGift"})weatherTargets.add("com.pixelmonmod.pixelmon.api.pokemon.ability.abilities."+ability);
        for(String effect:new String[]{"WeatherBall","Synthesis","Growth","HydroSteam"})weatherTargets.add("com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic."+effect);
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.multiTurn.SolarBeam");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.attackModifiers.AlwaysHit");
        weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.ChooseAttack");
        weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.ChooseAttack$MoveButton");
        weatherTargets.add("com.pixelmonmod.pixelmon.container.PokeBagContainer");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.raids.RaidData");
        weatherTargets.add("com.pixelmonmod.pixelmon.items.SpawnDenItem");
        weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.raids.RaidStartScreen");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.raids.RaidData$RaidPlayer");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.raids.RaidGovernor");
        weatherTargets.add("com.pixelmonmod.pixelmon.battles.controller.participants.RaidPixelmonParticipant");
        for(String status:new String[]{"Freeze","Sunny","Rainy","EntryHazard"})weatherTargets.add("com.pixelmonmod.pixelmon.battles.status."+status);
        try(JarFile jar=new JarFile(args[0])) {
            weatherTargets.add("com.pixelmonmod.pixelmon.entities.pixelmon.interactions.InteractionTechnicalMove");weatherTargets.add("com.pixelmonmod.pixelmon.battles.controller.participants.TrainerParticipant");weatherTargets.add("com.pixelmonmod.pixelmon.battles.controller.ai.TacticalAI");
            weatherTargets.add("com.pixelmonmod.pixelmon.world.structure.type.GymStructure$Start");
            weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.zygarde.ZygardeCubeScreen");
            weatherTargets.add("com.pixelmonmod.pixelmon.client.render.tileEntities.RenderTileEntityArcChalice");
            weatherTargets.add("com.pixelmonmod.pixelmon.blocks.tileentity.ArcChaliceTileEntity");
            weatherTargets.add("com.pixelmonmod.pixelmon.api.events.legendary.ArceusEvent");
            weatherTargets.add("com.pixelmonmod.pixelmon.api.pokemon.Pokemon");weatherTargets.add("com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity");
            weatherTargets.add("com.pixelmonmod.pixelmon.client.gui.moveskills.MoveSkillSelectScreen");
            for(String name:weatherTargets)verify(transformer,jar,name);
            for(String name:new String[]{"com.pixelmonmod.api.loader.DatapackLoader$Impl","com.pixelmonmod.pixelmon.client.models.smd.SmdAnimation","com.pixelmonmod.pixelmon.api.pokemon.species.palette.locator.ModelLocator","com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Sharpness","com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Bulletproof","com.pixelmonmod.pixelmon.items.TechnicalMoveItem","com.pixelmonmod.pixelmon.api.pokemon.species.moves.Moves","com.pixelmonmod.pixelmon.items.heldItems.BlankTechnicalMachineItem","com.pixelmonmod.pixelmon.api.registries.PixelmonItems","com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.BattleBond","com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.attackModifiers.MultipleHit","com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper","com.pixelmonmod.pixelmon.battles.attacks.Attack","com.pixelmonmod.pixelmon.battles.controller.BattleController","com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStats","com.pixelmonmod.pixelmon.api.registries.PixelmonForms","com.pixelmonmod.pixelmon.client.gui.battles.BattleScreen"}) {
                verify(transformer,jar,name);
            }
        }
        for(int i=1;i<args.length;i++)try(JarFile optional=new JarFile(args[i])){
            if(optional.getJarEntry("pixelmondetector/marker/PKMarker.class")!=null){verify(transformer,optional,"pixelmondetector.marker.PKMarker");verify(transformer,optional,"pixelmondetector.entities.EntityFinder");}
            if(optional.getJarEntry("com/chaosthedude/explorerscompass/util/StructureSearchWorker.class")!=null){verify(transformer,optional,"com.chaosthedude.explorerscompass.util.StructureSearchWorker");verify(transformer,optional,"com.chaosthedude.explorerscompass/util/StructureUtils".replace('/','.'));}
        }
        verifyFieldClientSelection();
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
    static void verify(IMixinTransformer transformer,JarFile jar,String name)throws Exception{
        byte[] original;
        try(InputStream in=jar.getInputStream(jar.getJarEntry(name.replace('.','/')+".class"))){original=in.readAllBytes();}
        byte[] patched=transformer.transformClassBytes(name,name,original);
        ClassNode node=new ClassNode();new ClassReader(patched).accept(node,0);int calls=0;
        for(MethodNode method:node.methods){
            new Analyzer<>(new BasicVerifier()).analyze(node.name,method);
            for(AbstractInsnNode instruction:method.instructions.toArray())if(instruction instanceof MethodInsnNode && ((MethodInsnNode)instruction).name.contains("backport$"))++calls;
        }
        if(calls<1)throw new AssertionError("No injected hook "+name);
        System.out.println("PASS transformed and verified "+name);
    }
}
