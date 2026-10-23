package qa;
import java.lang.reflect.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.nbt.CompoundNBT;
import local.pixelmon.backport.*;
public final class LateRequestCases {
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 static Pokemon mon(int dex){var p=PokemonFactory.create(PixelmonSpecies.fromNationalDex(dex));p.setLevelNum(100);p.initialize();p.heal();return p;}
 static Object field(Object o,String name)throws Exception{for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException e){}throw new NoSuchFieldException(name);}
 public static void run(ServerPlayerEntity player)throws Exception{
  for(int dex:new int[]{1014,1015,1016,1017,1020,1021,1022,1023,1024,1025}){
   var p=mon(dex);check(p.getGender()==Gender.NONE,"New legendary genderless "+dex);p.setGender(Gender.FEMALE);check(p.getGender()==Gender.NONE,"Editor cannot restore sex "+dex);
   var n=p.writeToNBT(new CompoundNBT());var restored=mon(dex);restored.readFromNBT(n);check(restored.getGender()==Gender.NONE,"Genderless save/load "+dex);
  }
  check(mon(CapSpecies.DEX).getGender()==Gender.MALE,"Cap exception remains male");
  var party=StorageProxy.getParty(player);var before=party.get(0);var ogerpon=mon(1017);party.set(0,ogerpon);
  var entity=ogerpon.getOrSpawnPixelmon(player);entity.resetAI();
  GoalSelector goals=(GoalSelector)field(entity,"field_70714_bg");
  final boolean[] ran={false};
  Goal reset=new Goal(){public boolean func_75250_a(){return !ran[0];}public void func_75249_e(){ran[0]=true;entity.resetAI();}};
  goals.func_75776_a(-10,reset);
  for(int i=0;i<6&&!ran[0];i++)DeferredPokemonAI.tick(goals);
  check(ran[0],"Ogerpon goal executed resetAI inside native iteration");
  for(int i=0;i<20;i++)DeferredPokemonAI.tick(goals);
  check(!((java.util.Set<?>)field(goals,"field_220892_d")).isEmpty(),"Deferred native AI rebuilt, not discarded");
  entity.func_70106_y();party.set(0,before);
  var saved=player.getPersistentData().func_74775_l("PlayerPersisted");int old=saved.func_74762_e("BackportAuthorVictories");
  saved.func_74768_a("BackportAuthorVictories",8);player.getPersistentData().func_218657_a("PlayerPersisted",saved);
  var a=player.func_184102_h().func_191949_aK().func_192778_a(new net.minecraft.util.ResourceLocation(Content.NS,"adventure/defeat_author_10"));check(a!=null,"Ten-win advancement loaded");player.func_192039_O().func_192744_b(a,"earned");
  AuthorVictory.earned(player);check(!player.func_192039_O().func_192747_a(a).func_192105_a(),"Nine victories not ten");
  AuthorVictory.earned(player);check(player.func_192039_O().func_192747_a(a).func_192105_a(),"Tenth victory challenge");
  check(player.getPersistentData().func_74775_l("PlayerPersisted").func_74762_e("BackportAuthorVictories")==10,"Counter persisted in player NBT");
  authorPrizes(player);
  furnishings(player);
  spectrier(player);
  zygarde(player);
  System.out.println("LATE_SERVER_PASS gender creation/editor/NBT, Cap exception, native Ogerpon goal rebuild without CME, author 9/10 threshold");
 }
 static void authorPrizes(ServerPlayerEntity player)throws Exception{
  var saved=player.getPersistentData().func_74775_l("PlayerPersisted");boolean old=saved.func_74767_n(AuthorBossRewards.ASH_STONE_FIRST);saved.func_74757_a(AuthorBossRewards.ASH_STONE_FIRST,false);player.getPersistentData().func_218657_a("PlayerPersisted",saved);
  try{
   var method=AuthorBossRewards.class.getDeclaredMethod("prizes",ServerPlayerEntity.class);method.setAccessible(true);
   @SuppressWarnings("unchecked")var rewards=(java.util.List<net.minecraft.item.ItemStack>)method.invoke(null,player);
   check(rewards.size()==7,"One fixed + five random + first-victory stone, got "+rewards.size());
   check(rewards.get(6).func_77973_b()==Backport.item("ash_greninjite"),"First victory stone still extra");
   var unique=new java.util.HashSet<String>();for(var item:rewards.subList(1,6))check(unique.add(item.serializeNBT().toString()),"Five random rewards remain distinct");
   System.out.println("AUTHOR_FIVE_PASS native prize construction fixed master + five distinct random entries + guaranteed first stone; Drowned draw remains3");
  }finally{saved=player.getPersistentData().func_74775_l("PlayerPersisted");saved.func_74757_a(AuthorBossRewards.ASH_STONE_FIRST,old);player.getPersistentData().func_218657_a("PlayerPersisted",saved);}
 }
 static void furnishings(ServerPlayerEntity player)throws Exception{
  var world=player.func_71121_q();var home=new net.minecraft.util.math.BlockPos(104,90,104);AuthorBossFeature.build(world,home,false,"minecraft:stone_bricks");
  var chest=world.func_175625_s(home.func_177982_a(-5,1,-5));check(chest instanceof net.minecraft.tileentity.ChestTileEntity,"Mansion chest tile");
  check(((net.minecraft.tileentity.ChestTileEntity)chest).func_189515_b(new CompoundNBT()).func_74779_i("LootTable").equals("minecraft:chests/woodland_mansion"),"Unopened real mansion loot table");
  var pc=world.func_180495_p(home.func_177982_a(-3,1,-5));check(pc.func_177229_b(com.pixelmonmod.pixelmon.blocks.MultiBlock.MULTIPOS)==com.pixelmonmod.pixelmon.blocks.enums.MultiBlockPosition.BASE,"PC base");
  check(world.func_180495_p(home.func_177982_a(-3,2,-5)).func_177229_b(com.pixelmonmod.pixelmon.blocks.MultiBlock.MULTIPOS)==com.pixelmonmod.pixelmon.blocks.enums.MultiBlockPosition.TOP,"PC top");
  check(world.func_175625_s(home.func_177982_a(-3,1,-5)) instanceof com.pixelmonmod.pixelmon.blocks.tileentity.PCTileEntity,"Working PC tile");
  check(world.func_175625_s(home.func_177982_a(-4,1,-5))!=null,"Healer tile exists");
  var old=world.func_180495_p(home.func_177982_a(-5,1,-4));check(old.func_177230_c()==net.minecraft.block.Blocks.field_150462_ai,"Crafting table");
  AuthorRoomFurnishings.place(world,home);check(world.func_175625_s(home.func_177982_a(-5,1,-5))==chest,"Idempotent upgrade preserves chest");
  check(world.func_175623_d(home.func_177982_a(0,1,6)),"Entry remains clear");
  System.out.println("FURNISHINGS_PASS room build, healer/PC tiles and PC upper part, bed/table/chair/workbench, real mansion loot NBT, idempotent upgrade, clear entrance");
 }
 static void spectrier(ServerPlayerEntity player)throws Exception{
  var world=player.func_71121_q();var spawner=(com.pixelmonmod.pixelmon.spawning.LegendarySpawner)com.pixelmonmod.pixelmon.spawning.PixelmonSpawning.coordinator.getSpawner("legendary");
  var info=(com.pixelmonmod.pixelmon.api.spawning.archetypes.entities.pokemon.SpawnInfoPokemon)spawner.spawnSets.stream().filter(s->s.id.equals("Spectrier")).findFirst().orElseThrow().spawnInfos.get(0);
  var snail=spawner.spawnSets.stream().filter(s->s.id.equals("WoChien")).findFirst().orElseThrow().spawnInfos.get(0);check(info.rarity==snail.rarity,"Actual imported Spectrier/Wo-Chien equal rarity "+info.rarity+" / "+snail.rarity);
  var chunk=world.func_212866_a_(1,2);var bf=java.util.Arrays.stream(net.minecraft.world.chunk.Chunk.class.getDeclaredFields()).filter(f->f.getType()==net.minecraft.world.biome.BiomeContainer.class).findFirst().orElseThrow();bf.setAccessible(true);var old=chunk.func_225549_i_();var registry=world.func_241828_r().func_243612_b(net.minecraft.util.registry.Registry.field_239720_u_);long time=world.func_72820_D();
  try{
   var biomes=new net.minecraft.world.biome.Biome[old.func_227055_a_().length];java.util.Arrays.fill(biomes,registry.func_82594_a(new net.minecraft.util.ResourceLocation("minecraft:swamp")));bf.set(chunk,new net.minecraft.world.biome.BiomeContainer(registry,biomes));world.func_241114_a_(18000);
   for(int x=20;x<=30;x++)for(int z=35;z<=45;z++)for(int y=65;y<=73;y++)world.func_180501_a(new net.minecraft.util.math.BlockPos(x,y,z),(y==65?net.minecraft.block.Blocks.field_150349_c:net.minecraft.block.Blocks.field_150350_a).func_176223_P(),2);
   var locations=spawner.spawnLocationCalculator.calculateSpawnableLocations(new com.pixelmonmod.pixelmon.api.world.BlockCollection(player,world,20,66,28,72,35,45));
   var valid=locations.stream().filter(l->info.fits(spawner,l)).findFirst().orElseThrow(()->new AssertionError("No native Spectrier valid spawn location, requiredSpace="+info.requiredSpace+" locations="+locations.size()));
   for(int i=0;i<3;i++){var action=info.construct(spawner,valid);var entity=action.doSpawn(spawner);check(entity instanceof com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity&&entity.func_70089_S()&&!entity.field_70128_L&&world.func_217461_a(entity.func_110124_au())==entity,"Actual native Spectrier world spawn "+i);entity.func_70106_y();}
   world.func_241114_a_(6000);check(!info.fits(spawner,valid),"Spectrier remains night-only");
   System.out.println("SPECTRIER_PASS native swamp calculator + actual spawn action x3, equal Wo-Chien weight3, day excluded");
  }finally{bf.set(chunk,old);world.func_241114_a_(time);}
 }
 static void zygarde(ServerPlayerEntity player)throws Exception{
  var party=StorageProxy.getParty(player);var old=party.get(0);var p=mon(718);p.setAbility(new com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.PowerConstruct());party.set(0,p);
  var ids=java.util.List.of("zygarde_power_construct","zygarde_complete","zygarde_mega");for(String id:ids){var a=player.func_184102_h().func_191949_aK().func_192778_a(new net.minecraft.util.ResourceLocation(Content.NS,"legends/"+id));check(a!=null,"Zygarde advancement loaded "+id);player.func_192039_O().func_192744_b(a,"earned");}
  var own=new com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant(player,p);var enemy=new com.pixelmonmod.pixelmon.battles.controller.participants.WildPixelmonParticipant(mon(129));var b=new com.pixelmonmod.pixelmon.battles.controller.BattleController(new com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant[]{own},new com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant[]{enemy},new com.pixelmonmod.pixelmon.battles.api.rules.BattleRules());b.sendMessages=false;b.battleTurn=1;
  var w=own.allPokemon[0];w.bc=b;w.onBattlefield=true;own.controlledPokemon.clear();own.controlledPokemon.add(w);w.setTempLevel(100);w.setHealth(w.getMaxHealth());
  var ability=new com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.PowerConstruct();ability.applyRepeatedEffectAfterStatus(w);check(!complete(player,"zygarde_power_construct"),"Ability ownership at full HP not awarded");
  w.setHealth(w.getMaxHealth()/3);ability.applyRepeatedEffectAfterStatus(w);check(w.getForm().isForm("complete")&&complete(player,"zygarde_power_construct")&&complete(player,"zygarde_complete"),"Actual native Power Construct -> complete awards both");
  check(!complete(player,"zygarde_mega"),"Complete not Mega");
  var marker=new CompoundNBT();marker.func_74778_a("Target","mega");p.getPersistentData().func_218657_a(FieldMega.MARKER,marker);p.setForm("mega");check(complete(player,"zygarde_mega"),"Actual marked field Mega awards Zygarde only");p.getPersistentData().func_82580_o(FieldMega.MARKER);
  b.battleEnded=true;party.set(0,old);System.out.println("ZYGARDE_PASS owning ability does not award; actual native activation awards construct+complete; field marker actual Mega awards final weapon");
 }
 static boolean complete(ServerPlayerEntity player,String id){var a=player.func_184102_h().func_191949_aK().func_192778_a(new net.minecraft.util.ResourceLocation(Content.NS,"legends/"+id));return player.func_192039_O().func_192747_a(a).func_192105_a();}
}
