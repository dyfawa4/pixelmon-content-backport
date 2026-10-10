import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.stats.BattleStatsType;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.*;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRules;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.attacks.Attack;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.*;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.multiTurn.SolarBeam;
import com.pixelmonmod.pixelmon.battles.controller.log.AttackResult;
import com.pixelmonmod.pixelmon.battles.controller.log.MoveResults;
import com.pixelmonmod.pixelmon.battles.status.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import local.pixelmon.backport.*;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import net.minecraftforge.common.util.FakePlayer;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.events.pokemon.ItemFormChangeEvent;
import net.minecraftforge.eventbus.api.EventPriority;

/** Native behavior regression for grooming, meteorite, masks and weather consumers. */
public final class RepairVerification {
    static void require(boolean ok,String label){if(!ok)throw new AssertionError(label);}
    static Pokemon mon(int dex){Pokemon p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(70);p.initialize();p.heal();return p;}
    public static void run(FakePlayer player){
        PlayerPartyStorage party=StorageProxy.getParty(player);Pokemon previous=party.get(0);
        try{
            Pokemon p=mon(676);p.setForm("natural");party.set(0,p);PixelmonEntity e=p.getOrSpawnPixelmon(player);
            ItemStack shears=new ItemStack(Items.field_151097_aZ);player.func_184611_a(Hand.MAIN_HAND,shears);
            String moves=p.getMoveset().toString();int hp=p.getHealth();
            for(int i=1;i<=6;i++){
                require(Grooming.apply(player,e,Hand.MAIN_HAND),"Grooming rejected");
                String form=Grooming.FORMS[i%6];require(p.getForm().getName().equals(form),"Grooming form "+form);
                require(PokemonFactory.create(p.writeToNBT(new CompoundNBT())).getForm().getName().equals(form),"Grooming NBT");
            }
            require(shears.func_77952_i()==6 && p.getHealth()==hp && p.getMoveset().toString().equals(moves),"Grooming durability/health/moves");
            require(!Grooming.apply(player,e,Hand.OFF_HAND),"Offhand groom accepted");
            boolean creative=player.field_71075_bZ.field_75098_d;
            try{player.field_71075_bZ.field_75098_d=true;int wear=shears.func_77952_i();
                require(Grooming.apply(player,e,Hand.MAIN_HAND) && shears.func_77952_i()==wear,"Creative grooming wears shears");
            }finally{player.field_71075_bZ.field_75098_d=creative;}
            java.util.function.Consumer<ItemFormChangeEvent> cancel=event->event.setCanceled(true);
            Pixelmon.EVENT_BUS.addListener(EventPriority.NORMAL,false,ItemFormChangeEvent.class,cancel);
            try{String form=p.getForm().getName();int wear=shears.func_77952_i();
                require(!Grooming.apply(player,e,Hand.MAIN_HAND) && p.getForm().getName().equals(form) && shears.func_77952_i()==wear,"Cancelled grooming changed Pokemon/item");
            }finally{Pixelmon.EVENT_BUS.unregister(cancel);}
            Pokemon unowned=mon(676);require(!Grooming.apply(player,unowned.getOrCreatePixelmon(player.field_70170_p,0,5,0),Hand.MAIN_HAND),"Unowned groom accepted");
            Pokemon eternatus=mon(890);party.set(0,eternatus);eternatus.setHeldItem(new ItemStack(Backport.REGISTERED.get("eternatus_meteorite").get()));
            require(eternatus.getForm().isForm("eternamax"),"Meteorite equip form");
            require(PokemonFactory.create(eternatus.writeToNBT(new CompoundNBT())).getForm().isForm("eternamax"),"Meteorite save");
            eternatus.setHeldItem(ItemStack.field_190927_a);require(eternatus.getForm().isForm("ordinary"),"Meteorite removal form");
        }finally{party.set(0,previous);}
        Pokemon first=mon(191),second=mon(6);
        BattleController b=new BattleController(new BattleParticipant[]{new WildPixelmonParticipant(first)},new BattleParticipant[]{new WildPixelmonParticipant(second)},new BattleRules());b.sendMessages=false;b.modifyStats();
        PixelmonWrapper user=b.participants.get(0).allPokemon[0],target=b.participants.get(1).allPokemon[0];
        try{
            b.globalStatusController.addGlobalStatus(user,new Sunny());
            ReferenceItems.Umbrella umbrella=(ReferenceItems.Umbrella)Backport.REGISTERED.get("utility_umbrella").get();
            Attack fire=new Attack("Flamethrower"),water=new Attack("Surf");
            require(new Sunny().modifyPowerAndAccuracyTarget(100,100,user,target,fire)[0]==150,"Sun control");
            target.setHeldItem(umbrella);
            require(new Sunny().modifyPowerAndAccuracyTarget(100,100,user,target,fire)[0]==100,"Umbrella incoming sun fire");
            require(new Rainy().modifyPowerAndAccuracyTarget(100,100,user,target,water)[0]==100,"Umbrella incoming rain water");
            target.removeHeldItem();user.setHeldItem(umbrella);
            require(new Sunny().modifyPowerAndAccuracyTarget(100,100,user,target,fire)[0]==150,"Outgoing boost incorrectly suppressed");
            int[] stats={100,100,100,100,100,100,100};new Chlorophyll().modifyStats(user,stats);require(stats[BattleStatsType.SPEED.getStatIndex()]==100,"Umbrella Chlorophyll");
            require(new WeatherBall().getOverrideType(user)==Element.NORMAL,"Umbrella Weather Ball");
            user.setHealth(1);int max=user.getMaxHealth();new Synthesis().applyEffectDuring(user,user);require(user.getHealth()==1+(int)(max*.5f),"Umbrella Synthesis");
            new Growth().applyEffectDuring(user,user);require(user.getBattleStats().getStage(BattleStatsType.ATTACK)==1,"Umbrella Growth");
            require(b.globalStatusController.getWeather() instanceof Sunny,"Shared weather mutated");
            weatherCases(user,target,umbrella);
            signatureHazards();
            user.pokemon.setAbility(new Klutz());require(new WeatherBall().getOverrideType(user)==Element.FIRE,"Klutz must disable umbrella");
            Pokemon ogerpon=mon(1017);ogerpon.setHeldItem(new ItemStack(Backport.REGISTERED.get("wellspring_mask").get()));
            BattleController masks=TeraVerification.battle(ogerpon);try{require(!masks.participants.get(0).allPokemon[0].isItemRemovable(user),"Mask can be stolen");}finally{Tera.end(masks);}
            System.out.println("BACKPORT_REPAIR_PASS grooming 6 forms + NBT/durability/ownership, meteorite equip/save/remove, umbrella weather/moves/ability/Klutz, mask removal");
        }finally{Tera.end(b);}
    }
    static int[] neutralStats(){return new int[]{100,100,100,100,100,100,100};}
    static void signatureHazards(){
        for(String name:new String[]{"Ceaseless Edge","Stone Axe"}){
            BattleController b=TeraVerification.battle(mon(name.equals("Stone Axe")?900:503));
            try{PixelmonWrapper u=b.participants.get(0).allPokemon[0],t=b.participants.get(1).allPokemon[0];u.pokemon.setAbility(new NoGuard());u.targets.clear();u.targets.add(t);u.targetIndex=0;
                StatusType type=name.equals("Stone Axe")?StatusType.StealthRock:StatusType.Spikes;int cap=name.equals("Stone Axe")?1:3;
                for(int i=0;i<cap+1;i++){
                    t.setHealth(t.getMaxHealth());u.attack=new Attack(name);MoveResults result=new MoveResults(t,0,0,AttackResult.proceed);
                    u.attack.use(u,t,result);require(t.getHealth()<t.getMaxHealth(),"Signature did no native damage "+name);
                    require(result.result==AttackResult.hit || result.result==AttackResult.killed,"Hazard cap must not make damaging move fail "+name);
                    require(t.getStatus(type) instanceof EntryHazard && ((EntryHazard)t.getStatus(type)).getNumLayers()==Math.min(cap,i+1),"Signature hazard layers "+name);
                    require(!t.hasStatus(StatusType.Splinters),"Modern signature still applies Splinters "+name);
                }
                int before=t.getMaxHealth();t.setHealth(before);((EntryHazard)t.getStatus(type)).applyEffectOnSwitch(t);require(t.getHealth()<before,"Hazard switching damage "+name);
                u.attack=new Attack(name.equals("Stone Axe")?"Stealth Rock":"Spikes");u.attack.moveResult=new MoveResults(t,0,0,AttackResult.proceed);
                ((EntryHazard)t.getStatus(type)).applyHazards(u,t);require(u.attack.moveResult.result==AttackResult.failed,"Ordinary hazard at cap must still fail");
                require(new Attack("Ancient "+name).getMove().effects.stream().anyMatch(e->e.getClass().getSimpleName().equals("Splinters")),"Ancient signature lost Splinters "+name);
            }finally{Tera.end(b);}
        }
    }
    static void weatherCases(PixelmonWrapper user,PixelmonWrapper target,ReferenceItems.Umbrella umbrella){
        user.pokemon.setAbility(new Chlorophyll());user.setHeldItem(umbrella);user.setHealth(user.getMaxHealth());
        int hp=user.getHealth();new SolarPower().applyRepeatedEffect(user);new DrySkin().applyRepeatedEffect(user);
        require(user.getHealth()==hp,"Umbrella solar/dry skin sun damage");
        require(new SolarPower().modifyStats(user,neutralStats())[BattleStatsType.SPECIAL_ATTACK.getStatIndex()]==100,"Umbrella solar power boost");
        require(new LeafGuard().allowsStatus(StatusType.Burn,user,target),"Umbrella leaf guard");
        require(Freeze.freeze(target,user),"Umbrella allows freezing in sun");user.removePrimaryStatus();
        user.attack=new Attack("Solar Beam");SolarBeam solar=new SolarBeam();
        require(solar.applyEffectDuring(user,target)==AttackResult.charging,"Umbrella Solar Beam must charge in sun");
        solar.removeEffect(user,target);
        user.attack=new Attack("Hydro Steam");new HydroSteam().applyEffectStart(user,target);
        require(user.attack.movePower==80 && !new HydroSteam().ignoresWeather(user),"Umbrella Hydro Steam must lose sun boost and immunity");
        require(new Sunny().modifyPowerAndAccuracyTarget(user.attack.movePower,100,user,target,user.attack)[0]==40,"Umbrella Hydro Steam outgoing sun penalty");
        user.bc.globalStatusController.addGlobalStatus(user,new Rainy());
        user.targets.clear();user.targets.add(target);user.targetIndex=0;user.attack=new Attack("Thunder");
        require(user.attack.cantMiss(user),"Holder Thunder must still always hit a non-holder in rain");
        target.setHeldItem(umbrella);require(!user.attack.cantMiss(user),"Target umbrella must cancel Thunder rain accuracy");
        target.removeHeldItem();
        require(new SwiftSwim().modifyStats(user,neutralStats())[BattleStatsType.SPEED.getStatIndex()]==100,"Umbrella swift swim");
        user.setHealth(1);new DrySkin().applyRepeatedEffect(user);new RainDish().applyRepeatedEffect(user);require(user.getHealth()==1,"Umbrella rain healing");
        user.addStatus(new Burn(),target);new Hydration().applyRepeatedEffect(user);require(user.hasStatus(StatusType.Burn),"Umbrella hydration");user.removePrimaryStatus();
        require(new WeatherBall().getOverrideType(user)==Element.NORMAL,"Umbrella rain weather ball");
        user.attack=new Attack("Solar Beam");solar=new SolarBeam();require(solar.applyEffectDuring(user,target)==AttackResult.charging,"Umbrella rain Solar Beam charging");
        require(solar.applyEffectDuring(user,target)==AttackResult.proceed && user.attack.movePower==120,"Umbrella rain Solar Beam power");
        user.attack=new Attack("Electro Shot");BackportEffects.ElectroShot electro=new BackportEffects.ElectroShot();
        require(electro.applyEffectDuring(user,target)==AttackResult.charging,"Umbrella Electro Shot rain charging");
        require(electro.applyEffectDuring(user,target)==AttackResult.proceed,"Umbrella Electro Shot second turn");
        user.removeHeldItem();user.attack=new Attack("Electro Shot");require(new BackportEffects.ElectroShot().applyEffectDuring(user,target)==AttackResult.proceed,"Electro Shot rain control");
        user.bc.globalStatusController.addGlobalStatus(user,new Sunny());
        // Attack.use initializes accuracy; this focused cantMiss test must do
        // the same instead of mistaking the constructor's zero for a failure.
        user.attack=new Attack("Hurricane");user.attack.moveAccuracy=user.attack.getMove().getAccuracy();user.attack.cantMiss(user);require(user.attack.moveAccuracy==50,"Holder Hurricane sun accuracy control");
        target.setHeldItem(umbrella);user.attack=new Attack("Hurricane");user.attack.moveAccuracy=user.attack.getMove().getAccuracy();user.attack.cantMiss(user);require(user.attack.moveAccuracy==70,"Target umbrella Hurricane sun accuracy");target.removeHeldItem();
        for(int dex:new int[]{351,421}){
            Pokemon p=mon(dex);p.setForm(dex==351?"base":"overcast");BattleController forms=TeraVerification.battle(p);
            try{PixelmonWrapper w=forms.participants.get(0).allPokemon[0];forms.globalStatusController.addGlobalStatus(w,new Sunny());
                w.getBattleAbility().onWeatherChange(w,forms.globalStatusController.getWeather());require(w.getForm().isForm(dex==351?"sun":"sunshine"),"Weather form control "+dex);
                w.setHeldItem(umbrella);require(w.getForm().isForm(dex==351?"base":"overcast"),"Weather form on umbrella equip "+dex);
                w.removeHeldItem();require(w.getForm().isForm(dex==351?"sun":"sunshine"),"Weather form on umbrella loss "+dex);
            }finally{Tera.end(forms);}
        }
        user.setHeldItem(umbrella);
    }
}
