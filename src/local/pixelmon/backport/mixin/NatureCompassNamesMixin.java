package local.pixelmon.backport.mixin;
import local.pixelmon.backport.CompassNames;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets="com.chaosthedude.naturescompass.util.BiomeUtils",remap=false)
public abstract class NatureCompassNamesMixin {
    @Shadow public static ResourceLocation getKeyForBiome(World world,Biome biome){throw new AssertionError();}
    @Inject(method={"getBiomeNameForDisplay(Lnet/minecraft/world/World;Lnet/minecraft/world/biome/Biome;)Ljava/lang/String;","getBiomeName(Lnet/minecraft/world/World;Lnet/minecraft/world/biome/Biome;)Ljava/lang/String;"},at=@At("HEAD"),cancellable=true)
    private static void backport$biomeName(World world,Biome biome,CallbackInfoReturnable<String> ci){
        if(world==null || biome==null)return;String value=CompassNames.name("biome",getKeyForBiome(world,biome));if(value!=null)ci.setReturnValue(value);
    }
    @Inject(method="getBiomeSource",at=@At("HEAD"),cancellable=true)
    private static void backport$biomeSource(World world,Biome biome,CallbackInfoReturnable<String> ci){
        if(world==null || biome==null)return;String value=CompassNames.name("source",getKeyForBiome(world,biome));if(value!=null)ci.setReturnValue(value);
    }
}
