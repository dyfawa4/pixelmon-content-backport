package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.SpawnPixelmonEntityForBattleEvent;
import com.pixelmonmod.pixelmon.api.pokemon.ability.AbilityRegistry;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.status.StatusBase;
import com.pixelmonmod.pixelmon.comm.EnumUpdateType;

/** Native revive lifecycle, also supporting synthetic raid allies with no party storage. */
public final class TeraRevival {
    private TeraRevival(){}
    public static boolean revive(BattleController battle,BattleParticipant owner,PixelmonWrapper fallen){
        if(battle==null || battle.battleEnded || battle.simulateMode || owner==null || owner.bc!=battle
            || fallen==null || fallen.getParticipant()!=owner || owner.getEntity()==null)return false;
        if(fallen.onBattlefield && fallen.isAlive())return true;
        var storage=owner.getStorage();
        var pokemon=storage==null?fallen.pokemon:storage.find(fallen.getPokemonUUID());
        // A player's removed Pokemon must never be reconstructed from a battle shadow.
        if(owner instanceof PlayerParticipant && storage==null)return false;
        if(pokemon==null || pokemon.getSpecies().getDex()<=0 || !pokemon.getUUID().equals(fallen.getPokemonUUID()))return false;
        if(Pixelmon.EVENT_BUS.post(new SpawnPixelmonEntityForBattleEvent.Pre(pokemon)))return false;
        List<StatusBase> team=new ArrayList<>();
        if(!owner.controlledPokemon.isEmpty())for(var status:owner.controlledPokemon.get(0).getStatuses())if(status.isWholeTeamStatus())team.add(status.copy());
        int before=fallen.getHealth();fallen.healByPercent(100);
        var entity=pokemon.getOrSpawnPixelmon(owner.getEntity());
        if(entity==null){fallen.setHealth(before);return false;}
        entity.func_70606_j(fallen.getHealth());entity.battleController=battle;
        fallen.entity=entity;entity.setPixelmonToFlyForBattle();
        int position=0;Set<Integer> occupied=new HashSet<>();
        for(var member:owner.controlledPokemon)if(member!=fallen)occupied.add(member.battlePosition);
        while(occupied.contains(position))position++;
        owner.controlledPokemon.remove(fallen);owner.controlledPokemon.add(fallen);
        fallen.battlePosition=position;fallen.onBattlefield=true;fallen.bc=battle;
        fallen.isSwitching=false;fallen.wait=false;fallen.newPokemonUUID=null;owner.isDefeated=false;
        fallen.update(EnumUpdateType.HP);
        Pixelmon.EVENT_BUS.post(new SpawnPixelmonEntityForBattleEvent.Post(pokemon,entity));
        for(var status:team)fallen.addStatus(status,fallen);
        if(!AbilityRegistry.ignoreAbility(fallen,battle))fallen.getBattleAbility().beforeSwitch(fallen);
        for(var participant:battle.participants)participant.updateOtherPokemon();
        fallen.afterSwitch();fallen.addAttackers();battle.sendSwitchPacket(null,fallen);
        return true;
    }
}
