package local.pixelmon.backport;
import java.util.*;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.events.battles.AttackEvent;
import com.pixelmonmod.pixelmon.api.events.npc.NPCEvent;
import com.pixelmonmod.pixelmon.battles.attacks.specialAttacks.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.battles.status.ProtectVariation;
import net.minecraftforge.eventbus.api.*;
import net.minecraft.util.text.StringTextComponent;

/** Restrictions and handicaps belong only to the player's side of an author challenge. */
public final class AuthorChallenge {
 public static final String DENIED="你不满足对战的宝可梦要求：上场的宝可梦不能重复，智蛙和智皮卡只能二选一，阿尔宙斯不能携带传说石板";
 private static final Map<PixelmonWrapper,Boolean> BUFF=new WeakHashMap<>();
 public static boolean ash(Pokemon p){return p.getSpecies().getDex()==658&&(PermanentAsh.isAsh(p.getForm())||p.getForm().isForm("battle_bond")||p.getAbility() instanceof com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.BattleBond||p.getHeldItem().func_77973_b()==Backport.item("ash_greninjite"));}
 public static boolean valid(PixelmonWrapper[] party){
  Set<Integer> species=new HashSet<>();boolean ash=false,cap=false;
  for(PixelmonWrapper w:party){if(w==null||w.pokemon.isEgg())continue;Pokemon p=w.pokemon;int dex=p.getSpecies().getDex();
   if(!species.add(dex))return false;
   ash|=ash(p);cap|=dex==CapSpecies.DEX||dex==25&&p.getForm().isForm(CapPikachu.FORM);
   if(dex==493&&new net.minecraft.util.ResourceLocation("pixelmon:legend_plate").equals(p.getHeldItem().func_77973_b().getRegistryName()))return false;
  }return !(ash&&cap);
 }
 @SubscribeEvent(priority=EventPriority.HIGHEST) public void start(NPCEvent.StartBattle event){
  if(event.isCanceled()||event.getNPCParticipant()==null||!AuthorBossTeams.is(event.getNPCParticipant().trainer)||event.getPlayerParticipant()==null)return;
  if(!valid(event.getPlayerParticipant().allPokemon)){event.setCanceled(true);event.player.func_146105_b(new StringTextComponent(DENIED),false);}
  else event.player.func_146105_b(new net.minecraft.util.text.TranslationTextComponent("pixelmon.backport.author.challenge.handicap"),false);
 }
 public static boolean affected(PixelmonWrapper p){return p!=null&&p.getParticipant() instanceof PlayerParticipant&&GymChallenge.author(p.bc)&&p.attack!=null&&!p.usingZ;}
 public static boolean protect(com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack attack){return attack.effects.stream().anyMatch(e->e instanceof ProtectVariation);}
 public static boolean selfBuff(com.pixelmonmod.pixelmon.battles.attacks.ImmutableAttack attack){return attack.effects.stream().anyMatch(e->e instanceof BackportEffects.NinjaTraining||e instanceof StatsEffect&&((StatsEffect)e).amount>0||e instanceof StatsEffectPost&&((StatsEffectPost)e).amount>0);}
 @SubscribeEvent(priority=EventPriority.LOWEST) public void accuracy(AttackEvent.Use event){
  if(!affected(event.user)||protect(event.attack)||event.user==event.target&&selfBuff(event.attack))return;
  event.accuracy=Math.max(0,(event.cantMiss?100:event.accuracy)-20);event.cantMiss=false;
 }
 public static void begin(PixelmonWrapper p){BUFF.remove(p);}
 public static boolean buff(PixelmonWrapper p){return !affected(p)||p.bc.simulateMode||BUFF.computeIfAbsent(p,k->com.pixelmonmod.pixelmon.api.util.helpers.RandomHelper.getRandomChance(.8f));}
}
