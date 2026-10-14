import java.util.*;
import java.lang.reflect.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import local.pixelmon.backport.NaturalPokeStops;

/** Probability, native API contracts and acquisition boundaries without starting a game. */
public final class V199FinishVerification {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static MethodNode method(String owner,String name)throws Exception{return Version13StaticVerification.node(owner).methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();}
    static boolean call(MethodNode m,String owner,String name){for(AbstractInsnNode i:m.instructions)if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;if(c.owner.equals(owner)&&c.name.equals(name))return true;}return false;}
    static Class<?> type(String name)throws Exception{return Class.forName(name,false,V199FinishVerification.class.getClassLoader());}
    public static void main(String[] args)throws Exception{
        int[] counts=new int[3];Random positions=new Random(141);
        for(int i=0;i<100000;i++){long pos=positions.nextLong();int n=NaturalPokeStops.tier(9128374,pos);check(n>=0&&n<=2,"Invalid supply tier");check(n==NaturalPokeStops.tier(9128374,pos),"Chunk revisit rerolls supply tier");counts[n]++;}
        for(int i=0;i<3;i++)check(Math.abs(counts[i]/100000.0-new double[]{.6,.3,.1}[i])<.008,"Incorrect supply probability "+Arrays.toString(counts));
        MethodNode convert=method("local/pixelmon/backport/NaturalPokeStops","convert");
        boolean worldGuard=false,commonGuard=false,copy=false,facing=false,loot=false,color=false;
        for(AbstractInsnNode i:convert.instructions){
            if(i instanceof TypeInsnNode&&i.getOpcode()==Opcodes.INSTANCEOF)worldGuard|=((TypeInsnNode)i).desc.equals("net/minecraft/world/gen/WorldGenRegion");
            if(i instanceof FieldInsnNode)commonGuard|=((FieldInsnNode)i).name.equals("common_pokestop");
            if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;copy|=c.name.equals("func_74737_b");facing|=c.name.equals("func_206870_a");loot|=c.name.equals("getLootTable");color|=c.name.equals("getColor");check(!c.name.equals("setBlock")&&!c.name.equals("func_180501_a"),"Supply conversion mutates live placed blocks");}
        }
        check(worldGuard&&commonGuard&&copy&&facing&&loot&&color,"Supply placement/NBT/orientation boundaries missing");
        check(call(convert,"local/pixelmon/backport/NaturalPokeStops","tier"),"Production conversion bypasses tested tier selector");
        check("栎树林祠堂".equals(local.pixelmon.backport.CompassNames.lookup("structure","pixelmon:ilex_shrine")),"Shrine translation mismatch");
        MethodNode capture=method("local/pixelmon/backport/LegendAdvancements","capture");int guard=-1,bond=-1,award=-1,index=0;
        for(AbstractInsnNode i:capture.instructions){if(i instanceof MethodInsnNode){var c=(MethodInsnNode)i;if(c.owner.endsWith("AcquisitionAdvancements")&&c.name.equals("ownedStored"))guard=index;if(c.owner.endsWith("LegendAdvancements")&&c.name.equals("award")&&award<0)award=index;}
            if(i instanceof TypeInsnNode&&i.getOpcode()==Opcodes.INSTANCEOF&&((TypeInsnNode)i).desc.endsWith("/BattleBond"))bond=index;index++;}
        check(guard>=0&&bond>guard&&award>bond,"Bond achievement bypasses actual stored ownership/ability check");
        var advancement=AshWaterBoostVerification.json("data/pixelmon_backport/advancements/legends/capture_658_bond.json").getAsJsonObject();
        check(advancement.getAsJsonObject("criteria").getAsJsonObject("earned").get("trigger").getAsString().equals("minecraft:impossible"),"Generic species capture unlocks Bond achievement");
        MethodNode display=method("local/pixelmon/backport/mixin/CapSpawnChanceDisplayMixin","backport$capChance");
        check(call(display,"net/minecraft/util/text/TranslationTextComponent","getString"),"Translated query candidate not recognized");
        // Check the actual installed quest classes against every reflective signature.
        Class<?> file=type("dev.ftb.mods.ftbquests.quest.QuestFile"),team=type("dev.ftb.mods.ftbquests.quest.TeamData"),player=type("net.minecraft.entity.player.ServerPlayerEntity");
        file.getMethod("getData",type("net.minecraft.entity.Entity"));file.getMethod("getAllData");
        type("dev.ftb.mods.ftbquests.quest.ServerQuestFile").getField("INSTANCE");
        type("dev.ftb.mods.ftbquests.net.SyncQuestsMessage").getConstructor(file);
        type("dev.ftb.mods.ftbquests.net.SyncQuestsMessage").getMethod("sendTo",player);
        type("dev.ftb.mods.ftbquests.net.SyncTeamDataMessage").getConstructor(team,boolean.class);
        type("dev.ftb.mods.ftbquests.net.SyncTeamDataMessage").getMethod("sendTo",player);
        Class<?> client=type("dev.ftb.mods.ftbquests.client.ClientQuestFile");client.getMethod("exists");client.getField("INSTANCE");client.getField("self");team.getField("name");
        type("dev.ftb.mods.ftbquests.FTBQuests").getField("PROXY");type("dev.ftb.mods.ftbquests.client.FTBQuestsClient").getMethod("openGui");
        MethodNode request=method("local/pixelmon/backport/QuestSyncRecovery$Request","handle");
        check(call(request,"net/minecraftforge/fml/network/NetworkEvent$Context","enqueueWork")&&call(request,"net/minecraftforge/fml/network/NetworkEvent$Context","getSender"),"Quest recovery not authenticated/on server thread");
        for(AbstractInsnNode i:method("local/pixelmon/backport/QuestSyncRecovery","resend").instructions)if(i instanceof LdcInsnNode&&((LdcInsnNode)i).cst instanceof String){String s=(String)((LdcInsnNode)i).cst;check(!s.equals("playerLoggedIn")&&!s.equals("reset")&&!s.equals("reward"),"Resync replays login or changes rewards");}
        check(call(method("local/pixelmon/backport/client/QuestRecoveryClient","request"),"net/minecraftforge/fml/network/simple/SimpleChannel","isRemotePresent"),"Missing server cannot fall back to native warning");
        System.out.println("PASS V199 probability sample common/rare/legendary="+Arrays.toString(counts)+"; stable placement and original NBT boundaries; translated Cap query/shrine; owned Battle Bond achievement; installed quest reflection API, authenticated main-thread recovery and no login/reward replay. Gameplay remains manual.");
    }
}
