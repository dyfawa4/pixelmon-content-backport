package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBall;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.logic.BallLogic;
import com.pixelmonmod.pixelmon.entities.pokeballs.PokeBallEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
/** Native successful-capture callback only. Already Shiny Pokemon stay Shiny. */
public final class SpectralBallLogic implements BallLogic {
 public static final String NAME="spectral_ball";
 public static boolean shiny(java.util.Random random){return random.nextBoolean();}
 @Override public void doAfterEffect(PokeBall ball,PokeBallEntity thrown,PixelmonEntity target){
  if(target!=null&&!target.field_70170_p.field_72995_K&&ball!=null&&NAME.equals(ball.getName())&&shiny(target.func_70681_au()))target.getPokemon().setShiny(true);
 }
}
