import local.pixelmon.backport.*;
import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.battles.raids.*;
import com.pixelmonmod.pixelmon.battles.controller.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.api.rules.*;
import com.pixelmonmod.pixelmon.battles.attacks.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.attacks.DamageTypeEnum;
import com.pixelmonmod.pixelmon.items.TechnicalMoveItem;
public final class TeraRaidVerification {
    static void require(boolean value,String label){if(!value)throw new AssertionError(label);}
    public static void run(){
        int encounters=0;for(JsonElement row:TeraRaids.DATA.getAsJsonArray("encounters")){Pokemon p=TeraRaids.encounter(row.getAsJsonObject());require(p.getSpecies().getDex()<=1025,"Disabled preview Tera encounter");encounters++;}
        for(JsonElement row:TeraRaids.DATA.getAsJsonArray("rewards"))TeraRaids.rewardItem(row.getAsJsonObject().get("item").getAsString());
        TeraRaidRules rules=new TeraRaidRules();require(!rules.shouldRaise(51),"Premature shield");require(rules.shouldRaise(50),"HP shield trigger");rules.raise();require(rules.shield==100 && rules.segments()==8,"Shield durability");
        require(rules.damage(100,false,false)==20 && rules.damage(100,true,false)==35 && rules.damage(100,true,true)==75,"Tera shield damage multipliers");rules.hit(75);require(rules.segments()==2,"Durability not hit count");rules.hit(35);require(rules.shield==0,"Shield break");require(!rules.shouldRaise(1),"Shield must not regenerate");
        int converted=0;for(int x=0;x<10000;x++)if(TeraRaids.convert(21,"minecraft:overworld",x,64,0))converted++;require(converted>4800 && converted<5200,"Half den distribution");
        ServerWorld world=ServerLifecycleHooks.getCurrentServer().func_241755_D_();
        TeraDenEntity anchored=new TeraDenEntity(world);anchored.func_226286_f_(2.5,65,2.5);
        world.func_180501_a(new net.minecraft.util.math.BlockPos(2,64,2),net.minecraft.block.Blocks.field_150348_b.func_176223_P(),3);
        anchored.roll((int)(world.func_72820_D()/24000));world.func_217376_c(anchored);
        for(int tick=0;tick<400;tick++){anchored.func_213317_d(new net.minecraft.util.math.vector.Vector3d(.2,-.8,.1));anchored.func_70071_h_();}
        require(anchored.func_226277_ct_()==2.5 && anchored.func_226278_cu_()==65 && anchored.func_226281_cx_()==2.5,"Tera den moved/fell; detector must track stationary coordinates");
        CompoundNBT anchoredNbt=anchored.serializeNBT();TeraDenEntity reloaded=new TeraDenEntity(world);reloaded.deserializeNBT(anchoredNbt);
        for(int tick=0;tick<100;tick++)reloaded.func_70071_h_();require(reloaded.func_226278_cu_()==65,"Reloaded Tera den fell");anchored.func_70106_y();
        for(int stars=1;stars<=6;stars++){
            TeraDenEntity den=new TeraDenEntity(world);den.roll(stars);den.type(2);
            Pokemon pokemon=TeraVerification.mon(143);RaidData data=new RaidData(den.func_145782_y(),stars,pokemon.getSpecies(),pokemon.getForm());den.setData(data);
            TeraRaidParticipant boss=new TeraRaidParticipant(data,den,pokemon);WildPixelmonParticipant ally=new WildPixelmonParticipant(TeraVerification.mon(6));
            BattleController battle=new BattleController(new BattleParticipant[]{ally},new BattleParticipant[]{boss},new BattleRules());battle.sendMessages=false;battle.modifyStats();
            PixelmonWrapper target=boss.getWrapper();target.onBattlefield=true;
            require(target.isDynamax==0,"Boss started Dynamax");require(target.getMoveset().stream().noneMatch(a->a.isMax),"Boss got Max move");require(!boss.canDynamax(ally),"Ally Dynamax allowed");
            require(target.getMaxHealth()==target.getMaxHealth(true)*TeraRaids.HP[stars],"Boss HP multiplier");require(Tera.select(target) && Tera.activate(target),"Boss Tera activation");require(target.type.equals(Collections.singletonList(Element.WATER)),"Boss Tera defense");
            PixelmonWrapper user=ally.allPokemon[0];user.onBattlefield=true;require(!Tera.eligible(user),"Player Tera without charge");for(int i=0;i<3;i++)boss.onUseAttackOther(battle,new Attack("Tackle"),ally,user);require(Tera.eligible(user),"Tera charge after three actions");
            for(int type=0;type<19;type++){java.util.List<net.minecraft.item.ItemStack> rewards=TeraRaids.rewards(stars,type,new Random(type));require(rewards.size()>=3 && rewards.get(0).func_190916_E()==2+stars*2,"Guaranteed renewable matching shards");}
            Tera.end(battle);
            CompoundNBT saved=new CompoundNBT();den.func_213281_b(saved);TeraDenEntity copy=new TeraDenEntity(world);copy.func_70037_a(saved);require(copy.teraType()==2 && copy.getData().get().getStars()==stars,"Saved Tera den state");
        }
        System.out.println("BACKPORT_TERA_RAID_PASS encounters="+encounters+" separate loot=114 contexts; native forms/palettes, six star tiers, no Dynamax/Max moves, HP, charge, shield durability, saved dens, deterministic half conversion");
    }
    public static void playerRaid(net.minecraftforge.common.util.FakePlayer player)throws Exception{
        ServerWorld world=player.func_71121_q();PlayerPartyStorage party=StorageProxy.getParty(player);
        Pokemon original=party.get(0);net.minecraft.item.ItemStack previous=player.func_184614_ca();TeraDenEntity den=new TeraDenEntity(world);BattleController battle=null;
        try{
            player.func_70634_a(0,5,0);Pokemon user=TeraVerification.mon(143);user.getMoveset().clear();user.getMoveset().add(new Attack("Tackle"));party.set(0,user);
            den.func_226286_f_(player.func_226277_ct_()+2,player.func_226278_cu_(),player.func_226281_cx_());den.type(2);Pokemon enemy=TeraVerification.mon(133);RaidData data=new RaidData(den.func_145782_y(),1,enemy.getSpecies(),enemy.getForm());den.setData(data);world.func_217376_c(den);
            require(data.addPlayer(4,player,user,0),"Native Tera lobby rejected player");data.populateWithNPCs();require(data.getPlayers().size()==4,"Native four-member lobby");
            data.startRaid(den);
            battle=BattleRegistry.getBattle(player);require(battle!=null,"Native Tera raid not started");battle.update();require(!battle.battleEnded,"Raid initialization failed");TeraRaidParticipant boss=TeraRaids.get(battle);require(boss!=null,"Native RaidData bypassed Tera constructor redirect");
            require(Tera.active(boss.getWrapper()) && boss.getWrapper().isDynamax==0,"Actual boss not Tera");require(battle.participants.size()==5,"Native raid participants missing");
            PixelmonWrapper attacker=battle.getParticipantForEntity(player).allPokemon[0];boss.getWrapper().setHealth(boss.getWrapper().getMaxHealth()/2);boss.onHit(attacker,1,DamageTypeEnum.ATTACK);require(boss.areShieldsUp(),"Actual raid shield trigger");
            float reduced=boss.onHit(attacker,100,DamageTypeEnum.ATTACK);require(reduced==20,"Actual native shield scaling");
            boss.isDefeated=true;boss.getWrapper().setHealth(0);battle.endBattle();RaidData.RaidPlayer reward=data.getPlayer(player);
            require(reward.canCatch && reward.possibleCatch!=null,"Victory catch route absent");require(reward.possibleCatch.getSpecies().getDex()==133 && Tera.type(reward.possibleCatch)==Element.WATER,"Raid catch species/type lost");
            require(player.field_71071_by.field_70462_a.stream().anyMatch(s->TechnicalMoveItem.getMove(s)!=null && TechnicalMoveItem.getMove(s).getAttackName().equals("Tera Blast")),"Victory TM171 not delivered to native inventory");
            require(player.field_71071_by.field_70462_a.stream().anyMatch(s->s.func_77973_b()==local.pixelmon.backport.Backport.REGISTERED.get("water_tera_shard").get() && s.func_190916_E()>=4),"Victory matching shards not delivered");
            party.set(1,null);
            net.minecraft.item.Item ball=TeraRaids.rewardItem("pixelmon:master_ball");net.minecraft.item.ItemStack stack=new net.minecraft.item.ItemStack(ball,2);player.func_184611_a(net.minecraft.util.Hand.MAIN_HAND,stack);int before=party.countPokemon();data.processAction(10,0,stack,player);require(stack.func_190916_E()==1 && party.countPokemon()==before+1,"Native raid capture/consumption failed");
            System.out.println("BACKPORT_TERA_RAID_PLAYER_PASS native lobby/four allies/start/actual Tera/shield/victory rewards/TM171/capture/ball consumption");
        }finally{if(battle!=null && !battle.battleEnded)battle.endBattleWithoutXP();den.func_70106_y();party.set(0,original);for(int i=1;i<6;i++)party.set(i,null);player.func_184611_a(net.minecraft.util.Hand.MAIN_HAND,previous);}
    }
}
