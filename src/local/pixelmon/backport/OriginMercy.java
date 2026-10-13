package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
public final class OriginMercy {
    private OriginMercy(){}
    public static boolean applies(int dex,String move,boolean wild,boolean raid,boolean canCapture){return dex==1026 && "Origin of Thunder".equals(move) && wild && !raid && canCapture;}
    public static boolean captureAllowed(boolean wild,boolean raid,boolean boss,boolean blocked,boolean owned,boolean bagRestricted,int liveOpponents){
        return wild && !raid && !boss && !blocked && !owned && !bagRestricted && liveOpponents==1;
    }
    /** Mirror native canCatchOpponent and ChooseBag's multi-target/BAG restrictions. */
    public static boolean canCapture(PlayerParticipant player,PixelmonWrapper target){
        if(player==null || target==null || target.bc==null || target.bc!=player.bc || target.bc.battleEnded
            || target.bc.simulateMode || target.getParticipant().team==player.team || !target.onBattlefield || !target.isAlive() || target.entity==null)return false;
        int opponents=0;
        for(var enemy:player.getOpponents())for(var pokemon:enemy.allPokemon)if(pokemon.onBattlefield && pokemon.isAlive())opponents++;
        return captureAllowed(target.getParticipant() instanceof WildPixelmonParticipant,target.bc.isRaid() || TeraRaids.get(target.bc)!=null,
            target.entity.getBossTier().isBoss(),target.pokemon.isUncatchable() || target.pokemon.isEgg(),
            target.entity.hasOwner() || target.entity.getTrainer()!=null,
            target.bc.rules.hasClause(com.pixelmonmod.pixelmon.battles.api.rules.clauses.BattleClauseRegistry.BAG_CLAUSE),opponents);
    }
    public static boolean applies(PixelmonWrapper user,PixelmonWrapper target,String move){
        return user!=null && target!=null && target.bc!=null && user.getParticipant() instanceof PlayerParticipant
            && applies(user.getSpecies().getDex(),move,target.getParticipant() instanceof WildPixelmonParticipant,
                target.bc.isRaid() || TeraRaids.get(target.bc)!=null,canCapture((PlayerParticipant)user.getParticipant(),target));
    }
}
