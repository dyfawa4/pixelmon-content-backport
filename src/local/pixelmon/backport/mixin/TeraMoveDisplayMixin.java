package local.pixelmon.backport.mixin;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.client.ClientProxy;
import com.pixelmonmod.pixelmon.client.gui.ScreenHelper;
import com.pixelmonmod.pixelmon.client.gui.battles.*;
import net.minecraft.util.ResourceLocation;
import local.pixelmon.backport.BattleInfoCompat;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Route native UI reads through battle-local display data, never mutate a move. */
@Mixin(targets="com.pixelmonmod.pixelmon.client.gui.battles.battleScreens.ChooseAttack$MoveButton",remap=false)
public abstract class TeraMoveDisplayMixin {
    @Redirect(method={"func_230431_b_","func_231044_a_"},at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/attacks/Attack;getDisabled()Z"),require=4)
    private boolean backport$bonusSetupDisabled(Attack a){PixelmonClientData user=backport$user();return a.getDisabled()||(local.pixelmon.backport.NinjaTraining.move(a)&&user!=null&&local.pixelmon.backport.ErosionClient.bonus(user.pokemonUUID));}
    @Inject(method="func_230431_b_",at=@At("RETURN"))
    private void backport$burstBorder(MatrixStack matrix,int mouseX,int mouseY,float partial,CallbackInfo ci){if(local.pixelmon.backport.BurstClient.move(attack))local.pixelmon.backport.BurstClient.moveOutline(matrix,(net.minecraft.client.gui.widget.Widget)(Object)this,id);}
    @Shadow public int id;
    @Shadow private Attack attack;
    @Shadow private int displayedPower;
    @Unique private static final ResourceLocation backport$stellarIcon=new ResourceLocation("pixelmon_backport","textures/gui/type/stellar.png");
    @Unique private PixelmonClientData backport$user(){return ClientProxy.battleManager==null?null:ClientProxy.battleManager.getCurrentPokemon();}
    @Inject(method="setupAttack",at=@At("RETURN"))
    private void backport$power(ClientBattleManager bm,PixelmonClientData user,CallbackInfo ci){displayedPower=BattleInfoCompat.movePower(attack,user,displayedPower);}
    @Inject(method="setupAttack",at=@At("HEAD"))
    private void backport$ordinaryRaidMove(ClientBattleManager bm,PixelmonClientData user,CallbackInfo ci){local.pixelmon.backport.TeraRaidGimmicks.clear(bm);}
    @Redirect(method="func_230431_b_",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/attacks/Attack;getType()Lcom/pixelmonmod/pixelmon/api/pokemon/Element;"))
    private Element backport$iconType(Attack move){return BattleInfoCompat.moveType(move,backport$user());}
    @Redirect(method="func_230431_b_",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/battles/attacks/Attack;getAttackCategory()Lcom/pixelmonmod/pixelmon/api/battles/AttackCategory;"))
    private AttackCategory backport$category(Attack move){return BattleInfoCompat.moveCategory(move,backport$user());}
    @Redirect(method="func_230431_b_",at=@At(value="INVOKE",target="Lcom/pixelmonmod/pixelmon/client/gui/ScreenHelper;drawImageQuadUV(Lnet/minecraft/util/ResourceLocation;Lcom/mojang/blaze3d/matrix/MatrixStack;FFFFFFFFFFFFF)V"))
    private void backport$stellar(ResourceLocation texture,MatrixStack matrix,float x,float y,float w,float h,float u,float v,float tw,float th,float r,float g,float b,float a,float z){
        if(BattleInfoCompat.moveType(attack,backport$user())==Element.MYSTERY)ScreenHelper.drawImageQuad(backport$stellarIcon,matrix,x,y,w,h,0,0,1,1,r,g,b,a,z);
        else ScreenHelper.drawImageQuadUV(texture,matrix,x,y,w,h,u,v,tw,th,r,g,b,a,z);
    }
}
