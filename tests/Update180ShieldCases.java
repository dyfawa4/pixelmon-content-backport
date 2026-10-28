package qa;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.ability.*;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.*;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.*;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.log.AttackResult;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.status.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import local.pixelmon.backport.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.block.HorizontalBlock;

public final class Update180ShieldCases {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void run(ServerPlayerEntity player){
        var npc=new NPCTrainer(player.field_70170_p);npc.setBossTier(BossTierRegistry.getBossTierOrNotBoss("equal"));for(int i=0;i<6;i++)npc.getPokemonStorage().set(i,null);npc.getPokemonStorage().set(0,Update180Cases.mon(150,"Skill Swap","Gastro Acid","Earthquake"));
        var trainer=new TrainerParticipant(npc,1);var enemy=new WildPixelmonParticipant(Update180Cases.mon(65,"Psychic"));
        var b=new BattleController(new BattleParticipant[]{trainer},new BattleParticipant[]{enemy},new BattleRules());b.sendMessages=false;
        for(var side:b.participants){side.controlledPokemon.clear();side.controlledPokemon.add(side.allPokemon[0]);for(var p:side.allPokemon){p.bc=b;p.onBattlefield=p==side.allPokemon[0];p.tempAbility=null;}}
        var user=trainer.allPokemon[0];var holder=enemy.allPokemon[0];user.pokemon.setAbility(new MoldBreaker());holder.pokemon.setAbility(new Levitate());holder.pokemon.setHeldItem(new ItemStack(Backport.item("ability_shield")));
        check(AbilityShield.active(holder),"Native registered shield active without ability recursion");
        check(!AbilityRegistry.ignoreAbility(user,holder)&&holder.getBattleAbility(user) instanceof Levitate,"Mold Breaker does not ignore shielded Levitate");
        holder.setTempAbility(new Insomnia());check(holder.getBattleAbility(false) instanceof Levitate,"External temporary ability blocked");
        var a=user.getBattleAbility(false);check(new SkillSwap().applyEffectDuring(user,holder)==AttackResult.failed&&user.getBattleAbility(false)==a&&holder.getBattleAbility(false) instanceof Levitate,"Skill Swap atomic, both preserved");
        user.attack=new Attack("Gastro Acid");new GastroAcid().applyEffect(user,holder);check(!holder.hasStatus(StatusType.GastroAcid),"Gastro Acid status prevented");
        user.tempAbility=new NeutralizingGas();check(!AbilityRegistry.ignoreAbility(holder,b)&&holder.getBattleAbility() instanceof Levitate,"Neutralizing Gas suppressed only for holder");
        holder.getStatuses().add(new Embargo());check(!AbilityShield.active(holder),"Embargo disables shield");holder.getStatuses().clear();
        b.globalStatusController.addGlobalStatus(user,new MagicRoom());check(!AbilityShield.active(holder),"Magic Room disables shield");b.globalStatusController.removeGlobalStatus(StatusType.MagicRoom);
        holder.tempAbility=new Klutz();check(!AbilityShield.active(holder),"Klutz disables shield");holder.tempAbility=null;
        holder.pokemon.setAbility(new Trace());user.tempAbility=new Intimidate();var trace=(Trace)holder.getBattleAbility(false);
        trace.applySwitchInEffect(holder);check(holder.getBattleAbility(false) instanceof Trace,"Own Trace prevented");holder.pokemon.setHeldItem(ItemStack.field_190927_a);trace.applyFoeSwitchInEffect(holder,user);check(holder.getBattleAbility(false) instanceof Trace,"Blocked entry Trace cannot retry after removal");
        holder.pokemon.setHeldItem(new ItemStack(Backport.item("ability_shield")));holder.tempAbility=null;holder.pokemon.setAbility(new Levitate());
        new Transformed(holder,user);check(holder.getBattleAbility(false) instanceof Intimidate,"Transform can change ability through shield");
        holder.setTempAbility(new Insomnia());check(holder.getBattleAbility(false) instanceof Intimidate,"Transformed ability protected");
        holder.pokemon.setHeldItem(ItemStack.field_190927_a);holder.setTempAbility(new Insomnia());check(holder.getBattleAbility(false) instanceof Insomnia,"Removing item restores native ability changes");
        int stones=0;for(var item:net.minecraftforge.registries.ForgeRegistries.ITEMS)if(item instanceof com.pixelmonmod.pixelmon.items.heldItems.MegaStoneItem){var stack=new ItemStack(item);check(item.func_77636_d(stack)&&item.func_77613_e(stack)==Rarity.EPIC,"Mega glint and purple "+item.getRegistryName());check(stack.func_77978_p()==null,"No enchantment NBT "+item.getRegistryName());stones++;}
        check(stones>50,"Native and backport Mega stones covered");check(!Update180Cases.stack("pixelmon:fire_stone").func_77973_b().func_77636_d(Update180Cases.stack("pixelmon:fire_stone")),"Unrequested ordinary evolution stones untouched");
        var world=player.func_71121_q();var home=new BlockPos(20,74,20);var chair=home.func_177982_a(-1,1,-4);
        AuthorRoomFurnishings.upgrade(world,home);check(world.func_180495_p(chair).func_177229_b(HorizontalBlock.field_185512_D)==Direction.NORTH,"Legacy chair faces the workplace");
        AuthorRoomFurnishings.place(world,home);check(world.func_180495_p(chair).func_177229_b(HorizontalBlock.field_185512_D)==Direction.NORTH,"Repeated furnish does not reverse chair");
        player.func_200619_a(world,home.func_177958_n()-2,home.func_177956_o()+1,home.func_177952_p()+1,180,10);
        System.out.println("SHIELD_ART_180_PASS native ability suppression/bypass/change, atomic swap, item disabled, Trace, Transform, removal; "+stones+" purple glint Mega stones without NBT; chair migration");
    }
}
