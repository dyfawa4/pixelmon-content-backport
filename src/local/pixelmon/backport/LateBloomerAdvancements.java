package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;

/** Permanent acquisition milestones, shared by real party and PC insertions. */
public final class LateBloomerAdvancements {
    private LateBloomerAdvancements(){}
    public static String target(int dex,String form){
        switch(dex){
            case 149:return "dragonite";
            case 248:return "tyranitar";
            case 373:return "salamence";
            case 376:return "metagross";
            case 445:return "garchomp";
            case 635:return "hydreigon";
            case 706:return "hisuian".equals(form)?"hisuian_goodra":"".equals(form)?"goodra":null;
            case 784:return "kommo_o";
            case 887:return "dragapult";
            case 998:return "baxcalibur";
            default:return null;
        }
    }
    public static void acquired(ServerPlayerEntity player,Pokemon pokemon){
        if(!AcquisitionAdvancements.ownedStored(player,pokemon) || pokemon.getForm()==null)return;
        String id=target(pokemon.getSpecies().getDex(),pokemon.getForm().getName());
        if(id==null)return;
        award(player,id,"earned");
        // Like the legend opening, the first earned species unlocks this branch.
        award(player,"root","earned");
    }
    private static void award(ServerPlayerEntity player,String id,String criterion){
        var advancement=player.func_184102_h().func_191949_aK().func_192778_a(new ResourceLocation(Content.NS,"late_bloomers/"+id));
        if(advancement!=null)player.func_192039_O().func_192750_a(advancement,criterion);
    }
}
