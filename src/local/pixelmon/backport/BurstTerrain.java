package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.battles.status.*;
import com.pixelmonmod.pixelmon.battles.controller.GlobalStatusController;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;

/** Battle-owned terrain instance; all native Electric Terrain behaviour is inherited. */
public final class BurstTerrain extends ElectricTerrain {
    public final int team;
    public BurstTerrain(int team){super(false);this.team=team;langStart="pixelmon.backport.burst.start";langEnd="pixelmon.backport.burst.end";}
    @Override public Terrain getNewInstance(){return new BurstTerrain(team);}
    @Override public void applyRepeatedEffect(GlobalStatusController controller){
        // No turn countdown. Native replacement/removal owns its entire lifetime.
        applyRepeatedEffect(controller.bc);
    }
    @Override public int getRemainingTurns(){return -1;}
    public boolean benefitsGrounded(PixelmonWrapper pokemon){return affectsPokemon(pokemon);}
    public static BurstTerrain current(com.pixelmonmod.pixelmon.battles.controller.BattleController bc){
        if(bc==null || bc.battleEnded)return null;
        Terrain terrain=bc.globalStatusController.getTerrain();return terrain instanceof BurstTerrain?(BurstTerrain)terrain:null;
    }
    public static double effectiveness(PixelmonWrapper user,PixelmonWrapper target,com.pixelmonmod.pixelmon.battles.attacks.Attack attack,double value){
        BurstTerrain field=current(user.bc);if(field==null || attack.getAttackCategory()==com.pixelmonmod.pixelmon.api.battles.AttackCategory.STATUS)return value;
        return BurstPolicy.effectiveness(value,user.getParticipant().team==field.team,user.hasType(com.pixelmonmod.pixelmon.api.pokemon.Element.ELECTRIC),
            attack.getType()==com.pixelmonmod.pixelmon.api.pokemon.Element.ELECTRIC,target.getParticipant().team==field.team,target.hasType(com.pixelmonmod.pixelmon.api.pokemon.Element.ELECTRIC));
    }
}
