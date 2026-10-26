package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.items.HeldItem;
import com.pixelmonmod.pixelmon.enums.heldItems.EnumHeldItems;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.Klutz;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.StatusType;
import net.minecraft.item.Item;
/** Native item usability without getBattleAbility() recursion while testing ability suppression. */
public final class AbilityShield extends HeldItem {
    public AbilityShield(){super(EnumHeldItems.other,new Item.Properties());}
    public static boolean active(PixelmonWrapper mon){
        if(mon==null||!(mon.getHeldItem() instanceof AbilityShield)||mon.getBattleAbility(false) instanceof Klutz)return false;
        if(AuthorHeldItems.blocked(mon,mon.getHeldItem(),"ability-shield"))return false;
        return !mon.hasStatus(StatusType.Embargo,StatusType.CorrosiveGas)
            &&(mon.bc==null||mon.bc.globalStatusController==null||!mon.bc.globalStatusController.hasStatus(StatusType.MagicRoom));
    }
    public static void blocked(PixelmonWrapper mon){if(mon.bc!=null)mon.bc.sendToAll("pixelmon.backport.ability_shield.protect",mon.getNickname());}
}
