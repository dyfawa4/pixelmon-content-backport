package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.items.medicine.*;
import com.pixelmonmod.pixelmon.items.EtherItem;
import com.pixelmonmod.pixelmon.items.ElixirItem;
import com.pixelmonmod.pixelmon.api.pokemon.stats.links.WrapperLink;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import local.pixelmon.backport.mixin.*;
import net.minecraft.item.ItemStack;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.comm.EnumUpdateType;
import com.pixelmonmod.pixelmon.battles.status.*;
/** Read native healing amounts without using the medicine, changing HP or spending an item. */
public final class NpcMedicineForecast {
    private NpcMedicineForecast(){}
    /** Native item effects on a detached battle/Pokemon copy, never the live party. */
    public static PixelmonWrapper project(PixelmonWrapper target,ItemStack stack,int info){
        if(stack==null)return target;
        var item=stack.func_77973_b();
        if(!(item instanceof MedicineItem)&&!(item instanceof EtherItem)&&!(item instanceof ElixirItem))return target;
        boolean simulation=target.bc.simulateMode;target.bc.simulateMode=true;
        try{
            PixelmonWrapper copy=new PixelmonWrapper(target);copy.pokemon=PokemonFactory.copy(target.pokemon);copy.entity=null;
            WrapperLink silent=new WrapperLink(copy){
                @Override public void setHealth(int health){copy.setHealth(health);}
                @Override public void update(EnumUpdateType... types){}
                @Override public void updateStats(){copy.getStats().setLevelStats(copy.getNature(),copy.getForm(),copy.getPokemonLevelNum());}
                @Override public void updateLevelUp(com.pixelmonmod.pixelmon.comm.PixelmonStatsData before){}
                @Override public void sendMessage(String key,Object... args){}
                @Override public net.minecraft.entity.player.ServerPlayerEntity getPlayerOwner(){return null;}
                @Override public com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity getEntity(){return null;}
                @Override public boolean removeStatuses(StatusType... types){
                    var matches=java.util.Arrays.asList(types);boolean changed=copy.getStatuses().removeIf(s->matches.contains(s.type));
                    if(copy.pokemon.getStatus()!=null&&matches.stream().anyMatch(copy.pokemon.getStatus()::isStatus)){copy.pokemon.setStatus(NoStatus.noStatus);changed=true;}
                    return changed;
                }
            };
            if(item instanceof MedicineItem)((MedicineItem)item).useMedicine(silent,info);
            else if(item instanceof EtherItem)((EtherItem)item).useEther(silent,info);
            else ((ElixirItem)item).useElixir(silent);
            return copy;
        }finally{target.bc.simulateMode=simulation;}
    }
    public static double health(PixelmonWrapper target,ItemStack stack){
        double hp=target.getHealth();if(hp<=0||stack==null||!(stack.func_77973_b() instanceof MedicineItem))return hp;
        for(var effect:((MedicineMethodsAccess)stack.func_77973_b()).backport$methods())if(effect instanceof MedicinePotion){
            int restored=((MedicinePotionAccess)effect).backport$healing().getHealAmount(new WrapperLink(target));
            hp=Math.min(target.getMaxHealth(),hp+Math.max(0,restored));
        }
        return hp;
    }
}
