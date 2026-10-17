package local.pixelmon.backport;

import java.util.*;
import com.google.common.collect.ImmutableList;
import com.pixelmonmod.pixelmon.api.pokemon.boss.DropSet;
import com.pixelmonmod.pixelmon.api.util.WeightedSet;
import com.pixelmonmod.pixelmon.entities.pixelmon.drops.DroppedItem;
import local.pixelmon.backport.mixin.RewardDropAccess;
import net.minecraft.item.ItemStack;

/** Filter before shared server/client reward lists diverge; never renumber reward IDs. */
public final class RewardSanitizer {
    private RewardSanitizer(){}
    public static List<ItemStack> stacks(List<ItemStack> source){
        if(source==null)return new ArrayList<>();
        boolean dirty=false;for(ItemStack stack:source)if(!PokeChestLoot.valid(stack)){dirty=true;break;}
        if(!dirty)return source;
        ArrayList<ItemStack> result=new ArrayList<>();for(ItemStack stack:source)if(PokeChestLoot.valid(stack))result.add(stack);return result;
    }
    public static boolean commands(DropSet.Drop drop){
        if(drop==null)return false;
        String[] commands=((RewardDropAccess)(Object)drop).backport$commands();
        if(commands!=null)for(String command:commands)if(command!=null&&!command.isBlank())return true;
        return false;
    }
    public static boolean valid(DropSet.Drop drop){return drop!=null&&(PokeChestLoot.valid(((RewardDropAccess)(Object)drop).backport$item())||commands(drop));}
    public static ImmutableList<DroppedItem> query(ImmutableList<DroppedItem> source){
        ImmutableList.Builder<DroppedItem> result=ImmutableList.builder();
        for(DroppedItem reward:source)if(reward!=null&&(reward.drop==null?PokeChestLoot.valid(reward.item):valid(reward.drop)))result.add(reward);
        return result.build();
    }
    public static WeightedSet<DropSet.Drop> pool(WeightedSet<DropSet.Drop> source){
        boolean dirty=false;for(Iterator<DropSet.Drop> it=source.iterator();it.hasNext();)if(!valid(it.next())){dirty=true;break;}
        if(!dirty)return source;
        WeightedSet<DropSet.Drop> result=WeightedSet.newWeightedSet();
        for(Iterator<DropSet.Drop> it=source.iterator();it.hasNext();){DropSet.Drop drop=it.next();if(valid(drop))result.add(source.getWeight(drop),drop);}
        return result;
    }
}
