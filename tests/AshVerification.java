import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.BattleBond;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import local.pixelmon.backport.*;
import net.minecraft.nbt.CompoundNBT;
public final class AshVerification {
    static class Trainer extends WildPixelmonParticipant {
        Trainer(Pokemon... p){super(p);}
        @Override public ParticipantType getType(){return ParticipantType.Trainer;}
        @Override public boolean hasMorePokemon(){return countAblePokemon()>0;}
    }
    static void require(boolean x,String s){if(!x)throw new AssertionError(s);}
    public static void run(){
        Pokemon p=TeraVerification.mon(658);p.setForm("battle_bond");
        Trainer a=new Trainer(p),b=new Trainer(TeraVerification.mon(143),TeraVerification.mon(242));
        BattleController battle=new BattleController(new BattleParticipant[]{a},new BattleParticipant[]{b},new BattleRules());battle.sendMessages=false;
        PixelmonWrapper user=a.allPokemon[0],target=b.allPokemon[0];BattleBond bond=(BattleBond)user.getBattleAbility();Attack water=new Attack("Water Shuriken");
        bond.tookDamageUser(1,user,target,water);require(!PermanentAsh.isPermanent(user),"No unlock without KO");
        target.setHealth(0);bond.tookDamageUser(300,user,target,water);require(PermanentAsh.isPermanent(user),"Native KO earns Ash");
        require(bond.modifyPowerAndAccuracyUser(15,100,user,b.allPokemon[1],water)[0]==20,"Boosted Water Shuriken power");
        user.resetOnSwitch();require(PermanentAsh.isPermanent(user),"Ash switch");
        bond.onSelfFaint(user,target);require(PermanentAsh.isPermanent(user),"Ash faint");
        bond.applyEndOfBattleEffect(user);user.resetBattleEvolution();require(PermanentAsh.isPermanent(user),"Ash battle end");
        Pokemon saved=PokemonFactory.create(user.pokemon.writeToNBT(new CompoundNBT()));require(saved.getForm().isForm(PermanentAsh.FORM),"Ash restart/trade data");saved.setHealth(0);saved.heal();require(saved.getForm().isForm(PermanentAsh.FORM),"Ash revive");
        Pokemon z=TeraVerification.mon(718);z.setForm("mega");BattleController za=TeraVerification.battle(z);PixelmonWrapper mega=za.participants.get(0).allPokemon[0];
        z.getMoveset().clear();Attack original=new Attack("Core Enforcer");original.pp=7;z.getMoveset().add(original);mega.attack=original;
        ZaMoves.mega(mega);require(mega.getMoveset().get(0).getMove().getAttackName().equals("Nihil Light"),"ZA signature move");require(z.getMoveset().get(0)==original,"No save move mutation");require(mega.attack.getMove().getAttackName().equals("Nihil Light"),"Selected move updates");
        mega.getMoveset().get(0).pp-=2;mega.resetBattleEvolution();require(mega.getMoveset().get(0)==original && original.pp==5,"Restored move with used PP");
        System.out.println("BACKPORT_ASH_ZA_PASS KO, switch, faint, revive, end, NBT, signature move and PP restoration");
    }
}
