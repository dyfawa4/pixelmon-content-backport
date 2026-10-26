package qa;

import java.util.*;
import local.pixelmon.backport.*;
import com.mojang.authlib.GameProfile;
import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.battles.*;
import com.pixelmonmod.pixelmon.api.events.battles.BattleEndEvent;
import com.pixelmonmod.pixelmon.api.pokemon.boss.BossTierRegistry;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.controller.*;
import com.pixelmonmod.pixelmon.battles.controller.participants.*;
import com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import net.minecraft.network.*;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.server.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.*;
import net.minecraft.world.*;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/** Disposable native players, packet recording, Forge interactions and real battle end events. */
public final class Update180R13AppendCases {
 static final class Connection extends ServerPlayNetHandler {
  final List<IPacket<?>> packets=new ArrayList<>();
  Connection(MinecraftServer server,ServerPlayerEntity player){super(server,new NetworkManager(PacketDirection.SERVERBOUND),player);}
  @Override public void func_147359_a(IPacket<?> packet){packets.add(packet);}
  @Override public void func_211148_a(IPacket<?> packet,io.netty.util.concurrent.GenericFutureListener<? extends io.netty.util.concurrent.Future<? super Void>> listener){packets.add(packet);}
 }
 static ServerPlayerEntity player(ServerPlayerEntity original,int index){
  var p=new ServerPlayerEntity(original.func_184102_h(),original.func_71121_q(),new GameProfile(UUID.nameUUIDFromBytes(("R13Player"+index).getBytes(java.nio.charset.StandardCharsets.UTF_8)),"R13Player"+index),new PlayerInteractionManager(original.func_71121_q()));p.field_71135_a=new Connection(original.func_184102_h(),p);p.func_70606_j(20);p.func_70107_b(45,75,45);return p;
 }
 static Connection connection(ServerPlayerEntity p){return (Connection)p.field_71135_a;}
 static void check(boolean v,String why){if(!v)throw new AssertionError(why);}
 static ItemStack tag(int count,boolean named){var stack=Update180R11Cases.item("minecraft:name_tag");stack.func_190920_e(count);if(named)stack.func_200302_a(new StringTextComponent("DoNotRename"));return stack;}
 static long sounds(ServerPlayerEntity p){return connection(p).packets.stream().filter(x->x instanceof SPlaySoundEffectPacket&&((SPlaySoundEffectPacket)x).func_186978_a().getRegistryName().equals(BattleVictorySound.SOUND)).count();}
 public static void server(ServerPlayerEntity original)throws Exception{
  names(original);soundCases(original);Update180R13WorldMusicCases.server(original);Update180LootNameCases.server(original);
  System.out.println("R13_APPEND_SERVER_PASS name-tag creative/survival deaths, exact death packet, hidden challenge and inventory/bag consumption; real winner-only deduplicated victory sounds");
 }
 static void names(ServerPlayerEntity original){
  var world=original.func_71121_q();boolean keep=world.func_82736_K().func_223586_b(GameRules.field_223600_c);world.func_82736_K().func_223585_a(GameRules.field_223600_c).func_223570_a(true,original.func_184102_h());
  var npc=new NPCTrainer(world);npc.getPersistentData().func_74757_a(AuthorBossTeams.MARKER,true);npc.setName("AuthorKeep");npc.setNickName("AuthorKeep");var nativeName=npc.getNickName();var display=npc.func_200200_C_().getString();
  var advancement=original.func_184102_h().func_191949_aK().func_192778_a(AuthorNameTag.CHALLENGE);check(advancement!=null,"Hidden advancement registered");var metadata=Content.read("data/pixelmon_backport/advancements/adventure/forbidden_name_tag.json");check(metadata.getAsJsonObject("display").get("hidden").getAsBoolean()&&metadata.getAsJsonObject("display").get("frame").getAsString().equals("challenge"),"Hidden challenge type");
  try{for(int i=0;i<4;i++){
   var p=player(original,100+i);p.func_71033_a((i&1)==0?GameType.SURVIVAL:GameType.CREATIVE);p.field_71071_by.func_174888_l();p.func_184611_a(Hand.MAIN_HAND,tag(3,true));p.func_184611_a(Hand.OFF_HAND,tag(5,false));p.field_71071_by.func_70299_a(5,tag(64,true));p.field_71071_by.func_70299_a(6,Update180R11Cases.item("minecraft:diamond"));var sophisticated=new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("sophisticatedbackpacks:netherite_backpack")));p.field_71071_by.func_70299_a(3,sophisticated);var sophisticatedInventory=sophisticated.getCapability(net.minecraftforge.items.CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).orElseThrow(()->new AssertionError("Real sophisticated backpack inventory capability"));check(sophisticatedInventory.insertItem(0,tag(20,true),false).func_190926_b(),"Sophisticated backpack accepts name tags");
   var bagItem=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValues().stream().filter(x->x instanceof com.pixelmonmod.pixelmon.items.PokeBagItem).findFirst().orElseThrow();var bagStack=new ItemStack(bagItem);p.field_71071_by.func_70299_a(2,bagStack);var bag=com.pixelmonmod.pixelmon.items.PokeBagItem.PokeBag.readFromItemStack(p,bagStack);bag.inventory.func_70299_a(0,tag(12,true));bag.inventory.func_70299_a(1,Update180R11Cases.item("minecraft:emerald"));bag.writeToStack(bagStack);
   // Open bags use the live container; closed bags use persisted NBT.
   if(i>=2){p.field_71071_by.field_70461_c=2;bagItem.func_77659_a(world,p,Hand.MAIN_HAND);check(p.field_71070_bA instanceof PokeInventory.OpenBag,"Real native bag opened");p.func_184611_a(Hand.OFF_HAND,tag(5,true));}
   Hand hand=i>=2?Hand.OFF_HAND:Hand.MAIN_HAND;boolean before=p.func_70089_S();
   if(i%2==0){var event=new PlayerInteractEvent.EntityInteract(p,hand,npc);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);check(event.isCanceled(),"Generic interaction canceled");}
   else{var event=new PlayerInteractEvent.EntityInteractSpecific(p,hand,npc,Vector3d.field_186680_a);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);check(event.isCanceled(),"Specific interaction canceled");}
   check(before&&!p.func_70089_S()&&p.func_110143_aJ()==0,"Real death despite creative/spawn immunity "+i);
   check(nativeName.equals(npc.getNickName())&&display.equals(npc.func_200200_C_().getString()),"Author name not changed");
   try(var inventory=new PokeInventory(p)){check(inventory.stacks.stream().noneMatch(s->!s.func_190926_b()&&s.func_77973_b() instanceof NameTagItem),"No carried name tags remain");}
   p.func_71053_j();var loaded=com.pixelmonmod.pixelmon.items.PokeBagItem.PokeBag.readFromItemStack(p,bagStack);check(loaded.inventory.func_70301_a(0).func_190926_b()&&loaded.inventory.func_70301_a(1).func_77973_b().getRegistryName().toString().equals("minecraft:emerald"),"Bag close does not restore tags or remove other items");
   check(p.field_71071_by.func_70301_a(6).func_77973_b().getRegistryName().toString().equals("minecraft:diamond"),"Other carried items retained under keepInventory");
   check(sophisticatedInventory.getStackInSlot(0).func_190926_b(),"Sophisticated backpack tags also cleared");check(p.func_192039_O().func_192747_a(advancement).func_192105_a(),"Hidden challenge awarded after death");
   var death=connection(p).packets.stream().filter(x->x instanceof SCombatPacket&&((SCombatPacket)x).field_179776_a==SCombatPacket.Event.ENTITY_DIED).map(x->(SCombatPacket)x).findFirst().orElseThrow();
   check(death.field_179773_e instanceof TranslationTextComponent&&((TranslationTextComponent)death.field_179773_e).func_150268_i().equals("death.attack.backport_author_nametag"),"Native death packet carries requested custom cause");
   check(death.field_179773_e.getString().equals(p.func_145748_c_().getString()+"做了不该做的事情!"),"Exact requested death description "+death.field_179773_e.getString());
  }}finally{world.func_82736_K().func_223585_a(GameRules.field_223600_c).func_223570_a(keep,original.func_184102_h());}
  var p=player(original,200);p.func_184611_a(Hand.MAIN_HAND,tag(2,true));var ordinary=new NPCTrainer(world);var event=new PlayerInteractEvent.EntityInteract(p,Hand.MAIN_HAND,ordinary);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);check(!event.isCanceled()&&p.func_70089_S()&&p.func_184586_b(Hand.MAIN_HAND).func_190916_E()==2,"Ordinary trainer name tag unaffected");
  p.func_184611_a(Hand.MAIN_HAND,Update180R11Cases.item("minecraft:stick"));event=new PlayerInteractEvent.EntityInteract(p,Hand.MAIN_HAND,npc);net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);check(!event.isCanceled()&&p.func_70089_S(),"Other author interactions unaffected");
  System.out.println("R13_NAMETAG_PASS four native deaths: survival/creative, generic/specific and native bags closed/open; exact death text, name kept, hidden challenge, unrelated items and ordinary NPC behavior");
 }
 static BattleController battle(ServerPlayerEntity p,BattleParticipant foe){
  var mon=Update180Cases.mon(6,"Tackle");StorageProxy.getParty(p).set(0,mon);var human=new PlayerParticipant(p,mon);var battle=new BattleController(new BattleParticipant[]{human},new BattleParticipant[]{foe},new com.pixelmonmod.pixelmon.battles.api.rules.BattleRules());battle.sendMessages=false;return battle;
 }
 static HashMap<BattleParticipant,BattleResults> results(BattleController b,boolean win){var map=new HashMap<BattleParticipant,BattleResults>();for(var p:b.participants)map.put(p,p instanceof PlayerParticipant?(win?BattleResults.VICTORY:BattleResults.DEFEAT):(win?BattleResults.DEFEAT:BattleResults.VICTORY));return map;}
 static void soundCases(ServerPlayerEntity original){
  int checked=0;for(var tier:BossTierRegistry.getBossTiers())if(tier.isBoss()){
   var p=player(original,300+checked);var mon=Update180Cases.mon(129,"Splash");var entity=mon.getOrSpawnPixelmon(original);entity.setBossTier(tier);var foe=new WildPixelmonParticipant(entity);var b=battle(p,foe);var map=results(b,true);var event=new BattleEndEvent(b,BattleEndCause.NORMAL,false,map);var handler=new BattleVictorySound();handler.ended(event);handler.ended(event);check(sounds(p)==1,"One sound for winning actual Boss tier despite repeated event "+tier.getID());b.battleEnded=true;entity.func_70106_y();checked++;
  }
  for(int mode=0;mode<4;mode++){
   var p=player(original,400+mode);var npc=new NPCTrainer(original.field_70170_p);npc.isGymLeader=mode!=0;if(mode==2){npc.isGymLeader=false;npc.getPersistentData().func_74778_a(GymCities.TYPE,"normal");npc.getPersistentData().func_74778_a("BackportGymRole","leader");}if(mode==3)npc.getPersistentData().func_74757_a(AuthorBossTeams.MARKER,true);npc.getPokemonStorage().set(0,Update180Cases.mon(129,"Splash"));var b=battle(p,new TrainerParticipant(npc,1));var handler=new BattleVictorySound();var event=new BattleEndEvent(b,BattleEndCause.NORMAL,false,results(b,true));handler.ended(event);handler.ended(event);check(sounds(p)==(mode==0?0:1),"Ordinary trainer silent, every gym/author once "+mode);b.battleEnded=true;
  }
  for(int mode=0;mode<4;mode++){
   var p=player(original,500+mode);var npc=new NPCTrainer(original.field_70170_p);npc.isGymLeader=true;npc.getPokemonStorage().set(0,Update180Cases.mon(129,"Splash"));var b=battle(p,new TrainerParticipant(npc,1));if(mode==2)b.simulateMode=true;var event=new BattleEndEvent(b,mode==1?BattleEndCause.FORCE:BattleEndCause.NORMAL,mode==1,results(b,mode!=0));
   if(mode==3){var map=new HashMap<BattleParticipant,BattleResults>();for(var side:b.participants)map.put(side,BattleResults.DRAW);event=new BattleEndEvent(b,BattleEndCause.NORMAL,false,map);}
   new BattleVictorySound().ended(event);check(sounds(p)==0,"Defeat/abnormal/simulation/draw never sounds "+mode);b.battleEnded=true;
  }
  // One native endBattle invocation proves the registered event path (no manual handler call).
  var p=player(original,600);var npc=new NPCTrainer(original.field_70170_p);npc.isGymLeader=true;npc.getPokemonStorage().set(0,Update180Cases.mon(129,"Splash"));var b=battle(p,new TrainerParticipant(npc,1));var map=results(b,true);b.endBattle(BattleEndCause.NORMAL,map);check(sounds(p)==1,"Native BattleController.endBattle emits registered winner sound");
  System.out.println("R13_VICTORY_SOUND_PASS "+checked+" Boss tiers, normal gym and author, replay deduplication, losing/abnormal/simulation/draw rejection and actual native battle end");
 }
 public static void client(int age)throws Exception{Update180R13WorldMusicCases.client(age);if(age>150)Update180LootNameCases.client(age-150);if(age>260)net.minecraft.client.Minecraft.func_71410_x().func_71400_g();}
}
