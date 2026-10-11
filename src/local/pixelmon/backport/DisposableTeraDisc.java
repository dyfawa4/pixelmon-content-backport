package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.battles.attack.AttackRegistry;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.attacks.*;
import com.pixelmonmod.pixelmon.comm.EnumUpdateType;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.enums.TMType;
import com.pixelmonmod.pixelmon.enums.technicalmoves.ITechnicalMove;
import com.pixelmonmod.pixelmon.items.TechnicalMoveItem;
import net.minecraft.entity.player.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import java.util.UUID;

/** A separate registry item. Native reusable TM9 and its configuration are untouched. */
public final class DisposableTeraDisc extends TechnicalMoveItem {
    public DisposableTeraDisc(){super(TMType.TM9);}
    public static ITechnicalMove move(){return ITechnicalMove.getMoveFor(TMType.TM9,171);}
    @Override public String func_77667_c(ItemStack stack){return "item.pixelmon_backport.tera_blast_disc_single_use";}
    @Override public void func_77663_a(ItemStack stack,World world,Entity entity,int slot,boolean selected){
        // Never allow the native random-TM initializer to change this fixed disc.
        stack.func_196082_o().func_74777_a("tm",(short)171);
    }
    @Override public void func_150895_a(net.minecraft.item.ItemGroup group,NonNullList<ItemStack> items){
        if(func_194125_a(group))items.add(new ItemStack(this));
    }
    public static boolean compatible(Pokemon pokemon){return !pokemon.isEgg() && pokemon.getForm().getMoves().canLearn(move());}
    public static boolean pay(ServerPlayerEntity player,UUID id,ItemStack offered){
        Pokemon pokemon=StorageProxy.getParty(player).get(id);
        if(pokemon==null || BattleRegistry.getBattle(player)!=null || !compatible(pokemon)
            || pokemon.getMoveset().hasAttack(move().getAttack()))return false;
        try(PokeInventory inventory=new PokeInventory(player)){
            // The cost is the registered one-use item, irrespective of its optional TM NBT.
            for(ItemStack stack:inventory.stacks)if(stack.func_77973_b()==offered.func_77973_b() && stack.func_190916_E()>0){
                stack.func_190918_g(1);player.field_71071_by.func_70296_d();return true;
            }
        }
        return false;
    }
    public static boolean interact(PixelmonEntity entity,PlayerEntity owner,Hand hand,ItemStack stack){
        if(!(stack.func_77973_b() instanceof DisposableTeraDisc))return false;
        if(!(owner instanceof ServerPlayerEntity) || hand!=Hand.MAIN_HAND)return true;
        ServerPlayerEntity player=(ServerPlayerEntity)owner;Pokemon pokemon=entity.getPokemon();
        if(!player.func_110124_au().equals(pokemon.getOwnerPlayerUUID()) || BattleRegistry.getBattle(player)!=null)return true;
        ImmutableAttack attack=move().getAttack();
        if(!compatible(pokemon)){player.func_146105_b(new TranslationTextComponent("pixelmon.interaction.tmcantlearn",pokemon.getNickname(),attack.getTranslatedName()),false);return true;}
        if(pokemon.getMoveset().hasAttack(attack)){player.func_146105_b(new TranslationTextComponent("pixelmon.interaction.tmknown",pokemon.getNickname(),attack.getTranslatedName()),false);return true;}
        UUID id=pokemon.getUUID();ItemStack cost=new ItemStack(stack.func_77973_b());
        if(pokemon.getMoveset().size()>=4){
            LearnMoveController.sendLearnMove(player,id,attack,p->pay(p,id,cost));
        }else if(pay(player,id,cost)){
            pokemon.getMoveset().add(new Attack(attack));entity.update(EnumUpdateType.Moveset);
            player.func_146105_b(new TranslationTextComponent("pixelmon.stats.learnedmove",pokemon.getNickname(),attack.getTranslatedName()),false);
        }
        return true;
    }
}
