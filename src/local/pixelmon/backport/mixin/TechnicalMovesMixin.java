package local.pixelmon.backport.mixin;
import com.pixelmonmod.pixelmon.enums.TMType;
import com.pixelmonmod.pixelmon.enums.technicalmoves.ITechnicalMove;
import com.pixelmonmod.pixelmon.items.TechnicalMoveItem;
import net.minecraft.item.ItemStack;
import local.pixelmon.backport.BackportTechnicalMoves;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=TechnicalMoveItem.class,remap=false)
public abstract class TechnicalMovesMixin {
    /** Build the label from the same resolved move used when teaching the Pokemon. */
    public net.minecraft.util.text.ITextComponent func_200295_i(ItemStack stack){
        if(stack.func_77973_b() instanceof local.pixelmon.backport.DisposableTeraDisc)return new net.minecraft.util.text.TranslationTextComponent("item.pixelmon_backport.tera_blast_disc_single_use");
        ITechnicalMove move=TechnicalMoveItem.getMove(stack);
        if(move==null || move.getAttack()==null)return new net.minecraft.util.text.TranslationTextComponent(stack.func_77973_b().func_77667_c(stack));
        String kind=((TechnicalMoveItem)stack.func_77973_b()).type().startsWith("TR")?"TR":"TM";
        String number=move.getGeneration()>=8?String.format(java.util.Locale.ROOT,"%02d",move.getId()):Integer.toString(move.getId());
        return new net.minecraft.util.text.TranslationTextComponent("pixelmon.backport.technical_move.name",
            new net.minecraft.util.text.TranslationTextComponent("pixelmon.backport.region.gen"+move.getGeneration()),kind,number,
            new net.minecraft.util.text.TranslationTextComponent(move.getAttack().getTranslationKey()));
    }
    @Inject(method="getMove",at=@At("RETURN"),cancellable=true)
    private static void backport$item(ItemStack stack,CallbackInfoReturnable<ITechnicalMove> ci){
        if(stack.func_77973_b() instanceof local.pixelmon.backport.DisposableTeraDisc){ci.setReturnValue(local.pixelmon.backport.DisposableTeraDisc.move());return;}
        if(ci.getReturnValue()==null && stack.func_77973_b() instanceof TechnicalMoveItem && stack.func_77942_o()
            && ((TechnicalMoveItem)stack.func_77973_b()).type().equals("TM9")) {
            BackportTechnicalMoves move=BackportTechnicalMoves.byId(stack.func_77978_p().func_74765_d("tm"));
            if(move!=null)ci.setReturnValue(move);
        }
    }
    @Redirect(method="func_150895_a",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/enums/TMType;getMoves()[Lcom/pixelmonmod/pixelmon/enums/technicalmoves/ITechnicalMove;"))
    private ITechnicalMove[] backport$creative(TMType type){return BackportTechnicalMoves.all(type,type.getMoves());}
    @Redirect(method="func_77663_a",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/enums/technicalmoves/ITechnicalMove;getAllFor(Lcom/pixelmonmod/pixelmon/enums/TMType;)[Lcom/pixelmonmod/pixelmon/enums/technicalmoves/ITechnicalMove;"))
    private ITechnicalMove[] backport$random(TMType type){return BackportTechnicalMoves.all(type,ITechnicalMove.getAllFor(type));}
}
