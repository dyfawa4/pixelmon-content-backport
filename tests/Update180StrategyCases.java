package qa;
import java.util.*;
import java.nio.file.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.ai.*;
import com.pixelmonmod.pixelmon.battles.controller.log.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import net.minecraft.entity.player.ServerPlayerEntity;

/** User-requested native battle scenarios, including actual Baton Pass and faint-count scaling. */
public final class Update180StrategyCases {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static ThreadLocal<Object> context;static java.lang.reflect.Constructor<?> scope;
    static void bind(BattleController b)throws Exception{context.set(scope.newInstance(b));}
    public static void run(ServerPlayerEntity player)throws Exception{
        var field=NpcTurnKnowledge.class.getDeclaredField("CURRENT");field.setAccessible(true);context=(ThreadLocal<Object>)field.get(null);
        scope=Class.forName("local.pixelmon.backport.NpcTurnKnowledge$Context").getDeclaredConstructor(BattleController.class);scope.setAccessible(true);
        var party=StorageProxy.getParty(player);Pokemon[] saved=party.getAll().clone();List<String> report=new ArrayList<>();
        try{for(String theme:List.of("plains","ocean","desert","swamp"))for(boolean pass:new boolean[]{true,false}){
            var lead=Update180Cases.mon(pass?313:902,pass?new String[]{"Tail Glow","Baton Pass","Bug Buzz","Protect"}:new String[]{"Wave Crash","Last Respects","Aqua Jet","Flip Turn"});
            var receiver=Update180Cases.mon(898,"Astral Barrage","Psychic","Draining Kiss","Nasty Plot");receiver.setForm("shadowrider");receiver.getEVs().fillFromArray(6,0,0,252,0,252);for(var stat:List.of(BattleStatsType.HP,BattleStatsType.ATTACK,BattleStatsType.DEFENSE,BattleStatsType.SPECIAL_ATTACK,BattleStatsType.SPECIAL_DEFENSE,BattleStatsType.SPEED))receiver.getIVs().setStat(stat,31);receiver.setMintNature(Nature.TIMID);receiver.heal();
            for(int i=0;i<6;i++)party.set(i,i==0?lead:i==1&&pass?receiver:Update180Cases.mon(129,"Splash"));
            var npc=AuthorBossTeams.create(player.func_71121_q(),new net.minecraft.util.math.BlockPos(80,74,80),theme);
            var human=new PlayerParticipant(player,lead);var trainer=new TrainerParticipant(npc,1);var b=new BattleController(new BattleParticipant[]{human},new BattleParticipant[]{trainer},new BattleRules());b.sendMessages=false;
            var init=BattleController.class.getDeclaredMethod("initBattle");init.setAccessible(true);init.invoke(b);
            try{
                var foe=human.controlledPokemon.get(0);
                if(pass){
                    boolean raised=false;for(int t=0;t<12&&!raised;t++){
                        b.battleTurn=t;foe.attack=new Attack("Tail Glow");foe.selectedAttack=foe.attack;foe.targets=new ArrayList<>(List.of(foe));foe.useAttack(true);raised=foe.getBattleStats().getStage(BattleStatsType.SPECIAL_ATTACK)>=3;
                    }
                    check(raised,"actual Tail Glow succeeded");foe.attack=new Attack("Baton Pass");foe.selectedAttack=foe.attack;foe.targets=new ArrayList<>(List.of(foe));foe.useAttack(true);
                    b.switchPokemon(foe.getPokemonUUID(),receiver.getUUID(),true);foe=human.controlledPokemon.get(0);foe.takenTurn=false;
                    check(foe.getSpecies().getDex()==898&&foe.getForm().isForm("shadowrider")&&foe.getBattleStats().getStage(BattleStatsType.SPECIAL_ATTACK)==3,"native Baton Pass transfers +3 to shadow rider");
                    foe.pokemon.setHeldItem(Update180Cases.stack("pixelmon:focus_sash"));
                    System.out.println("STRATEGY_BATON_PASS "+theme+" actual Volbeat Tail Glow -> Calyrex-Shadow +3");
                }else{
                    foe.pokemon.setAbility(new com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.SwiftSwim());
                    foe.pokemon.getEVs().fillFromArray(0,252,0,0,6,252);for(var stat:List.of(BattleStatsType.HP,BattleStatsType.ATTACK,BattleStatsType.DEFENSE,BattleStatsType.SPECIAL_ATTACK,BattleStatsType.SPECIAL_DEFENSE,BattleStatsType.SPEED))foe.pokemon.getIVs().setStat(stat,31);foe.pokemon.setMintNature(Nature.ADAMANT);
                    foe.pokemon.setHeldItem(Update180Cases.stack("pixelmon:choice_band"));
                    for(var mon:human.allPokemon)if(mon!=foe){mon.setHealth(0);human.incrementFaintCount();}
                    check(human.getTotalFainted()==5,"five fainted teammates counted by native participant");
                    foe.attack=new Attack("Last Respects");foe.selectedAttack=foe.attack;
                    for(var effect:foe.attack.getMove().effects)effect.applyEffectStart(foe,trainer.controlledPokemon.get(0));
                    check(foe.attack.overridePower==300,"native Last Respects reaches 300 base power");
                    b.globalStatusController.addGlobalStatus(foe,new com.pixelmonmod.pixelmon.battles.status.Rainy(-1));b.modifyStats();
                    System.out.println("STRATEGY_BASCULEGION "+theme+" five actual faint slots; native Last Respects 300; rain Swift Swim + Choice Band");
                }
                for(int round:new int[]{2,5})for(String move:pass?List.of("Astral Barrage"):List.of("Wave Crash","Last Respects")){
                    b.battleTurn=round;var own=trainer.controlledPokemon.get(0);foe.attack=new Attack(move);foe.selectedAttack=foe.attack;foe.targets=new ArrayList<>(List.of(own));foe.wait=false;foe.takenTurn=false;own.takenTurn=false;own.wait=false;own.choiceLocked=null;b.modifyStats();
                    if(round>=3)for(int retry=0;retry<30&&!AuthorForesight.accuracy(foe,own,70);retry++)b.battleTurn++;
                    trainer.switchingIn.clear();bind(b);
                    try{
                        MoveChoice choice=trainer.getMove(own);check(choice!=null,"native AdvancedAI returned a legal action");
                        double incoming=NpcTactics.incoming(own,NpcTactics.threats(own,choice));
                        boolean survives=incoming<own.getHealth(),finishes=choice.isOffensiveMove()&&NpcTactics.finishFirst(own,choice,NpcTactics.threats(own,choice));
                        String action=choice.isAttack()?choice.attack.getMove().getAttackName():"switch:"+choice.switchPokemon;
                        if(choice.switchPokemon!=null){
                            var reserve=trainer.getPokemonFromParty(choice.switchPokemon);var outgoing=trainer.controlledPokemon.get(0);trainer.controlledPokemon.set(0,reserve);boolean fielded=reserve.onBattlefield;reserve.onBattlefield=true;
                            try{var hit=NpcIntentResponse.raw(own.getBattleAI(),foe,foe.attack,reserve);incoming=hit.result==null?0:Math.max(hit.result.damage,hit.result.fullDamage);survives=incoming<reserve.getHealth();action="switch:"+reserve.getSpecies().getName()+"/"+reserve.getForm().getName();}
                            finally{reserve.onBattlefield=fielded;trainer.controlledPokemon.set(0,outgoing);}
                        }
                        String row=theme+" "+(pass?"TailGlow+BatonPass Calyrex":"Basculegion")+" turn="+(b.battleTurn+1)+" move="+move+" own="+own.getSpecies().getName()+" action="+action+" incoming="+(int)incoming+" hp="+own.getHealth()+" safe="+(survives||finishes);
                        report.add(row);System.out.println("STRATEGY_CHOICE "+row);
                        check(Float.isFinite(choice.weight),"strategy weight finite");
                        check(survives||finishes,"native AI must avoid a losing action in this prepared counter scenario: "+row);
                        for(var reserve:trainer.allPokemon){var clone=NpcSimulationCopy.of(reserve);check(clone.getHealth()==reserve.getHealth()&&clone.getMaxHealth()==reserve.getMaxHealth()&&clone.getPokemonLevelNum()==reserve.getPokemonLevelNum(),"detached equal-level snapshot retains level and HP");}
                        if(pass&&theme.equals("plains")&&round>=3){
                            var attack=own.getMoveset().stream().filter(a->a.getAttackCategory()!=com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS).findFirst().orElseThrow();
                            var doomed=NpcIntentResponse.raw(own.getBattleAI(),own,attack,foe);double danger=NpcTactics.incoming(own,NpcTactics.threats(own,doomed));
                            if(danger*1.1>=own.getHealth()&&!NpcTactics.finishFirst(own,doomed,NpcTactics.threats(own,doomed))){trainer.switchingIn.clear();var rescue=NpcEmergencySwitch.refine(own,doomed);check(rescue.switchPokemon!=null,"a doomed attack is replaced with the available legal counter");System.out.println("STRATEGY_RESCUE_PASS explicit lethal +3 attack rescued by legal reserve");}
                        }
                    }finally{context.remove();}
                }
            }finally{context.remove();Erosion.end(b);CalamityBattle.end(b);AuthorForesight.end(b);NpcTurnKnowledge.end(b);GymChallenge.end(b);b.battleEnded=true;}
        }}finally{for(int i=0;i<6;i++)party.set(i,i<saved.length?saved[i]:null);}
        Files.write(Path.of("strategy180-results.txt"),report);System.out.println("STRATEGY_180_PASS "+report.size()+" native AdvancedAI responses logged");
    }
}
