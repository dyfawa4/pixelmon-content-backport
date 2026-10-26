package qa;
import java.util.*;
import com.pixelmonmod.pixelmon.entities.npcs.*;
import com.pixelmonmod.pixelmon.entities.npcs.registry.*;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import local.pixelmon.backport.*;
import local.pixelmon.backport.client.NpcNameplates;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
public final class Update180TrainerDisplayCases {
    static final List<Integer> ids=new ArrayList<>();static final List<String> rawNames=new ArrayList<>();
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void setup(ServerPlayerEntity player){
        var world=player.func_71121_q();world.func_241114_a_(6000);for(int x=30;x<=47;x++)for(int z=35;z<=47;z++)world.func_180501_a(new BlockPos(x,73,z),net.minecraft.block.Blocks.field_150426_aN.func_176223_P(),2);
        for(int i=0;i<4;i++){var npc=new NPCTrainer(world);npc.init(ServerNPCRegistry.trainers.getRandomBaseWithData());npc.setName("0");npc.usingDefaultName=true;for(int j=0;j<6;j++)npc.getPokemonStorage().set(j,null);npc.getPokemonStorage().set(0,Update180Cases.mon(25,"Thunderbolt"));npc.getPokemonStorage().get(0).setLevelNum(52);npc.getPokemonStorage().set(1,Update180Cases.mon(6,"Flamethrower"));npc.getPokemonStorage().get(1).setLevelNum(72);npc.updateTrainerLevel();
            check(npc.getTrainerLevel()==62,"Native displayed level = actual party mean");npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss(i==1?"equal":"notboss"));if(i==2)npc.setNickName("自定义训练家");if(i==3)npc.func_200203_b(new StringTextComponent("自定义名字"));npc.func_70012_b(34+i*3,74,39,0,0);npc.setAIMode(com.pixelmonmod.pixelmon.enums.EnumTrainerAI.StandStill);npc.func_110163_bv();check(world.func_217376_c(npc),"Spawn trainer display fixture");ids.add(npc.getNPCID());rawNames.add(npc.func_200200_C_().getString());System.out.println("TRAINER_DISPLAY_SERVER id="+npc.getNPCID()+" raw="+npc.func_200200_C_().getString()+" localized="+npc.getName("zh_cn")+" cachedLevel="+npc.getTrainerLevel()+" tier="+npc.getBossTier().getID());}
        player.func_200619_a(world,34,74,44,180,0);System.out.println("TRAINER_DISPLAY_SERVER_PASS indexed native identity retained; native levels52/72 and synchronized mean62; ordinary, equal, nickname and custom fixtures");
    }
    public static void client(int age)throws Exception{
        var mc=net.minecraft.client.Minecraft.func_71410_x();if(age==35){for(int i=0;i<ids.size();i++){var npc=(NPCTrainer)mc.field_71441_e.func_73045_a(ids.get(i));check(npc!=null,"Real networked trainer loaded");check(npc.getTrainerLevel()==62,"Native level synchronized to client");check(npc.func_200200_C_().getString().equals(rawNames.get(i)),"Do not rewrite name-list index in entity data");String name=NpcNameplates.name(npc),level=NpcNameplates.level(npc);check(!name.equals("0")&&!name.isBlank(),"No raw zero/name index");check(level.equals(i==1?"等级随队伍调整":"等级 62"),"Correct level/dynamic text "+level);if(i==2)check(name.equals("自定义训练家"),"Nickname preserved");if(i==3)check(name.equals("自定义名字"),"Custom name preserved");String jade=(String)Class.forName("mcp.mobius.waila.addons.core.HUDHandlerEntities").getMethod("getEntityName",net.minecraft.entity.Entity.class).invoke(null,npc);check(jade.equals(NpcNameplates.tooltip(npc)),"Actual Jade entry uses proper label");System.out.println("TRAINER_DISPLAY_CLIENT id="+ids.get(i)+" raw="+npc.func_200200_C_().getString()+" name="+name+" level="+level+" jade="+jade);}
            var literal=new NPCTrainer(mc.field_71441_e);literal.setName("123");literal.usingDefaultName=false;check(NpcNameplates.name(literal).equals("123"),"Literal custom numeric names not treated as indices");var blank=new NPCTrainer(mc.field_71441_e);blank.setName("0");check(NpcNameplates.level(blank).isBlank(),"Unsynchronized level never shown as0");System.out.println("TRAINER_DISPLAY_CLIENT_PASS native synced level, default index resolution/fallback, real Jade provider, custom names, numeric custom names and unknown-level guard");}
        if(age==45)Update180R9Cases.shot("qa-trainer-display.png");if(age>60)mc.func_71400_g();
    }
}
