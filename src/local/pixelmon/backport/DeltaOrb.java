package local.pixelmon.backport;
import com.pixelmonmod.pixelmon.api.registries.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.blocks.OrbBlock;
import com.pixelmonmod.pixelmon.blocks.tileentity.OrbTileEntity;
import net.minecraft.item.*;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
/** Hidden transformation: no evolution data or drop catalogue entries are added. */
public final class DeltaOrb {
 public static boolean shard(ItemStack s){return s.func_77973_b()==PixelmonItems.delta_shard;}
 public static ActionResultType place(ItemUseContext c){
  var w=c.func_195991_k();var p=c.func_195999_j();var s=c.func_195996_i();if(p==null)return ActionResultType.FAIL;
  BlockPos pos=c.func_195995_a();boolean existing=w.func_180495_p(pos).func_177230_c()==PixelmonBlocks.jade_orb;
  if(!existing){if(c.func_196000_l()!=Direction.UP)return ActionResultType.FAIL;pos=pos.func_177984_a();if(!w.func_180495_p(pos).func_185904_a().func_76222_j())return ActionResultType.FAIL;}
  if(!w.func_175660_a(p,pos)||!p.func_175151_a(pos,c.func_196000_l(),s))return ActionResultType.FAIL;
  if(w.field_72995_K)return ActionResultType.SUCCESS;
  if(existing){var te=w.func_175625_s(pos);if(!(te instanceof OrbTileEntity)||((OrbTileEntity)te).getPieces()>=10)return ActionResultType.FAIL;var tile=(OrbTileEntity)te;tile.setPieces(tile.getPieces()>=8?10:tile.getPieces()+1);}
  else if(!w.func_180501_a(pos,PixelmonBlocks.jade_orb.func_176223_P().func_206870_a(OrbBlock.FACING,p.func_184172_bi()),3))return ActionResultType.FAIL;
  if(!p.field_71075_bZ.field_75098_d)s.func_190918_g(1);placementSound(w,pos);return ActionResultType.SUCCESS;
 }
 @SubscribeEvent(priority=EventPriority.HIGH) public void use(PlayerInteractEvent.EntityInteract e){
  if(e.isCanceled()||e.getItemStack().func_77973_b()!=PixelmonItems.jade_orb||!(e.getTarget() instanceof PixelmonEntity))return;
  var entity=(PixelmonEntity)e.getTarget();var pokemon=entity.getPokemon();var player=e.getPlayer();
  if(pokemon.isEgg()||pokemon.getSpecies().getDex()!=10||!player.func_110124_au().equals(pokemon.getOwnerPlayerUUID())||entity.battleController!=null||com.pixelmonmod.pixelmon.battles.BattleRegistry.getBattle(player)!=null)return;
  e.setCanceled(true);e.setCancellationResult(ActionResultType.SUCCESS);if(!(player instanceof ServerPlayerEntity))return;
  if(entity.func_110143_aJ()<1||querying(pokemon))return;
  // An unregistered, private route: native choice/animation/events without exposing the secret in guides.
  var evolution=new HiddenEvolution();
  evolution.doEvolution(entity,"backport_delta_orb");
 }
 private static boolean querying(com.pixelmonmod.pixelmon.api.pokemon.Pokemon pokemon){
  synchronized(com.pixelmonmod.pixelmon.entities.pixelmon.helpers.EvolutionQueryList.queryList){return com.pixelmonmod.pixelmon.entities.pixelmon.helpers.EvolutionQueryList.queryList.stream().anyMatch(q->q.pokemonUUID.equals(pokemon.getUUID())&&!q.isEnded());}
 }
 public static final class HiddenEvolution extends com.pixelmonmod.pixelmon.api.pokemon.stats.evolution.Evolution {
  private boolean finished;
  HiddenEvolution(){super("interact",com.pixelmonmod.api.pokemon.PokemonSpecificationProxy.create("Rayquaza","ability:AirLock"));}
  @Override public void finishedEvolving(com.pixelmonmod.pixelmon.api.pokemon.Pokemon pokemon){
   if(finished||pokemon.getSpecies().getDex()!=384)return;finished=true;
   var player=pokemon.getOwnerPlayer();if(player!=null&&!player.field_71075_bZ.field_75098_d){for(int i=0;i<player.field_71071_by.func_70302_i_();i++){var stack=player.field_71071_by.func_70301_a(i);if(stack.func_77973_b()==PixelmonItems.jade_orb){stack.func_190918_g(1);player.field_71071_by.func_70296_d();break;}}}
   pokemon.rerollMoveset();
   // loadMoveset has no ability: set it AFTER replacing the moveset, never before.
   pokemon.setAbility(new com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.AirLock());
   pokemon.getPersistentData().func_74757_a("BackportDeltaEvolution",true);
   if(pokemon.getOwnerPlayer()!=null)LegendAdvancements.award(pokemon.getOwnerPlayer(),"dragon_gate");
  }
 }
 public static boolean ready(com.pixelmonmod.pixelmon.api.pokemon.stats.evolution.Evolution evolution,com.pixelmonmod.pixelmon.api.pokemon.Pokemon pokemon){
  if(!(evolution instanceof HiddenEvolution))return true;var player=pokemon.getOwnerPlayer();if(player==null)return false;if(player.field_71075_bZ.field_75098_d)return true;
  for(int i=0;i<player.field_71071_by.func_70302_i_();i++)if(player.field_71071_by.func_70301_a(i).func_77973_b()==PixelmonItems.jade_orb)return true;return false;
 }
 public static void placementSound(net.minecraft.world.World world,BlockPos pos){if(!world.field_72995_K)world.func_184133_a(null,pos,net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(new net.minecraft.util.ResourceLocation("minecraft:block.glass.place")),net.minecraft.util.SoundCategory.BLOCKS,1,.8f);}
 public static ActionResultType placeOrb(ItemUseContext c){
  var w=c.func_195991_k();var p=c.func_195999_j();if(p==null||c.func_196000_l()!=Direction.UP)return ActionResultType.FAIL;
  BlockPos pos=c.func_195995_a().func_177984_a();
  if(!w.func_180495_p(pos).func_185904_a().func_76222_j()||!w.func_175660_a(p,pos)||!p.func_175151_a(pos,Direction.UP,c.func_195996_i()))return ActionResultType.FAIL;
  if(w.field_72995_K)return ActionResultType.SUCCESS;
  if(!w.func_180501_a(pos,PixelmonBlocks.jade_orb.func_176223_P().func_206870_a(OrbBlock.FACING,p.func_184172_bi()),3))return ActionResultType.FAIL;
  var tile=w.func_175625_s(pos);if(!(tile instanceof OrbTileEntity))return ActionResultType.FAIL;((OrbTileEntity)tile).setPieces(10);
  if(!p.field_71075_bZ.field_75098_d)c.func_195996_i().func_190918_g(1);placementSound(w,pos);return ActionResultType.SUCCESS;
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public void drops(com.pixelmonmod.pixelmon.api.events.DropEvent e){
  if(e.isCanceled()||e.player==null||!(e.entity instanceof PixelmonEntity))return;var p=(PixelmonEntity)e.entity;
  if(!p.field_70170_p.field_72995_K&&!p.hasOwner()&&p.getTrainer()==null&&p.getPokemon().getSpecies().getDex()==384&&p.func_70681_au().nextBoolean())e.addDrop(new ItemStack(PixelmonItems.delta_shard));
 }
}
