package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.pokemon.ItemFormChangeEvent;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Arrays;

/** Explicit survival adaptation: owned Furfrou trims cycle with vanilla shears. */
@Mod.EventBusSubscriber(modid=Content.NS)
public final class Grooming {
    public static final String[] FORMS={"natural","dandy","debutante","heart","matron","star"};
    private Grooming(){}
    public static boolean apply(ServerPlayerEntity player,PixelmonEntity entity,Hand hand){
        Pokemon p=entity.getPokemon();ItemStack tool=player.func_184586_b(hand);
        if(hand!=Hand.MAIN_HAND || tool.func_77973_b()!=Items.field_151097_aZ || p.getSpecies().getDex()!=676
            || p.isEgg() || !player.func_110124_au().equals(p.getOwnerPlayerUUID())
            || BattleRegistry.getBattle(player)!=null || entity.battleController!=null)return false;
        int current=Arrays.asList(FORMS).indexOf(p.getForm().getName());String next=FORMS[(current+1)%FORMS.length];
        if(Pixelmon.EVENT_BUS.post(new ItemFormChangeEvent(player,tool,entity,null)))return false;
        p.setForm(next);
        if(!player.field_71075_bZ.field_75098_d)tool.func_222118_a(1,player,who->who.func_213334_d(hand));
        player.func_146105_b(new TranslationTextComponent("pixelmon.backport.groom.changed",p.getFormattedDisplayName(),new TranslationTextComponent("pixelmon.backport.groom."+next)),true);
        return true;
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract e){
        if(e.getPlayer() instanceof ServerPlayerEntity && e.getTarget() instanceof PixelmonEntity
            && apply((ServerPlayerEntity)e.getPlayer(),(PixelmonEntity)e.getTarget(),e.getHand())){
            e.setCancellationResult(net.minecraft.util.ActionResultType.SUCCESS);e.setCanceled(true);
        }
    }
}
