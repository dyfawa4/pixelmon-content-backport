package local.pixelmon.backport.mixin;
import local.pixelmon.backport.TcgRemoval;
import net.minecraft.client.gui.screen.inventory.*;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Also filter the vanilla search tab, which enumerates every registered item. */
@Mixin(value=CreativeScreen.class,remap=false)
public abstract class TcgCreativeSearchMixin extends ContainerScreen<CreativeScreen.CreativeContainer> {
    protected TcgCreativeSearchMixin(CreativeScreen.CreativeContainer menu,PlayerInventory inventory,ITextComponent title){super(menu,inventory,title);}
    @Inject(method={"func_147050_b","func_147053_i"},at=@At("RETURN"))
    private void backport$hideSearch(CallbackInfo ci){if(field_147002_h.field_148330_a.removeIf(TcgRemoval::item))field_147002_h.func_148329_a(0);}
}
