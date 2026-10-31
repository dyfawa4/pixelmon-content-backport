package qa;
import java.nio.file.*;
import java.util.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemStack;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.pokemon.boss.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.*;
import com.pixelmonmod.pixelmon.entities.pixelmon.drops.DropItemQueryList;
import local.pixelmon.backport.*;
@Mod("backport_clone110_probe") @Mod.EventBusSubscriber(modid="backport_clone110_probe")
public class Clone110Probe {
 static int ticks,age,checks,depthCalls,depthOff;static boolean opened,hidden,done;static volatile int stage;static int previousStage;static final Map<String,Integer> depthStates=new TreeMap<>();
 public static void depth(boolean value){depthStates.merge("stage"+stage,1,Integer::sum);depthCalls++;if(!value)depthOff++;try{Files.writeString(Path.of("clone110-depth.txt"),"calls="+depthCalls+" depthWriteDisabled="+depthOff+" states="+depthStates);}catch(Exception e){throw new RuntimeException(e);}}
 static void check(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static int dust(net.minecraft.entity.player.ServerPlayerEntity p){int n=0;for(int i=0;i<36;i++){var s=p.field_71071_by.func_70301_a(i);if(s.func_77973_b() instanceof GlowScaleDust)n+=s.func_190916_E();}return n;}
 static void loot(net.minecraft.entity.player.ServerPlayerEntity player,BossTier tier,boolean shiny,boolean expected){
  var p=PokemonFactory.create(com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies.fromNationalDex(25));p.setShiny(shiny);var e=new PixelmonEntity(player.field_70170_p,p);if(tier!=null)e.setBossTier(tier);e.getPokemon().setShiny(shiny);
  int before=dust(player);DropItemQueryList.removeQuery(player);if(tier!=null)e.dropBossItems(player);else e.dropNormalItems(player);DropItemQueryList.takeAllItems(player);check(dust(player)-before==(expected?1:0),"Actual drop claim shiny="+shiny+" tier="+(tier==null?"none":tier.getID()));e.func_70106_y();
 }
 @SubscribeEvent public static void server(TickEvent.ServerTickEvent ev){if(ev.phase!=TickEvent.Phase.END||done)return;var server=ServerLifecycleHooks.getCurrentServer();if(server==null||server.func_184103_al().func_181057_v().isEmpty())return;var p=server.func_184103_al().func_181057_v().get(0);var w=p.func_71121_q();try{
  if(++ticks==100){if(!Files.exists(Path.of("skip-dust"))){loot(p,null,true,true);loot(p,null,false,false);for(var t:BossTierRegistry.getBossTiers())if(t.isBoss()){loot(p,t,true,false);loot(p,t,false,false);}for(var gender:com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender.values()){
    if(gender==com.pixelmonmod.pixelmon.api.pokemon.species.gender.Gender.NONE)continue;
    var karp=PokemonFactory.create(com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies.fromNationalDex(129));karp.setGender(gender);
    for(var palette:karp.getGenderProperties().getPalettes()){karp.setPalette(palette);boolean shinyPalette=palette.getName().equalsIgnoreCase("shiny");check(GlowScaleDust.qualifies(karp)==shinyPalette,"Karp palette identity "+palette.getName());var entity=new PixelmonEntity(w,karp);int before=dust(p);DropItemQueryList.removeQuery(p);entity.dropNormalItems(p);DropItemQueryList.takeAllItems(p);check(dust(p)-before==(shinyPalette?1:0),"Actual Karp palette claim "+palette.getName());entity.func_70106_y();}
   }var raidMon=PokemonFactory.create(com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies.fromNationalDex(129));raidMon.setShiny(true);var rp=new com.pixelmonmod.pixelmon.battles.raids.RaidData.RaidPlayer(0,p,raidMon);var raidDrops=new com.pixelmonmod.pixelmon.api.events.raids.RaidDropsEvent(null,rp,null,raidMon,new ArrayList<>(),true);com.pixelmonmod.pixelmon.Pixelmon.EVENT_BUS.post(raidDrops);check(raidDrops.getDrops().stream().noneMatch(v->v.func_77973_b() instanceof GlowScaleDust),"Raid shiny Boss excludes powder");Files.writeString(Path.of("dust110-pass.txt"),"PASS "+checks);if(Files.exists(Path.of("dust-only"))){done=true;return;}}
   for(int x=-4;x<=9;x++)for(int z=0;z<=9;z++)for(int y=89;y<=94;y++)w.func_180501_a(new BlockPos(x,y,z),(y==89?net.minecraft.block.Blocks.field_150348_b:net.minecraft.block.Blocks.field_150350_a).func_176223_P(),3);
   for(int x=-2;x<=2;x+=2)for(int y=90;y<=91;y++)w.func_180501_a(new BlockPos(x,y,3),net.minecraft.block.Blocks.field_150486_ae.func_176223_P(),3);p.func_70012_b(.5,90,-2,0,0);stage=1;
  }
  if(ticks==220){var block=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pixelmon:cloning_machine"));check(block!=null,"Cloner registered");var stack=new ItemStack(block);p.func_184611_a(net.minecraft.util.Hand.MAIN_HAND,stack);var hit=new net.minecraft.util.math.BlockRayTraceResult(new net.minecraft.util.math.vector.Vector3d(4.5,90,5.5),net.minecraft.util.Direction.UP,new BlockPos(4,89,5),false);var result=stack.func_196084_a(new net.minecraft.item.ItemUseContext(p,net.minecraft.util.Hand.MAIN_HAND,hit));Files.writeString(Path.of("clone110-placement.txt"),result+" "+w.func_180495_p(new BlockPos(4,90,5))+" "+w.func_175625_s(new BlockPos(4,90,5)));stage=2;}
  if(ticks==280)stage=3;
  if(ticks==340)stage=4;
  if(ticks==400)stage=5;
  if(ticks==460){w.func_180501_a(new BlockPos(4,90,5),net.minecraft.block.Blocks.field_150350_a.func_176223_P(),3);stage=6;}
  if(ticks>550)done=true;
 }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("probe110-fail.txt"),t.toString());}catch(Exception e){}done=true;}}
 static final Map<Integer,Boolean> actualReturns=new TreeMap<>();
 @SubscribeEvent public static void afterWorld(net.minecraftforge.client.event.RenderWorldLastEvent ev){
  if(stage<2||stage>5||previousStage!=stage||actualReturns.containsKey(stage))return;var mc=Minecraft.func_71410_x();if(mc.field_71441_e==null)return;var tile=mc.field_71441_e.func_175625_s(new BlockPos(4,90,5));if(!(tile instanceof com.pixelmonmod.pixelmon.blocks.tileentity.CloningMachineTileEntity))return;
  var c=(com.pixelmonmod.pixelmon.blocks.tileentity.CloningMachineTileEntity)tile;var render=net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.field_147556_a.func_147547_b(c);var ms=new com.mojang.blaze3d.matrix.MatrixStack();ms.func_227860_a_();ms.func_227860_a_();var buffer=net.minecraft.client.renderer.IRenderTypeBuffer.func_228455_a_(new net.minecraft.client.renderer.BufferBuilder(256));
  com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
  ((com.pixelmonmod.pixelmon.client.render.tileEntities.RenderTileEntityCloningMachine)render).render(c,c.func_195044_w(),0,0,0,0,ms,buffer,15728880,net.minecraft.client.renderer.texture.OverlayTexture.field_229196_a_);
  actualReturns.put(stage,org.lwjgl.opengl.GL11.glGetBoolean(org.lwjgl.opengl.GL11.GL_DEPTH_WRITEMASK));buffer.func_228461_a_();com.mojang.blaze3d.systems.RenderSystem.depthMask(true);
  try{Files.writeString(Path.of("clone110-actual-return.txt"),actualReturns.toString());}catch(Exception e){throw new RuntimeException(e);}
 }
 @SubscribeEvent public static void client(TickEvent.ClientTickEvent ev){if(ev.phase!=TickEvent.Phase.END)return;var mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}
  if(!opened&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_("backport-qa-single");}
  if(mc.field_71439_g!=null){com.pixelmonmod.pixelmon.client.ServerStorageDisplay.showStarterScreen=false;mc.func_147108_a(null);mc.field_71439_g.func_70012_b(.5,90,-2,0,0);}
  if(stage>0&&mc.field_71441_e!=null){if(previousStage!=stage){age=0;previousStage=stage;var tile=mc.field_71441_e.func_175625_s(new BlockPos(4,90,5));if(tile instanceof com.pixelmonmod.pixelmon.blocks.tileentity.CloningMachineTileEntity){var c=(com.pixelmonmod.pixelmon.blocks.tileentity.CloningMachineTileEntity)tile;if(stage==3)c.isBroken=true;if(stage==4){c.isBroken=false;c.isFinished=true;}if(stage==5){c.isBroken=false;c.isFinished=false;c.mew=PokemonFactory.create(com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies.MEW.getValueUnsafe());}}}if(++age==40){final String name="clone110-stage"+stage+".png";net.minecraft.util.ScreenShotHelper.func_148259_a(mc.field_71412_D,name,mc.func_228018_at_().func_198107_o(),mc.func_228018_at_().func_198087_p(),mc.func_147110_a(),msg->{});}if(age>90)age=0;}
  if(done)mc.func_71400_g();
 }
}
