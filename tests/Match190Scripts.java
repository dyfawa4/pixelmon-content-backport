package qa;
import java.util.*;
import com.google.gson.*;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.ai.MoveChoice;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.status.*;
/** Explicit challenger scripts for reproducible exploit attempts, all using native legal moves. */
public final class Match190Scripts {
    static JsonArray changes;static final Map<BattleParticipant,PixelmonWrapper> active=new IdentityHashMap<>();
    static boolean hazard(){return Conventional190Matches.pendingSource!=null&&Conventional190Matches.pendingSource.get("name").getAsString().startsWith("hazard-");}
    static boolean ohko(){return Conventional190Matches.pendingSource!=null&&Conventional190Matches.pendingSource.get("name").getAsString().startsWith("ohko-");}
    static JsonArray actions;static boolean registered;
    static final List<Pending> pending=new ArrayList<>();
    static final class Pending {
        final PixelmonWrapper user,target;final Attack attack;final com.pixelmonmod.pixelmon.battles.controller.log.MoveResults result;final int hp,turn;final boolean room;
        Pending(com.pixelmonmod.pixelmon.api.events.battles.AttackEvent.Use e){user=e.user;target=e.target;attack=e.getAttack();result=attack.moveResult;hp=target.getHealth();turn=user.bc.battleTurn;room=user.bc.globalStatusController.hasStatus(StatusType.TrickRoom);}
    }
    public static final class Trace {
        @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
        public void used(com.pixelmonmod.pixelmon.api.events.battles.AttackEvent.Use e){if(ohko()&&e.user.bc==Conventional190Matches.battle&&!e.user.bc.simulateMode)pending.add(new Pending(e));}
    }
    static MoveChoice move(PixelmonWrapper own,String name){
        for(var a:own.getMoveset())if(a.getMove().getAttackName().equals(name)&&a.pp>0&&!a.getDisabled()
            &&!(own.getStatus(StatusType.Taunt)!=null&&a.getAttackCategory()==com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS)
            &&(own.choiceLocked==null||own.choiceLocked.getMove().equals(a.getMove()))){var targets=own.getTargets(a);if(!targets.isEmpty())return new MoveChoice(own,a,targets);}
        return null;
    }
    public static MoveChoice choose(PixelmonWrapper own){
        if(ohko())return ohkoMove(own);
        if(!hazard())return null;
        String last=own.lastAttack==null?"":own.lastAttack.getMove().getAttackName(),wanted=null;
        int dex=own.getSpecies().getDex();
        if(dex==227){
            var hazards=own.getOpponentPokemon().get(0).getEntryHazards();
            if(hazards.stream().noneMatch(s->s instanceof Spikes))wanted="Spikes";
            else if(hazards.stream().noneMatch(s->s instanceof StealthRock))wanted="Stealth Rock";
            else wanted=own.getHealthPercent()<70?"Roost":"Brave Bird";
        }else if(dex==748)wanted=last.equals("Protect")?"Baneful Bunker":"Protect";
        else if(dex==226)wanted=last.equals("Protect")?"Wide Guard":"Protect";
        else if(dex==113||dex==242)wanted=own.getHealthPercent()<65&&!last.equals("Soft-Boiled")?"Soft-Boiled":"Protect";
        else wanted=own.getHealthPercent()<65&&!last.equals("Recover")?"Recover":"Protect";
        return move(own,wanted);
    }
    static MoveChoice ohkoMove(PixelmonWrapper own){
        boolean room=own.bc.globalStatusController.hasStatus(StatusType.TrickRoom);
        if(own.getSpecies().getDex()==488){
            if(!room){var c=move(own,"Trick Room");if(c!=null)return c;return null;}
            if(own.getHealthPercent()<30){var c=move(own,"Lunar Dance");if(c!=null)return c;}
            var next=Arrays.stream(own.getParticipant().allPokemon).filter(p->p.isAlive()&&!p.onBattlefield&&p.getSpecies().getDex()!=488).max(Comparator.comparingDouble(PixelmonWrapper::getHealthPercent)).orElse(null);
            if(next!=null)return new MoveChoice(own,next.getPokemonUUID());return null;
        }
        var target=own.getOpponentPokemon().get(0);
        for(String name:List.of("Sheer Cold","Fissure","Horn Drill","Guillotine")){
            var candidate=move(own,name);if(candidate==null)continue;
            if(name.equals("Sheer Cold")&&target.hasType(com.pixelmonmod.pixelmon.api.pokemon.Element.ICE))continue;
            if(name.equals("Fissure")&&(target.hasType(com.pixelmonmod.pixelmon.api.pokemon.Element.FLYING)||target.getBattleAbility(own).getClass().getSimpleName().equals("Levitate")))continue;
            if((name.equals("Horn Drill")||name.equals("Guillotine"))&&target.hasType(com.pixelmonmod.pixelmon.api.pokemon.Element.GHOST))continue;
            if(target.getBattleAbility(own).getClass().getSimpleName().equals("Sturdy")||target.getPokemonLevelNum()>own.getPokemonLevelNum())continue;
            return candidate;
        }
        return null;
    }
    public static void start(){
        changes=new JsonArray();active.clear();
        pending.clear();actions=new JsonArray();
        if(ohko()){
            if(!registered){com.pixelmonmod.pixelmon.Pixelmon.EVENT_BUS.register(new Trace());registered=true;}
            if(Conventional190Matches.pendingSource.has("preloaded_room")&&Conventional190Matches.pendingSource.get("preloaded_room").getAsBoolean()){
                Conventional190Matches.bot.controlledPokemon.get(0).addGlobalStatus(new TrickRoom());
                Conventional190Matches.result.addProperty("setup","controlled initial Trick Room; not claimed guaranteed natural setup");
            }
            Conventional190Matches.result.add("native_actions",actions);Conventional190Matches.result.add("field_changes",changes);observe();return;
        }
        if(!hazard())return;
        if(Conventional190Matches.pendingSource.has("preloaded")&&Conventional190Matches.pendingSource.get("preloaded").getAsBoolean()){
            var foe=Conventional190Matches.defender.controlledPokemon.get(0);var source=Conventional190Matches.bot.controlledPokemon.get(0);
            foe.addStatus(new Spikes(),source);foe.addStatus(new StealthRock(),source);
            Conventional190Matches.result.addProperty("setup","controlled post-setup position: one Spikes layer and Stealth Rock; not claimed naturally guaranteed");
        }
        Conventional190Matches.result.add("field_changes",changes);
        observe();
    }
    public static void observe(){
        if(ohko()&&actions!=null){for(var p:pending){var r=new JsonObject();r.addProperty("turn",p.turn);r.addProperty("side",p.user.getParticipant()==Conventional190Matches.bot?"challenger":"defender");r.addProperty("user",p.user.getSpecies().getName());r.addProperty("target",p.target.getSpecies().getName());r.addProperty("move",p.attack.getMove().getAttackName());r.addProperty("room",p.room);r.addProperty("target_hp_before",p.hp);r.addProperty("damage",p.result==null?0:p.result.damage);r.addProperty("result",p.result==null?"none":p.result.result.toString());r.addProperty("accuracy",p.result==null?-999:p.result.accuracy);actions.add(r);}pending.clear();}
        if(changes==null||!hazard()&&!ohko()||Conventional190Matches.battle.battleEnded)return;
        for(var side:Conventional190Matches.battle.participants)if(!side.controlledPokemon.isEmpty()){
            var now=side.controlledPokemon.get(0);var old=active.put(side,now);
            if(old!=null&&!old.getPokemonUUID().equals(now.getPokemonUUID())){
                var r=new JsonObject();r.addProperty("turn",side.bc.battleTurn);r.addProperty("side",side==Conventional190Matches.bot?"challenger":"defender");r.addProperty("from",old.getSpecies().getName());r.addProperty("to",now.getSpecies().getName());r.addProperty("old_alive",old.isAlive());r.addProperty("arrival_hp",now.getHealth());r.addProperty("arrival_max_hp",now.getMaxHealth());JsonArray hazards=new JsonArray();int damage=0;
                for(var h:now.getEntryHazards()){hazards.add(h.type.toString());if(!h.isImmune(now))damage+=Math.max(0,h.getDamage(now));}r.add("hazards",hazards);r.addProperty("native_entry_damage",damage);changes.add(r);
            }
        }
    }
}
