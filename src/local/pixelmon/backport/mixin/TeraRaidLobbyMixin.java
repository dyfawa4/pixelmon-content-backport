package local.pixelmon.backport.mixin;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.battles.raids.RaidData;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.client.gui.raids.RaidStartScreen;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;
@Mixin(value=RaidStartScreen.class,remap=false)
public abstract class TeraRaidLobbyMixin extends Screen {
    @Shadow private RaidData raid;
    protected TeraRaidLobbyMixin(){super(new StringTextComponent(""));}
    private TeraDenEntity den(){return raid.getDenEntity(Minecraft.func_71410_x().field_71441_e).filter(d->d instanceof TeraDenEntity).map(d->(TeraDenEntity)d).orElse(null);}
    @Redirect(method="func_230430_a_",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/api/pokemon/species/Stats;getTypes()Ljava/util/List;"))
    private List<Element> backport$bossType(Stats stats){TeraDenEntity d=den();return d==null?stats.getTypes():Collections.singletonList(Tera.TYPES[d.teraType()]);}
    @Inject(method="func_230430_a_",at=@At("RETURN"))
    private void backport$instructions(MatrixStack m,int x,int y,float partial,CallbackInfo ci){
        if(den()==null)return;int cx=field_230708_k_/2;
        func_238471_a_(m,field_230712_o_,"太晶团战 · 300 秒",cx,7,0xffffff);
        func_238471_a_(m,field_230712_o_,"使用 3 次招式充能；倒下后等待复活",cx,19,0xffffff);
        func_238471_a_(m,field_230712_o_,"胜利：属性碎片、太晶爆发光盘与星级奖励",cx,31,0xffffff);
    }
}
