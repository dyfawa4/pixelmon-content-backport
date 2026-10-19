import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.BattleBond;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.tasks.BattleMessagePacket;
import com.pixelmonmod.pixelmon.client.gui.pokechecker.PokeCheckerMovesScreen;
import com.pixelmonmod.pixelmon.client.gui.starter.ChooseStarterScreen;
import local.pixelmon.backport.*;
import local.pixelmon.backport.client.AshMoveDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.text.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import java.nio.file.*;
import java.util.*;
import io.netty.buffer.Unpooled;
import com.mojang.blaze3d.matrix.MatrixStack;

/** Private real-client controlled native lifecycle regression; never part of distributable maps. */
@Mod("backport_ash143_probe")
public final class Ash143ClientVerification {
    public static final class SummaryPreview extends PokeCheckerMovesScreen {
        SummaryPreview(PokemonStorage storage){super(storage,new StoragePosition(-1,0),null);}
        @Override public void drawSelection(MatrixStack matrix,int x,int y){super.drawSelection(matrix,field_230708_k_/2,field_230709_l_/2-88);}
    }
    static volatile int state;
    static int ticks;
    static UUID earned;
    static volatile PokemonStorage displayStorage;
    static final long DEADLINE=System.nanoTime()+240_000_000_000L;
    static void require(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    static void result(String value){try{Files.writeString(Paths.get("ash143-result.txt"),value+"\n");System.out.println("BACKPORT_ASH143_"+value);}catch(Exception e){e.printStackTrace();}}
    static void same(Pokemon pokemon){require(pokemon!=null && pokemon.getUUID().equals(earned) && pokemon.getForm().isForm(PermanentAsh.FORM) && pokemon.getAbility() instanceof BattleBond,"Permanent Ash identity/form/ability lost");}
    @Mod.EventBusSubscriber(modid="backport_ash143_probe")
    public static final class ServerEvents {
        @SubscribeEvent(priority=EventPriority.LOWEST) public static void tick(TickEvent.ServerTickEvent e){
            if(e.phase!=TickEvent.Phase.END || (state!=8 && state!=4) || !TestMaps.prepared)return;
            var server=ServerLifecycleHooks.getCurrentServer();if(server==null)return;
            try{
                ServerPlayerEntity p=server.func_184103_al().func_181057_v().stream().findFirst().orElse(null);
                if(p==null)return;
                if(state==4){same(StorageProxy.getParty(p).get(0));displayStorage=StorageProxy.getParty(p);state=5;return;}
                for(int category=0;category<3;category++){
                    var pool=LuckySpecies.pool(category);require(!pool.isEmpty(),"Empty lucky pool "+category);
                    for(var species:pool){require(LuckySpecies.allowed(species),"Invalid lucky species");if(category==1)require(species.isUltraBeast(),"Wrong beast category");if(category==2)require(PixelmonSpecies.getLegendaries(false).contains(species.getDex()),"Wrong legend category");}
                    for(int i=0;i<100;i++)require(PokemonFactory.create(pool.get(i%pool.size())).getSpecies().getDex()>0,"Factory fell back to MissingNo");
                    System.out.println("BACKPORT_ASH143_LUCKY category="+category+" valid="+pool.size());
                }
                require(!LuckySpecies.allowed(PixelmonSpecies.fromDex(0).orElse(null)),"MissingNo allowed");
                TestMaps.action(p,"ash");BattleController battle=BattleRegistry.getBattle(p);require(battle!=null,"No native battle");
                PixelmonWrapper own=null,target=null;
                for(BattleParticipant participant:battle.participants){if(participant instanceof PlayerParticipant)own=participant.allPokemon[0];else target=participant.allPokemon[0];}
                require(own!=null && target!=null,"Native battle wrappers missing");
                Pokemon mon=own.pokemon;earned=mon.getUUID();require(mon.getAbility() instanceof BattleBond,"Fixture lacks Battle Bond");
                require(!PermanentAsh.isAsh(mon.getForm()),"Fixture starts already evolved");
                require(mon.getPalette().getSprite().toString().contains("/base/"),"Fresh fixture has the wrong appearance");
                require(target.getParticipant().controlledPokemon.size()==1 && target.getParticipant().hasMorePokemonReserve(),"Fixture has no genuine reserve");
                require(target.getHealth()==1,"Actual opponent is not 1 HP after battle initialization");
                Attack attack=new Attack("Water Shuriken");target.pokemon.setHealth(0);
                // Exercise the real transformed KO hook with its real reserve/participant conditions.
                mon.getAbility().tookDamageUser(1,own,target,attack);same(mon);own.evolution=null;target.pokemon.heal();
                own.attack=attack;int start=battle.battleTurn;AshWaterBoost.start(own);
                require(AshWaterBoost.power(own,attack,20)==30,"Boost not applied");
                for(int i=1;i<=5;i++){battle.endTurn();require(battle.battleTurn==start+i,"Native turn counter mismatch");require(AshWaterBoost.power(own,attack,20)==(i==5?20:30),"Native five-turn expiry mismatch");}
                mon.setHealth(0);mon.getAbility().onSelfFaint(own,target);same(mon);mon.heal();
                mon.getAbility().applyEndOfBattleEffect(own);same(mon);battle.endBattleWithoutXP();same(mon);
                Pokemon restored=PokemonFactory.create(mon.writeToNBT(new CompoundNBT()));same(restored);
                var party=StorageProxy.getParty(p);var pc=StorageProxy.getPCForPlayer(p);
                require(pc.get(0,0)==null,"Private PC fixture slot occupied");party.set(0,null);pc.set(0,0,restored);same(pc.get(0,0));
                restored=pc.get(0,0);pc.set(0,0,null);party.set(0,restored);same(party.get(0));
                require(AshMoveDisplay.eligible(restored,attack.getActualMove()),"Permanent summary eligibility lost");
                Pokemon ordinary=TestMaps.mon(658);ordinary.setForm("battle_bond");require(!AshMoveDisplay.eligible(ordinary,attack.getActualMove()),"Ordinary move preview changed");
                displayStorage=party;state=2;
            }catch(Throwable failure){failure.printStackTrace();state=-1;result("FAIL "+failure);}
        }
    }
    @Mod.EventBusSubscriber(modid="backport_ash143_probe",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class ClientEvents {
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
            if(e.phase!=TickEvent.Phase.END || state<0)return;
            Minecraft mc=Minecraft.func_71410_x();
            try{
                if(System.nanoTime()>DEADLINE)throw new AssertionError("Timeout at "+state);
                if(state==0 && mc.field_71462_r instanceof MainMenuScreen){state=1;ticks=0;mc.func_238191_a_("backport-qa-forms");}
                else if(state==1 && mc.field_71439_g!=null && mc.field_71441_e!=null && ++ticks>40){state=8;ticks=0;}
                else if(state==2 && ++ticks>40){
                    state=3;ticks=0;
                    if(mc.field_71441_e!=null)mc.field_71441_e.func_72882_A();
                    mc.func_213231_b(new MainMenuScreen());mc.func_147108_a(new MainMenuScreen());
                }
                else if(state==3 && mc.field_71441_e==null && mc.func_71401_C()==null && ++ticks>30){state=4;mc.func_238191_a_("backport-qa-forms");}
                else if(state==5 && mc.field_71439_g!=null){
                    for(String key:new String[]{"pixelmon_backport.battle.ash_water_boost","pixelmon_backport.battle.ash_water_boost_end"}){
                        BattleMessagePacket sent=new BattleMessagePacket(new TranslationTextComponent(key,"小智甲贺忍蛙"));
                        PacketBuffer buffer=new PacketBuffer(Unpooled.buffer());sent.encode(buffer);BattleMessagePacket received=new BattleMessagePacket();received.decode(buffer);buffer.release();
                        String text=received.getMessage().getString();require(text.contains(key.endsWith("_end")?"加成结束":"强化了自身"),"Native battle packet text: "+text);
                    }
                    SummaryPreview preview=new SummaryPreview(displayStorage);
                    boolean checked=false;
                    for(var method:PokeCheckerMovesScreen.class.getDeclaredMethods())if(method.getName().contains("backport$ashPower")){
                        method.setAccessible(true);require(((Integer)method.invoke(preview,displayStorage.get(new StoragePosition(-1,0)).getMoveset().get(0).getActualMove()))==20,"Actual summary power is not 20");checked=true;
                    }
                    require(checked,"Summary power redirect not found");
                    mc.func_147108_a(preview);state=6;ticks=0;
                }else if(state==6 && ++ticks>20){
                    require(!(mc.field_71462_r instanceof ChooseStarterScreen),"Starter UI replaced summary");
                    try(NativeImage image=ScreenShotHelper.func_198052_a(mc.func_228018_at_().func_198105_m(),mc.func_228018_at_().func_198083_n(),mc.func_147110_a())){image.func_209270_a(Paths.get("ash143-summary.png"));}
                    result("PASS controlled native KO/form/faint/heal/end-battle/PC/NBT/world-reentry; native five endTurn expiry; start/end battle packet text; lucky registered/category pools; summary screenshot");state=7;ticks=0;
                }else if(state==7 && ++ticks>15){state=-2;mc.func_71400_g();}
            }catch(Throwable failure){failure.printStackTrace();state=-1;result("FAIL client "+failure);}
        }
    }
}
