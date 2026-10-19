package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.battles.BattleEndCause;
import com.pixelmonmod.pixelmon.api.battles.AttackCategory;
import com.pixelmonmod.pixelmon.battles.attacks.*;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.basic.OHKO;
import com.pixelmonmod.pixelmon.battles.controller.BattleController;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.raids.*;
import com.pixelmonmod.pixelmon.battles.status.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import net.minecraft.entity.LivingEntity;

/** Native four-member lobby/capture, independent Tera combat (Java 11). */
public final class TeraRaidParticipant extends RaidPixelmonParticipant {
    public final int stars,teraType;
    public final String originalForm,palette;
    public final TeraRaidRules rules=new TeraRaidRules();
    private final Map<BattleParticipant,Integer> charges=new IdentityHashMap<>(),faints=new IdentityHashMap<>();
    private final Map<BattleParticipant,Long> revives=new IdentityHashMap<>();
    private final Map<BattleParticipant,PixelmonWrapper> fallen=new IdentityHashMap<>();
    private long lastTick=-1;
    private boolean started,finished;
    public TeraRaidParticipant(RaidData raid,TeraDenEntity den,Pokemon pokemon){
        super(raid,TeraRaids.settings(raid,pokemon),pokemon);stars=raid.getStars();teraType=den.teraType();originalForm=pokemon.getForm().getName();palette=den.palette();prepare(raid);
    }
    public TeraRaidParticipant(RaidData raid,TeraDenEntity den,PixelmonEntity entity){
        super(raid,TeraRaids.settings(raid,entity.getPokemon()),entity);stars=raid.getStars();teraType=den.teraType();originalForm=entity.getPokemon().getForm().getName();palette=den.palette();prepare(raid);
    }
    private void prepare(RaidData raid){getWrapper().pokemon.getPersistentData().func_74768_a("TeraType",teraType+1);getWrapper().pokemon.setPalette(palette);TeraRaids.RAIDS.put(raid,this);}
    @Override public void startBattle(){super.startBattle();started=true;PixelmonWrapper p=getWrapper();p.isDynamax=0;dynamax=null;Tera.select(p);if(!Tera.activate(p))throw new IllegalStateException("Tera boss transformation failed");p.setHealth(p.getMaxHealth());bc.sendHealPacket(p,p.getMaxHealth());lastTick=gameTime();announce("start");}
    public int charge(BattleParticipant p){return charges.getOrDefault(p,0);}
    public boolean pendingRevives(){return !finished && !revives.isEmpty();}
    private long gameTime(){LivingEntity entity=getEntity();return entity==null?lastTick:entity.field_70170_p.func_82737_E();}
    private void announce(String key){if(!bc.simulateMode)bc.sendToAll("pixelmon.backport.tera.raid."+key);}
    public void tick(){
        if(!started || finished || bc==null || bc.battleEnded || bc.simulateMode)return;
        long now=gameTime(),delta=Math.max(0,now-lastTick);lastTick=now;if(delta==0)return;
        rules.elapsed+=delta;
        if(rules.elapsed>=6000){finished=true;getGovernor().done=true;announce("timeout");bc.endBattle();return;}
        if(rules.shouldRaise(getWrapper().getHealthPercent()))raiseShield();
        for(Iterator<Map.Entry<BattleParticipant,Long>> it=revives.entrySet().iterator();it.hasNext();){Map.Entry<BattleParticipant,Long> entry=it.next();if(now<entry.getValue())continue;BattleParticipant p=entry.getKey();if(TeraRevival.revive(bc,p,fallen.get(p))){it.remove();fallen.remove(p);announce("revive");}else entry.setValue(now+20);}
        if(!rules.stolen && rules.elapsed>=1800){rules.stolen=true;charges.replaceAll((p,v)->Math.max(0,v-1));announce("stolen");}
        int second=(int)(rules.elapsed/20);if(second!=rules.lastSecond){rules.lastSecond=second;TeraNetwork.raidStatus(this);}
    }
    private void raiseShield(){rules.raise();getWrapper().removePrimaryStatus(false);getWrapper().setRaidShields(8);announce("shield");}
    @Override public boolean canDynamax(){return false;}
    @Override public boolean canDynamax(BattleParticipant p){return false;}
    @Override public boolean areShieldsUp(){return rules.shield>0;}
    @Override public void onSwitchIn(BattleController bc,PixelmonWrapper p){Tera.enforceType(p);}
    @Override public void onEndTurn(BattleController bc){tick();}
    @Override public boolean onTakeTurn(BattleController bc,PixelmonWrapper p){return false;}
    @Override public boolean onUseAttack(BattleController bc,PixelmonWrapper p){return finished || bc.getOpponentPokemon(this).stream().noneMatch(PixelmonWrapper::isAlive);}
    @Override public void onUseAttackPost(BattleController bc,PixelmonWrapper p){}
    @Override public boolean onUseAttackOther(BattleController bc,Attack attack,BattleParticipant participant,PixelmonWrapper user){
        if(participant!=this && user.isAlive())charges.put(participant,Math.min(3,charge(participant)+1));
        String name=attack.getActualMove().getAttackName();
        boolean blocked=attack.getActualMove().hasEffect(OHKO.class) || Arrays.asList("Destiny Bond","Explosion","Self-Destruct","Final Gambit","Misty Explosion","Perish Song").contains(name);
        if(blocked)bc.sendToAll("pixelmon.effect.effectfailed");return blocked;
    }
    @Override public boolean onTargeted(PixelmonWrapper user,Attack attack){return areShieldsUp() && attack.getActualMove().getAttackCategory()==AttackCategory.STATUS;}
    @Override public boolean onAddStatus(BattleController bc,PixelmonWrapper user,PixelmonWrapper target,StatusBase status){return areShieldsUp() && status.type.isPrimaryStatus();}
    @Override public float onHit(PixelmonWrapper source,float damage,DamageTypeEnum kind){
        if(finished || bc.simulateMode || damage<=0)return damage;
        if(!rules.raised && getWrapper().getHealth()-damage>0 && getWrapper().getHealthPercent(getWrapper().getHealth()-damage)<=50)raiseShield();
        if(!areShieldsUp() || kind!=DamageTypeEnum.ATTACK)return damage;
        boolean active=source!=null && Tera.active(source),matching=active && source.attack!=null && source.attack.getType()==Tera.state(source).type;
        float scaled=rules.damage(damage,active,matching);rules.hit(scaled);
        getWrapper().updateRaidShields(rules.segments());if(!areShieldsUp())announce("broken");return scaled;
    }
    @Override public void onOpponentKO(BattleController bc,PixelmonWrapper p){
        BattleParticipant owner=p.getParticipant();if(owner.team==team || revives.containsKey(owner))return;
        fallen.put(owner,p);int count=faints.getOrDefault(owner,0);faints.put(owner,count+1);revives.put(owner,gameTime()+Math.min(30,5+count*5)*20L);
        if(owner instanceof PlayerParticipant)rules.elapsed+=600;
        TeraNetwork.raidStatus(this);
    }
    @Override public void endBattle(BattleEndCause cause){finished=true;super.endBattle(cause);}
}
