import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.client.ClientProxy;
import com.pixelmonmod.pixelmon.client.gui.battles.*;
import com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.ChooseAttack;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import local.pixelmon.backport.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import java.lang.reflect.*;
import java.awt.Color;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRuleRegistry;
import com.pixelmonmod.pixelmon.api.battles.BattleType;
import com.pixelmonmod.pixelmon.enums.EnumMegaItem;
import com.pixelmonmod.pixelmon.enums.EnumOldGenMode;

/** Private integrated-server battle verifies actual request/reply/snapshot delivery. */
public final class ClientNetworkVerification {
    static Pokemon mon(int dex){Pokemon p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(70);p.initialize();p.heal();return p;}
    @Mod.EventBusSubscriber(modid="backport_client_test")
    public static class ServerEvents {
        static int ticks,stage;static BattleController battle;
        @SubscribeEvent public static void commands(net.minecraftforge.event.RegisterCommandsEvent e){
            if(!Boolean.getBoolean("backport.network.test"))return;
            LiteralArgumentBuilder<CommandSource> root=Commands.func_197057_a("backportqa");
            for(String scene:new String[]{"reset","end","bag","bagbattle","raids","raidteam","eternamax","eternabattle","interactionbattle","careteam","breedteam","evolveteam","starterteam","nextteam","hurt","faint","showfirst","ogerpon","forms","ash","za","tera","stellar","mega","z","terapagos","double","doubletera","doublestar","raichux","raichuy","dragonite","starmie","greninja","zygarde","darkrai"})root.then(Commands.func_197057_a("map").then(Commands.func_197057_a(scene).executes(ctx->{
                ServerPlayerEntity player=ctx.getSource().func_197035_h();if(!TestMaps.isMap(player))return 0;TestMaps.action(player,scene);return 1;
            })));
            for(String kind:new String[]{"mega","z","double"})root.then(Commands.func_197057_a(kind).executes(ctx->{
                ServerPlayerEntity player=ctx.getSource().func_197035_h();BattleController old=BattleRegistry.getBattle(player);
                stage=3;if(old!=null)old.endBattleWithoutXP();
                Pokemon user=mon(6),ally=mon(9),enemy=mon(143),other=mon(242);
                user.getMoveset().clear();for(String name:new String[]{"Flamethrower","Surf","Rock Slide","Protect"})user.getMoveset().add(new Attack(name));
                user.setHeldItem(new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new net.minecraft.util.ResourceLocation("pixelmon",kind.equals("z")?"firium_z":"charizardite_x"))));
                StorageProxy.getParty(player).set(0,user);StorageProxy.getParty(player).set(1,ally);
                StorageProxy.getParty(player).unlockMega(true);StorageProxy.getParty(player).setMegaItem(EnumMegaItem.BraceletORAS,true);
                player.field_71071_by.field_70462_a.set(1,new ItemStack(Backport.REGISTERED.get("tera_orb").get()));
                user.getOrSpawnPixelmon(player);ally.getOrSpawnPixelmon(player);
                enemy.getOrSpawnPixelmon(player.field_70170_p,player.func_226277_ct_()+4,player.func_226278_cu_(),player.func_226281_cx_()+4);
                other.getOrSpawnPixelmon(player.field_70170_p,player.func_226277_ct_()+6,player.func_226278_cu_(),player.func_226281_cx_()+4);
                BattleRules rules=new BattleRules(kind.equals("double")?BattleType.DOUBLE:BattleType.SINGLE);
                rules.set(BattleRuleRegistry.GEN_MODE,EnumOldGenMode.Mega);
                battle=BattleRegistry.startBattle(new BattleParticipant[]{new PlayerParticipant(player,kind.equals("double")?new Pokemon[]{user,ally}:new Pokemon[]{user})},new BattleParticipant[]{new WildPixelmonParticipant(kind.equals("double")?new Pokemon[]{enemy,other}:new Pokemon[]{enemy})},rules);
                if(battle==null)throw new AssertionError("QA battle rejected "+kind);
                System.out.println("BACKPORT_UI_SCENARIO "+kind);return 1;
            }));
            e.getDispatcher().register(root);
        }
        @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
            if(!Boolean.getBoolean("backport.network.test") || e.phase!=TickEvent.Phase.END)return;
            if(ServerLifecycleHooks.getCurrentServer()==null)return;
            java.util.List<ServerPlayerEntity> players=ServerLifecycleHooks.getCurrentServer().func_184103_al().func_181057_v();
            if(players.isEmpty())return;ServerPlayerEntity player=players.get(0);
            if(TestMaps.isMap(player)){try{TestMaps.tick(player);}catch(Throwable failure){System.err.println("BACKPORT_QA_MAP_FAIL");failure.printStackTrace();TestMaps.prepared=true;}return;}
            if(stage==3)return;++ticks;
            try{
                if(stage==0 && ticks>=120){
                    Pokemon user=mon(143),enemy=mon(6);user.getMoveset().clear();for(String name:new String[]{"Tera Blast","Flamethrower","Surf","Rock Slide"})user.getMoveset().add(new Attack(name));
                    Tera.changeType(user,Element.WATER);Tera.changeType(enemy,Element.GRASS);StorageProxy.getParty(player).set(0,user);
                    player.field_71071_by.field_70462_a.set(1,new ItemStack(Backport.REGISTERED.get("tera_orb").get()));
                    user.getOrSpawnPixelmon(player);enemy.getOrSpawnPixelmon(player.field_70170_p,player.func_226277_ct_()+4,player.func_226278_cu_(),player.func_226281_cx_()+4);
                    battle=BattleRegistry.startBattle(new PlayerParticipant(player,user),new WildPixelmonParticipant(enemy));
                    if(battle==null)throw new AssertionError("Native player battle rejected");stage=1;ticks=0;
                }else if(stage==1 && ticks>20){
                    PixelmonWrapper enemy=battle.participants.get(1).allPokemon[0];Tera.select(enemy);if(!enemy.megaEvolve())throw new AssertionError("Enemy Tera failed");stage=2;
                }else if(stage==2){
                    PixelmonWrapper user=battle.participants.get(0).allPokemon[0];if(Tera.selected(user) && !Tera.active(user))if(!user.megaEvolve())throw new AssertionError("Player Tera failed");
                    if(ticks>3600){battle.endBattleWithoutXP();stage=3;}
                }
            }catch(Throwable failure){stage=3;System.err.println("BACKPORT_NETWORK_SERVER_FAIL");failure.printStackTrace();}
        }
    }
    @Mod.EventBusSubscriber(modid="backport_client_test",value=Dist.CLIENT)
    public static class ClientEvents {
        static boolean sent,finished;
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
            if(!Boolean.getBoolean("backport.network.test") || finished || e.phase!=TickEvent.Phase.END || TestMaps.mode!=null)return;
            ClientBattleManager bm=ClientProxy.battleManager;if(bm==null || bm.battleControllerIndex<0 || bm.getCurrentPokemon()==null || bm.displayedEnemyPokemon==null)return;
            try{
                PixelmonClientData user=bm.getCurrentPokemon(),enemy=bm.displayedEnemyPokemon[0];
                if(!sent){TeraNetwork.CHANNEL.sendToServer(new TeraNetwork.Request(bm.battleControllerIndex,user.pokemonUUID,0,0));sent=true;}
                TeraNetwork.Snapshot state=TeraClient.snapshot;if(state==null || state.battle!=bm.battleControllerIndex || !state.active.containsKey(user.pokemonUUID) || !state.active.containsKey(enemy.pokemonUUID))return;
                if(!BattleInfoCompat.defense(enemy).equals(java.util.Arrays.asList(Element.GRASS)))throw new AssertionError("Actual server defense snapshot missing");
                ChooseAttack.MoveButton button=new ChooseAttack.MoveButton(null,0);Field field=button.getClass().getDeclaredField("attack");field.setAccessible(true);
                Method color=java.util.Arrays.stream(button.getClass().getDeclaredMethods()).filter(m->m.getName().contains("pmbattleinfo$getEffectivenessColor")).findFirst().orElseThrow();color.setAccessible(true);
                field.set(button,new Attack("Tera Blast"));if(!color.invoke(button).equals(new Color(255,85,85)))throw new AssertionError("Live Tera Blast color");
                field.set(button,new Attack("Flamethrower"));if(!color.invoke(button).equals(new Color(85,255,85)))throw new AssertionError("Live Tera defense color");
                finished=true;System.out.println("BACKPORT_NETWORK_CLIENT_PASS native player battle, real select request, activation broadcast, enemy defense, dynamic move colors");
            }catch(Throwable failure){finished=true;System.err.println("BACKPORT_NETWORK_CLIENT_FAIL");failure.printStackTrace();}
        }
    }
}
