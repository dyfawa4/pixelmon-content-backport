import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.attacks.*;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.status.*;
import local.pixelmon.backport.*;
import net.minecraft.nbt.CompoundNBT;

/** Runs on native registered Pokemon and native battle wrappers in Forge. */
public final class TeraVerification {
    static void require(boolean b,String label){if(!b)throw new AssertionError(label);}
    static void close(double a,double b,String label){require(Math.abs(a-b)<.001,label+" "+a+" != "+b);}
    static Pokemon mon(int dex){Pokemon p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(70);p.initialize();p.heal();return p;}
    static BattleController battle(Pokemon... mons){
        WildPixelmonParticipant ours=new WildPixelmonParticipant(mons),enemy=new WildPixelmonParticipant(mon(143),mon(242));
        BattleController b=new BattleController(new BattleParticipant[]{ours},new BattleParticipant[]{enemy},new BattleRules());b.sendMessages=false;b.modifyStats();return b;
    }
    public static void run(){
        int cases=0;
        Pokemon regional=mon(37);regional.setForm("alolan");
        require(Tera.type(regional)==Element.ICE,"Regional initial Tera must use current form");
        regional.setForm("base");require(Tera.type(regional)==Element.ICE,"Saved Tera type must persist across form changes");
        for(Element type:Tera.TYPES){
            Pokemon p=mon(6);require(Tera.changeType(p,type),"Type change "+type);
            require(Tera.type(PokemonFactory.create(p.writeToNBT(new CompoundNBT())))==type,"Type NBT "+type);
            BattleController b=battle(p,mon(9));PixelmonWrapper u=b.participants.get(0).allPokemon[0],other=b.participants.get(0).allPokemon[1];
            require(Tera.select(u),"Select "+type);require(u.megaEvolve(),"Native evolution phase "+type);
            require(Tera.active(u),"Active "+type);require(!Tera.select(other),"One per participant "+type);
            require(!u.canDynamax() && !u.canMegaEvolve(),"Other gimmicks blocked "+type);
            require(u.type.equals(type==Element.MYSTERY?Arrays.asList(Element.FIRE,Element.FLYING):Collections.singletonList(type)),"Defensive types "+type);
            u.setTempType(Element.WATER);require(u.type.contains(type==Element.MYSTERY?Element.FIRE:type),"Conversion blocked "+type);
            u.resetOnSwitch();require(Tera.active(u),"Re-entry persists "+type);require(u.type.contains(type==Element.MYSTERY?Element.FIRE:type),"Re-entry type "+type);
            close(Tera.stab(u,Element.FIRE,1.5),type==Element.MYSTERY||type==Element.FIRE?2:1.5,"Original STAB "+type);
            if(type!=Element.MYSTERY)close(Tera.stab(u,type,1),type==Element.FIRE||type==Element.FLYING?2:1.5,"Tera STAB "+type);
            PixelmonWrapper cloned=u.copy();require(Tera.active(cloned),"AI copy "+type);
            require(!Tera.select(cloned),"AI clone cannot commit live participant selection "+type);
            Tera.end(b);require(!Tera.active(u) && !Tera.used(u.getParticipant()),"End cleanup "+type);++cases;
        }
        BattleController b=battle(mon(6));PixelmonWrapper u=b.participants.get(0).allPokemon[0];Tera.changeType(u.pokemon,Element.FIRE);Tera.select(u);u.megaEvolve();
        Attack ember=new Attack("Ember"),quick=new Attack("Quick Attack"),shuriken=new Attack("Water Shuriken");
        require(Tera.floorPower(u,ember,40)==60,"60 floor");require(Tera.floorPower(u,quick,40)==40,"Other type floor excluded");require(Tera.floorPower(u,shuriken,15)==15,"Multihit excluded");Tera.end(b);
        b=battle(mon(6));u=b.participants.get(0).allPokemon[0];
        require(Tera.select(u) && u.willEvolve,"Queued Tera");Tera.cancelSelection(u);
        require(!Tera.selected(u) && !u.willEvolve && !Tera.used(u.getParticipant()),"Cancel must not consume gimmick");
        require(Tera.select(u) && u.megaEvolve(),"Tera reselect after native cancellation");Tera.cancelSelection(u);
        require(Tera.active(u) && Tera.used(u.getParticipant()),"Cannot cancel active Tera");Tera.end(b);
        for(String mask:new String[]{"teal","wellspring","hearthflame","cornerstone"}){
            Pokemon p=mon(1017);p.setForm(mask);b=battle(p);u=b.participants.get(0).allPokemon[0];
            require(!Tera.changeType(p,Element.NORMAL),"Ogerpon locked type");Tera.select(u);u.megaEvolve();
            require(u.getForm().isForm("tera"+mask),"Tera mask "+mask);
            BattleStatsType stat=mask.equals("wellspring")?BattleStatsType.SPECIAL_DEFENSE:mask.equals("hearthflame")?BattleStatsType.ATTACK:mask.equals("cornerstone")?BattleStatsType.DEFENSE:BattleStatsType.SPEED;
            require(u.getBattleStats().getStage(stat)==1,"Aspect exactly once "+mask);
            u.attack=new Attack("Ivy Cudgel");new BackportEffects.IvyCudgel().applyEffectStart(u,b.participants.get(1).allPokemon[0]);require(u.attack.getType()==Tera.type(p),"Cudgel Tera type "+mask);
            Tera.end(b);require(p.getForm().isForm(mask),"Mask restored "+mask);++cases;
        }
        b=battle(mon(1024));u=b.participants.get(0).allPokemon[0];u.getBattleAbility().applySwitchInEffect(u);
        require(u.getForm().isForm("terastal"),"Tera Shift");require(u.hasFullHealth(),"Tera Shift full HP");
        b.globalStatusController.addGlobalStatus(u,new Rainy());b.globalStatusController.addGlobalStatus(u,new ElectricTerrain(5));
        u.setHealth(u.getMaxHealth()-20);int lost=u.getMaxHealth()-u.getHealth();
        require(Tera.select(u) && u.megaEvolve(),"Terapagos Tera");require(u.getForm().isForm("stellar"),"Stellar form");require(u.getMaxHealth()-u.getHealth()==lost,"Stellar transformation preserves lost HP");
        require(b.globalStatusController.getWeatherIgnoreAbility()==null,"Zero removed weather");require(b.globalStatusController.getTerrain() instanceof NoTerrain,"Zero removed terrain");
        u.attack=new Attack("Tera Starstorm");new BackportEffects.TeraStarstorm().applyEffectStart(u,b.participants.get(1).allPokemon[0]);require(u.attack.getType()==Element.MYSTERY,"Starstorm Stellar");require(u.getTargets(u.attack).size()==2,"Starstorm AOE");
        u.setHealth(u.getMaxHealth()/2);double ratio=u.getHealth()/(double)u.getMaxHealth();
        u.getBattleAbility().applyEndOfBattleEffect(u);Tera.end(b);require(u.pokemon.getForm().isForm("normal"),"Terapagos restored");
        require(Math.abs(u.getHealth()/(double)u.getMaxHealth()-ratio)<=1.0/u.getMaxHealth(),"Terapagos end healed injured Pokemon");
        b=battle(mon(1024));u=b.participants.get(0).allPokemon[0];u.getBattleAbility().applySwitchInEffect(u);Tera.select(u);u.megaEvolve();u.setHealth(0);u.getBattleAbility().applyEndOfBattleEffect(u);Tera.end(b);require(u.getHealth()==0,"Terapagos end revived fainted Pokemon");
        System.out.println("BACKPORT_TERA_PASS cases="+(cases+2)+" native evolution phase, NBT, switch, STAB, locked forms, weather, terrain, targets");
    }
}
