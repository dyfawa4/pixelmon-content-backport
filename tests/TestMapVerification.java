import java.util.*;
import com.mojang.authlib.GameProfile;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.BattleBond;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import local.pixelmon.backport.PermanentAsh;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.*;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.util.ResourceLocation;

/** Executes map setup and actions against real registered blocks and native battles. */
public final class TestMapVerification {
    static void require(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    public static void run(MinecraftServer server){
        ServerWorld world=server.func_71218_a(World.field_234918_g_);UUID uuid=UUID.fromString("93ac49bc-6e3a-41ee-aa57-e449b4d8b028");FakePlayer player=FakePlayerFactory.get(world,new GameProfile(uuid,"MapFixtureTest"));
        Map<UUID,ServerPlayerEntity> players=ObfuscationReflectionHelper.getPrivateValue(PlayerList.class,server.func_184103_al(),"field_177454_f");players.put(uuid,player);world.func_217369_A().add(player);
        try{
            for(String mode:new String[]{"eternatus","interactions"}){
                TestMaps.mode=mode;player.getPersistentData().func_82580_o(DedicatedTestMaps.marker());DedicatedTestMaps.setup(player);
                require(world.func_180495_p(new BlockPos(0,64,11)).func_177230_c()==ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:smooth_quartz")),"Spawn outside platform "+mode);
                require(player.func_226278_cu_()==65,"Player spawn height "+mode);
                require(world.func_175625_s(new BlockPos(8,65,-10))!=null,"Missing actual healer "+mode);
                require(world.func_175625_s(new BlockPos(-8,65,-10))!=null,"Missing actual PC "+mode);
                require(TestMaps.stations.size()==(mode.equals("eternatus")?6:10),"Missing/colliding map stations "+mode);
                Pokemon first=StorageProxy.getParty(player).get(0);
                require(first!=null && first.getSpecies().getDex()==(mode.equals("eternatus")?890:1018),"Wrong initial party "+mode);
                BlockPos barrelPos=new BlockPos(mode.equals("eternatus")?-16:-18,65,9);IInventory barrel=(IInventory)world.func_175625_s(barrelPos);barrel.func_70299_a(0,ItemStack.field_190927_a);
                if(mode.equals("eternatus"))first.setHeldItem(DedicatedTestMaps.stack("pixelmon_backport:eternatus_meteorite",1));
                first.setHealth(1);DedicatedTestMaps.setup(player);
                require(StorageProxy.getParty(player).get(0).getUUID().equals(first.getUUID()) && first.getHealth()==1,"Reload resets party/HP "+mode);
                require(barrel.func_70301_a(0).func_190926_b(),"Reload refills consumed map materials "+mode);
                if(mode.equals("eternatus"))require(first.getForm().isForm("eternamax"),"Reload removes manually equipped form");
                DedicatedTestMaps.action(player,mode.equals("eternatus")?"eternabattle":"interactionbattle");require(BattleRegistry.getBattle(player)!=null,"Map battle failed "+mode);TestMaps.end(player);
                require(StorageProxy.getParty(player).get(0).getUUID().equals(first.getUUID()),"Battle replaces user's tested Pokemon "+mode);
                if(mode.equals("interactions")){
                    require(world.func_180495_p(new BlockPos(11,64,2)).func_177230_c()==ForgeRegistries.BLOCKS.getValue(new ResourceLocation("minecraft:water")),"Missing real pond");
                    require(world.func_180495_p(new BlockPos(-11,65,-10)).func_177230_c()==ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pixelmon:black_day_care")),"Missing native daycare");
                    for(String action:new String[]{"breedteam","evolveteam","starterteam","careteam","nextteam","hurt","faint"})require(DedicatedTestMaps.action(player,action),"Map action unhandled "+action);
                }
            }
            TestMaps.mode="forms";TestMaps.action(player,"ash");
            BattleController ashBattle=BattleRegistry.getBattle(player);
            require(ashBattle!=null,"Ash map battle failed");
            PixelmonWrapper ashUser=ashBattle.participants.get(0).allPokemon[0],ashTarget=ashBattle.participants.get(1).allPokemon[0];
            require(ashUser.getForm().isForm("battle_bond") && ashUser.pokemon.getPalette().getSprite().toString().contains("/base/"),"Fresh fixture must use ordinary Greninja appearance");
            require(ashBattle.participants.get(1).controlledPokemon.size()==1 && ashTarget.getParticipant().hasMorePokemonReserve(),"Ash fixture must have one active target and a real reserve");
            require(ashTarget.getHealth()==1,"Ash fixture HP must survive native startup healing");
            ashTarget.setHealth(0);
            require(ashTarget.getParticipant().hasMorePokemon(),"Ash fixture needs a live reserve after the KO");
            ((BattleBond)ashUser.getBattleAbility()).tookDamageUser(300,ashUser,ashTarget,new Attack("Water Shuriken"));
            require(PermanentAsh.isPermanent(ashUser),"Ash map cannot unlock earned form");TestMaps.end(player);
            require(StorageProxy.getParty(player).get(0).getForm().isForm(PermanentAsh.FORM),"Ash fixture loses form after battle");
            System.out.println("BACKPORT_QA_MAP_FIXTURES_PASS two independent layouts, real healers/PC/daycare/pond/supplies, stations, native battles preserving held forms, relog preserves party/HP/inventories, evolution/breeding/starter/care groups; Ash unlock fixture with live reserve and retained form");
        }finally{TestMaps.end(player);players.remove(uuid);world.func_217369_A().remove(player);TestMaps.mode=null;TestMaps.stations.clear();}
    }
}
