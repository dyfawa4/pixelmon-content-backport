package local.pixelmon.backport.mixin;
import local.pixelmon.backport.BattleMotion;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CMoveVehiclePacket;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=ServerPlayNetHandler.class,remap=false)
public abstract class BattleMotionPacketMixin {
    @Shadow public ServerPlayerEntity field_147369_b;
    @Shadow private Vector3d field_184362_y;
    @Shadow private boolean field_184344_B;
    @Shadow private boolean field_184345_D;
    @Shadow private int field_147365_f;
    @Shadow private int field_184346_E;
    @Inject(method={"func_73660_a","func_147347_a","func_184338_a"},at=@At("RETURN"))
    private void backport$noFloatingKick(CallbackInfo ci){
        if(BattleMotion.frozen(field_147369_b)){field_184344_B=false;field_184345_D=false;field_147365_f=0;field_184346_E=0;}
    }
    @Inject(method="func_73660_a",at=@At("HEAD"))
    private void backport$beforeFloatingCheck(CallbackInfo ci){backport$noFloatingKick(ci);}
    private boolean backport$held(){return field_184362_y==null && BattleMotion.frozen(field_147369_b);}
    // Preserve packet validation, rotation, idle handling and teleport acknowledgements.
    @Redirect(method="func_147347_a",at=@At(value="INVOKE",target="Lnet/minecraft/network/play/client/CPlayerPacket;func_186997_a(D)D"))
    private double backport$x(CPlayerPacket packet,double fallback){return backport$held()?field_147369_b.func_226277_ct_():packet.func_186997_a(fallback);}
    @Redirect(method="func_147347_a",at=@At(value="INVOKE",target="Lnet/minecraft/network/play/client/CPlayerPacket;func_186996_b(D)D"))
    private double backport$y(CPlayerPacket packet,double fallback){return backport$held()?field_147369_b.func_226278_cu_():packet.func_186996_b(fallback);}
    @Redirect(method="func_147347_a",at=@At(value="INVOKE",target="Lnet/minecraft/network/play/client/CPlayerPacket;func_187000_c(D)D"))
    private double backport$z(CPlayerPacket packet,double fallback){return backport$held()?field_147369_b.func_226281_cx_():packet.func_187000_c(fallback);}
    @Redirect(method="func_184338_a",at=@At(value="INVOKE",target="Lnet/minecraft/network/play/client/CMoveVehiclePacket;func_187004_a()D"))
    private double backport$vehicleX(CMoveVehiclePacket packet){return backport$held()?field_147369_b.func_184208_bv().func_226277_ct_():packet.func_187004_a();}
    @Redirect(method="func_184338_a",at=@At(value="INVOKE",target="Lnet/minecraft/network/play/client/CMoveVehiclePacket;func_187002_b()D"))
    private double backport$vehicleY(CMoveVehiclePacket packet){return backport$held()?field_147369_b.func_184208_bv().func_226278_cu_():packet.func_187002_b();}
    @Redirect(method="func_184338_a",at=@At(value="INVOKE",target="Lnet/minecraft/network/play/client/CMoveVehiclePacket;func_187003_c()D"))
    private double backport$vehicleZ(CMoveVehiclePacket packet){return backport$held()?field_147369_b.func_184208_bv().func_226281_cx_():packet.func_187003_c();}
}
