package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import net.minecraft.entity.player.ServerPlayerEntity;
public final class ZygardeAdvancements {
    private ZygardeAdvancements(){}
    private static ServerPlayerEntity owner(PixelmonWrapper p){
        if(p==null||p.bc==null||p.bc.simulateMode||p.bc.battleEnded||p.getSpecies().getDex()!=718||!p.isAlive()||!p.onBattlefield
            ||!(p.getParticipant() instanceof PlayerParticipant)||!p.getParticipant().controlledPokemon.contains(p))return null;
        ServerPlayerEntity player=((PlayerParticipant)p.getParticipant()).player;
        return AcquisitionAdvancements.ownedStored(player,p.pokemon)?player:null;
    }
    private static void award(ServerPlayerEntity player,String id){
        if(player==null)return;LegendAdvancements.award(player,"root");LegendAdvancements.award(player,"forms");LegendAdvancements.award(player,id);
    }
    public static void powerConstruct(PixelmonWrapper p){
        if(!p.getForm().isForm("complete"))return;
        ServerPlayerEntity player=owner(p);award(player,"zygarde_power_construct");award(player,"zygarde_complete");
    }
    public static void mega(PixelmonWrapper p){if(p.getForm().isForm("mega"))award(owner(p),"zygarde_mega");}
    public static void field(Pokemon p,ServerPlayerEntity player){
        if(!FieldMega.active(p))return;
        String target=p.getPersistentData().func_74775_l(FieldMega.MARKER).func_74779_i("Target");
        if(target.equals("complete")&&p.getForm().isForm("complete"))award(player,"zygarde_complete");
        if(target.equals("mega")&&p.getForm().isForm("mega"))award(player,"zygarde_mega");
    }
}
