package local.pixelmon.backport.client;

import java.util.*;
import local.pixelmon.backport.EditorNameAliases;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.*;
import com.pixelmonmod.pixelmon.api.battles.attack.AttackRegistry;
import com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.api.pokemon.Nature;
import com.pixelmonmod.pixelmon.api.util.ITranslatable;
import com.pixelmonmod.pixelmon.init.registry.ItemRegistration;
import com.pixelmonmod.pixelmon.items.HeldItem;
import net.minecraft.item.*;

/** Resolve editor labels using the live language/resource packs and registries. */
public final class EditorNames {
    private EditorNames(){}
    public static String species(String input){return EditorNameAliases.resolve(input,PixelmonSpecies.getAll(),Species::getName,ITranslatable::getLocalizedName);}
    public static String ball(String input){return EditorNameAliases.resolve(input,PokeBallRegistry.getAll(),PokeBall::getName,ITranslatable::getLocalizedName);}
    public static String move(String input,Attack current){
        var moves=AttackRegistry.getAllAttacks();
        if(input!=null&&input.endsWith("]"))for(var move:moves)
            if(EditorNameAliases.key(input).equals(EditorNameAliases.key(qualified(move))))return move.getAttackName();
        if(EditorNameAliases.ambiguous(input,moves,ImmutableAttack::getAttackName,ITranslatable::getLocalizedName)){
            if(current!=null&&EditorNameAliases.key(input).equals(EditorNameAliases.key(current.getMove().getLocalizedName())))return current.getMove().getAttackName();
            // Native lookup otherwise chooses the first translated duplicate.
            // Keep its invalid-field handling; never send an invented move.
            return "__backport_ambiguous_editor_move__";
        }
        return EditorNameAliases.resolve(input,moves,ImmutableAttack::getAttackName,ITranslatable::getLocalizedName);
    }
    private static List<Item> heldItems(){
        List<Item> result=new ArrayList<>();for(Item item:ItemRegistration.getAllItems())if(item instanceof HeldItem&&item.getRegistryName()!=null)result.add(item);return result;
    }
    public static String item(String input){return EditorNameAliases.resolve(input,heldItems(),i->i.getRegistryName().toString(),i->new ItemStack(i).func_200301_q().getString());}
    public static Collection<String> moves(Collection<String> current){
        var moves=AttackRegistry.getAllAttacks();Map<String,Integer> counts=new HashMap<>();
        for(var move:moves)counts.merge(EditorNameAliases.key(move.getLocalizedName()),1,Integer::sum);
        Set<String> result=new LinkedHashSet<>(current);for(var move:moves){result.add(move.getAttackName());if(counts.get(EditorNameAliases.key(move.getLocalizedName()))>1)result.add(qualified(move));}return result;
    }
    private static String qualified(ImmutableAttack move){return move.getLocalizedName()+" ["+move.getAttackName()+"]";}
    public static Collection<String> items(Collection<String> current){
        Set<String> result=new LinkedHashSet<>(current);for(var item:heldItems())result.add(new ItemStack(item).func_200301_q().getString());return result;
    }
    public static Collection<String> natures(Collection<String> current){
        Set<String> result=new LinkedHashSet<>(current);for(var nature:Nature.values())result.add(nature.toString());return result;
    }
    public static String id(ITranslatable value){
        return value instanceof Species?((Species)value).getName():value instanceof PokeBall?((PokeBall)value).getName():value.getLocalizedName();
    }
    public static String label(ITranslatable value,String fallback){
        String label=value.getLocalizedName();return label.equals(value.getTranslationKey())?fallback:label;
    }
}
