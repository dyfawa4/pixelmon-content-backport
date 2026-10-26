package qa;
import java.util.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import net.minecraft.entity.player.ServerPlayerEntity;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.battles.BattleAIMode;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.log.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import com.pixelmonmod.pixelmon.items.heldItems.*;

public final class Update180AppendCases {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(ServerPlayerEntity player)throws Exception{
        for(int i=0;i<6;i++){var p=AuthorRoster.strong(i,new Random(i));check(p.getSpecies().getDex()>0,"strong species");if(i==4)check(p.getForm().isForm("shadowrider"),"shadow rider form");if(i==5)check(p.getForm().isForm("icerider"),"ice rider form");System.out.println("AUTHOR_STRONG "+i+" "+p.getSpecies().getName()+"/"+p.getForm().getName());}
        List<String> themes=new ArrayList<>(AuthorBossPolicy.TYPES.keySet());themes.add("ultra");themes.add("paradox");
        for(String theme:themes){var team=AuthorBossTeams.roster(theme,new Random(180));check(team.size()==6,"six author slots "+theme);
            for(var p:team)for(var s:List.of(BattleStatsType.HP,BattleStatsType.ATTACK,BattleStatsType.DEFENSE,BattleStatsType.SPECIAL_ATTACK,BattleStatsType.SPECIAL_DEFENSE,BattleStatsType.SPEED)){check(p.getIVs().getStat(s)==31,"full IVs "+theme);check(p.getEVs().getStat(s)==252,"full EVs "+theme);}
            int fixed=theme.equals("plains")?493:theme.equals("ocean")?382:theme.equals("desert")?383:theme.equals("swamp")?890:0;
            if(fixed>0)check(team.get(0).getSpecies().getDex()==fixed,"fixed core "+theme);
            if(theme.equals("ocean")||theme.equals("desert"))check(team.get(0).getForm().isForm("primal"),"primal core "+theme);
            if(theme.equals("swamp"))check(team.get(0).getForm().isForm("eternamax"),"swamp eternamax");
            if(theme.equals("badlands"))check(team.stream().anyMatch(p->p.getSpecies().getDex()==CapSpecies.DEX),"retained cap");
            System.out.println("AUTHOR_ROSTER_PASS "+theme+" "+team.stream().map(p->p.getSpecies().getName()+"/"+p.getForm().getName()).collect(java.util.stream.Collectors.joining(",")));
        }
        mechanics(player);
        lead(player);
        compass(player);
    }
    static void mechanics(ServerPlayerEntity player)throws Exception{
        var party=StorageProxy.getParty(player);var saved=party.get(0);var pokemon=Update180Cases.mon(493,"Tackle","Swords Dance","Protect","Recover");party.set(0,pokemon);
        var npc=new NPCTrainer(player.field_70170_p);npc.isGymLeader=true;npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));
        for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,null);
        npc.getPokemonStorage().set(0,Update180Cases.mon(242,"Tackle","Protect","Soft-Boiled","Earthquake"));
        npc.getPersistentData().func_74757_a(AuthorBossTeams.MARKER,true);
        var human=new PlayerParticipant(player,pokemon);var trainer=new TrainerParticipant(npc,1);
        var battle=new BattleController(new BattleParticipant[]{human},new BattleParticipant[]{trainer},new BattleRules());battle.sendMessages=false;battle.battleTurn=0;
        var own=human.allPokemon[0];var enemy=trainer.allPokemon[0];
        for(var side:battle.participants){side.controlledPokemon.clear();side.controlledPokemon.add(side.allPokemon[0]);for(var mon:side.allPokemon){mon.bc=battle;mon.onBattlefield=mon==side.allPokemon[0];mon.setHealth(mon.getMaxHealth());}}
        try{
            for(int turn=0;turn<3;turn++){
                battle.battleTurn=turn;
                for(String id:List.of("pixelmon:focus_sash","pixelmon:black_sludge","pixelmon:leftovers","pixelmon:life_orb","pixelmon:toxic_orb","pixelmon:choice_band")){
                    own.pokemon.setHeldItem(Update180Cases.stack(id));check(own.getUsableHeldItem() instanceof NoItem,"first three rounds block "+id+" turn "+turn);
                    var item=own.getHeldItem();int hp=Math.max(1,own.getMaxHealth()/2);own.setHealth(hp);item.applyRepeatedEffect(own);check(own.getHealth()==hp,"no passive healing "+id);
                    double damage=item.modifyDamageIncludeFixed(own.getMaxHealth()*2,enemy,own,new Attack("Tackle"));check(damage==own.getMaxHealth()*2,"no sash protection in round "+turn);
                }
                for(String id:List.of("pixelmon:legend_plate","pixelmon:flame_plate","pixelmon:red_orb",Content.NS+":ash_greninjite",Content.NS+":teal_mask",Content.NS+":hearthflame_mask","pixelmon:pikashunium_z")){
                    own.pokemon.setHeldItem(Update180Cases.stack(id));check(!AuthorHeldItems.suppressed(own,own.getHeldItem()),"mechanic exempt "+id);
                }
            }
            battle.battleTurn=3;own.pokemon.setHeldItem(Update180Cases.stack("pixelmon:leftovers"));int heals=0,misses=0;
            for(int turn=3;turn<63;turn++){
                battle.battleTurn=turn;own.setHealth(own.getMaxHealth()/2);int before=own.getHealth();var item=own.getHeldItem();boolean expected=!AuthorHeldItems.blocked(own,item,"applyRepeatedEffect");item.applyRepeatedEffect(own);
                check((own.getHealth()>before)==expected,"native leftover outcome matches reserved proc");if(expected)heals++;else misses++;
            }
            check(heals>0&&misses>0,"both native leftover outcomes observed");
            int sashWorks=0,sashFails=0;
            for(int turn=70;turn<130;turn++){
                battle.battleTurn=turn;own.pokemon.setHeldItem(Update180Cases.stack("pixelmon:focus_sash"));own.setHealth(own.getMaxHealth());var item=own.getHeldItem();boolean expected=!AuthorHeldItems.blocked(own,item,"modifyDamageIncludeFixed:"+enemy.getPokemonUUID());
                double damage=item.modifyDamageIncludeFixed(own.getMaxHealth()*2,enemy,own,new Attack("Tackle"));check((damage<own.getHealth())==expected,"native sash outcome matches proc");if(expected)sashWorks++;else sashFails++;
            }
            check(sashWorks>0&&sashFails>0,"both sash outcomes observed");
            System.out.println("AUTHOR_ITEMS_PASS first three global rounds; sash/sludge/leftovers/orb/choice; masks/Z/Mega/plates/orbs exempt; native leftovers "+heals+"/"+misses+", sash "+sashWorks+"/"+sashFails);
            own.pokemon.setHeldItem(net.minecraft.item.ItemStack.field_190927_a);battle.battleTurn=150;own.setHealth(own.getMaxHealth());
            int old=own.getMaxHealth();CalamityBattle.persistent(own,old,own.getHealth(),20);int single=own.getMaxHealth();Erosion.apply(own);int doubleMax=own.getMaxHealth();
            check(((Erosion.active(own)?1:0)+(CalamityBattle.active(own)?1:0))==2&&doubleMax<single,"two independent erosion layers");
            int expected=ErosionMath.stat(own.getForm().getBattleStats().getStat(BattleStatsType.HP),40,own.pokemon.getIVs().getStat(BattleStatsType.HP),own.pokemon.getEVs().getStat(BattleStatsType.HP),own.getPokemonLevelNum(),true,0);
            check(doubleMax==expected,"combined base reduction exactly 40");
            var detached=NpcSimulationCopy.of(own);check(((Erosion.active(detached)?1:0)+(CalamityBattle.active(detached)?1:0))==2&&detached.getMaxHealth()==doubleMax,"AI copy retains both erosion sources");
            battle.battleTurn=154;Erosion.expire(battle);check(((Erosion.active(own)?1:0)+(CalamityBattle.active(own)?1:0))==2,"fifth round retains timed layer");battle.battleTurn=155;Erosion.expire(battle);
            check(((Erosion.active(own)?1:0)+(CalamityBattle.active(own)?1:0))==1&&CalamityBattle.active(own)&&own.getMaxHealth()==single,"only timed source expires");
            System.out.println("AUTHOR_EROSION_PASS total40; copied stats; five-turn expiry keeps permanent20; maxHP "+single+" -> "+doubleMax+" -> "+own.getMaxHealth());
            trainer.setBattleAI(BattleAIMode.ADVANCED.createAI(trainer));
            new com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStats(own.getBattleStats()).notifyActivePokemonOfStatChange();
            exactHits(battle,own,enemy);
            protect(battle,own,enemy);
            int recovered=0,failedRecovery=0;for(int turn=350;turn<390;turn++){battle.battleTurn=turn;own.attack=new Attack("Recover");own.selectedAttack=own.attack;own.targets=List.of(own);own.setHealth(own.getMaxHealth()/3);int hp=own.getHealth();boolean success=AuthorForesight.buff(own);var result=new MoveResults(own);own.attack.use(own,own,result);check((own.getHealth()>hp)==success,"native self recovery shares single 70% success check");if(success)recovered++;else failedRecovery++;}check(recovered>0&&failedRecovery>0,"recovery success and failure");System.out.println("AUTHOR_RECOVERY_PASS native Recover "+recovered+" heal / "+failedRecovery+" fail, one success roll");
            enemy.getStatuses().add(new com.pixelmonmod.pixelmon.battles.status.Flying());
            check(NpcIntentResponse.unreachable(own,new Attack("Tackle"),enemy,null),"avoid generic attack against airborne target");
            check(!NpcIntentResponse.unreachable(own,new Attack("Thunder"),enemy,null),"retain legal airborne Thunder");enemy.getStatuses().clear();
            var temp=Update180Cases.mon(890,"Dynamax Cannon");temp.setForm("eternamax");var restriction=new PixelmonWrapper(human,temp,0);check(!AuthorChallenge.valid(new PixelmonWrapper[]{restriction}),"player eternamax rejected");
            trainer.setBattleAI(BattleAIMode.ADVANCED.createAI(trainer));
            System.out.println("AUTHOR_APPEND_PASS native item effects, erosion, exact hit reservation, semi-invulnerability and player restriction");
        }finally{Erosion.end(battle);CalamityBattle.end(battle);AuthorForesight.end(battle);battle.battleEnded=true;party.set(0,saved);}
    }
    static void exactHits(BattleController battle,PixelmonWrapper own,PixelmonWrapper enemy)throws Exception{
        var inner=Class.forName("local.pixelmon.backport.NpcTurnKnowledge$Context");var ctor=inner.getDeclaredConstructor(BattleController.class);ctor.setAccessible(true);
        var field=NpcTurnKnowledge.class.getDeclaredField("CURRENT");field.setAccessible(true);@SuppressWarnings("unchecked") ThreadLocal<Object> context=(ThreadLocal<Object>)field.get(null);
        int successes=0,misses=0;
        for(int turn=200;turn<260;turn++){
            battle.battleTurn=turn;own.attack=new Attack("Tackle");own.selectedAttack=own.attack;own.targets=List.of(enemy);own.wait=false;own.usingZ=false;enemy.setHealth(enemy.getMaxHealth());
            boolean expected=AuthorForesight.accuracy(own,enemy,70);context.set(ctor.newInstance(battle));
            battle.simulateMode=true;MoveResults predicted;
            try{var candidate=new MoveChoice(own,own.attack.deepCopy(),List.of(enemy));((local.pixelmon.backport.mixin.NpcDamagePreviewAccess)enemy.getBattleAI()).backport$damage(own,candidate);predicted=candidate.result;}finally{battle.simulateMode=false;context.remove();}
            var result=new MoveResults(enemy);own.attack.use(own,enemy,result);
            check((result.damage>0)==expected,"actual native attack shares reserved hit roll on turn "+turn);
            check((predicted.damage>0)==expected,"native AI preview agrees with execution on turn "+turn+" predicted="+predicted.result+" damage="+predicted.damage+" accuracy="+predicted.accuracy+" expected="+expected);
            if(expected)successes++;else misses++;
        }
        check(successes>0&&misses>0,"native hit and miss cases both covered");System.out.println("AUTHOR_FORESIGHT_PASS 60 native simulation/execution pairs "+successes+" hit / "+misses+" miss");
    }
    static void protect(BattleController battle,PixelmonWrapper human,PixelmonWrapper npc)throws Exception{
        var inner=Class.forName("local.pixelmon.backport.NpcTurnKnowledge$Context");var ctor=inner.getDeclaredConstructor(BattleController.class);ctor.setAccessible(true);
        var field=NpcTurnKnowledge.class.getDeclaredField("CURRENT");field.setAccessible(true);@SuppressWarnings("unchecked") ThreadLocal<Object> context=(ThreadLocal<Object>)field.get(null);
        int guards=0,open=0;
        for(int turn=300;turn<340;turn++){
            battle.battleTurn=turn;human.attack=new Attack("Protect");human.selectedAttack=human.attack;human.targets=List.of(human);human.wait=false;human.protectsInARow=0;
            human.setHealth(human.getMaxHealth());context.set(ctor.newInstance(battle));
            try{
                var hit=NpcIntentResponse.raw(npc.getBattleAI(),npc,new Attack("Tackle"),human);boolean succeeds=AuthorForesight.protect(human);
                NpcIntentResponse.weight(npc,new ArrayList<>(List.of(hit)));
                check(hit.result!=null&&(hit.result.damage>0)!=succeeds,"NPC attack remains useful exactly when submitted Protect fails: "+hit.result.result+" damage "+hit.result.damage+" guard "+succeeds);
                if(succeeds)guards++;else open++;
            }finally{context.remove();}
        }
        check(guards>0&&open>0,"both known guard outcomes");System.out.println("AUTHOR_RESPONSE_PASS 40 submitted Protect decisions; "+guards+" guarded / "+open+" open, no false defence on failed guards");
    }
    static void lead(ServerPlayerEntity player)throws Exception{
        var party=StorageProxy.getParty(player);var saved=party.get(0);var p=Update180Cases.mon(894,"Thunder","Thunderbolt","Volt Switch","Electro Ball");party.set(0,p);
        try{for(boolean author:new boolean[]{false,true}){
            var npc=new NPCTrainer(player.field_70170_p);npc.isGymLeader=true;npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));npc.getPersistentData().func_74778_a(GymCities.TYPE,"normal");npc.getPersistentData().func_74757_a(AuthorBossTeams.MARKER,author);
            for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,null);
            npc.getPokemonStorage().set(0,Update180Cases.mon(382,"Surf","Thunder","Ice Beam","Calm Mind"));
            npc.getPokemonStorage().set(1,Update180Cases.mon(383,"Earthquake","Fire Punch","Rock Slide","Swords Dance"));
            var human=new PlayerParticipant(player,p);var trainer=new TrainerParticipant(npc,1);var battle=new BattleController(new BattleParticipant[]{human},new BattleParticipant[]{trainer},new BattleRules());battle.sendMessages=false;var init=BattleController.class.getDeclaredMethod("initBattle");init.setAccessible(true);init.invoke(battle);
            check(trainer.controlledPokemon.size()==1,"exactly one native lead, no stale provisional occupant");
            check(trainer.controlledPokemon.get(0).getSpecies().getDex()==383,"actual native send-out chooses Ground response to submitted Electric lead, author="+author);
            check(npc.getPokemonStorage().get(0).getSpecies().getDex()==382,"lead selection preserves stored team order");battle.battleEnded=true;
        }}finally{party.set(0,saved);}
        System.out.println("NPC_LEAD_PASS native startBattle chooses electric immunity for ordinary equal gym and author, stored order unchanged");
    }
    static void compass(ServerPlayerEntity player)throws Exception{
        var world=player.func_71121_q();var max=new com.pixelmonmod.pixelmon.entities.DenEntity(world);var tera=new TeraDenEntity(world);
        max.getPersistentData().func_74768_a("BackportDenKind",0);max.func_70107_b(42.5,75,16.5);tera.func_70107_b(46.5,75,16.5);
        world.func_175726_f(max.func_233580_cy_());world.func_175726_f(tera.func_233580_cy_());
        check(world.func_217376_c(max)&&world.func_217376_c(tera),"insert mixed native den fixtures");
        var box=new net.minecraft.util.math.AxisAlignedBB(40,70,14,49,80,19);
        try{
            check(max.func_233580_cy_().equals(TeraDenLocator.Candidate.find(world,box,false,p->true)),"Max search excludes live Tera subclass");
            check(tera.func_233580_cy_().equals(TeraDenLocator.Candidate.find(world,box,true,p->true)),"Tera search excludes native Max");
            max.getPersistentData().func_74768_a("BackportDenKind",1);check(TeraDenLocator.Candidate.find(world,box,false,p->true)==null,"exclude Max placeholder pending Tera conversion");max.getPersistentData().func_74768_a("BackportDenKind",0);
            tera.setDecoration(true);check(TeraDenLocator.Candidate.find(world,box,true,p->true)==null,"exclude decorative den");tera.setDecoration(false);
            var pos=max.func_233580_cy_();TeraDenLocator.found(world,player.func_110124_au(),"max",pos);
            player.func_200619_a(world,100,75,100,0,0);
            check(!TeraDenLocator.eligible(world,player,"max",pos)&&TeraDenLocator.eligible(world,player,"tera",pos),"visited positions separated by requested kind");
            var nativeDen=net.minecraftforge.registries.ForgeRegistries.STRUCTURE_FEATURES.getValue(new net.minecraft.util.ResourceLocation("pixelmon:plains_den"));
            var alias=net.minecraftforge.registries.ForgeRegistries.STRUCTURE_FEATURES.getValue(new net.minecraft.util.ResourceLocation(Content.NS+":plains_tera_den"));
            check(TeraDenLocator.handles(nativeDen)&&TeraDenLocator.handles(alias)&&TeraDenLocator.source(alias)==nativeDen,"both search keys use native candidate source");
            if(net.minecraftforge.fml.ModList.get().isLoaded("explorerscompass")){
                var compass=(net.minecraft.item.Item)Class.forName("com.chaosthedude.explorerscompass.ExplorersCompass").getField("explorersCompass").get(null);
                var stack=new net.minecraft.item.ItemStack(compass);stack.func_196082_o().func_74778_a("StructureKey","pixelmon:plains_den");stack.func_196082_o().func_74768_a("FoundX",1234);
                var worker=Class.forName("com.chaosthedude.explorerscompass.util.StructureSearchWorker");
                var ctor=worker.getConstructor(net.minecraft.world.server.ServerWorld.class,net.minecraft.entity.player.PlayerEntity.class,net.minecraft.item.ItemStack.class,net.minecraft.world.gen.feature.structure.Structure.class,net.minecraft.util.math.BlockPos.class);
                Object search=ctor.newInstance(world,player,stack,nativeDen,player.func_233580_cy_());
                stack.func_196082_o().func_74778_a("StructureKey",Content.NS+":plains_tera_den");
                check(!(boolean)worker.getMethod("hasWork").invoke(search),"changing kind cancels old Max worker");
                var finish=worker.getDeclaredMethod("finish",boolean.class);finish.setAccessible(true);finish.invoke(search,true);
                check(stack.func_196082_o().func_74762_e("FoundX")==1234&&stack.func_196082_o().func_74779_i("StructureKey").endsWith("plains_tera_den"),"stale worker cannot overwrite new search");
                Object config=Class.forName("com.chaosthedude.explorerscompass.config.ConfigHandler").getField("GENERAL").get(null);
                var clean=(net.minecraftforge.common.ForgeConfigSpec.BooleanValue)config.getClass().getField("cleanCache").get(config);boolean previous=clean.get();
                try{clean.set(true);stack.func_196082_o().func_74778_a("StructureKey","pixelmon:plains_den");Object retry=ctor.newInstance(world,player,stack,nativeDen,player.func_233580_cy_());TeraDenLocator.found(world,player.func_110124_au(),"pixelmon:plains_den",pos);check(!TeraDenLocator.eligible(world,player,"pixelmon:plains_den",pos),"typed cache records visit");finish.invoke(retry,false);check(TeraDenLocator.eligible(world,player,"pixelmon:plains_den",pos),"failed search honors native cleanCache preference");}finally{clean.set(previous);}
                System.out.println("DEN_COMPASS_WORKER_PASS actual installed compass, Max stale search cancelled, finish blocked, typed failure cache reset");
            }
            System.out.println("DEN_COMPASS_PASS mixed native entities select exact kind; pending/decoration excluded; typed visited cache; original structure mapping");
        }finally{max.func_70106_y();tera.func_70106_y();}
    }
}
