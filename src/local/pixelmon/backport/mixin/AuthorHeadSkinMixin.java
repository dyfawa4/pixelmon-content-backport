package local.pixelmon.backport.mixin;
import com.mojang.authlib.GameProfile;
import local.pixelmon.backport.AuthorVictory;
import net.minecraft.block.SkullBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.SkullTileEntityRenderer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Render the bundled author skin without a network texture download or a player account lookup. */
@Mixin(value=SkullTileEntityRenderer.class,remap=false)
public abstract class AuthorHeadSkinMixin {
    @Inject(method="func_228878_a_",at=@At("HEAD"),cancellable=true)
    private static void backport$authorSkin(SkullBlock.ISkullType type,GameProfile profile,CallbackInfoReturnable<RenderType> cir){
        if(profile!=null&&AuthorVictory.PROFILE.equals(profile.getId()))
            cir.setReturnValue(RenderType.func_228640_c_(new ResourceLocation("pixelmon","textures/steve/mystery_task_man.png")));
    }
}
