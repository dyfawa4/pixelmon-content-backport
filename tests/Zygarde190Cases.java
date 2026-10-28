package qa;

import java.util.*;
import java.nio.file.*;
import local.pixelmon.backport.*;
import com.pixelmonmod.pixelmon.api.pokemon.*;
import com.pixelmonmod.pixelmon.api.storage.*;
import com.pixelmonmod.pixelmon.api.pokemon.ability.abilities.*;
import com.pixelmonmod.pixelmon.blocks.tileentity.ReassemblyUnitTileEntity;
import com.pixelmonmod.pixelmon.items.ZygardeCubeItem;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ItemStack;
import com.google.gson.*;

/** Native ownership, assembly, merge and battle boundaries, in a disposable world. */
public final class Zygarde190Cases {
    static int step,wait,assertions,mismatches;static Pokemon assembled;
    static final JsonArray rows=new JsonArray();
    static boolean baseline(){return Files.exists(Path.of("zygarde-baseline"));}
    static void check(boolean yes,String message){assertions++;if(!yes)throw new AssertionError(message);}
    static boolean complete(ServerPlayerEntity player,String id){return player.func_192039_O().func_192747_a(adv(player,id)).func_192105_a();}
    static net.minecraft.advancements.Advancement adv(ServerPlayerEntity p,String id){return p.func_184102_h().func_191949_aK().func_192778_a(new ResourceLocation(Content.NS,"legends/"+id));}
    static void clear(ServerPlayerEntity p){for(String id:List.of("zygarde_power_construct","zygarde_complete","zygarde_mega"))p.func_192039_O().func_192744_b(adv(p,id),"earned");}
    static Pokemon mon(){var p=BossBalance190Probe.mon(718,100,"Tackle");p.setForm("fifty_percent");p.setAbility(new PowerConstruct());return p;}
    static void expected(ServerPlayerEntity player,boolean power,String label){boolean actual=complete(player,"zygarde_power_construct");var row=new JsonObject();row.addProperty("case",label);row.addProperty("expected",power);row.addProperty("actual",actual);rows.add(row);if(actual!=power){mismatches++;if(!baseline())throw new AssertionError(label+" power="+actual);}check(!complete(player,"zygarde_complete")&&!complete(player,"zygarde_mega"),label+" does not award Complete/Mega for ownership");}
    static void acquisition(ServerPlayerEntity player){
        var party=StorageProxy.getParty(player);for(int i=0;i<6;i++)party.set(i,null);
        for(boolean ability:new boolean[]{false,true})for(String form:new String[]{"ten_percent","fifty_percent"}){
            clear(player);var p=mon();p.setForm(form);p.setAbility(ability?new PowerConstruct():new AuraBreak());party.set(0,p);AcquisitionAdvancements.species(player,p);expected(player,ability,"party/"+form+"/power="+ability);
        }
        clear(player);var pc=mon();StorageProxy.getPCForPlayer(player).getBox(0).set(new StoragePosition(0,29),pc);AcquisitionAdvancements.species(player,pc);expected(player,true,"PC acquisition");
        clear(player);var egg=mon();egg.setEggCycles(10);party.set(0,egg);AcquisitionAdvancements.species(player,egg);expected(player,false,"egg excluded");
        clear(player);var wild=mon();LegendAdvancements.capture(player,wild);expected(player,false,"unowned excluded");
        clear(player);var other=BossBalance190Probe.mon(25,100,"Tackle");other.setAbility(new PowerConstruct());party.set(0,other);AcquisitionAdvancements.species(player,other);expected(player,false,"other species excluded");
        clear(player);var real=mon();party.set(0,real);var detached=PokemonFactory.copy(real);LegendAdvancements.capture(player,detached);expected(player,false,"noncanonical copy excluded");
        clear(player);AcquisitionAdvancements.species(player,real);int earned=com.google.common.collect.Iterables.size(player.func_192039_O().func_192747_a(adv(player,"zygarde_power_construct")).func_192102_e());for(int i=0;i<8;i++)AcquisitionAdvancements.species(player,real);check(com.google.common.collect.Iterables.size(player.func_192039_O().func_192747_a(adv(player,"zygarde_power_construct")).func_192102_e())==earned,"Repeat idempotent single criterion");
    }
    static void assemble(ServerPlayerEntity player,boolean merge){
        clear(player);var party=StorageProxy.getParty(player);for(int i=0;i<6;i++)party.set(i,null);
        Pokemon original=null;if(merge){original=mon();original.setAbility(new AuraBreak());party.set(0,original);}
        var cube=new ItemStack(com.pixelmonmod.pixelmon.api.registries.PixelmonItems.zygarde_cube);ZygardeCubeItem.setCellCount(cube,merge?50:100);ZygardeCubeItem.addCore(cube,ZygardeCubeItem.CoreType.CORE_ENFORCER);ZygardeCubeItem.addCore(cube,ZygardeCubeItem.CoreType.THOUSAND_ARROWS);player.field_71071_by.func_70299_a(0,cube);
        var w=player.func_71121_q();var pos=new BlockPos(36,80,36);w.func_175726_f(pos);var block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new ResourceLocation("pixelmon:reassembly_unit"));check(block!=null,"Native reassembly block exists");w.func_180501_a(pos,block.func_176223_P().func_206870_a(com.pixelmonmod.pixelmon.blocks.GenericDoubleRotatableModelBlock.BLOCKPOS,com.pixelmonmod.pixelmon.blocks.enums.EnumBlockPos.BOTTOM),2);w.func_180501_a(pos.func_177984_a(),block.func_176223_P().func_206870_a(com.pixelmonmod.pixelmon.blocks.GenericDoubleRotatableModelBlock.BLOCKPOS,com.pixelmonmod.pixelmon.blocks.enums.EnumBlockPos.TOP),2);var te=(ReassemblyUnitTileEntity)w.func_175625_s(pos);if(te==null){te=new ReassemblyUnitTileEntity();te.func_226984_a_(w,pos);}check(te!=null,"Native reassembly tile fixture");
        te.onSelection(player,ReassemblyUnitTileEntity.Mode.ASSEMBLING,merge?new StoragePosition(-1,0):null,0);for(int i=0;i<1000&&te.getMode()!=ReassemblyUnitTileEntity.Mode.INACTIVE;i++)te.func_73660_a();
        assembled=party.get(0);check(assembled!=null&&assembled.getAbility() instanceof PowerConstruct,"Real native100-cell assembly or50-cell merge produces PowerConstruct");check(ZygardeCubeItem.getCellCount(cube)==0,"Native cells consumed");if(merge)check(assembled==original,"Native merge preserves canonical individual");
    }
    static void battle(ServerPlayerEntity player)throws Exception{
        var party=StorageProxy.getParty(player);for(int i=0;i<6;i++)party.set(i,i==0?mon():null);var own=new com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant(player,party.get(0));var npc=new com.pixelmonmod.pixelmon.entities.npcs.NPCTrainer(player.field_70170_p);npc.getPersistentData().func_74757_a("BackportNpcDisable",true);npc.getPokemonStorage().set(0,BossBalance190Probe.mon(143,100,"Tackle"));var foe=new com.pixelmonmod.pixelmon.battles.controller.participants.TrainerParticipant(npc,1);var b=new com.pixelmonmod.pixelmon.battles.controller.BattleController(new com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant[]{own},new com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant[]{foe},new com.pixelmonmod.pixelmon.battles.api.rules.BattleRules());b.sendMessages=false;var init=b.getClass().getDeclaredMethod("initBattle");init.setAccessible(true);init.invoke(b);
        try{clear(player);var p=own.controlledPokemon.get(0);p.setHealth(p.getMaxHealth());new PowerConstruct().applyRepeatedEffectAfterStatus(p);check(!complete(player,"zygarde_complete"),"FullHP does not transform or award Complete");p.setHealth(p.getMaxHealth()/3);new PowerConstruct().applyRepeatedEffectAfterStatus(p);check(p.getForm().isForm("complete")&&complete(player,"zygarde_complete")&&complete(player,"zygarde_power_construct"),"Real battle native PowerConstruct still awards Complete and ability");check(!complete(player,"zygarde_mega"),"Complete not Mega");}finally{b.battleEnded=true;npc.func_70106_y();}
    }
    public static boolean tick(ServerPlayerEntity player)throws Exception{
        if(wait-->0)return false;
        if(step==0){acquisition(player);assemble(player,false);step++;wait=2;return false;}
        if(step==1){expected(player,true,"native100-cell new assembly");assemble(player,true);step++;wait=2;return false;}
        if(step==2){expected(player,true,"native existing50+50cell merge");clear(player);new AcquisitionQueue().login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(player));step++;wait=3;return false;}
        if(step==3){expected(player,true,"existing canonical login catchup");battle(player);var result=new JsonObject();result.addProperty("assertions",assertions);result.addProperty("mismatches",mismatches);result.addProperty("baseline",baseline());result.add("cases",rows);Files.writeString(Path.of("zygarde190-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));BossBalance190Probe.log("ZYGARDE190_PASS assertions="+assertions+" mismatches="+mismatches+" baseline="+baseline());return true;}
        return true;
    }
}
