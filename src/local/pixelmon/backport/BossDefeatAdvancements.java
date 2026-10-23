package local.pixelmon.backport;

import java.util.Set;
import com.pixelmonmod.pixelmon.api.events.BeatWildPixelmonEvent;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Native victory event, independent of reward rolls and inventory capacity. */
public final class BossDefeatAdvancements {
    private static final Set<String> TIERS=Set.of("common","uncommon","rare","epic","legendary","ultimate","drowned");
    @SubscribeEvent public void defeated(BeatWildPixelmonEvent event){
        if(event.player==null||event.player.field_70170_p.field_72995_K||event.wpp==null||event.wpp.bc==null||event.wpp.bc.simulateMode)return;
        for(var pokemon:event.wpp.controlledPokemon){
            if(pokemon.entity==null||!pokemon.isFainted())continue;
            String tier=pokemon.entity.getBossTier().getID();if(!TIERS.contains(tier))continue;
            var advancement=event.player.func_71121_q().func_73046_m().func_191949_aK().func_192778_a(new ResourceLocation(Content.NS,"adventure/defeat_boss_"+tier));
            if(advancement!=null)event.player.func_192039_O().func_192750_a(advancement,"earned");
        }
    }
}
