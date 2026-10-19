package local.pixelmon.backport.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.render.entity.layers.PokemonShimLayer;
import com.pixelmonmod.pixelmon.entities.pixelmon.AbstractClientEntity;
import local.pixelmon.backport.client.EvolutionBeamOrigin;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value=PokemonShimLayer.class,remap=false)
public abstract class EvolutionBeamOriginMixin {
    @ModifyArgs(method="renderEvolutionLightBeams",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/matrix/MatrixStack;func_227861_a_(DDD)V"),require=2)
    private void backport$centerBeam(Args args,AbstractClientEntity entity,MatrixStack matrix,IRenderTypeBuffer buffer){
        args.set(1,EvolutionBeamOrigin.modelY(entity.getPokemon().getForm().getDimensions().getHeight()));
    }
    @ModifyArgs(method="renderEvolutionLightBeams",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/client/render/entity/layers/PokemonShimLayer;drawBeam(Lnet/minecraft/util/math/vector/Matrix4f;FFLcom/mojang/blaze3d/vertex/IVertexBuilder;)V"),require=6)
    private void backport$compactBeam(Args args,AbstractClientEntity entity,MatrixStack matrix,IRenderTypeBuffer buffer){
        var size=entity.getPokemon().getForm().getDimensions();
        args.set(1,EvolutionBeamOrigin.width(args.get(1)));
        args.set(2,EvolutionBeamOrigin.length(args.get(2),size.getWidth(),size.getHeight()));
    }
}
