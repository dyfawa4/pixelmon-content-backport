package local.pixelmon.backport;
import java.util.Set;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.items.HeldItem;
import com.pixelmonmod.pixelmon.items.heldItems.*;

/** Suppresses effect callbacks without removing, consuming or replacing the stored item. */
public final class AuthorHeldItems {
    private AuthorHeldItems(){}
    public static boolean mechanic(HeldItem item){
        if(item instanceof MegaStoneItem||item instanceof ZCrystalItem||item instanceof ReferenceItems.Meteorite||item instanceof Backport.Mask)return true;
        if(item==null||item.getRegistryName()==null)return true;
        String id=item.getRegistryName().func_110623_a();
        return id.endsWith("_plate")||id.endsWith("_memory")||id.endsWith("_drive")||Set.of("red_orb","blue_orb","rusted_sword","rusted_shield","griseous_core","griseous_orb","adamant_crystal","lustrous_globe").contains(id);
    }
    public static boolean suppressed(PixelmonWrapper owner,HeldItem item){return AuthorForesight.player(owner)&&!mechanic(item)&&owner.bc.battleTurn<3;}
    public static boolean blocked(PixelmonWrapper owner,HeldItem item,String trigger){
        return suppressed(owner,item);
    }
}
