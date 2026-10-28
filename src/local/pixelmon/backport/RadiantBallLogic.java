package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.logic.BallLogic;
import com.pixelmonmod.pixelmon.entities.pokeballs.PokeBallEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;

/** Native success callback, shared by field, battle and raid capture. Never runs on a ball change. */
public final class RadiantBallLogic implements BallLogic {
    public static final String NAME="radiant_ball";
    @Override public void doAfterEffect(PokeBall ball,PokeBallEntity thrown,PixelmonEntity target){
        if(target!=null&&!target.field_70170_p.field_72995_K&&ball!=null
            &&NAME.equals(ball.getName())&&!CapBallPreference.forbidden(ball,target.getPokemon()))
            target.getPokemon().setShiny(true);
    }
}
