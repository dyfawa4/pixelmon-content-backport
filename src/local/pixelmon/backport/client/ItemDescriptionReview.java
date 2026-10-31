package local.pixelmon.backport.client;
import java.util.*;
import local.pixelmon.backport.Content;
import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
/** Presentation overrides do not alter JEI's shared acquisition catalogue. */
@Mod.EventBusSubscriber(modid=Content.NS,value=Dist.CLIENT)
public final class ItemDescriptionReview {
 public static final JsonObject DATA=Content.read("backport/item_description_review_zh_cn.json");
 public static boolean replaces(ItemStack stack){return stack.func_77973_b().getRegistryName()!=null&&DATA.has(stack.func_77973_b().getRegistryName().toString())&&net.minecraft.client.Minecraft.func_71410_x().field_71474_y.field_74363_ab.equals("zh_cn");}
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void tooltip(ItemTooltipEvent e){apply(e.getItemStack(),e.getToolTip());}
 public static void apply(ItemStack stack,List<ITextComponent> lines){
  var key=stack.func_77973_b().getRegistryName();if(key==null||lines.isEmpty())return;
  String id=key.toString();
  if(replaces(stack)){
   String description=DATA.get(id).getAsString();
   // Remove native tooltip/hint output, but retain name, enchantments, lore and advanced metadata.
   var nativeText=new TranslationTextComponent("item."+id.replace(':','.')+".tooltip").getString();
   var oldHelp=local.pixelmon.backport.ItemHelp.details(stack);
   var remove=new HashSet<String>();remove.add(nativeText);remove.add(description);
   for(String part:nativeText.split("\n"))remove.add(part);
   remove.add(new TranslationTextComponent("pixelmon.backport.item.shift_help").getString());
   remove.add(new TranslationTextComponent("pixelmon.tooltip.shift").getString());
   for(var line:oldHelp)remove.add(TextFormatting.func_110646_a(line.getString()));
   // Pixelmon's collapsed hint is a sibling translation with its own key.
   remove.add("按潜行键查看更多信息");remove.add(new TranslationTextComponent("gui.tooltip.collapsed").getString());
   for(int i=lines.size()-1;i>0;i--)if(remove.contains(TextFormatting.func_110646_a(lines.get(i).getString())))lines.remove(i);
   int insert=1;var font=net.minecraft.client.Minecraft.func_71410_x().field_71466_p;StringBuilder row=new StringBuilder();
   for(int cp:description.codePoints().toArray()){String glyph=new String(Character.toChars(cp));if(font.func_78256_a(row+glyph)>240&&row.length()>0){lines.add(insert++,new StringTextComponent(row.toString()).func_240699_a_(TextFormatting.GRAY));row.setLength(0);}row.append(glyph);}
   if(row.length()>0)lines.add(insert,new StringTextComponent(row.toString()).func_240699_a_(TextFormatting.GRAY));
  }
  // Advanced registry metadata follows every description, including third-party appended hints.
  int registry=-1;for(int i=1;i<lines.size();i++)if(id.equals(TextFormatting.func_110646_a(lines.get(i).getString()))){registry=i;break;}
  if(registry>=0){
   var label=lines.remove(registry);var nbt=new ArrayList<ITextComponent>();
   for(int i=lines.size()-1;i>0;i--){String text=TextFormatting.func_110646_a(lines.get(i).getString());if(text.startsWith("NBT:")||text.startsWith("NBT："))nbt.add(0,lines.remove(i));}
   int end=lines.size();String mod=net.minecraftforge.fml.ModList.get().getModContainerById(key.func_110624_b()).map(m->m.getModInfo().getDisplayName()).orElse("");
   if(end>1&&!mod.isEmpty()&&mod.equals(TextFormatting.func_110646_a(lines.get(end-1).getString())))end--;
   lines.add(end++,label);lines.addAll(end,nbt);
  }
 }
}
