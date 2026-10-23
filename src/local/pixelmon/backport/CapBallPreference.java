package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.eventbus.api.*;

/** A wild individual's saved preference is shared by field and battle capture. */
public final class CapBallPreference {
    public static final String TAG="BackportCapPreferredBall";
    public static final List<String> BALLS=List.of("poke_ball","great_ball","ultra_ball","fast_ball","level_ball","lure_ball","moon_ball","friend_ball","love_ball","heavy_ball","sport_ball","premier_ball","repeat_ball","timer_ball","nest_ball","net_ball","dive_ball","luxury_ball","heal_ball","quick_ball","dusk_ball","dream_ball");
    public static boolean cap(Pokemon pokemon){return pokemon!=null&&!pokemon.isEgg()&&pokemon.getSpecies().getDex()==CapSpecies.DEX;}
    public static String selected(Pokemon pokemon){
        if(!cap(pokemon))return "";
        var data=pokemon.getPersistentData();String saved=data.func_74779_i(TAG);
        if(BALLS.contains(saved))return saved;
        UUID id=pokemon.getUUID();saved=BALLS.get(new Random(id.getMostSignificantBits()^id.getLeastSignificantBits()).nextInt(BALLS.size()));
        data.func_74778_a(TAG,saved);return saved;
    }
    public static boolean forbidden(PokeBall ball,Pokemon pokemon){return cap(pokemon)&&ball!=null&&Set.of("master_ball","origin_ball","park_ball").contains(ball.getName());}
    public static boolean preferred(PokeBall ball,Pokemon pokemon){return cap(pokemon)&&ball!=null&&ball.getName().equals(selected(pokemon));}
    public static double multiplier(PokeBall ball,Pokemon pokemon,double nativeBonus){return forbidden(ball,pokemon)?0:preferred(ball,pokemon)?2:nativeBonus*.5;}
    public static void announce(ServerPlayerEntity player,Pokemon pokemon){
        if(player==null||!cap(pokemon))return;
        String ball=selected(pokemon);
        player.func_146105_b(new TranslationTextComponent("pixelmon.backport.cap.capture_warning",new net.minecraft.util.text.StringTextComponent(PokeBallRegistry.getPokeBall(ball).getValueUnsafe().getLocalizedName()).func_240699_a_(net.minecraft.util.text.TextFormatting.GREEN)),false);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public void spawn(net.minecraftforge.event.entity.EntityJoinWorldEvent event){
        if(event.isCanceled()||event.getWorld().field_72995_K||!(event.getEntity() instanceof PixelmonEntity))return;
        PixelmonEntity entity=(PixelmonEntity)event.getEntity();Pokemon pokemon=entity.getPokemon();
        if(entity.hasOwner()||entity.getTrainer()!=null||!cap(pokemon))return;
        boolean first=!BALLS.contains(pokemon.getPersistentData().func_74779_i(TAG));selected(pokemon);
        if(first&&event.getWorld() instanceof net.minecraft.world.server.ServerWorld)
            for(var player:((net.minecraft.world.server.ServerWorld)event.getWorld()).func_217369_A())if(entity.func_70068_e(player)<=128*128)announce(player,pokemon);
    }
    public static void stored(Pokemon pokemon){
        if(cap(pokemon)&&AcquisitionAdvancements.ownedStored(pokemon.getOwnerPlayer(),pokemon))pokemon.setFriendship(255);
    }
}
