package local.pixelmon.backport;

import java.util.List;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonPartEntity;
import net.minecraft.entity.boss.dragon.phase.PhaseType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.text.*;
import net.minecraft.world.World;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.*;

/** Three boss eliminations, with native death processing and no vanilla sword/repair carrier. */
public final class DivineSword extends Item {
    public static final int USES=3;
    public static final String SPENT="BackportDivineSwordSpent";
    public DivineSword(){super(new Item.Properties().func_200918_c(USES).setNoRepair().func_208103_a(Rarity.EPIC).func_200916_a(Backport.CREATIVE_TAB));}
    @Override public boolean func_77616_k(ItemStack stack){return false;}
    @Override public int func_77619_b(){return 0;}
    @Override public int getItemEnchantability(ItemStack stack){return 0;}
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack,Enchantment enchantment){return false;}
    @Override public boolean isBookEnchantable(ItemStack stack,ItemStack book){return false;}
    @Override public boolean isRepairable(ItemStack stack){return false;}
    @Override public boolean func_82789_a(ItemStack stack,ItemStack material){return false;}
    @Override public float getXpRepairRatio(ItemStack stack){return 0;}
    public static boolean is(ItemStack stack){return !stack.func_190926_b()&&stack.func_77973_b() instanceof DivineSword;}
    public static int spent(ItemStack stack){return Math.max(0,Math.min(USES,Math.max(stack.func_77952_i(),stack.func_77942_o()?stack.func_77978_p().func_74762_e(SPENT):0)));}
    public static void sanitize(ItemStack stack){
        if(!is(stack))return;
        int used=spent(stack);
        if(stack.func_77942_o())for(String key:new String[]{"Enchantments","StoredEnchantments","Unbreakable"})stack.func_77978_p().func_82580_o(key);
        if(used>=USES){stack.func_190920_e(0);return;}
        if(used>0)stack.func_196082_o().func_74768_a(SPENT,used);
        if(stack.func_77952_i()!=used)stack.func_196085_b(used);
    }
    @Override public void func_77663_a(ItemStack stack,World world,Entity entity,int slot,boolean selected){if(!world.field_72995_K)sanitize(stack);}
    @Override public void func_77624_a(ItemStack stack,World world,List<ITextComponent> lines,ITooltipFlag flag){
        lines.add(new TranslationTextComponent("item.pixelmon_backport.divine_sword.desc").func_240699_a_(TextFormatting.GRAY));
        lines.add(new TranslationTextComponent("item.pixelmon_backport.divine_sword.uses",USES-spent(stack)).func_240699_a_(TextFormatting.AQUA));
        lines.add(new TranslationTextComponent("item.pixelmon_backport.divine_sword.shop").func_240699_a_(TextFormatting.DARK_GRAY));
    }
    private static LivingEntity boss(Entity target){
        if(target instanceof EnderDragonPartEntity)return ((EnderDragonPartEntity)target).field_213852_b;
        return target instanceof EnderDragonEntity||target instanceof WitherEntity?(LivingEntity)target:null;
    }
    /** Return true only when a new lethal hit was accepted; repeated dragon death-animation hits cost nothing. */
    public static boolean strike(PlayerEntity player,Entity target,ItemStack stack){
        if(player.field_70170_p.field_72995_K||!is(stack))return false;
        sanitize(stack);if(stack.func_190926_b())return false;
        LivingEntity victim=boss(target);
        if(victim==null||!victim.func_70089_S()||victim.field_70170_p!=player.field_70170_p)return false;
        if(victim instanceof EnderDragonEntity&&((EnderDragonEntity)victim).func_184670_cT().func_188756_a().func_188652_i()==PhaseType.field_188750_j)return false;
        int oldHurt=victim.field_70172_ad,oldInvulnerability=victim instanceof WitherEntity?((WitherEntity)victim).func_82212_n():0;
        victim.field_70172_ad=0;
        if(victim instanceof WitherEntity)((WitherEntity)victim).func_82215_s(0);
        DamageSource source=new BossDamage(player);
        float damage=Math.min(1.0e30f,Math.max(1024f,(victim.func_110143_aJ()+victim.func_110139_bj()+1)*32));
        boolean accepted=victim instanceof EnderDragonEntity?
            ((EnderDragonEntity)victim).func_213403_a(target instanceof EnderDragonPartEntity?(EnderDragonPartEntity)target:((EnderDragonEntity)victim).func_213404_dT()[0],source,damage):victim.func_70097_a(source,damage);
        boolean lethal=!victim.func_70089_S()||victim instanceof EnderDragonEntity&&((EnderDragonEntity)victim).func_184670_cT().func_188756_a().func_188652_i()==PhaseType.field_188750_j;
        if(!accepted||!lethal){
            victim.field_70172_ad=oldHurt;
            if(victim instanceof WitherEntity)((WitherEntity)victim).func_82215_s(oldInvulnerability);
            return false;
        }
        // Vanilla restores a lethally hit flying dragon to 1 HP for its descent phase.
        // Keep its native player-attributed death, XP and fight-manager animation, but start it now.
        if(victim instanceof EnderDragonEntity&&victim.func_110143_aJ()>0)victim.func_70606_j(0);
        int used=spent(stack)+1;
        if(used>=USES){stack.func_190918_g(1);player.func_213334_d(Hand.MAIN_HAND);}
        else{stack.func_196082_o().func_74768_a(SPENT,used);stack.func_196085_b(used);}
        player.field_71071_by.func_70296_d();player.field_71070_bA.func_75142_b();return true;
    }
    private static final class BossDamage extends EntityDamageSource {
        BossDamage(PlayerEntity player){super("player",player);func_76348_h();func_151518_m();}
    }
    public static final class Events {
        @SubscribeEvent(priority=EventPriority.LOWEST) public void attack(AttackEntityEvent event){
            var player=event.getPlayer();var stack=player.func_184614_ca();if(!is(stack))return;
            // Skip canceled protection events, then suppress vanilla attacks and their separate durability path.
            event.setCanceled(true);strike(player,event.getTarget(),stack);
        }
        @SubscribeEvent public void anvil(AnvilUpdateEvent event){if(is(event.getLeft())||is(event.getRight()))event.setCanceled(true);}
    }
}
