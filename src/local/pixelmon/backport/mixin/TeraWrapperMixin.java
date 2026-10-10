package local.pixelmon.backport.mixin;
import local.pixelmon.backport.Tera;
import local.pixelmon.backport.Backport;
import local.pixelmon.backport.UmbrellaHooks;
import local.pixelmon.backport.TeraRaids;
import local.pixelmon.backport.TeraRaidParticipant;
import com.pixelmonmod.pixelmon.items.HeldItem;
import com.pixelmonmod.pixelmon.api.pokemon.Element;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import java.util.List;
import java.util.ArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=PixelmonWrapper.class,remap=false)
public abstract class TeraWrapperMixin {
    private PixelmonWrapper self(){return (PixelmonWrapper)(Object)this;}
    @Inject(method="setHeldItem",at=@At("RETURN"))
    private void backport$weatherAfterItem(HeldItem item,CallbackInfo ci){PixelmonWrapper p=self();if(p.bc!=null && !p.bc.simulateMode)p.getBattleAbility().onWeatherChange(p,UmbrellaHooks.backport$weather(p.bc.globalStatusController,p));}
    @Inject(method="isItemRemovable",at=@At("HEAD"),cancellable=true)
    private void backport$keepMask(PixelmonWrapper user,CallbackInfoReturnable<Boolean> cir){if(self().getSpecies().getDex()==1017 && self().getHeldItem() instanceof Backport.Mask)cir.setReturnValue(false);}
    @Inject(method="<init>(Lcom/pixelmonmod/pixelmon/battles/controller/participants/PixelmonWrapper;)V",at=@At("RETURN"))
    private void backport$copy(PixelmonWrapper original,CallbackInfo ci){Tera.copy(original,self());}
    @Inject(method="megaEvolve",at=@At("HEAD"),cancellable=true)
    private void backport$tera(CallbackInfoReturnable<Boolean> cir){if(Tera.selected(self()))cir.setReturnValue(Tera.activate(self()));}
    @Inject(method="setAttack(Lcom/pixelmonmod/pixelmon/battles/attacks/Attack;Ljava/util/List;Z)V",at=@At("RETURN"))
    private void backport$choice(Attack attack,List<PixelmonWrapper> targets,boolean evolving,CallbackInfo ci){if(Tera.selected(self()))self().willEvolve=true;if(Tera.selected(self()) || Tera.active(self()) || Tera.used(self().getParticipant()))self().usingZ=false;if(self().getSpecies().getDex()==1024 && (Tera.active(self()) || Tera.selected(self())) && attack.getMove().getAttackName().equals("Tera Starstorm"))self().targets=self().getTargets(attack);}
    @Inject(method="getTargets",at=@At("HEAD"),cancellable=true)
    private void backport$starstorm(Attack attack,CallbackInfoReturnable<ArrayList<PixelmonWrapper>> cir){
        if(attack!=null && attack.getMove().getAttackName().equals("Tera Starstorm") && self().getSpecies().getDex()==1024 && (Tera.active(self()) || Tera.selected(self()))){
            ArrayList<PixelmonWrapper> targets=new ArrayList<>();for(PixelmonWrapper p:self().bc.getAdjacentPokemon(self()))if(!self().isAlly(p))targets.add(p);cir.setReturnValue(targets);
        }
    }
    @Inject(method="setTempType(Ljava/util/List;)V",at=@At("HEAD"),cancellable=true)
    private void backport$blockType(List<Element> type,CallbackInfo ci){if(Tera.active(self()))ci.cancel();}
    @Inject(method={"resetOnSwitch","setForm(Lcom/pixelmonmod/pixelmon/api/pokemon/species/Stats;)V"},at=@At("RETURN"))
    private void backport$keepType(CallbackInfo ci){Tera.enforceType(self());}
    @Inject(method={"canMegaEvolve()Z","canDynamax","canGigantamax","canUltraBurst","registerZMove"},at=@At("HEAD"),cancellable=true)
    private void backport$gimmick(CallbackInfoReturnable<Boolean> cir){if(TeraRaids.get(self().bc)!=null || Tera.active(self()) || Tera.used(self().getParticipant()))cir.setReturnValue(false);}
    @Inject(method="getMaxHealth(Z)I",at=@At("RETURN"),cancellable=true)
    private void backport$raidHP(boolean ignoreDynamax,CallbackInfoReturnable<Integer> cir){if(!ignoreDynamax && self().getParticipant() instanceof TeraRaidParticipant)cir.setReturnValue(cir.getReturnValue()*TeraRaids.HP[((TeraRaidParticipant)self().getParticipant()).stars]);}
    @Inject(method="useAttack(Z)V",at=@At("RETURN"))
    private void backport$spent(boolean affectPP,CallbackInfo ci){Tera.consumeBoost(self());Tera.enforceType(self());}
    @Inject(method="useAttack(Z)V",at=@At("HEAD"))
    private void backport$beginMove(boolean affectPP,CallbackInfo ci){Tera.beginMove(self());}
}
