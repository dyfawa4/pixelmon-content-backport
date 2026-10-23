package local.pixelmon.backport.client;

import java.util.*;
import local.pixelmon.backport.*;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.*;

/** Loaded only on a JEI client. All displayed outputs are copies of server data. */
public final class LootSourceJei {
 public static final String[] GROUPS={"structures","max_raids","tera_raids","bosses"};
 private static final String[] TITLES={"建筑与奖励箱","极巨巢穴掉落","太晶巢穴掉落","Boss等级掉落"};
 private static IJeiRuntime runtime;private static UUID generation;private static int expected;
 private static final Map<Integer,List<LootSourceCatalog.Source>> batches=new TreeMap<>();
 private static List<LootSourceCatalog.Source> current=List.of();private static final List<Page> installed=new ArrayList<>();
 private static final Map<String,Page> known=new HashMap<>();
 public static ResourceLocation uid(String group){return new ResourceLocation(Content.NS,"loot_"+group);}
 public static ItemStack icon(String group){String id=group.equals("structures")?"minecraft:chest":group.equals("max_raids")?"pixelmon:wishing_piece":group.equals("bosses")?"pixelmon:rare_candy":Content.NS+":tera_den_spawner";return new ItemStack(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)));}
 public static void categories(IRecipeCategoryRegistration registry){for(int i=0;i<GROUPS.length;i++)registry.addRecipeCategories(new Category(GROUPS[i],TITLES[i],registry.getJeiHelpers().getGuiHelper()));net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Disconnect());}
 public static void catalysts(IRecipeCatalystRegistration registry){for(String group:GROUPS)registry.addRecipeCatalyst(icon(group),uid(group));}
 public static void attach(IJeiRuntime value){runtime=value;installed.clear();known.clear();apply();}
 public static void receive(LootSourceNetwork.Batch batch){if(!batch.generation.equals(generation)){generation=batch.generation;expected=batch.total;batches.clear();}if(batch.total!=expected)return;batches.put(batch.index,batch.sources);if(batches.size()!=expected)return;List<LootSourceCatalog.Source> all=new ArrayList<>();for(var values:batches.values())all.addAll(values);current=List.copyOf(all);batches.clear();apply();}
 private static void apply(){if(runtime==null)return;var manager=runtime.getRecipeManager();for(Page old:installed)manager.hideRecipe(old,uid(old.source.group));installed.clear();for(var source:current)for(int offset=0;offset<source.entries.size();offset+=28){Page candidate=new Page(source,offset);String key=candidate.key();Page page=known.get(key);if(page==null){page=candidate;known.put(key,page);manager.addRecipe(page,uid(source.group));}else manager.unhideRecipe(page,uid(source.group));installed.add(page);}}
 public static void clear(){generation=null;expected=0;batches.clear();current=List.of();apply();}
 public static final class Disconnect {@net.minecraftforge.eventbus.api.SubscribeEvent public void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggedOutEvent event){clear();}}
 public static final class Page {
  public final LootSourceCatalog.Source source;public final int offset;public final List<LootSourceCatalog.Entry> entries;
  public Page(LootSourceCatalog.Source source,int offset){this.source=source;this.offset=offset;entries=source.entries.subList(offset,Math.min(source.entries.size(),offset+28));}
  private String key(){StringBuilder key=new StringBuilder(source.group).append('\0').append(source.id).append('\0').append(source.title).append('\0').append(source.detail).append('\0').append(offset).append('/').append(source.entries.size());for(var entry:entries)key.append('\0').append(entry.stack.func_77955_b(new net.minecraft.nbt.CompoundNBT())).append('\0').append(entry.note);return key.toString();}
 }
 public static final class Category implements IRecipeCategory<Page> {
  private final String group,title;private final IDrawable background,icon;
  public Category(String group,String title,mezz.jei.api.helpers.IGuiHelper helper){this.group=group;this.title=title;background=helper.createBlankDrawable(164,132);icon=helper.createDrawableIngredient(LootSourceJei.icon(group));}
  @Override public ResourceLocation getUid(){return uid(group);}
  @Override public Class<Page> getRecipeClass(){return Page.class;}
  @Override public String getTitle(){return title;}
  @Override public IDrawable getBackground(){return background;}
  @Override public IDrawable getIcon(){return icon;}
  @Override public void setIngredients(Page page,mezz.jei.api.ingredients.IIngredients ingredients){ingredients.setInput(VanillaTypes.ITEM,LootSourceJei.icon(group));List<ItemStack> outputs=new ArrayList<>();for(var entry:page.entries)outputs.add(entry.stack);ingredients.setOutputs(VanillaTypes.ITEM,outputs);}
  @Override public void setRecipe(mezz.jei.api.gui.IRecipeLayout layout,Page page,mezz.jei.api.ingredients.IIngredients ingredients){var slots=layout.getItemStacks();slots.init(0,true,0,4);slots.set(0,LootSourceJei.icon(group));for(int i=0;i<page.entries.size();i++){slots.init(i+1,false,18+(i%7)*20,50+(i/7)*20);slots.set(i+1,page.entries.get(i).stack);}slots.addTooltipCallback((slot,input,stack,tooltip)->{if(slot>0&&slot<=page.entries.size()){tooltip.add(new StringTextComponent(page.source.title).func_240699_a_(TextFormatting.GRAY));for(String part:page.entries.get(slot-1).note.split("；"))tooltip.add(new StringTextComponent(part).func_240699_a_(TextFormatting.GRAY));}});}
  @Override public void draw(Page page,com.mojang.blaze3d.matrix.MatrixStack matrices,double x,double y){var font=net.minecraft.client.Minecraft.func_71410_x().field_71466_p;int line=2;for(var text:font.func_238425_b_(new StringTextComponent(page.source.title),140)){if(line>20)break;font.func_238422_b_(matrices,text,23,line,0x333333);line+=10;}font.func_238421_b_(matrices,"候选物品 "+(page.offset+1)+"～"+(page.offset+page.entries.size())+" / "+page.source.entries.size(),0,29,0x555555);font.func_238421_b_(matrices,"悬停查看数量和获取条件",0,40,0x555555);}
  @Override public List<ITextComponent> getTooltipStrings(Page page,double x,double y){if(y>=0&&y<27)return List.of(new StringTextComponent(page.source.title),new StringTextComponent(page.source.detail),new StringTextComponent("仅列基础奖池和本模组追加；事件可修改实际奖励"),new StringTextComponent(page.source.id).func_240699_a_(TextFormatting.DARK_GRAY));return List.of();}
 }
}
