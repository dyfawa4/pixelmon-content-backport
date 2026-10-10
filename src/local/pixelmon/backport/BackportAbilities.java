package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.pokemon.ability.AbstractAbility;
import com.pixelmonmod.pixelmon.battles.controller.participants.PixelmonWrapper;
import com.pixelmonmod.pixelmon.battles.status.*;

public final class BackportAbilities {
    private BackportAbilities(){}
    public static class TeraShift extends AbstractAbility {
        @Override public void applySwitchInEffect(PixelmonWrapper pokemon) {
            if(pokemon.getSpecies().getDex()!=1024 || !pokemon.getForm().isForm("normal"))return;
            float ratio=pokemon.getHealthPercent()/100f;boolean full=pokemon.hasFullHealth();
            pokemon.setForm("terastal");pokemon.recalculateMaxHP();
            pokemon.setHealth(full?pokemon.getMaxHealth():Math.round(pokemon.getMaxHealth()*ratio));
            pokemon.bc.modifyStats(pokemon);pokemon.updateBattleDamage(pokemon.getHealth());
        }
        @Override public boolean canBeDisabled(){return false;}
        @Override public boolean canBeCopied(){return false;}
        @Override public boolean canBeIgnored(){return false;}
        @Override public void applyEndOfBattleEffect(PixelmonWrapper pokemon) {
            Tera.restoreTerapagos(pokemon);
        }
    }
    public static class TeraShell extends AbstractAbility {
        private transient PixelmonWrapper attacker;
        public boolean resists(PixelmonWrapper user,PixelmonWrapper target){
            if(!user.inMultipleHit)attacker=null;
            boolean result=target.hasFullHealth() || (user.inMultipleHit && attacker==user);
            if(result && user.inMultipleHit)attacker=user;
            return result;
        }
        @Override public void applyRepeatedEffect(PixelmonWrapper pokemon){attacker=null;}
        @Override public void applyEndOfBattleEffect(PixelmonWrapper pokemon) {
            // Tera Shift changes the native form and therefore the ability.
            // End-of-battle dispatch now reaches Tera Shell, not Tera Shift.
            if(pokemon.getForm().isForm("terastal"))Tera.restoreTerapagos(pokemon);
        }
    }
    public static class PoisonPuppeteer extends AbstractAbility {
        @Override public void onStatusAdded(StatusBase status,PixelmonWrapper user,PixelmonWrapper target){
            if(user!=target && user.getSpecies().getDex()==1025 && (status.type==StatusType.Poison || status.type==StatusType.PoisonBadly)) Confusion.confuse(user,target);
        }
        @Override public boolean canBeCopied(){return false;}
    }
    public static class TeraformZero extends AbstractAbility {
        @Override public boolean canBeDisabled(){return false;}
        @Override public boolean canBeCopied(){return false;}
        @Override public boolean canBeIgnored(){return false;}
        @Override public void applyEndOfBattleEffect(PixelmonWrapper p){Tera.restoreTerapagos(p);}
    }
    public static class EmbodyAspect extends AbstractAbility {
        @Override public void applySwitchInEffect(PixelmonWrapper p){Tera.aspect(p,true);}
        @Override public boolean canBeCopied(){return false;}
    }
}
