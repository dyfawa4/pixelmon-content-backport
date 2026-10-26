package local.pixelmon.backport;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.text.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;

/** Stop the interaction before either NPC handling or the vanilla name-tag item runs. */
public final class AuthorNameTag {
    public static final ResourceLocation CHALLENGE=new ResourceLocation(Content.NS,"adventure/forbidden_name_tag");
    public static final DamageSource SOURCE=new DamageSource("backport_author_nametag"){
        @Override public ITextComponent func_151519_b(LivingEntity victim){return new TranslationTextComponent("death.attack.backport_author_nametag",victim.func_145748_c_());}
    };
    private static void erase(ItemStack stack){if(stack.func_77973_b() instanceof NameTagItem)stack.func_190920_e(0);}
    private static void carriedBag(ItemStack stack){
        var id=stack.func_77973_b().getRegistryName();
        if(stack.func_190926_b()||id==null||!id.func_110624_b().equals("sophisticatedbackpacks"))return;
        stack.getCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(inventory->{
            for(int i=0;i<inventory.getSlots();i++)if(inventory.getStackInSlot(i).func_77973_b() instanceof NameTagItem)inventory.extractItem(i,inventory.getStackInSlot(i).func_190916_E(),false);
        });
    }
    public static void punish(ServerPlayerEntity player){
        if(!player.func_70089_S())return;
        // Commit bag contents before vanilla death drops/keepInventory copy the inventory.
        try(var inventory=new PokeInventory(player)){for(var stack:inventory.stacks){carriedBag(stack);erase(stack);}}
        for(var stack:player.field_71071_by.field_70460_b){carriedBag(stack);erase(stack);}
        erase(player.field_71071_by.func_70445_o());
        player.field_71071_by.func_70296_d();player.field_71070_bA.func_75142_b();
        // Direct death also applies in creative, bypasses totems, armor and spawn invulnerability.
        var combat=player.func_110142_aN();combat.func_94549_h();combat.func_94547_a(SOURCE,player.func_110143_aJ(),Float.MAX_VALUE);
        player.func_70606_j(0);player.func_70645_a(SOURCE);
        var advancement=player.func_184102_h().func_191949_aK().func_192778_a(CHALLENGE);
        if(advancement!=null)player.func_192039_O().func_192750_a(advancement,"earned");
    }
    private void interact(PlayerInteractEvent event,net.minecraft.entity.Entity target){
        var player=event.getPlayer();var stack=player.func_184586_b(event.getHand());
        if(!(target instanceof com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer)||!AuthorBossTeams.is((com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer)target)||stack.func_190926_b()||!(stack.func_77973_b() instanceof NameTagItem))return;
        event.setCanceled(true);event.setCancellationResult(ActionResultType.SUCCESS);
        if(player instanceof ServerPlayerEntity)punish((ServerPlayerEntity)player);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void generic(PlayerInteractEvent.EntityInteract event){interact(event,event.getTarget());}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void specific(PlayerInteractEvent.EntityInteractSpecific event){interact(event,event.getTarget());}
}
