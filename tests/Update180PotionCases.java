package qa;
import java.util.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.species.Stats;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.api.pokemon.boss.*;
import com.pixelmonmod.pixelmon.entities.npcs.registry.DropItemRegistry;
import com.pixelmonmod.pixelmon.entities.pixelmon.helpers.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.interactions.InteractionRareCandy;
import net.minecraft.entity.player.*;
import net.minecraft.inventory.*;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraft.nbt.*;
import net.minecraft.util.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
public final class Update180PotionCases {
 public static final class CancelEvolution {
  boolean candy;
  @SubscribeEvent public void before(com.pixelmonmod.pixelmon.api.events.EvolveEvent.Pre event){if(!candy)event.setCanceled(true);}
  @SubscribeEvent public void candy(com.pixelmonmod.pixelmon.api.events.RareCandyEvent event){if(candy)event.setCanceled(true);}
 }
 static UUID clientId;static int clientDex;static String clientForm;
 static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
 static boolean potion(ItemStack stack){return stack.func_77973_b()==Backport.item("mysterious_potion");}
 static Pokemon mon(int dex){return Update180Cases.mon(dex,"Tackle","Growl","Protect","Rest");}
 static void preserved(CompoundNBT before,Pokemon pokemon,String label)throws Exception{
  var after=pokemon.writeToNBT(new CompoundNBT());var a=before.func_74737_b();var b=after.func_74737_b();
  for(String key:List.of("ndex","form","Variant","StatsHP","StatsAttack","StatsDefense","StatsSpecialAttack","StatsSpecialDefense","StatsSpeed","Health","Ability","PAbilitySlot","PHiddenAbility")){a.func_82580_o(key);b.func_82580_o(key);}
  a.func_74775_l("PersistentData").func_82580_o(MysteriousPotion.HISTORY);b.func_74775_l("PersistentData").func_82580_o(MysteriousPotion.HISTORY);
  var dir=java.nio.file.Path.of("qa-potion-data");java.nio.file.Files.createDirectories(dir);java.nio.file.Files.writeString(dir.resolve(label+"-before.snbt"),before.toString());java.nio.file.Files.writeString(dir.resolve(label+"-after.snbt"),after.toString());
  var keys=new TreeSet<String>();keys.addAll(a.func_150296_c());keys.addAll(b.func_150296_c());for(String key:keys)check(Objects.equals(a.func_74781_a(key),b.func_74781_a(key)),"Native saved field retained "+label+"/"+key+" before="+a.func_74781_a(key)+" after="+b.func_74781_a(key));
 }
 static void finish(Pokemon pokemon)throws Exception{
  var query=EvolutionQueryList.queryList.stream().filter(q->q.pokemonUUID.equals(pokemon.getUUID())&&!q.isEnded()).findFirst().orElseThrow();
  var accept=EvolutionQuery.class.getDeclaredMethod("accept");accept.setAccessible(true);accept.invoke(query);
  var complete=EvolutionQuery.class.getDeclaredMethod("doEvoSwitch");complete.setAccessible(true);complete.invoke(query);
  query.abort();EvolutionQueryList.queryList.remove(query);
 }
 public static void server(ServerPlayerEntity player)throws Exception{
  player.func_71033_a(net.minecraft.world.GameType.SURVIVAL);
  recipes(player);loot(player);var party=StorageProxy.getParty(player);var item=(MysteriousPotion)Backport.item("mysterious_potion");
  int transitions=0;
  for(int[] pair:new int[][]{{6,5},{5,4},{26,25},{25,172},{134,133},{94,93},{242,113},{892,891},{902,550},{975,974},{1019,1011}}){
   var p=mon(pair[0]);party.set(0,p);var entity=p.getOrSpawnPixelmon(player);p.setNickname("PotionKeep");p.setShiny(true);p.setHeldItem(Update180R11Cases.item("minecraft:diamond"));p.getPersistentData().func_74778_a("Unrelated","retained");
   p.setHealth(Math.max(1,p.getMaxHealth()/3));var before=p.writeToNBT(new CompoundNBT());var id=p.getUUID();var iv=p.getIVs().getArray().clone();var ev=p.getEVs().getArray().clone();var moves=p.writeToNBT(new CompoundNBT()).func_150295_c("Moveset",10).func_74737_b();var ball=p.getBall();var nature=p.getNature();int level=p.getPokemonLevel(),exp=p.getExperience();String form=p.getForm().getName();
   var stack=new ItemStack(item,2);check(item.func_111207_a(stack,player,entity,Hand.MAIN_HAND)==ActionResultType.SUCCESS&&p.getSpecies().getDex()==pair[1]&&stack.func_190916_E()==1,"Exactly one previous stage and one potion "+pair[0]);
   check(id.equals(p.getUUID())&&Arrays.equals(iv,p.getIVs().getArray())&&Arrays.equals(ev,p.getEVs().getArray())&&moves.equals(p.writeToNBT(new CompoundNBT()).func_150295_c("Moveset",10))&&p.isShiny()&&p.getBall()==ball&&p.getNature()==nature&&level==p.getPokemonLevel()&&exp==p.getExperience()&&p.getHeldItem().func_77973_b()==Items.field_151045_i&&p.getNickname().equals("PotionKeep"),"Identity, training, moves, level/XP, held item, ball, shiny retained "+pair[0]);
   preserved(before,p,"devolve-"+pair[0]);
   var saved=PokemonFactory.create(p.writeToNBT(new CompoundNBT()));check(saved.getPersistentData().func_150295_c(MysteriousPotion.HISTORY,10).size()==1&&saved.getPersistentData().func_74779_i("Unrelated").equals("retained"),"Native NBT history and unrelated data persisted");
   var candy=Update180R11Cases.item("pixelmon:rare_candy");candy.func_190920_e(2);player.func_184611_a(Hand.MAIN_HAND,candy);
   check(new InteractionRareCandy().processInteract(entity,player,Hand.MAIN_HAND,candy)&&candy.func_190916_E()==1,"Native candy handler consumes once");finish(p);
   check(p.getSpecies().getDex()==pair[0]&&p.getForm().getName().equals(form)&&!p.getPersistentData().func_74764_b(MysteriousPotion.HISTORY),"Native animation completion restores exact original branch "+pair[0]);entity.func_70106_y();transitions++;
  }
  for(String[] test:new String[][]{{"26","alolan","25"},{"103","alolan","102"},{"863","","52"},{"902","female","550"}}){
   var p=mon(Integer.parseInt(test[0]));if(!test[1].isEmpty())check(p.setForm(test[1]),"Native regional form exists");var target=MysteriousPotion.target(p);check(target!=null&&target.getParentSpecies().getDex()==Integer.parseInt(test[2]),"Regional/sex predecessor "+Arrays.toString(test));if(test[0].equals("863"))check(target.getName().equals("galarian"),"Perrserker returns Galarian Meowth");
   party.set(0,p);var entity=p.getOrSpawnPixelmon(player);String originalForm=p.getForm().getName();var before=p.writeToNBT(new CompoundNBT());var stack=new ItemStack(item,2);player.func_184611_a(Hand.MAIN_HAND,stack);
   check(player.func_190775_a(entity,Hand.MAIN_HAND)==ActionResultType.SUCCESS&&stack.func_190916_E()==1&&p.getSpecies().getDex()==Integer.parseInt(test[2]),"Actual player interaction regional potion "+Arrays.toString(test));preserved(before,p,"regional-"+test[0]+"-"+test[1]);
   var candy=Update180R11Cases.item("pixelmon:rare_candy");player.func_184611_a(Hand.MAIN_HAND,candy);check(new InteractionRareCandy().processInteract(entity,player,Hand.MAIN_HAND,candy)&&candy.func_190926_b(),"Regional candy consumed");finish(p);check(p.getSpecies().getDex()==Integer.parseInt(test[0])&&p.getForm().getName().equals(originalForm),"Exact regional/sex branch restored "+Arrays.toString(test));entity.func_70106_y();
  }
  var p=mon(6);p.setLevel(100);party.set(0,p);var entity=p.getOrSpawnPixelmon(player);var potion=new ItemStack(item,3);
  check(item.func_111207_a(potion,player,entity,Hand.MAIN_HAND)==ActionResultType.SUCCESS&&p.getSpecies().getDex()==5,"Full-level first devolution");System.out.println("POTION_CHAIN_BEFORE species="+p.getSpecies().getDex()+" form="+p.getForm().getName()+" target="+(MysteriousPotion.target(p)==null?"null":MysteriousPotion.target(p).getParentSpecies().getDex())+" entityMon="+entity.getPokemon().getSpecies().getDex()+" busy="+FieldMega.busy(entity)+" active="+FieldMega.active(p)+" count="+potion.func_190916_E());var second=item.func_111207_a(potion,player,entity,Hand.MAIN_HAND);check(second==ActionResultType.SUCCESS&&p.getSpecies().getDex()==4,"Repeated devolution exactly one step result="+second+" species="+p.getSpecies().getDex()+" entity="+entity.getPokemon().getSpecies().getDex());check(item.func_111207_a(potion,player,entity,Hand.MAIN_HAND)==ActionResultType.FAIL&&potion.func_190916_E()==1,"Base stage unchanged and not consumed");
  for(int expected:new int[]{5,6}){var candy=Update180R11Cases.item("pixelmon:xs_exp_candy");player.func_184611_a(Hand.MAIN_HAND,candy);check(new InteractionRareCandy().processInteract(entity,player,Hand.MAIN_HAND,candy)&&candy.func_190926_b(),"Full-level XP candy consumed once");finish(p);check(p.getSpecies().getDex()==expected&&p.getPokemonLevel()==100,"Full-level history stack restores one step "+expected);}entity.func_70106_y();
  for(int dex:new int[]{4,133,150,132,CapSpecies.DEX}){p=mon(dex);party.set(0,p);entity=p.getOrSpawnPixelmon(player);potion=new ItemStack(item,2);check(item.func_111207_a(potion,player,entity,Hand.MAIN_HAND)==ActionResultType.FAIL&&potion.func_190916_E()==2&&p.getSpecies().getDex()==dex,"No predecessor no consumption "+dex);entity.func_70106_y();}
  p=mon(6);party.set(0,p);entity=p.getOrSpawnPixelmon(player);potion=new ItemStack(item,2);p.setEggCycles(10);check(item.func_111207_a(potion,player,entity,Hand.MAIN_HAND)==ActionResultType.FAIL&&potion.func_190916_E()==2,"Egg no consumption");p.setEggCycles(-1);
  var stranger=net.minecraftforge.common.util.FakePlayerFactory.get(player.func_71121_q(),new com.mojang.authlib.GameProfile(UUID.fromString("013e6334-6666-4666-8666-000000001913"),"PotionStranger"));check(item.func_111207_a(potion,stranger,entity,Hand.MAIN_HAND)==ActionResultType.FAIL&&potion.func_190916_E()==2,"Foreign Pokemon no consumption");
  check(p.setForm("megax")&&MysteriousPotion.target(p)==null,"Temporary Mega is not evolutionary stage");p.setForm(p.getSpecies().getDefaultForm());
  var battle=new com.pixelmonmod.pixelmon.battles.controller.BattleController(new com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant[]{new com.pixelmonmod.pixelmon.battles.controller.participants.WildPixelmonParticipant(mon(129))},new com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant[]{new com.pixelmonmod.pixelmon.battles.controller.participants.WildPixelmonParticipant(mon(129))},new com.pixelmonmod.pixelmon.battles.api.rules.BattleRules());
  com.pixelmonmod.pixelmon.battles.BattleRegistry.registerBattle(battle);entity.battleController=battle;check(item.func_111207_a(potion,player,entity,Hand.MAIN_HAND)==ActionResultType.FAIL&&potion.func_190916_E()==2,"Battle blocked");battle.battleEnded=true;com.pixelmonmod.pixelmon.battles.BattleRegistry.deRegisterBattle(battle);entity.battleController=null;
  check(item.func_111207_a(potion,player,entity,Hand.MAIN_HAND)==ActionResultType.SUCCESS,"Final devolution for client sync");
  var cancel=new CancelEvolution();Pixelmon.EVENT_BUS.register(cancel);try{for(boolean cancelCandy:new boolean[]{false,true}){cancel.candy=cancelCandy;var candy=Update180R11Cases.item("pixelmon:rare_candy");player.func_184611_a(Hand.MAIN_HAND,candy);check(new InteractionRareCandy().processInteract(entity,player,Hand.MAIN_HAND,candy)&&candy.func_190916_E()==1&&p.getSpecies().getDex()==5&&p.getPersistentData().func_150295_c(MysteriousPotion.HISTORY,10).size()==1,"Canceled native pre/candy event retains item and history "+cancelCandy);}}finally{Pixelmon.EVENT_BUS.unregister(cancel);}
  clientId=p.getUUID();clientDex=p.getSpecies().getDex();clientForm=p.getForm().getName();MysteriousPotion.sync(player);entity.func_70106_y();
  Update180LootNameCases.server(player);System.out.println("R13_POTION_SERVER_PASS "+transitions+" native devolve/candy evolution pairs, level100 multi-step XP candy, regional/sex forms, exact consumption, NBT preservation, egg/foreign/battle/temporary/base rejection");
 }
 static void recipes(ServerPlayerEntity player){
  var recipe=(ICraftingRecipe)player.func_184102_h().func_199529_aN().func_215367_a(new ResourceLocation(Content.NS,"mysterious_potion")).orElseThrow();
  var grid=new CraftingInventory(new Container(null,991){public boolean func_75145_c(PlayerEntity p){return true;}},3,3);
  String[] ids={"minecraft:lily_pad","minecraft:weeping_vines","minecraft:acacia_sapling","pixelmon:ultra_jungle_sapling","minecraft:golden_apple","minecraft:cactus","minecraft:grass","minecraft:potion","minecraft:warped_fungus"};for(int i=0;i<9;i++)grid.func_70299_a(i,Update180R11Cases.item(ids[i]));grid.func_70301_a(7).func_196082_o().func_74778_a("Potion","minecraft:water");
  check(recipe.func_77569_a(grid,player.field_70170_p)&&potion(recipe.func_77572_b(grid))&&recipe.func_77572_b(grid).func_190916_E()==1,"Exact corrected 3x3 recipe produces one");
  for(String id:List.of("awkward","healing","mundane")){grid.func_70301_a(7).func_196082_o().func_74778_a("Potion","minecraft:"+id);check(!recipe.func_77569_a(grid,player.field_70170_p),"Nonwater NBT rejected "+id);}grid.func_70301_a(7).func_196082_o().func_74778_a("Potion","minecraft:water");
  grid.func_70299_a(6,Update180R11Cases.item("minecraft:seagrass"));check(!recipe.func_77569_a(grid,player.field_70170_p),"Seagrass cannot replace corrected grass");grid.func_70299_a(6,Update180R11Cases.item("minecraft:grass"));
  var buffer=new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());try{((IRecipeSerializer)recipe.func_199559_b()).func_199427_a_(buffer,recipe);var decoded=(ICraftingRecipe)recipe.func_199559_b().func_199426_a_(recipe.func_199560_c(),buffer);check(decoded.func_77569_a(grid,player.field_70170_p),"Native recipe/nbt ingredient network roundtrip");}finally{buffer.release();}
  System.out.println("R13_RECIPE_PASS corrected 9 cells, exact water NBT, grass not seagrass, actual recipe packet codec");
 }
 static void loot(ServerPlayerEntity player){
  int tiers=0;for(var tier:BossTierRegistry.getBossTiers())if(tier.isBoss()){
   if(tier.getID().equals("drowned"))continue;List<DropSet> sets=BossLootCatalog.field(tier,"dropSets");int hits=0;for(var set:sets){com.pixelmonmod.pixelmon.api.util.WeightedSet<DropSet.Drop> drops=BossLootCatalog.field(set,"drops");for(var it=drops.iterator();it.hasNext();)if(potion(it.next().getDisplayItem()))hits++;}check(hits==1,"All native Boss tiers contain potion exactly once "+tier.getID()+" got "+hits);tiers++;
  }
  for(var pool:List.of(DropItemRegistry.tier1,DropItemRegistry.tier2,DropItemRegistry.tier3,DropItemRegistry.ultraSpace))check(pool.stream().filter(Update180PotionCases::potion).count()==1,"All four actual PokeLoot chest pools contain exactly one potion");
  check(AuthorBossRewards.randomPool().stream().map(AuthorBossRewards::stack).noneMatch(Update180PotionCases::potion),"Author final merged pool excludes potion");
  for(int i=0;i<1000;i++)check(AuthorBossRewards.randomPrizes(new Random(i)).stream().noneMatch(Update180PotionCases::potion),"Author never awards potion");
  var wild=mon(129).getOrSpawnPixelmon(player);boolean found=false;var drowned=BossTierRegistry.getBossTierOrNotBoss("drowned");wild.setBossTier(drowned);for(int i=0;i<2000&&!found;i++)found=drowned.getDrops(player,wild).stream().map(DropSet.Drop::getDisplayItem).anyMatch(Update180PotionCases::potion);check(found,"Drowned actual overridden reward method can roll potion");wild.func_70106_y();
  System.out.println("R13_LOOT_PASS "+tiers+" actual Boss tiers, drowned real reward rolls, 4 native chest pools, 1000 author rolls excluded");
 }
 public static void client(int age)throws Exception{
  if(age==30){var p=com.pixelmonmod.pixelmon.client.storage.ClientStorageManager.party.get(clientId);check(p!=null&&p.getSpecies().getDex()==clientDex&&p.getForm().getName().equals(clientForm),"Native client receives devolved species/form");var mc=net.minecraft.client.Minecraft.func_71410_x();var model=mc.func_175599_af().func_184393_a(new ItemStack(Backport.item("mysterious_potion")),mc.field_71441_e,mc.field_71439_g);check(!model.func_200117_a(null,null,new Random(1)).isEmpty()&&model.func_200117_a(null,null,new Random(1)).stream().noneMatch(q->q.func_187508_a().func_195668_m().toString().contains("missingno")),"Native potion item model baked");check(new ItemStack(Backport.item("mysterious_potion")).func_200301_q().getString().equals("神秘药剂"),"Exact Chinese item name");System.out.println("R13_POTION_CLIENT_PASS native species/form synchronization and item texture/name");}
  Update180LootNameCases.client(age);
 }
}
