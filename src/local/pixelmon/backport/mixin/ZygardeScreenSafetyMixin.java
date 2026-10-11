package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.species.Species;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.client.gui.zygarde.ZygardeCubeScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** A party update may arrive after the native open-screen packet. The native init
 * already closes an invalid screen; allow its constructor to reach that check. */
@Mixin(value=ZygardeCubeScreen.class,remap=false)
public abstract class ZygardeScreenSafetyMixin {
    @Redirect(method="<init>",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/Pokemon;getSpecies()Lcom/pixelmonmod/pixelmon/api/pokemon/species/Species;"))
    private Species backport$missingParty(Pokemon pokemon){return pokemon==null?PixelmonSpecies.BULBASAUR.getValueUnsafe():pokemon.getSpecies();}
    @Redirect(method="func_231160_c_",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/items/ZygardeCubeItem;getCoreTypes(Lnet/minecraft/item/ItemStack;)Ljava/util/Set;"))
    private java.util.Set<com.pixelmonmod.pixelmon.items.ZygardeCubeItem.CoreType> backport$validMoves(net.minecraft.item.ItemStack cube){
        var cores=com.pixelmonmod.pixelmon.items.ZygardeCubeItem.getCoreTypes(cube);cores.removeIf(core->core.getMove()==null);return cores;
    }
}
