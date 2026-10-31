package local.pixelmon.backport.mixin;
import java.util.*;
import com.pixelmonmod.pixelmon.blocks.tileentity.FridgeTileEntity;
import net.minecraft.tileentity.*;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.*;
/** Native chest-container callbacks: first viewer opens, last viewer closes. */
@Mixin(value=FridgeTileEntity.class,remap=false)
public abstract class FridgeSoundMixin extends TileEntity {
 @Unique private final Set<UUID> backport$viewers=new HashSet<>();
 protected FridgeSoundMixin(TileEntityType<?> type){super(type);}
 public void func_174889_b(PlayerEntity player){if(field_145850_b!=null&&!field_145850_b.field_72995_K&&!player.func_175149_v()&&backport$viewers.add(player.func_110124_au())&&backport$viewers.size()==1)local.pixelmon.backport.FridgeSounds.play(field_145850_b,field_174879_c,true);}
 public void func_174886_c(PlayerEntity player){if(field_145850_b!=null&&!field_145850_b.field_72995_K&&backport$viewers.remove(player.func_110124_au())&&backport$viewers.isEmpty())local.pixelmon.backport.FridgeSounds.play(field_145850_b,field_174879_c,false);}
}
