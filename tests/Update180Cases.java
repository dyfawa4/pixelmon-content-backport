package qa;
import java.util.*;
import com.mojang.authlib.GameProfile;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.*;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.events.BeatTrainerEvent;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.log.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import com.pixelmonmod.pixelmon.enums.EnumEncounterMode;
import local.pixelmon.backport.*;
import net.minecraft.block.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Small native checks for only the requested 1.8.0 changes. */
public final class Update180Cases {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static ItemStack stack(String id){var item=ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));check(item!=null&&item!=Items.field_190931_a,"Registered "+id);return new ItemStack(item);}
    static Pokemon mon(int dex,String...moves){var p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(100);p.initialize();p.getMoveset().clear();for(String move:moves)p.getMoveset().add(new Attack(move));p.heal();return p;}
    static String item(Pokemon p){return p.getHeldItem().func_77973_b().getRegistryName().toString();}
    static void expected(Pokemon p,String expected){
        var moves=new ArrayList<>(p.getMoveset());var ability=p.getAbility();var ivs=p.getIVs().getArray().clone();
        NpcHeldItems.equip(p);check(item(p).equals("pixelmon:"+expected),p.getSpecies().getName()+" expected "+expected+" got "+item(p));
        check(p.getMoveset().equals(moves)&&p.getAbility()==ability&&Arrays.equals(ivs,p.getIVs().getArray()),"Equipment does not alter moves/ability/IVs");
    }
    static void equipment(){
        var p=mon(472,"Earthquake","Facade","Roost","Swords Dance");p.setAbility(new PoisonHeal());expected(p,"toxic_orb");
        p=mon(534,"Drain Punch","Mach Punch","Facade","Knock Off");p.setAbility(new Guts());expected(p,"flame_orb");
        p=mon(65,"Psychic","Shadow Ball","Focus Blast","Energy Ball");p.setAbility(new MagicGuard());expected(p,"life_orb");
        p=mon(91,"Shell Smash","Icicle Spear","Rock Blast","Razor Shell");expected(p,"white_herb");
        p=mon(233,"Recover","Ice Beam","Thunderbolt","Toxic");expected(p,"eviolite");
        p=mon(25,"Thunderbolt","Surf","Grass Knot","Volt Switch");expected(p,"light_ball");
        p=mon(6,"Flamethrower","Dragon Pulse","Roost","Air Slash");p.setHeldItem(stack("pixelmon:charizardite_y"));expected(p,"charizardite_y");
        p=mon(493,"Judgment","Recover");p.setHeldItem(stack("pixelmon:legend_plate"));expected(p,"legend_plate");
        p=mon(637,"Quiver Dance","Fiery Dance","Bug Buzz","Giga Drain");NpcHeldItems.equip(p);
        check(!item(p).contains("choice_")&&!item(p).endsWith("assault_vest")&&!p.getHeldItem().func_190926_b(),"Setup user keeps access to utility");
        for(var member:AuthorBossTeams.roster("plains",new Random(180)))check(!member.getHeldItem().func_190926_b(),"Every author team slot equipped");
        for(var member:AuthorBossTeams.roster("badlands",new Random(180)))if(member.getSpecies().getDex()==CapSpecies.DEX)check(item(member).equals("pixelmon:pikashunium_z"),"Authored Cap Pikachu crystal retained");
        System.out.println("EQUIPMENT_180_PASS nine builds, special items retained, no moves/ability/IV rewrite, author six equipped");
    }
    static void survival(ServerPlayerEntity player){
        var npc=new NPCTrainer(player.field_70170_p);npc.isGymLeader=true;npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));
        for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,null);npc.getPokemonStorage().set(0,mon(150,"Earthquake","Double Kick"));
        var trainer=new TrainerParticipant(npc,1);var enemy=new WildPixelmonParticipant(mon(25,"Splash"));
        var battle=new BattleController(new BattleParticipant[]{trainer},new BattleParticipant[]{enemy},new BattleRules());battle.sendMessages=false;battle.battleTurn=1;
        for(var side:battle.participants){side.controlledPokemon.clear();side.controlledPokemon.add(side.allPokemon[0]);for(var w:side.allPokemon){w.bc=battle;w.onBattlefield=w==side.allPokemon[0];w.setHealth(w.getMaxHealth());}}
        var own=trainer.allPokemon[0];var target=enemy.allPokemon[0];
        own.choiceLocked=null;own.getMoveset().clear();own.getMoveset().add(new Attack("Earthquake"));own.getMoveset().add(new Attack("Double Kick"));
        var hit=new MoveChoice(own,own.getMoveset().get(0),List.of(target));hit.tier=3;hit.weight=100;hit.result=new MoveResults(target);hit.result.result=AttackResult.hit;hit.result.accuracy=100;hit.result.damage=target.getHealth();hit.result.fullDamage=target.getMaxHealth()*3;
        target.pokemon.setHeldItem(ItemStack.field_190927_a);check(NpcSurvival.reliable(hit,target,target.getHealth()),"No item retains previous KO");
        target.pokemon.setHeldItem(stack("pixelmon:focus_band"));check(!NpcSurvival.reliable(hit,target,target.getHealth()),"Focus Band is probabilistic, not guaranteed KO");
        check(!NpcTactics.finishFirst(own,hit,List.of()),"Band does not skip emergency medicine as a guaranteed first-strike KO");
        check(NpcSurvival.chance(hit,target,target.getHealth())==.9,"Native band survival chance10%");
        target.pokemon.setHeldItem(stack("pixelmon:focus_sash"));check(!NpcSurvival.reliable(hit,target,target.getHealth()),"Full HP sash prevents one-hit KO");
        check(NpcSurvival.incoming(hit,target,target.getHealth()*3)==target.getHealth()-1,"Sash caps known single incoming hit");
        target.setHealth(target.getMaxHealth()-1);check(NpcSurvival.reliable(hit,target,target.getHealth()),"Damaged sash user follows native KO");
        target.setHealth(target.getMaxHealth());hit.attack=own.getMoveset().get(1);check(NpcSurvival.reliable(hit,target,target.getHealth()),"Guaranteed two-hit overkill breaks sash");
        hit.attack=own.getMoveset().get(0);target.setTempAbility(new Klutz());check(NpcSurvival.reliable(hit,target,target.getHealth()),"Suppressed sash does not grant false survival");
        target.tempAbility=null;target.pokemon.setHeldItem(ItemStack.field_190927_a);check(NpcSurvival.reliable(hit,target,target.getHealth()),"Consumed/missing item not assumed present");
        System.out.println("SURVIVAL_180_PASS band probability, full/damaged sash, two hits, Klutz, missing item; no survival RNG or item consumption in forecast");
        var copy=NpcMedicineForecast.project(own,stack("pixelmon:max_potion"),-1);
        try{
            var field=com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStats.class.getDeclaredField("pixelmon");field.setAccessible(true);
            check(field.get(copy.getBattleStats())==copy&&field.get(own.getBattleStats())==own,"Forecast stat owner rebound without changing original");
        }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        boolean simulated=battle.simulateMode;battle.simulateMode=true;
        try{
            new com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStats(own.getBattleStats()).notifyActivePokemonOfStatChange();
            var attack=new Attack("Leaf Storm");var candidate=new MoveChoice(copy,attack,List.of(target));
            var ai=com.pixelmonmod.pixelmon.api.battles.BattleAIMode.ADVANCED.createAI(trainer);
            ((local.pixelmon.backport.mixin.NpcDamagePreviewAccess)ai).backport$damage(copy,candidate);
            check(candidate.result!=null,"Actual self-stat-changing offensive forecast completes");
            target.setTempAbility(new Klutz());target.pokemon.setHeldItem(stack("pixelmon:focus_sash"));copy.getBattleStats().notifyActivePokemonOfStatChange();target.tempAbility=null;
        }finally{battle.simulateMode=simulated;}
        System.out.println("AUTHOR_CRASH_180_PASS native medicine copy owner, Leaf Storm forecast, unbound stats and unusable item notification");
    }
    static void author(ServerPlayerEntity player){
        var world=player.func_71121_q();var home=new BlockPos(20,74,20);AuthorBossFeature.build(world,home,false,"minecraft:stone_bricks");
        var npc=AuthorBossTeams.create(world,home,"plains");world.func_217376_c(npc);
        check(npc.getEncounterMode()==EnumEncounterMode.OncePerPlayer,"Native per-player encounter mode");
        float hp=npc.func_110143_aJ();boolean creative=player.field_71075_bZ.field_75098_d;
        try{for(boolean mode:new boolean[]{false,true}){
            player.field_71075_bZ.field_75098_d=mode;var event=new AttackEntityEvent(player,npc);new AuthorBossArenas().attack(event);check(event.isCanceled(),"Direct attack canceled, creative="+mode);
            check(!npc.func_70097_a(DamageSource.func_76365_a(player),1000000)&&npc.func_110143_aJ()==hp,"Native hurt blocked, creative="+mode);
        }}finally{player.field_71075_bZ.field_75098_d=creative;}
        check(!npc.func_70097_a(DamageSource.field_76380_i,1000000),"Bypassing damage cannot kill author");
        var ordinary=new NPCTrainer(world);var ordinaryEvent=new AttackEntityEvent(player,ordinary);new AuthorBossArenas().attack(ordinaryEvent);check(!ordinaryEvent.isCanceled(),"Ordinary NPC not globally protected");
        var other=FakePlayerFactory.get(world,new GameProfile(UUID.fromString("c4decf90-95a2-4d32-8748-55c5a9bb0180"),"Backport180B"));
        int before=player.getPersistentData().func_74775_l("PlayerPersisted").func_74762_e("BackportAuthorVictories");
        // This component fixture has no battle message queue; disable only the irrelevant defeat dialogue.
        npc.usingDefaultLose=false;npc.loseMessage=null;
        AuthorBossArenas.active(npc,player);var winner=new PlayerParticipant(player,StorageProxy.getParty(player).get(0));npc.loseBattle(new ArrayList<>(List.of(winner)));
        check(!npc.field_70128_L&&npc.func_70089_S(),"Author remains alive after native victory");
        check(npc.playerEncounters.containsKey(player.func_110124_au())&&!npc.playerEncounters.containsKey(other.func_110124_au()),"Native encounters isolated per player");
        check(AuthorBossArenas.won(npc,player.func_110124_au())&&!AuthorBossArenas.won(npc,other.func_110124_au()),"Reward guard isolated per player");
        Pixelmon.EVENT_BUS.post(new BeatTrainerEvent(player,npc));
        check(player.getPersistentData().func_74775_l("PlayerPersisted").func_74762_e("BackportAuthorVictories")==before+1,"Duplicate event cannot grant another win");
        var saved=new CompoundNBT();check(npc.func_70039_c(saved),"NPC saves");var loaded=new NPCTrainer(world);loaded.func_70020_e(saved);
        check(loaded.getEncounterMode()==EnumEncounterMode.OncePerPlayer&&loaded.playerEncounters.containsKey(player.func_110124_au())&&AuthorBossArenas.won(loaded,player.func_110124_au()),"Both locks survive native NBT roundtrip");
        var pc=home.func_177982_a(-3,1,-5);var healer=home.func_177982_a(-4,1,-5);
        check(world.func_180495_p(pc).func_177229_b(HorizontalBlock.field_185512_D)==Direction.NORTH&&world.func_180495_p(pc.func_177984_a()).func_177229_b(HorizontalBlock.field_185512_D)==Direction.NORTH,"PC base/top face room");
        check(world.func_180495_p(healer).func_177229_b(HorizontalBlock.field_185512_D)==Direction.NORTH,"Healer faces room");
        var chest=(ChestTileEntity)world.func_175625_s(home.func_177982_a(-5,1,-5));chest.func_70299_a(0,new ItemStack(Items.field_151045_i,3));
        var tile=world.func_175625_s(pc);for(var pos:List.of(pc,pc.func_177984_a(),healer))world.func_180501_a(pos,world.func_180495_p(pos).func_206870_a(HorizontalBlock.field_185512_D,Direction.SOUTH),2);
        AuthorRoomFurnishings.upgrade(world,home);AuthorRoomFurnishings.place(world,home);
        check(world.func_175625_s(pc)==tile&&chest.func_70301_a(0).func_190916_E()==3,"Migration retains machine tile and chest contents");
        check(world.func_180495_p(pc).func_177229_b(HorizontalBlock.field_185512_D)==Direction.NORTH,"Legacy PC rotated");
        player.func_200619_a(world,home.func_177958_n()-3,home.func_177956_o()+1,home.func_177952_p()+2,180,10);
        System.out.println("AUTHOR_180_PASS creative/survival/bypass immunity; actual loseBattle retains NPC and records only winner; duplicate reward and NBT; machine orientation and safe migration");
    }
    public static void run(ServerPlayerEntity player){
        var party=StorageProxy.getParty(player);party.starterPicked=true;party.guiOpened=true;for(int i=0;i<6;i++)party.set(i,null);party.set(0,mon(150,"Psychic"));
        var errors=new ArrayList<Throwable>();
        try{equipment();}catch(Throwable e){e.printStackTrace();errors.add(e);}
        try{survival(player);}catch(Throwable e){e.printStackTrace();errors.add(e);}
        try{author(player);}catch(Throwable e){e.printStackTrace();errors.add(e);}
        check(Content.read("backport/description_fixes_zh_cn.json").get("item.pixelmon.ultraspace_music_disc.desc").getAsString().equals("Zeecount - 究极空间/溺水世界"),"Music credit");
        if(!errors.isEmpty())throw new AssertionError("Component failures: "+errors);
        System.out.println("UPDATE180_PASS all requested native component checks, no full regression");
    }
}
