import com.pixelmonmod.pixelmon.entities.DenEntity;
import com.pixelmonmod.pixelmon.items.SpawnDenItem;
import local.pixelmon.backport.Backport;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/** Six actual native-item uses in the named disposable Mega map, with a fake player. */
public final class DenPlacementVerification {
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    public static final class RejectTestDen {
        @SubscribeEvent public void spawn(EntityJoinWorldEvent e){if(e.getEntity() instanceof DenEntity&&e.getEntity().func_226277_ct_()>990)e.setCanceled(true);}
    }
    public static void run(ServerPlayerEntity owner){
        var world=owner.func_71121_q();var p=FakePlayerFactory.getMinecraft(world);boolean creative=p.field_71075_bZ.field_75098_d,build=p.field_71075_bZ.field_75099_e;
        var previous=p.func_184586_b(Hand.MAIN_HAND);boolean opOnly=SpawnDenItem.isOPOnly;
        // Permission behavior is not under test; its real setting is restored.
        SpawnDenItem.isOPOnly=false;
        try{
            p.field_71075_bZ.field_75099_e=true;
            for(Item item:new Item[]{ForgeRegistries.ITEMS.getValue(new ResourceLocation("pixelmon","den_spawner")),Backport.item("tera_den_spawner")}){
                check(item!=null,"Missing den placement item");
                for(int mode=0;mode<3;mode++){
                    double x=1000+mode*8+(item instanceof SpawnDenItem?0:40);world.func_180501_a(new BlockPos(x,64,1000),net.minecraft.block.Blocks.field_150348_b.func_176223_P(),2);p.func_70012_b(x+.5,65,1000.5,0,90);
                    p.field_71075_bZ.field_75098_d=mode==1;var stack=new ItemStack(item);p.func_184611_a(Hand.MAIN_HAND,stack);
                    var reject=new RejectTestDen();if(mode==2)MinecraftForge.EVENT_BUS.register(reject);
                    try{item.func_77659_a(world,p,Hand.MAIN_HAND);}finally{if(mode==2)MinecraftForge.EVENT_BUS.unregister(reject);}
                    check(stack.func_190916_E()==(mode==0?0:1),item.getRegistryName()+" mode "+mode+" wrong consumption "+stack.func_190916_E());
                }
            }
            System.out.println("DEN_PLACEMENT_PASS native Max and Tera: survival consumes one, creative reusable, canceled insertion consumes none (six actual uses).");
        }finally{
            for(var den:world.func_175647_a(DenEntity.class,new AxisAlignedBB(990,60,990,1100,80,1010),v->true))den.func_70106_y();
            p.field_71075_bZ.field_75098_d=creative;p.field_71075_bZ.field_75099_e=build;p.func_184611_a(Hand.MAIN_HAND,previous);SpawnDenItem.isOPOnly=opOnly;
        }
    }
}
