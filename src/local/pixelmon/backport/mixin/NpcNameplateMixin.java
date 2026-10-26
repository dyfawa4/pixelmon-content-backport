package local.pixelmon.backport.mixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.render.entity.renderers.NPCRenderer;
import com.pixelmonmod.pixelmon.entities.npcs.NPCEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.Minecraft;
import local.pixelmon.backport.client.NpcNameplates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=NPCRenderer.class,remap=false)
public abstract class NpcNameplateMixin {
    // Nurse/Chatting return empty display text, so native drawNameTag never calls renderLivingLabel.
    // The caller still owns the configured render distance; preserve the native HUD gate here.
    @Inject(method="drawNameTag",at=@At("HEAD"),cancellable=true)
    private void backport$name(NPCEntity npc,float yaw,float partial,MatrixStack matrix,IRenderTypeBuffer buffer,int light,CallbackInfo ci){
        if(Minecraft.func_71382_s())NpcNameplates.draw(npc,npc.getDisplayText(),npc.getSubTitleText(),matrix,buffer,light);
        ci.cancel();
    }
}
