package local.pixelmon.backport;

import java.util.*;
import com.pixelmonmod.api.pokemon.PokemonSpecification;
import com.pixelmonmod.api.pokemon.requirement.impl.*;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.*;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.species.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.evolution.types.LevelingEvolution;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.items.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.util.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** One native evolution edge backwards; retained branch history is private Pokemon NBT. */
public final class MysteriousPotion extends PixelmonItem {
    public static final String HISTORY="BackportDevolutionHistory";
    private static Map<String,List<Stats>> previous;
    public MysteriousPotion(){super(new Item.Properties().func_200916_a(Backport.CREATIVE_TAB));}
    public static synchronized void clear(){previous=null;}
    private static String key(Stats form){return form.getParentSpecies().getDex()+"/"+form.getName();}
    private static synchronized Map<String,List<Stats>> graph(){
        if(previous!=null)return previous;
        var result=new LinkedHashMap<String,List<Stats>>();
        for(var species:PixelmonSpecies.getAll())for(var form:species.getForms())if(!form.isTemporary())for(var evolution:form.getEvolutions()){
            var target=evolution.to.getValue(SpeciesRequirement.class).orElse(null);
            if(target==null||!target.isInitialized()||target.getValueUnsafe().getDex()==species.getDex())continue;
            var next=target.getValueUnsafe();String name=evolution.to.getValue(FormRequirement.class).orElse(form.getName());
            Stats end=next.getForm(name);if(end==null)end=next.getDefaultForm();
            if(end==null||end.isTemporary())continue;
            var parents=result.computeIfAbsent(key(end),k->new ArrayList<>());
            if(parents.stream().noneMatch(p->key(p).equals(key(form))))parents.add(form);
        }
        previous=result;return result;
    }
    public static Stats target(Pokemon pokemon){
        if(pokemon==null||pokemon.isEgg()||pokemon.getForm().isTemporary()||FieldMega.active(pokemon))return null;
        var choices=graph().get(key(pokemon.getForm()));
        if(choices==null||choices.isEmpty())return null;
        // Prefer matching regional/sex form when native data contains equivalent edges.
        for(var form:choices)if(form.getName().equals(pokemon.getForm().getName()))return form;
        return choices.get(0);
    }
    private static CompoundNBT identity(Stats form){var row=new CompoundNBT();row.func_74768_a("Dex",form.getParentSpecies().getDex());row.func_74778_a("Form",form.getName());return row;}
    private static Stats resolve(CompoundNBT row){var species=PixelmonSpecies.fromDex(row.func_74762_e("Dex")).orElse(null);return species==null?null:species.getForm(row.func_74779_i("Form"));}
    private static boolean same(Pokemon p,CompoundNBT row){return p.getSpecies().getDex()==row.func_74762_e("Dex")&&p.getForm().getName().equals(row.func_74779_i("Form"));}
    public static boolean devolve(Pokemon pokemon){
        Stats form=target(pokemon);if(form==null)return false;
        var history=pokemon.getPersistentData().func_150295_c(HISTORY,10);var row=identity(pokemon.getForm());row.func_218657_a("From",identity(form));
        int health=pokemon.getHealth();float ratio=pokemon.getHealthPercentage();
        pokemon.setSpecies(form.getParentSpecies(),false);pokemon.setForm(form);
        pokemon.setHealth(health==0?0:Math.max(1,Math.min(pokemon.getMaxHealth(),Math.round(pokemon.getMaxHealth()*ratio/100f))));
        history.add(row);pokemon.getPersistentData().func_218657_a(HISTORY,history);pokemon.markDirty(Pokemon.FORM);return true;
    }
    public static void sync(ServerPlayerEntity player){StorageProxy.getParty(player).notifyListenerAll(player,com.pixelmonmod.pixelmon.comm.EnumUpdateType.CLIENT);player.field_71071_by.func_70296_d();}
    @Override public ActionResultType func_111207_a(ItemStack stack,PlayerEntity player,LivingEntity target,Hand hand){
        if(!(target instanceof PixelmonEntity)||stack.func_190926_b())return ActionResultType.PASS;
        if(player.field_70170_p.field_72995_K)return ActionResultType.SUCCESS;
        var entity=(PixelmonEntity)target;var p=entity.getPokemon();
        if(!(player instanceof ServerPlayerEntity)||p==null||!player.func_110124_au().equals(p.getOwnerPlayerUUID())||BattleRegistry.getBattle(player)!=null||FieldMega.busy(entity)||!devolve(p)){
            player.func_146105_b(new TranslationTextComponent("pixelmon.backport.mysterious.failed"),true);return ActionResultType.FAIL;
        }
        stack.func_190918_g(1);sync((ServerPlayerEntity)player);player.func_146105_b(new TranslationTextComponent("pixelmon.backport.mysterious.changed"),true);return ActionResultType.SUCCESS;
    }
    /** Returns null for ordinary candy use, otherwise owns this interaction (including rejection). */
    public static Boolean candy(PixelmonEntity entity,PlayerEntity player,Hand hand,ItemStack stack){
        if(!(player instanceof ServerPlayerEntity)||!(stack.func_77973_b() instanceof ExpCandyItem)||entity.getOwner()!=player)return null;
        var p=entity.getPokemon();var history=p.getPersistentData().func_150295_c(HISTORY,10);if(history.isEmpty())return null;
        var row=history.func_150305_b(history.size()-1);Stats target=resolve(row);
        if(target==null||!same(p,row.func_74775_l("From")))return null;
        if(stack.func_190926_b()||p.isEgg()||p.isFainted()||!p.doesLevel()||FieldMega.busy(entity)||BattleRegistry.getBattle(player)!=null)return true;
        if(Pixelmon.EVENT_BUS.post(new RareCandyEvent((ServerPlayerEntity)player,entity,stack,(ExpCandyItem)stack.func_77973_b())))return true;
        // Native evolution clones a spec by reparsing its original text.
        var spec=com.pixelmonmod.api.pokemon.PokemonSpecificationProxy.create(target.getParentSpecies().getName()+(target.getName().isEmpty()?"":" form:"+target.getName()));
        if(new LevelingEvolution(spec,p.getPokemonLevel()).doEvolution(entity,"mysterious_potion_candy")){
            if(!player.func_184812_l_())stack.func_190918_g(1);sync((ServerPlayerEntity)player);
        }
        return true;
    }
    public static final class Events {
        @SubscribeEvent public void evolved(EvolveEvent.Post event){
            var p=event.getPokemon();var history=p.getPersistentData().func_150295_c(HISTORY,10);
            if(!history.isEmpty()&&same(p,history.func_150305_b(history.size()-1))){history.remove(history.size()-1);if(history.isEmpty())p.getPersistentData().func_82580_o(HISTORY);else p.getPersistentData().func_218657_a(HISTORY,history);}
        }
    }
    @Override public void func_77624_a(ItemStack stack,World world,List<ITextComponent> lines,ITooltipFlag flag){lines.add(new TranslationTextComponent("pixelmon.backport.mysterious.description").func_240699_a_(TextFormatting.GRAY));}
}
