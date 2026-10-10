package local.pixelmon.backport.mixin;
import local.pixelmon.backport.*;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.client.gui.battles.*;
import com.pixelmonmod.pixelmon.api.battles.BattleMode;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=BattleScreen.class,remap=false)
public abstract class TeraScreenMixin extends Screen {
    @Shadow public ClientBattleManager bm;
    @Unique private TeraButton backport$button;
    @Unique private long backport$nextQuery;
    protected TeraScreenMixin(){super(new StringTextComponent(""));}
    @Inject(method="func_231160_c_",at=@At("RETURN"))
    private void backport$init(CallbackInfo ci){backport$button=func_230480_a_(new TeraButton(4,4,new TranslationTextComponent("pixelmon.backport.tera.button"),b->{PixelmonClientData p=bm.getCurrentPokemon();if(p!=null){bm.megaEvolving=false;bm.dynamaxing=false;bm.showZMoves=false;TeraNetwork.CHANNEL.sendToServer(new TeraNetwork.Request(bm.battleControllerIndex,p.pokemonUUID,1,-1));}}));backport$button.field_230694_p_=false;}
    @Inject(method="func_230430_a_",at=@At("HEAD"))
    private void backport$hideBeforeNativeRender(MatrixStack matrix,int x,int y,float partial,CallbackInfo ci){if(backport$button!=null)backport$button.field_230694_p_=false;}
    @Inject(method="func_230430_a_",at=@At("RETURN"))
    private void backport$render(MatrixStack matrix,int x,int y,float partial,CallbackInfo ci){
        if(backport$button==null)return;PixelmonClientData p=bm.getCurrentPokemon();BattleMode mode=bm.getMode();
        boolean visible=bm.isBattling() && p!=null && (mode==BattleMode.CHOOSE_ATTACK || mode==BattleMode.MAIN_MENU);
        backport$button.field_230694_p_=visible;backport$button.field_230693_o_=false;
        if(!visible)return;
        if(System.nanoTime()>backport$nextQuery){backport$nextQuery=System.nanoTime()+500000000L;TeraNetwork.CHANNEL.sendToServer(new TeraNetwork.Request(bm.battleControllerIndex,p.pokemonUUID,0,-1));}
        TeraNetwork.Reply r=TeraClient.battle;
        boolean matches=r!=null && r.battle==bm.battleControllerIndex && r.pokemon.equals(p.pokemonUUID);
        backport$button.field_230693_o_=matches && (r.eligible||r.selected);
        backport$button.configure(!matches?0:r.active?3:r.selected?2:r.eligible?1:0,matches?r.type:0);
        backport$button.func_238482_a_(new TranslationTextComponent(matches && r.active?"pixelmon.backport.tera.active":matches && r.selected?"pixelmon.backport.tera.selected":"pixelmon.backport.tera.button"));
        // Match the native ChooseAttack move-area anchor. Single battles leave
        // space above its left column; doubles place additional name cards
        // there, so move just above that row instead of covering their labels.
        int moveWidth=Math.max(1,Math.min(field_230708_k_-138,300)/2);
        backport$button.field_230690_l_=Math.max(4,field_230708_k_-80-moveWidth*2-8);
        boolean multiple=bm.displayedOurPokemon!=null && bm.displayedOurPokemon.length>1;
        backport$button.field_230691_m_=Math.max(4,field_230709_l_-(multiple?154:104));
        backport$button.func_230431_b_(matrix,x,y,partial);
    }
}
