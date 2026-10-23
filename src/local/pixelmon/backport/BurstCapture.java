package local.pixelmon.backport;

import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.*;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import net.minecraft.entity.player.ServerPlayerEntity;

public final class BurstCapture {
    public BurstCapture(){}
    private static final java.util.Map<com.pixelmonmod.pixelmon.battles.controller.BattleController,java.util.Set<java.util.UUID>> WARNED=new java.util.WeakHashMap<>();
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public void battle(com.pixelmonmod.pixelmon.api.events.battles.BattleStartedEvent.Post event){
        var battle=event.getBattleController();
        if(battle==null || battle.simulateMode || battle.battleEnded)return;
        for(var participant:battle.participants){
            if(!(participant instanceof com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant))continue;
            var player=((com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant)participant).player;
            if(player==null)continue;
            Pokemon cap=null;
            for(var opponent:battle.participants)if(opponent.team!=participant.team && opponent instanceof com.pixelmonmod.pixelmon.battles.controller.participants.WildPixelmonParticipant)
                for(var target:opponent.allPokemon)if(CapBallPreference.cap(target.pokemon))cap=target.pokemon;
            if(cap!=null && WARNED.computeIfAbsent(battle,key->new java.util.HashSet<>()).add(player.func_110124_au()))CapBallPreference.announce(player,cap);
        }
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public void capture(com.pixelmonmod.pixelmon.api.events.CaptureEvent.StartCapture event){
        if(event.getPokemon()!=null&&CapBallPreference.cap(event.getPokemon().getPokemon())
            &&(event.getPokeBall()==null||event.getPokeBall().getMode()!=com.pixelmonmod.pixelmon.entities.pokeballs.PokeBallMode.BATTLE||forbidden(event.getPokeBall().getBallType(),event.getPokemon().getPokemon())))event.setCanceled(true);
    }
    public static boolean forbidden(PokeBall ball,Pokemon pokemon){
        return CapBallPreference.forbidden(ball,pokemon);
    }
    public static boolean active(ServerPlayerEntity player,Pokemon target){
        if(player==null || target==null || target.isEgg() || target.isUncatchable())return false;
        var bc=BattleRegistry.getBattle(player);var field=BurstTerrain.current(bc);
        if(field==null)return false;var team=bc.getParticipantForEntity(player);
        if(!(team instanceof com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant) || team.team!=field.team)return false;
        for(var opponent:bc.participants)if(opponent.team!=field.team && opponent instanceof com.pixelmonmod.pixelmon.battles.controller.participants.WildPixelmonParticipant)
            for(var wrapper:opponent.allPokemon)if(wrapper.getPokemonUUID().equals(target.getUUID())
                && OriginMercy.canCapture((com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant)team,wrapper))return true;
        return false;
    }
    public static double bonus(ServerPlayerEntity player,Pokemon target,PokeBall ball,double nativeBonus){
        if(CapBallPreference.cap(target))return CapBallPreference.multiplier(ball,target,nativeBonus);
        return BurstPolicy.catchBonus(nativeBonus,active(player,target),forbidden(ball,target));
    }
    public static java.util.List<String> outlinedBalls(com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant participant){
        java.util.Set<String> names=new java.util.LinkedHashSet<>();
        for(var opponent:participant.getOpponents())if(opponent instanceof com.pixelmonmod.pixelmon.battles.controller.participants.WildPixelmonParticipant)
            for(var wrapper:opponent.allPokemon){
                Pokemon target=wrapper.pokemon;
                if(!wrapper.onBattlefield || !wrapper.isAlive() || !active(participant.player,target))continue;
                var entity=target.getPixelmonEntity();
                if(!entity.isPresent() || entity.get().getOwner()!=null || entity.get().getBossTier().isBoss())continue;
                names.addAll(outlinedBalls(participant.player,target,com.pixelmonmod.pixelmon.entities.pokeballs.PokeBallMode.BATTLE));
            }
        return new java.util.ArrayList<>(names);
    }
    public static java.util.List<String> outlinedBalls(ServerPlayerEntity player,Pokemon target,com.pixelmonmod.pixelmon.entities.pokeballs.PokeBallMode mode){
        java.util.List<String> names=new java.util.ArrayList<>();
        if(target==null || target.isEgg() || target.isUncatchable())return names;
        for(PokeBall ball:PokeBallRegistry.getAll()){
            // Native raid calculations also accept a null projectile. Read-only:
            // no capture events, RNG, inventory changes, or turn consumption.
            var logic=ball.getLogic();double nativeBonus=CapBallPreference.preferred(ball,target)||forbidden(ball,target)?0:logic.getCatchBonus(ball,null,player,target,mode);double bonus=bonus(player,target,ball,nativeBonus);
            int rate=CapBallPreference.preferred(ball,target)?target.getForm().getCatchRate():logic.modifyCaptureRate(ball,null,target,target.getForm().getCatchRate());
            if(BurstDisplayPolicy.ball(bonus,rate,forbidden(ball,target),(!CapBallPreference.cap(target)&&ball.isGuaranteedCatch())))names.add(ball.getName());
        }
        return names;
    }
}
