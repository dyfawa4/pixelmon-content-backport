package qa;
import java.util.*;import java.nio.file.*;
import local.pixelmon.backport.*;import local.pixelmon.backport.client.*;
import net.minecraft.client.Minecraft;import net.minecraft.item.*;import net.minecraft.util.*;import net.minecraft.util.text.*;import net.minecraft.client.util.ITooltipFlag;
import net.minecraftforge.fml.common.Mod;import net.minecraftforge.eventbus.api.*;import net.minecraftforge.event.TickEvent;import net.minecraftforge.registries.ForgeRegistries;
@Mod("backport_tooltips110_probe") @Mod.EventBusSubscriber(modid="backport_tooltips110_probe")
public class Tooltips110Probe {
 static boolean opened,hidden,done,checking,serverReady;static int age,index,checks,nativeDescriptions,helpDescriptions,events,serverAge;static List<Item> items;static List<String> problems=new ArrayList<>();
 @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){if(serverReady||done||e.phase!=TickEvent.Phase.END)return;var s=net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer();if(s==null||s.func_184103_al().func_181057_v().isEmpty()||++serverAge<100)return;try{FridgeSounds110Cases.run(s.func_184103_al().func_181057_v().get(0));Candy110Cases.run(s.func_184103_al().func_181057_v().get(0));serverReady=true;}catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("tooltips110-fail.txt"),t.toString());}catch(Exception ignored){}done=true;}}
 static void check(boolean b,String why){checks++;if(!b)throw new AssertionError(why);}
 static String plain(String text){return TextFormatting.func_110646_a(text).replace("\n","");}
 static String text(List<ITextComponent> lines){return plain(lines.stream().map(ITextComponent::getString).collect(java.util.stream.Collectors.joining("")));}
 @SubscribeEvent(priority=EventPriority.HIGHEST) public static void event(net.minecraftforge.event.entity.player.ItemTooltipEvent e){if(checking){events++;check(ExpandedItemTooltips.active()&&net.minecraft.client.gui.screen.Screen.func_231173_s_(),"Tooltip extensions get expanded scope");}}
 static void finish(Minecraft mc)throws Exception{
  check(problems.isEmpty(),"Registered item tooltip failures: "+problems);check(nativeDescriptions>500&&helpDescriptions>50,"Broad native and helper coverage");
  try{ExpandedItemTooltips.during(()->{throw new IllegalStateException("scope cleanup fixture");});}catch(IllegalStateException expected){}check(!ExpandedItemTooltips.active(),"Exception restores scope");
  ExpandedItemTooltips.during(()->{ExpandedItemTooltips.during(()->{check(ExpandedItemTooltips.active(),"Nested tooltip scope");return null;});check(ExpandedItemTooltips.active(),"Outer tooltip scope retained");return null;});check(!ExpandedItemTooltips.active(),"Nested scope cleanup");
  var quest=new com.pixelmonmod.pixelmon.entities.npcs.NPCQuestGiver(mc.field_71441_e);for(var row:NpcLocalization.DATA.getAsJsonObject("names").entrySet()){quest.setName(row.getKey());check(NpcNameplates.name(quest).equals(row.getValue().getAsString()),"All stock NPC name mappings "+row.getKey());}quest.setName("Team Skull Grunt");check(NpcNameplates.name(quest).equals("骷髅队队员"),"Reported Team Skull NPC");quest.setNickName("My NPC");check(NpcNameplates.name(quest).equals("My NPC"),"Custom NPC nickname retained");
  var juice=new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("pixelmon:yellow_juice")));String info=text(juice.func_82840_a(mc.field_71439_g,ITooltipFlag.TooltipFlags.ADVANCED));check(!info.contains("按潜行")&&!info.contains("按住 Shift")&&info.length()>50,"Reported yellow juice shows direct full description");
  Files.writeString(Path.of("tooltips110-yellow-juice.txt"),info);Files.writeString(Path.of("tooltips110-pass.txt"),"PASS "+checks+" registered_items="+items.size()+" native_descriptions="+nativeDescriptions+" helper_descriptions="+helpDescriptions+" actual_tooltip_events="+events+"; normal and advanced tooltip, no Shift prompts, native/helper info visible, metadata order, vanilla stone, scoped input unchanged, exception cleanup, no NBT mutations");
  mc.func_147108_a(new net.minecraft.client.gui.screen.Screen(new StringTextComponent("物品说明")){
   @Override public void func_230430_a_(com.mojang.blaze3d.matrix.MatrixStack m,int mx,int my,float partial){func_238467_a_(m,0,0,field_230708_k_,field_230709_l_,0xff30343a);func_238471_a_(m,mc.field_71466_p,"无需按Shift：黄色果汁",field_230708_k_/2,35,0xffffff);func_230457_a_(m,juice,field_230708_k_/2-100,100);}
  });
 }
 static boolean capture;
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.func_71410_x();mc.field_71474_y.field_82881_y=false;if(!hidden){org.lwjgl.glfw.GLFW.glfwHideWindow(mc.func_228018_at_().func_198092_i());hidden=true;}if(done){mc.func_71400_g();return;}if(!opened&&mc.field_71462_r instanceof net.minecraft.client.gui.screen.MainMenuScreen){opened=true;mc.func_238191_a_("backport-qa-single");}if(mc.field_71439_g==null)return;com.pixelmonmod.pixelmon.client.ServerStorageDisplay.showStarterScreen=false;if(++age<100)return;
  try{
   if(capture){if(age<180)return;Files.createDirectories(Path.of("screenshots"));try(var image=net.minecraft.util.ScreenShotHelper.func_198052_a(mc.func_228018_at_().func_198105_m(),mc.func_228018_at_().func_198083_n(),mc.func_147110_a())){image.func_209270_a(Path.of("screenshots/direct-yellow-juice.png"));}done=true;return;}
   if(!serverReady)return;if(items==null){items=new ArrayList<>(ForgeRegistries.ITEMS.getValues());check(!ExpandedItemTooltips.active(),"Initial scope empty");}
   for(int n=0;n<30&&index<items.size();n++,index++){
    Item item=items.get(index);var stack=new ItemStack(item);if(stack.func_190926_b())continue;stack.func_82840_a(mc.field_71439_g,ITooltipFlag.TooltipFlags.NORMAL);var before=stack.func_77955_b(new net.minecraft.nbt.CompoundNBT());String id=item.getRegistryName().toString();
    for(var flag:new ITooltipFlag[]{ITooltipFlag.TooltipFlags.NORMAL,ITooltipFlag.TooltipFlags.ADVANCED}){
     boolean shift=net.minecraft.client.gui.screen.Screen.func_231173_s_(),key=com.pixelmonmod.pixelmon.client.gui.ScreenHelper.isKeyDown(340);checking=true;List<ITextComponent> lines;try{lines=stack.func_82840_a(mc.field_71439_g,flag);}finally{checking=false;}
     check(!ExpandedItemTooltips.active()&&shift==net.minecraft.client.gui.screen.Screen.func_231173_s_()&&key==com.pixelmonmod.pixelmon.client.gui.ScreenHelper.isKeyDown(340),"Real Shift state retained "+id);
     String all=text(lines);String collapsed=plain(new TranslationTextComponent("gui.tooltip.collapsed").getString());if(all.contains(collapsed)||all.contains("按住 Shift 查看用途与获取方式"))problems.add(id+": collapsed hint");
     var details=ItemHelp.details(stack);if(!ItemDescriptionReview.replaces(stack)&&!details.isEmpty()){helpDescriptions++;for(var detail:details)check(all.contains(plain(detail.getString())),"Always visible helper text "+id);}
     if(item instanceof com.pixelmonmod.pixelmon.items.PixelmonItem&&!ItemDescriptionReview.replaces(stack)&&!(item instanceof com.pixelmonmod.pixelmon.items.heldItems.MegaStoneItem)&&!(item instanceof TeraDenItem)&&!(item instanceof com.pixelmonmod.pixelmon.items.TechnicalMoveItem)&&!id.matches("pixelmon:hm[0-9]+")){
      String description=((com.pixelmonmod.pixelmon.items.PixelmonItem)item).getTooltipText(stack);if(description!=null&&!description.isBlank()){nativeDescriptions++;if(!all.contains(plain(description)))problems.add(id+": native description absent");}
     }
     if(flag==ITooltipFlag.TooltipFlags.ADVANCED){int registry=-1;for(int i=0;i<lines.size();i++)if(plain(lines.get(i).getString()).equals(id))registry=i;if(registry>=0)for(var detail:details)if(!ItemDescriptionReview.replaces(stack))check(all.indexOf(plain(detail.getString()))<all.indexOf(id),"All info before registry "+id);}
    }
    check(before.equals(stack.func_77955_b(new net.minecraft.nbt.CompoundNBT())),"Read-only item NBT "+id);
   }
   if(index>=items.size()){finish(mc);capture=true;age=100;}
  }catch(Throwable t){t.printStackTrace();try{Files.writeString(Path.of("tooltips110-fail.txt"),t.toString()+"\n"+problems);}catch(Exception ignored){}done=true;}
 }
}
