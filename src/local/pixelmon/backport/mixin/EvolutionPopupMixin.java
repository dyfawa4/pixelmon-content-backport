package local.pixelmon.backport.mixin;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.client.gui.Resources;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import com.pixelmonmod.pixelmon.client.gui.widgets.*;
import local.pixelmon.backport.client.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(value=PokeCheckerButton.class,remap=false)
public abstract class EvolutionPopupMixin extends IndexedButton {
    @Shadow private Screen parent;
    @Shadow private PokemonStorage storage;
    @Shadow private StoragePosition position;
    @Shadow private Pokemon pokemon;
    @Unique private EvolutionMiniButtonAccess[] backport$rows;
    protected EvolutionPopupMixin(){super(-1,0,0,67,94,StringTextComponent.field_240750_d_,b->{});}
    @Inject(method="<init>",at=@At("RETURN"))
    private void backport$fourRows(Screen parent,CallbackInfo ci){field_230689_k_=94;backport$rows=EvolutionNativePopup.rows((PokeCheckerButton)(Object)this);}
    @Inject(method="setPokemon",at=@At("RETURN"))
    private void backport$fit(PokemonStorage storage,StoragePosition position,Pokemon pokemon,double x,double y,CallbackInfo ci){field_230690_l_=Math.max(2,field_230690_l_);field_230691_m_=Math.max(2,Math.min(field_230691_m_,parent.field_230709_l_-field_230689_k_-2));}
    @Inject(method="func_230430_a_",at=@At("HEAD"),cancellable=true)
    private void backport$render(MatrixStack m,int mx,int my,float partial,CallbackInfo ci){
        ci.cancel();if(!field_230693_o_||pokemon==null)return;int x=field_230690_l_,y=field_230691_m_;
        field_230692_n_=mx>=x&&mx<x+67&&my>=y&&my<y+94;
        m.func_227860_a_();m.func_227861_a_(0,0,400);
        try{
            parent.getMinecraft().func_110434_K().func_110577_a(Resources.pokecheckerPopup);
            func_238474_b_(m,x,y,0,0,67,69);
            func_238474_b_(m,x,y+69,0,50,67,19);
            func_238474_b_(m,x,y+88,0,69,67,6);
            for(var row:backport$rows)row.backport$render(m,mx,my,partial);
            ScreenHelper.drawCenteredString(m,pokemon.getDisplayName(),x+33.5f,y+2,0xffffff,true);
            for(var row:backport$rows)row.backport$drawText(m);
        }finally{m.func_227865_b_();}
    }
    @Inject(method="func_231044_a_",at=@At("HEAD"),cancellable=true)
    private void backport$click(double x,double y,int button,CallbackInfoReturnable<Boolean> ci){
        if(!field_230693_o_||button!=0||x<field_230690_l_+10||x>=field_230690_l_+57||y<field_230691_m_+72||y>=field_230691_m_+85)return;
        if(parent.getMinecraft().field_71439_g.field_71071_by.func_70445_o().func_190926_b()){
            com.pixelmonmod.pixelmon.client.SoundHelper.playButtonPressSound();
            parent.getMinecraft().func_147108_a(new EvolutionGuideScreen(storage,position,parent));
        }
        ci.setReturnValue(true);
    }
}
