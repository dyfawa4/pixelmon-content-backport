package local.pixelmon.backport.mixin;

import local.pixelmon.backport.TeraVisualState;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractClientEntity;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.network.datasync.*;
import net.minecraft.nbt.CompoundNBT;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=AbstractClientEntity.class,remap=false)
public abstract class TeraVisualEntityMixin implements TeraVisualState {
    @Unique private static final DataParameter<Integer> backport$teraType=EntityDataManager.func_187226_a(AbstractClientEntity.class,DataSerializers.field_187192_b);
    @Unique private int backport$observedTeraType=-2;
    @Unique private static final DataParameter<CompoundNBT> backport$teraStarted=EntityDataManager.func_187226_a(AbstractClientEntity.class,DataSerializers.field_192734_n);
    @Unique private long backport$observedTeraStart=-2;
    @Inject(method="func_70088_a",at=@At("RETURN"))
    private void backport$defineTera(CallbackInfo ci){
        ((AbstractClientEntity)(Object)this).func_184212_Q().func_187214_a(backport$teraType,-1);
        CompoundNBT start=new CompoundNBT();start.func_74772_a("Tick",-1L);
        ((AbstractClientEntity)(Object)this).func_184212_Q().func_187214_a(backport$teraStarted,start);
    }
    @Override public int backport$teraVisualType(){return ((AbstractClientEntity)(Object)this).func_184212_Q().func_187225_a(backport$teraType);}
    @Override public long backport$teraVisualStart(){return ((AbstractClientEntity)(Object)this).func_184212_Q().func_187225_a(backport$teraStarted).func_74763_f("Tick");}
    @Override public void backport$setTeraVisualStart(long tick){CompoundNBT start=new CompoundNBT();start.func_74772_a("Tick",tick);((AbstractClientEntity)(Object)this).func_184212_Q().func_187227_b(backport$teraStarted,start);}
    @Override public void backport$setTeraVisualType(int type){
        if(type < -1 || type>=19)throw new IllegalArgumentException("Tera visual type");
        ((AbstractClientEntity)(Object)this).func_184212_Q().func_187227_b(backport$teraType,type);
    }
    @Inject(method="func_70071_h_",at=@At("RETURN"))
    private void backport$updateTera(CallbackInfo ci){
        AbstractClientEntity self=(AbstractClientEntity)(Object)this;
        if(!self.field_70170_p.field_72995_K && self instanceof PixelmonEntity){
            int type=TeraVisualState.authoritativeType((PixelmonEntity)self);
            if(type!=backport$observedTeraType){backport$observedTeraType=type;backport$setTeraVisualType(type);}
            long start=local.pixelmon.backport.Tera.visualStart((PixelmonEntity)self);
            if(start!=backport$observedTeraStart){backport$observedTeraStart=start;backport$setTeraVisualStart(start);}
        }
    }
}
