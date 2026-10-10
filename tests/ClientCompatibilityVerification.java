import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.client.ClientProxy;
import com.pixelmonmod.pixelmon.client.gui.battles.*;
import com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.ChooseAttack;
import local.pixelmon.backport.*;
import java.util.*;
import java.lang.reflect.*;
import java.awt.Color;

/** Tests the actually transformed PMBattleInfo method in the isolated client. */
public final class ClientCompatibilityVerification {
    static void require(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    static PixelmonClientData mon(int dex){Pokemon p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(70);p.initialize();return new PixelmonClientData(p);}
    public static void run() throws Exception {
        ClientBattleManager previous=ClientProxy.battleManager;
        TeraNetwork.Snapshot previousSnapshot=TeraClient.snapshot;
        TeraNetwork.Reply previousReply=TeraClient.battle;
        try {
            ClientBattleManager bm=new ClientBattleManager();ClientProxy.battleManager=bm;
            bm.battleControllerIndex=321;bm.currentPokemon=0;
            PixelmonClientData user=mon(143),enemy=mon(6);
            bm.fullOurPokemon=Arrays.asList(user);bm.displayedOurPokemon=new PixelmonClientData[]{user};bm.displayedEnemyPokemon=new PixelmonClientData[]{enemy};
            TeraClient.battle=null;TeraClient.snapshot=null;
            ChooseAttack.MoveButton button=new ChooseAttack.MoveButton(null,0);
            Field attackField=button.getClass().getDeclaredField("attack");attackField.setAccessible(true);
            Method color=Arrays.stream(button.getClass().getDeclaredMethods()).filter(m->m.getName().contains("pmbattleinfo$getEffectivenessColor")).findFirst().orElseThrow(()->new AssertionError("PMBattleInfo mixin missing"));color.setAccessible(true);
            attackField.set(button,new Attack("Rock Slide"));require(color.invoke(button).equals(new Color(0,170,0)),"Native 4x color");
            attackField.set(button,new Attack("Earthquake"));require(color.invoke(button).equals(new Color(85,85,85)),"Native immunity color");
            TeraNetwork.Snapshot state=new TeraNetwork.Snapshot(321);TeraClient.snapshot=state;
            state.defense.put(enemy.pokemonUUID,Arrays.asList(Element.GRASS));state.active.put(enemy.pokemonUUID,Arrays.asList(Tera.TYPES).indexOf(Element.GRASS));
            attackField.set(button,new Attack("Flamethrower"));System.out.println("BACKPORT_PM_DEBUG method="+color.getName()+" actual="+color.invoke(button)+" helper="+BattleInfoCompat.multiplier((Attack)attackField.get(button),user,enemy,false)+" battle="+ClientProxy.battleManager.battleControllerIndex);require(color.invoke(button).equals(new Color(85,255,85)),"Tera defense super-effective color");
            attackField.set(button,new Attack("Surf"));require(color.invoke(button).equals(new Color(255,85,85)),"Tera defense resisted color");
            attackField.set(button,new Attack("Rock Slide"));require(color.invoke(button).equals(Color.WHITE),"Tera defense neutral color");
            state.active.put(user.pokemonUUID,Arrays.asList(Tera.TYPES).indexOf(Element.WATER));
            Attack blast=new Attack("Tera Blast");attackField.set(button,blast);
            require(color.invoke(button).equals(new Color(255,85,85)),"Dynamic Tera Blast color");require(blast.getType()==Element.NORMAL,"Shared move definition mutated");
            Method icon=Arrays.stream(button.getClass().getDeclaredMethods()).filter(m->m.getName().contains("backport$iconType")).findFirst().orElseThrow(()->new AssertionError("Native icon hook missing"));icon.setAccessible(true);
            Method category=Arrays.stream(button.getClass().getDeclaredMethods()).filter(m->m.getName().contains("backport$category")).findFirst().orElseThrow(()->new AssertionError("Native category hook missing"));category.setAccessible(true);
            Field power=button.getClass().getDeclaredField("displayedPower");power.setAccessible(true);
            user.moveset.clear();user.moveset.add(blast);
            for(int i=0;i<Tera.TYPES.length;i++){
                state.active.put(user.pokemonUUID,i);
                require(icon.invoke(button,blast)==Tera.TYPES[i],"Native icon Tera type "+i);
                for(int flags=0;flags<4;flags++){
                    state.attackStats.put(user.pokemonUUID,flags);
                    require(category.invoke(button,blast)==((flags&1)!=0?AttackCategory.PHYSICAL:AttackCategory.SPECIAL),"Native Tera Blast category "+flags);
                }
                button.setupAttack(bm,user);
                require(power.getInt(button)==(Tera.TYPES[i]==Element.MYSTERY?100:80),"Native displayed power "+i);
            }
            state.active.remove(user.pokemonUUID);TeraNetwork.Reply reply=new TeraNetwork.Reply();reply.battle=321;reply.pokemon=user.pokemonUUID;reply.selected=true;reply.type=2;TeraClient.battle=reply;
            require(icon.invoke(button,blast)==Element.WATER,"Selected Tera icon");
            reply.selected=false;require(icon.invoke(button,blast)==Element.NORMAL,"Cancelled Tera icon");
            reply.selected=true;reply.pokemon=enemy.pokemonUUID;require(icon.invoke(button,blast)==Element.NORMAL,"Other Pokemon selected state leaked");
            reply.pokemon=user.pokemonUUID;reply.battle=322;require(icon.invoke(button,blast)==Element.NORMAL,"Stale reply icon");TeraClient.battle=null;
            state.active.put(user.pokemonUUID,2);
            bm.showZMoves=true;require(icon.invoke(button,blast)==Element.NORMAL,"Z selection icon overwritten");bm.showZMoves=false;
            bm.megaEvolving=true;require(icon.invoke(button,blast)==Element.NORMAL,"Mega selection icon overwritten");bm.megaEvolving=false;
            bm.dynamaxing=true;require(icon.invoke(button,blast)==Element.NORMAL,"Max selection icon overwritten");bm.dynamaxing=false;
            require(icon.invoke(button,new Attack("Flamethrower"))==Element.FIRE,"Ordinary move icon overwritten");
            PixelmonClientData turtle=mon(1024);bm.displayedOurPokemon=new PixelmonClientData[]{turtle};bm.fullOurPokemon=Arrays.asList(turtle);
            state.active.put(turtle.pokemonUUID,18);Attack starstorm=new Attack("Tera Starstorm");
            for(int flags=0;flags<4;flags++){
                state.attackStats.put(turtle.pokemonUUID,flags);
                require(icon.invoke(button,starstorm)==Element.MYSTERY,"Starstorm Stellar icon");
                require(category.invoke(button,starstorm)==((flags&2)!=0?AttackCategory.PHYSICAL:AttackCategory.SPECIAL),"Starstorm raw-stat category "+flags);
            }
            bm.displayedOurPokemon=new PixelmonClientData[]{user};bm.fullOurPokemon=Arrays.asList(user);
            require(blast.getType()==Element.NORMAL && blast.getAttackCategory()==AttackCategory.SPECIAL,"Display changed shared move data");
            System.out.println("BACKPORT_NATIVE_MOVE_DISPLAY_PASS types=19 categories=76 power, selected/cancelled, switched Pokemon, stale reply, Mega/Z/Max coexistence, Stellar Starstorm, no shared mutation");
            state.active.put(user.pokemonUUID,Arrays.asList(Tera.TYPES).indexOf(Element.MYSTERY));
            require(color.invoke(button).equals(new Color(85,255,85)),"Stellar versus Tera color");
            state.active.remove(enemy.pokemonUUID);require(color.invoke(button).equals(Color.WHITE),"Stellar versus non-Tera color");
            Class<?> tooltip=Class.forName("com.inhumanity.pmbattleinfo.util.Tooltip");Method types=tooltip.getDeclaredMethod("getEnemyTypes");types.setAccessible(true);
            require(types.invoke(null).equals(Arrays.asList(Element.GRASS)),"Tooltip Tera defense");
            TeraClient.snapshot=new TeraNetwork.Snapshot(322);attackField.set(button,new Attack("Rock Slide"));require(color.invoke(button).equals(new Color(0,170,0)),"Stale battle snapshot leaked");
            attackField.set(button,blast);require(icon.invoke(button,blast)==Element.NORMAL,"Stale snapshot icon");
            System.out.println("BACKPORT_PM_BATTLE_INFO_PASS actual transformed colors=10 tooltip defense, dynamic Tera Blast, Stellar, stale battle, no shared mutation");
        } finally {ClientProxy.battleManager=previous;TeraClient.snapshot=previousSnapshot;TeraClient.battle=previousReply;}
    }
}
