package qa;
import java.util.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonFactory;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.entities.pixelmon.PixelmonEntity;
import com.pixelmonmod.pixelmon.enums.EnumGrowth;
import net.minecraft.item.*;
import net.minecraft.item.crafting.*;
import net.minecraft.inventory.*;
import net.minecraft.inventory.container.*;
import net.minecraft.entity.player.*;
import net.minecraft.util.*;
import net.minecraft.world.gen.settings.*;
import net.minecraft.world.gen.feature.structure.Structure;
import net.minecraftforge.registries.ForgeRegistries;
public final class Update180ExpansionCases {
    static volatile int grownId;static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static CraftingInventory grid(){return new CraftingInventory(new Container(null,83){public boolean func_75145_c(PlayerEntity p){return true;}},3,3);}
    public static void run(ServerPlayerEntity p){
        var loot=com.pixelmonmod.pixelmon.entities.npcs.registry.DropItemRegistry.tier3;long masters=loot.stream().filter(s->s.func_77942_o()&&"master_ball".equals(s.func_77978_p().func_74779_i("PokeBallID"))).count();System.out.println("MASTER_CHEST_BASELINE pool="+loot.size()+" masterEntries="+masters+" perVisibleMasterChest="+(100d*masters/loot.size())+" hidden="+(15d*masters/loot.size()));
        var world=p.func_71121_q();for(int x=29;x<=47;x++)for(int z=30;z<=48;z++)world.func_180501_a(new net.minecraft.util.math.BlockPos(x,73,z),net.minecraft.block.Blocks.field_180398_cJ.func_176223_P(),2);
        var inventory=grid();String[] ids={"pixelmon:ultra_elm_sapling","pixelmon:ultra_desert_cactus","pixelmon:ultra_jungle_sapling","pixelmon:ultra_space_stone",null,"pixelmon:ultra_space_stone","pixelmon:ultra_deep_stone","pixelmon:ultra_desert_cactus","pixelmon:ultra_deep_stone"};
        for(int i=0;i<9;i++)inventory.func_70299_a(i,ids[i]==null?com.pixelmonmod.pixelmon.items.PokeBallItem.of(com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry.getPokeBall("beast_ball").getValueUnsafe(),1):Update180Cases.stack(ids[i]));
        var raw=world.func_199532_z().func_215367_a(new ResourceLocation(Content.NS,"ultra_space_teleporter")).orElseThrow();
        @SuppressWarnings("unchecked") var recipe=(IRecipe<CraftingInventory>)raw;
        check(recipe.func_77569_a(inventory,world),"Actual native Beast Ball matches complete portal grid");check(recipe.func_77572_b(inventory).func_77973_b().getRegistryName().toString().equals("pixelmon:ultra_space_teleporter"),"Portal recipe native output");
        var ingredient=new BeastBallIngredient();check(!ingredient.isSimple()&&ingredient.test(ingredient.func_193365_a()[0]),"NBT-sensitive JEI ingredient display stack");var buffer=new net.minecraft.network.PacketBuffer(io.netty.buffer.Unpooled.buffer());ingredient.func_199564_a(buffer);var restored=Ingredient.func_199566_b(buffer);check(restored.test(BeastBallIngredient.modern()),"Native ingredient network roundtrip");buffer.release();
        for(String ball:List.of("poke_ball","master_ball","ultra_ball")){inventory.func_70299_a(4,com.pixelmonmod.pixelmon.items.PokeBallItem.of(com.pixelmonmod.pixelmon.api.pokemon.item.pokeball.PokeBallRegistry.getPokeBall(ball).getValueUnsafe(),1));check(!recipe.func_77569_a(inventory,world),"Other ball cannot substitute Beast Ball: "+ball);}
        inventory.func_70299_a(4,BeastBallIngredient.modern());inventory.func_70299_a(3,new ItemStack(Items.field_151055_y));check(!recipe.func_77569_a(inventory,world),"Wrong surrounding material rejected");
        for(String lamp:List.of("shrinking_lamp","enlarging_lamp")){var r=world.func_199532_z().func_215367_a(new ResourceLocation(Content.NS,lamp)).orElseThrow();check(r.func_77571_b().func_77973_b()==Backport.item(lamp),"Lamp native recipe output");for(var material:r.func_192400_c())if(material!=Ingredient.field_193370_a)check(material.func_193365_a().length>0,"Lamp recipe material exists");}
        System.out.println("EXPANSION180_PORTAL_PASS native shaped grid, real Beast Ball, three wrong variants rejected, network/JEI ingredient, native teleporter output; both lamp recipes");
        p.func_71033_a(net.minecraft.world.GameType.SURVIVAL);p.func_200619_a(world,38,74,46,180,0);var mon=Update180Cases.mon(130,"Tackle");StorageProxy.getParty(p).set(0,mon);var entity=mon.getOrCreatePixelmon(world,38,74,40);entity.func_70012_b(38,74,40,180,0);entity.func_94061_f(true);if(world.func_73045_a(entity.func_145782_y())==null)world.func_217376_c(entity);grownId=entity.func_145782_y();
        var ladder=new ArrayList<>(Arrays.asList(EnumGrowth.values()));ladder.sort(Comparator.comparingDouble(g->g.scaleValue));int hp=mon.getMaxHealth();var moves=new ArrayList<>(mon.getMoveset());mon.setGrowth(ladder.get(0));
        for(int direction:new int[]{1,-1}){
            var stack=new ItemStack(Backport.item(direction>0?"enlarging_lamp":"shrinking_lamp"),32);p.func_184611_a(Hand.MAIN_HAND,stack);
            for(int step=1;step<ladder.size();step++){var before=mon.getGrowth();int count=stack.func_190916_E();check(entity.func_230254_b_(p,Hand.MAIN_HAND)==ActionResultType.SUCCESS,"Native right click uses growth lamp before riding");check(mon.getGrowth()==GrowthLamp.next(before,direction)&&stack.func_190916_E()==count-1,"Exactly one native growth step and one consumed lamp");}
            int count=stack.func_190916_E();var extreme=mon.getGrowth();check(entity.func_230254_b_(p,Hand.MAIN_HAND)==ActionResultType.FAIL&&mon.getGrowth()==extreme&&stack.func_190916_E()==count,"Limit does not wrap or consume");
        }
        check(mon.getMaxHealth()==hp&&mon.getMoveset().equals(moves),"Growth does not change battle stats or moves");mon.setGrowth(EnumGrowth.Ordinary);var one=new ItemStack(Backport.item("enlarging_lamp"),1);p.func_184611_a(Hand.MAIN_HAND,one);entity.func_230254_b_(p,Hand.MAIN_HAND);check(one.func_190926_b()&&mon.getGrowth()==EnumGrowth.Huge,"Last lamp consumed and growth sync target set");
        var saved=PokemonFactory.create(mon.writeToNBT(new net.minecraft.nbt.CompoundNBT()));check(saved.getGrowth()==EnumGrowth.Huge,"Growth saved and restored natively");var stranger=net.minecraftforge.common.util.FakePlayerFactory.get(world,new com.mojang.authlib.GameProfile(UUID.fromString("ddf338cb-1111-4000-9000-000000001838"),"LampStranger"));var unauthorized=new ItemStack(Backport.item("shrinking_lamp"));check(((GrowthLamp)unauthorized.func_77973_b()).func_111207_a(unauthorized,stranger,entity,Hand.MAIN_HAND)==ActionResultType.FAIL&&unauthorized.func_190916_E()==1&&mon.getGrowth()==EnumGrowth.Huge,"Other owner cannot change growth or spend a lamp");
        System.out.println("EXPANSION180_LAMP_PASS all9 growth levels up/down, native right click priority, one consumed, limits no consume, owner guard, unchanged stats, native NBT restore");
        var pillar=ForgeRegistries.STRUCTURE_FEATURES.getValue(SpearPillarFrequency.ID);var settings=world.func_72863_F().func_201711_g().func_235957_b_();var actual=settings.func_236197_a_(pillar);check(actual!=null&&actual.func_236668_a_()==29&&actual.func_236671_b_()==23&&actual.func_236673_c_()==82316132,"Actual world's Spear Pillar spacing and original salt");check(!SpearPillarFrequency.apply(settings),"World load/reload idempotence");
        var source=new HashMap<>(settings.func_236195_a_());source.put(pillar,new StructureSeparationSettings(50,40,82316132));var test=new DimensionStructuresSettings(Optional.empty(),source);check(SpearPillarFrequency.apply(test),"Upgrade native spacing");for(var entry:source.entrySet())if(entry.getKey()!=pillar)check(test.func_236197_a_(entry.getKey())==entry.getValue(),"Other structure setting unchanged");check(!SpearPillarFrequency.apply(new DimensionStructuresSettings(Optional.empty(),new HashMap<Structure<?>,StructureSeparationSettings>())),"Do not enable a disabled structure");
        double ratio=2500d/(29*29);check(ratio>2.95&&ratio<3.05,"Threefold candidate density within integral chunk spacing");System.out.println("EXPANSION180_PILLAR_PASS actual50->29 / separation40->23, original salt, candidate density="+ratio+", idempotent and other structures/disabled maps preserved");
    }
    public static void client(){var mc=net.minecraft.client.Minecraft.func_71410_x();var entity=mc.field_71441_e.func_73045_a(grownId);check(entity instanceof PixelmonEntity&&((PixelmonEntity)entity).getPokemon().getGrowth()==EnumGrowth.Huge,"Native growth update reaches the rendering client");for(String name:List.of("shrinking_lamp","enlarging_lamp")){var model=mc.func_175599_af().func_184393_a(new ItemStack(Backport.item(name)),mc.field_71441_e,mc.field_71439_g);check(!model.func_177554_e().func_195668_m().toString().contains("missingno"),"Original lamp 3D model baked: "+name);}System.out.println("EXPANSION180_CLIENT_PASS actual growth network and both native 3D lamp models");}
}
