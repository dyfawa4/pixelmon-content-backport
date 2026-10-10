package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.pixelmon.items.PokeBagItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;

/** First-level native Poke Bags only. Commit copied inventories after mutation. */
public final class PokeInventory implements AutoCloseable {
    public interface OpenBag {ItemStack backport$bagStack();IInventory backport$bagInventory();}
    private final PlayerEntity owner;
    public final List<ItemStack> stacks=new ArrayList<>();
    private final Map<ItemStack,PokeBagItem.PokeBag> bags=new IdentityHashMap<>();
    private final Map<ItemStack,CompoundNBT> before=new IdentityHashMap<>();
    private final Map<ItemStack,ItemStack> originals=new IdentityHashMap<>();
    private IInventory open;
    public PokeInventory(PlayerEntity owner){
        this.owner=owner;
        stacks.addAll(owner.field_71071_by.field_70462_a);stacks.addAll(owner.field_71071_by.field_184439_c);
        for(ItemStack stack:new ArrayList<>(stacks))if(!stack.func_190926_b() && stack.func_77973_b() instanceof PokeBagItem){
            IInventory inventory;
            if(owner.field_71070_bA instanceof OpenBag && ((OpenBag)owner.field_71070_bA).backport$bagStack()==stack){
                open=((OpenBag)owner.field_71070_bA).backport$bagInventory();inventory=open;
            }else {PokeBagItem.PokeBag bag=PokeBagItem.PokeBag.readFromItemStack(owner,stack);bags.put(stack,bag);CompoundNBT initial=new CompoundNBT();bag.saveInventory(initial);before.put(stack,initial);inventory=bag.inventory;}
            for(int i=0;i<inventory.func_70302_i_();i++){
                ItemStack item=inventory.func_70301_a(i);
                if(!item.func_190926_b() && !(item.func_77973_b() instanceof PokeBagItem))stacks.add(item);
            }
        }
        for(ItemStack stack:stacks)originals.put(stack,stack.func_77946_l());
    }
    @Override public void close(){
        boolean changed=false;for(Map.Entry<ItemStack,ItemStack> entry:originals.entrySet())if(!ItemStack.func_77989_b(entry.getKey(),entry.getValue())){changed=true;break;}
        if(!changed)return;
        if(open!=null)open.func_70296_d();
        for(Map.Entry<ItemStack,PokeBagItem.PokeBag> entry:bags.entrySet()){
            CompoundNBT next=new CompoundNBT();entry.getValue().saveInventory(next);
            if(!next.equals(before.get(entry.getKey())))entry.getValue().writeToStack(entry.getKey());
        }
        owner.field_71071_by.func_70296_d();
    }
}
