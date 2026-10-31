package local.pixelmon.backport.mixin;
import java.util.*;
import com.pixelmonmod.pixelmon.client.gui.pokechecker.*;
import com.pixelmonmod.pixelmon.client.gui.widgets.IndexedButton;
import local.pixelmon.backport.client.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.util.text.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=PokeCheckerScreen.class,remap=false)
public abstract class EvolutionCheckerTabsMixin extends Screen {
    protected EvolutionCheckerTabsMixin(){super(StringTextComponent.field_240750_d_);}
    @Inject(method="func_231160_c_",at=@At("RETURN"))
    private void backport$fourTabs(CallbackInfo ci){
        var self=(PokeCheckerScreen)(Object)this;
        var old=new ArrayList<Widget>(field_230710_m_);
        for(var button:old)if(button instanceof PokeCheckerTabsScreen){
            int id=((IndexedButton)button).id;if(id<1||id>3)continue;
            field_230710_m_.remove(button);field_230705_e_.remove(button);
            boolean selected=id==1&&self.getClass()==PokeCheckerScreen.class||id==2&&self instanceof PokeCheckerMovesScreen||id==3&&self instanceof PokeCheckerStatsScreen;
            func_230480_a_(new EvolutionTabs.Tab(field_230708_k_/2-127+(id-1)*58,field_230709_l_/2+80,57,15,button.func_230458_i_(),b->self.actionPerformed((net.minecraft.client.gui.widget.button.Button)button),selected));
        }
        func_230480_a_(new EvolutionTabs.Tab(field_230708_k_/2+47,field_230709_l_/2+80,58,15,new TranslationTextComponent("pixelmon_backport.evolution.tab"),b->{if(!(self instanceof EvolutionGuideScreen))field_230706_i_.func_147108_a(new EvolutionGuideScreen(self));},self instanceof EvolutionGuideScreen));
    }
}
